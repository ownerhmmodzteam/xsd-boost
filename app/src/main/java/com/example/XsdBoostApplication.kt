package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.example.core.database.AppDatabase
import com.example.core.database.GameProfileRepository
import com.example.core.system.booster.RamBoosterEngine
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.games.ForegroundGameMonitor
import com.example.core.system.graphics.GraphicsEngine
import com.example.core.system.hardware.HardwareMonitor
import com.example.core.system.media.MusicPlayerEngine
import com.example.core.system.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

data class ScreenshotFeedback(
    val message: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class XsdSystemContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "xsd_boost_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    val gameProfileRepo: GameProfileRepository by lazy {
        GameProfileRepository(database.gameProfileDao(), database.sensitivityPresetDao())
    }
    val foregroundMonitor: ForegroundGameMonitor by lazy {
        ForegroundGameMonitor(appContext)
    }

    val shizukuClient: com.example.core.system.shizuku.ShizukuClient by lazy {
        com.example.core.system.shizuku.ShizukuClient(appContext)
    }
    val shizukuBridge: com.example.core.system.shizuku.ShizukuSystemBridge by lazy {
        com.example.core.system.shizuku.ShizukuSystemBridge(appContext, shizukuClient)
    }
    val prefsRepo: UserPreferencesRepository by lazy { UserPreferencesRepository(appContext) }
    val hardwareMonitor: HardwareMonitor by lazy { HardwareMonitor(appContext, shizukuClient) }
    val ramBooster: RamBoosterEngine by lazy { RamBoosterEngine(appContext) }
    val graphicsEngine: GraphicsEngine by lazy { GraphicsEngine(appContext) }
    val musicPlayer: MusicPlayerEngine by lazy {
        MusicPlayerEngine(appContext, database.musicHistoryDao())
    }

    val detectedForegroundGame = MutableStateFlow<String?>(null)
    val activeGameCrosshairConfig = MutableStateFlow<CrosshairConfig?>(null)
    val screenshotFeedback = MutableStateFlow<ScreenshotFeedback?>(null)
}

class XsdBoostApplication : Application() {

    val container: XsdSystemContainer by lazy { XsdSystemContainer(this) }

    companion object {
        const val OVERLAY_CHANNEL_ID = "xsd_boost_overlay_channel"
        const val MONITOR_CHANNEL_ID = "xsd_boost_monitor_channel"
        const val SCREENSHOT_CHANNEL_ID = "xsd_boost_screenshot_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val overlayChannel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "XSD BOOST Floating HUD",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for XSD BOOST in-game overlay"
                setShowBadge(false)
            }

            val monitorChannel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "XSD BOOST System Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System performance tracking notification"
                setShowBadge(false)
            }

            val screenshotChannel = NotificationChannel(
                SCREENSHOT_CHANNEL_ID,
                "XSD BOOST Screenshot Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Temporary notification during MediaProjection screen capture"
                setShowBadge(false)
            }

            notificationManager?.createNotificationChannel(overlayChannel)
            notificationManager?.createNotificationChannel(monitorChannel)
            notificationManager?.createNotificationChannel(screenshotChannel)
        }
    }
}
