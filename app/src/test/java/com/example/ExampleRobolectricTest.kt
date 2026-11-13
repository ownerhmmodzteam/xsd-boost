package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.GameProfileRepository
import com.example.core.system.games.ForegroundGameMonitor
import com.example.core.system.games.GameDetectionForegroundService
import com.example.core.system.media.MusicPlayerEngine
import com.example.core.system.media.SmartMediaUrlResolver
import com.example.core.system.overlay.EdgeSide
import com.example.core.system.overlay.FloatingEdgeHandle
import com.example.core.system.overlay.FloatingOverlayService
import com.example.core.system.overlay.RightEdgeFloatingHandle
import com.example.core.system.sensitivity.InputCompatibilityLayer
import com.example.core.system.shizuku.ShizukuClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("XSD BOOST", appName)
    }

    @Test
    fun `shizukuClient handles disconnected state safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = ShizukuClient(context)
        val state = client.refreshState()
        assertFalse(state.isBinderAlive)
        assertFalse(client.requestPermission())
        val metrics = client.readSystemMetrics()
        assertNull(metrics.cpuUsagePercent)
        assertNull(metrics.temperatureCelsius)
        client.dispose()
    }

    @Test
    fun `inputCompatibilityLayer inspects android APIs honestly without claiming game injection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = InputCompatibilityLayer.inspect(context, null)
        assertTrue(report.officialGameApiStatus.contains("Tidak Tersedia"))
        assertTrue(report.honestExplanation.contains("tidak disuntikkan"))
    }

    @Test
    fun `gameProfileRepository isolates per-game overlay side sensitivity and presets cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = GameProfileRepository(db.gameProfileDao(), db.sensitivityPresetDao())

            // Free Fire uses Right Edge
            repo.setBoosterEnabled("com.dts.freefireth", "Free Fire", true)
            repo.updateProfile("com.dts.freefireth", "Free Fire") {
                it.copy(
                    sensitivityX = 20,
                    sensitivityY = 24,
                    overlaySide = "RIGHT",
                    handleSizeDp = 90f,
                    handleOpacity = 0.75f,
                    longPressDurationMs = 500L,
                    musicUrl = "https://cdn.example.com/ff.mp3",
                    dynamicIslandEnabled = false,
                    isHandleHidden = true
                )
            }

            // Free Fire MAX uses Left Edge
            repo.setBoosterEnabled("com.dts.freefiremax", "Free Fire MAX", true)
            repo.updateProfile("com.dts.freefiremax", "Free Fire MAX") {
                it.copy(
                    sensitivityX = 12,
                    sensitivityY = 14,
                    overlaySide = "LEFT",
                    handleSizeDp = 64f
                )
            }

            // Save & load named sensitivity preset
            val presetId = repo.saveSensitivityPreset("Headshot Calibration", 18, 22, "com.dts.freefireth")
            assertNotNull(presetId)
            val savedPresets = repo.sensitivityPresets!!.first()
            assertEquals(1, savedPresets.size)
            assertEquals(18, savedPresets[0].sensitivityX)
            assertEquals(22, savedPresets[0].sensitivityY)

            val ff = db.gameProfileDao().getProfile("com.dts.freefireth")!!
            val ffMax = db.gameProfileDao().getProfile("com.dts.freefiremax")!!
            assertEquals("RIGHT", ff.overlaySide)
            assertEquals(90f, ff.handleSizeDp)
            assertEquals(0.75f, ff.handleOpacity)
            assertEquals(500L, ff.longPressDurationMs)
            assertEquals(20, ff.sensitivityX)
            assertFalse(ff.dynamicIslandEnabled)
            assertTrue(ff.isHandleHidden)

            assertEquals("LEFT", ffMax.overlaySide)
            assertEquals(64f, ffMax.handleSizeDp)
            assertEquals(12, ffMax.sensitivityX)
            assertTrue(ffMax.dynamicIslandEnabled)

            val resetFf = repo.resetProfile("com.dts.freefireth")!!
            assertEquals(1, resetFf.sensitivityX)
            assertEquals("RIGHT", resetFf.overlaySide)
            assertEquals("", resetFf.musicUrl)
            assertTrue(resetFf.boosterEnabled)
            assertEquals("LEFT", db.gameProfileDao().getProfile("com.dts.freefiremax")!!.overlaySide)
        } finally {
            db.close()
        }
    }

    @Test
    fun `foregroundGameMonitor degrades safely without usage permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        Shadows.shadowOf(appOps).setMode(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
            android.app.AppOpsManager.MODE_IGNORED
        )
        val monitor = ForegroundGameMonitor(context)
        val state = monitor.pollForegroundPackage()
        assertFalse(state.hasUsagePermission)
        assertNull(state.foregroundPackage)
    }

    @Test
    fun `gameDetectionForegroundService evaluates registered game and triggers floating handle`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<XsdBoostApplication>()
        val appOps = app.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        Shadows.shadowOf(appOps).setMode(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            app.packageName,
            android.app.AppOpsManager.MODE_ALLOWED
        )

        app.container.gameProfileRepo.setBoosterEnabled("com.dts.freefireth", "Free Fire", true)
        app.container.foregroundMonitor.reset()

        val usm = app.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val shadowUsm = Shadows.shadowOf(usm)
        val now = System.currentTimeMillis()
        val event = android.app.usage.UsageEvents.Event()
        val pkgField = android.app.usage.UsageEvents.Event::class.java.getField("mPackage")
        val typeField = android.app.usage.UsageEvents.Event::class.java.getField("mEventType")
        val timeField = android.app.usage.UsageEvents.Event::class.java.getField("mTimeStamp")
        pkgField.set(event, "com.dts.freefireth")
        typeField.setInt(event, android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED)
        timeField.setLong(event, now - 500L)
        shadowUsm.addEvent(event)

        val controller = Robolectric.buildService(GameDetectionForegroundService::class.java).create()
        val service = controller.get()

        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        service.evaluateOnce()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals("com.dts.freefireth", app.container.detectedForegroundGame.value)
        val nextServiceIntent = Shadows.shadowOf(app).nextStartedService
        assertNotNull(nextServiceIntent)
        assertEquals(FloatingOverlayService.ACTION_SHOW_FOR_GAME, nextServiceIntent.action)
        assertEquals(
            "com.dts.freefireth",
            nextServiceIntent.getStringExtra(FloatingOverlayService.EXTRA_GAME_PACKAGE)
        )

        controller.destroy()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNull(app.container.detectedForegroundGame.value)
    }

    @Test
    fun `rightEdgeFloatingHandle supports long press drag and ignores single tap`() {
        var longPressCount = 0
        var totalDragY = 0f
        var dragEndCount = 0

        composeTestRule.setContent {
            Box(modifier = Modifier.size(width = 36.dp, height = 160.dp)) {
                RightEdgeFloatingHandle(
                    isHiddenMode = false,
                    onLongPress = { longPressCount++ },
                    onDragDeltaY = { totalDragY += it },
                    onDragEnd = { dragEndCount++ }
                )
            }
        }

        val handleNode = composeTestRule.onNodeWithTag("right_edge_floating_handle")

        handleNode.performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertEquals(0, longPressCount)
        assertEquals(0, dragEndCount)

        handleNode.performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        assertTrue("Expected positive vertical drag delta", totalDragY > 0f)
        assertEquals(1, dragEndCount)
        assertEquals(0, longPressCount)

        handleNode.performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        assertEquals(1, longPressCount)
        assertEquals(1, dragEndCount)
    }

    @Test
    fun `leftEdgeFloatingHandle supports long press drag and ignores single tap`() {
        var longPressCount = 0
        var totalDragY = 0f
        var dragEndCount = 0

        composeTestRule.setContent {
            Box(modifier = Modifier.size(width = 36.dp, height = 160.dp)) {
                FloatingEdgeHandle(
                    side = EdgeSide.LEFT,
                    handleSizeDp = 84f,
                    handleOpacity = 0.8f,
                    longPressDurationMs = 350L,
                    isHiddenMode = false,
                    onLongPress = { longPressCount++ },
                    onDragDeltaY = { totalDragY += it },
                    onDragEnd = { dragEndCount++ }
                )
            }
        }

        val handleNode = composeTestRule.onNodeWithTag("left_edge_floating_handle")

        handleNode.performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertEquals(0, longPressCount)

        handleNode.performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        assertTrue(totalDragY > 0f)
        assertEquals(1, dragEndCount)
        assertEquals(0, longPressCount)

        handleNode.performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        assertEquals(1, longPressCount)
    }

    @Test
    fun `musicPlayerEngine handles volume repeat and youtube spotify handoffs cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "text/html")
                    .body("<html></html>".toResponseBody("text/html".toMediaType()))
                    .build()
            })
            .build()
        val engine = MusicPlayerEngine(context, null, SmartMediaUrlResolver(fakeClient))

        engine.setVolume(0.45f)
        assertEquals(0.45f, engine.state.value.volume, 0.01f)

        engine.toggleRepeat()
        assertTrue(engine.state.value.isLooping)

        engine.syncGameContext("com.dts.freefireth", "https://youtu.be/dQw4w9WgXcQ", "YT Track")
        assertTrue(engine.state.value.isYouTubeUrl)
        assertFalse(engine.state.value.isPlaying)

        engine.syncGameContext("com.dts.freefiremax", "https://open.spotify.com/track/xyz", "Spotify Track")
        assertTrue(engine.state.value.isSpotifyUrl)
        assertFalse(engine.state.value.isPlaying)

        engine.stopPlayback()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals("Stopped", engine.state.value.playbackStatus)
        engine.release()
    }
}
