package com.example

import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.crosshair.CrosshairStyle
import com.example.core.system.games.GameDetectionForegroundService
import com.example.core.system.hardware.SystemStats
import com.example.core.system.media.MediaUrlValidationResult
import com.example.core.system.media.MediaUrlValidator
import com.example.core.system.media.ResolvedMediaSource
import com.example.core.system.media.SmartMediaUrlResolver
import com.example.core.system.overlay.EdgeSide
import com.example.core.system.prefs.EdgeSwipeConfig
import com.example.core.system.sensitivity.InputCompatibilityLayer
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class ExampleUnitTest {

    private fun mockHttpResolver(handler: (okhttp3.Request) -> Response): SmartMediaUrlResolver {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain -> handler(chain.request()) })
            .build()
        return SmartMediaUrlResolver(client)
    }

    private fun buildResponse(
        request: okhttp3.Request,
        code: Int,
        contentType: String?,
        finalUrl: String? = null
    ): Response {
        val effectiveRequest = if (finalUrl != null) {
            request.newBuilder().url(finalUrl).build()
        } else {
            request
        }
        val body = "".toResponseBody((contentType ?: "application/octet-stream").toMediaType())
        val builder = Response.Builder()
            .request(effectiveRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body)
        if (contentType != null) {
            builder.header("Content-Type", contentType)
        }
        return builder.build()
    }

    @Test
    fun crosshairConfig_defaultValues_areValid() {
        val config = CrosshairConfig()
        assertEquals(CrosshairStyle.CLASSIC_CROSS, config.style)
        assertEquals(28f, config.sizeDp)
        assertEquals(2.5f, config.thicknessDp)
        assertTrue(config.hasCenterDot)
        assertTrue(config.hasOutline)
    }

    @Test
    fun sensitivityBounds_andSimulationMultiplier_scaleAccurately() {
        assertEquals(1, 0.coerceIn(1, 26))
        assertEquals(26, 30.coerceIn(1, 26))

        val (lowDx, lowDy) = InputCompatibilityLayer.computeSimulatedReticleDelta(10f, 10f, 1, 1)
        val (highDx, highDy) = InputCompatibilityLayer.computeSimulatedReticleDelta(10f, 10f, 26, 20)
        assertTrue("High sensitivity X must displace more than low X", highDx > lowDx)
        assertTrue("High sensitivity Y must displace more than low Y", highDy > lowDy)
    }

    @Test
    fun systemStats_reportsCpuUnavailableWhenRawIsNull() {
        val restrictedStats = SystemStats(rawCpuUsagePercent = null)
        assertFalse(restrictedStats.isCpuMeasurementAvailable)

        val realStats = SystemStats(rawCpuUsagePercent = 42.5f, cpuUsagePercent = 42.5f)
        assertTrue(realStats.isCpuMeasurementAvailable)
    }

    @Test
    fun edgeSwipeConfig_defaults_areWithinSafeBounds() {
        val cfg = EdgeSwipeConfig()
        assertTrue(cfg.enabled)
        assertTrue(cfg.sensitivity in 0.2f..1.0f)
        assertTrue(cfg.triggerYFraction in 0.1f..0.85f)
        assertTrue(cfg.triggerWidthDp in 16f..36f)
        assertTrue(cfg.triggerHeightDp in 60f..220f)
    }

    @Test
    fun edgeSide_parsesLeftAndRightSafely() {
        assertEquals(EdgeSide.LEFT, EdgeSide.fromString("LEFT"))
        assertEquals(EdgeSide.LEFT, EdgeSide.fromString("left"))
        assertEquals(EdgeSide.RIGHT, EdgeSide.fromString("RIGHT"))
        assertEquals(EdgeSide.RIGHT, EdgeSide.fromString(null))
        assertEquals(EdgeSide.RIGHT, EdgeSide.fromString("unknown"))
    }

    @Test
    fun mediaUrlValidator_classifiesDirectYouTubeSpotifyAndInvalidUrls() {
        val direct = MediaUrlValidator.validate("https://cdn.example.com/audio/theme_track.mp3")
        assertTrue(direct is MediaUrlValidationResult.DirectStream)
        assertEquals("theme_track.mp3", (direct as MediaUrlValidationResult.DirectStream).derivedTitle)

        val yt = MediaUrlValidator.validate("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(yt is MediaUrlValidationResult.YouTubeLink)

        val spotify = MediaUrlValidator.validate("https://open.spotify.com/track/123456")
        assertTrue(spotify is MediaUrlValidationResult.SpotifyLink)

        val invalidScheme = MediaUrlValidator.validate("ftp://example.com/song.mp3")
        assertTrue(invalidScheme is MediaUrlValidationResult.Invalid)

        val empty = MediaUrlValidator.validate("   ")
        assertTrue(empty is MediaUrlValidationResult.Invalid)
    }

    @Test
    fun smartResolver_resolvesDirectAudioFormats_mp3M4aAacOggFlacWav() = runBlocking {
        val mimes = listOf(
            "https://cdn.example.com/a.mp3?token=1" to "audio/mpeg",
            "https://cdn.example.com/b.m4a" to "audio/mp4",
            "https://cdn.example.com/c.aac" to "audio/aac",
            "https://cdn.example.com/d.ogg" to "audio/ogg",
            "https://cdn.example.com/e.flac" to "audio/flac",
            "https://cdn.example.com/f.wav" to "audio/wav"
        )
        mimes.forEach { (url, mime) ->
            val resolver = mockHttpResolver { req -> buildResponse(req, 200, mime) }
            val res = resolver.resolve(url)
            assertTrue("Expected DirectAudio for $mime", res is ResolvedMediaSource.DirectAudio)
            assertEquals(mime, (res as ResolvedMediaSource.DirectAudio).mimeType)
        }
    }

    @Test
    fun smartResolver_resolvesHlsM3u8Stream() = runBlocking {
        val resolver = mockHttpResolver { req ->
            buildResponse(req, 200, "application/vnd.apple.mpegurl")
        }
        val res = resolver.resolve("https://live.example.com/radio/master.m3u8?quality=high")
        assertTrue(res is ResolvedMediaSource.HlsStream)
        assertEquals("live.example.com", (res as ResolvedMediaSource.HlsStream).sourceDomain)
    }

    @Test
    fun smartResolver_rejectsHtmlPageEvenWithMp3Extension() = runBlocking {
        val resolver = mockHttpResolver { req ->
            buildResponse(req, 200, "text/html; charset=utf-8")
        }
        val res = resolver.resolve("https://fake-download.example.com/track.mp3")
        assertTrue("HTML page disguised as .mp3 must NOT be treated as audio", res is ResolvedMediaSource.UnsupportedWebPage)
    }

    @Test
    fun smartResolver_handlesRedirectsAndQueryParameters() = runBlocking {
        val resolver = mockHttpResolver { req ->
            buildResponse(
                request = req,
                code = 200,
                contentType = "audio/mp4",
                finalUrl = "https://edge-cdn.example.com/stream.m4a?sig=abc"
            )
        }
        val res = resolver.resolve("https://short.example.com/play?id=42")
        assertTrue(res is ResolvedMediaSource.DirectAudio)
        val audio = res as ResolvedMediaSource.DirectAudio
        assertEquals("https://edge-cdn.example.com/stream.m4a?sig=abc", audio.resolvedUrl)
        assertEquals("edge-cdn.example.com", audio.sourceDomain)
    }

    @Test
    fun smartResolver_handlesAuth403_404_429_5xx_andTimeout() = runBlocking {
        val res403 = mockHttpResolver { req -> buildResponse(req, 403, "text/plain") }
            .resolve("https://private.example.com/song.mp3")
        assertTrue(res403 is ResolvedMediaSource.AuthRequired)
        assertEquals(403, (res403 as ResolvedMediaSource.AuthRequired).statusCode)

        val res404 = mockHttpResolver { req -> buildResponse(req, 404, "text/html") }
            .resolve("https://cdn.example.com/missing.mp3")
        assertTrue(res404 is ResolvedMediaSource.HttpError)
        assertEquals(404, (res404 as ResolvedMediaSource.HttpError).statusCode)

        val res429 = mockHttpResolver { req -> buildResponse(req, 429, "application/json") }
            .resolve("https://cdn.example.com/ratelimit.mp3")
        assertTrue(res429 is ResolvedMediaSource.HttpError)
        assertEquals(429, (res429 as ResolvedMediaSource.HttpError).statusCode)

        val res503 = mockHttpResolver { req -> buildResponse(req, 503, "text/plain") }
            .resolve("https://cdn.example.com/down.mp3")
        assertTrue(res503 is ResolvedMediaSource.HttpError)
        assertEquals(503, (res503 as ResolvedMediaSource.HttpError).statusCode)

        val timeoutResolver = mockHttpResolver { throw SocketTimeoutException("Simulated timeout") }
        val resTimeout = timeoutResolver.resolve("https://slow.example.com/song.mp3")
        assertTrue(resTimeout is ResolvedMediaSource.NetworkFailure)
    }

    @Test
    fun smartResolver_identifiesYouTubeAndSpotifyWithoutNetworkProbe() = runBlocking {
        val ytResolver = mockHttpResolver { throw AssertionError("Should not probe YouTube") }
        val ytRes = ytResolver.resolve("https://www.youtube.com/watch?v=abc123XYZ")
        assertTrue(ytRes is ResolvedMediaSource.YouTubeMedia)
        assertEquals("abc123XYZ", (ytRes as ResolvedMediaSource.YouTubeMedia).videoId)

        val spResolver = mockHttpResolver { throw AssertionError("Should not probe Spotify") }
        val spRes = spResolver.resolve("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC")
        assertTrue(spRes is ResolvedMediaSource.SpotifyMedia)
    }

    @Test
    fun gameDetection_evaluateTargetPackage_matchesOnlyRegisteredInteractiveGames() {
        val registered = setOf("com.dts.freefireth", "com.mobile.legends")
        val selfPkg = "com.example"

        assertEquals(
            "com.dts.freefireth",
            GameDetectionForegroundService.evaluateTargetPackage(
                foregroundPackage = "com.dts.freefireth",
                registeredPackages = registered,
                selfPackageName = selfPkg,
                isScreenInteractive = true
            )
        )

        assertEquals(
            null,
            GameDetectionForegroundService.evaluateTargetPackage(
                foregroundPackage = "com.android.chrome",
                registeredPackages = registered,
                selfPackageName = selfPkg,
                isScreenInteractive = true
            )
        )

        assertEquals(
            null,
            GameDetectionForegroundService.evaluateTargetPackage(
                foregroundPackage = selfPkg,
                registeredPackages = registered,
                selfPackageName = selfPkg,
                isScreenInteractive = true
            )
        )

        assertEquals(
            null,
            GameDetectionForegroundService.evaluateTargetPackage(
                foregroundPackage = "com.dts.freefireth",
                registeredPackages = registered,
                selfPackageName = selfPkg,
                isScreenInteractive = false
            )
        )
    }
}
