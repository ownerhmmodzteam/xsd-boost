package com.example.core.system.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class MediaSourceKind {
    DIRECT_AUDIO,
    HLS_STREAM,
    YOUTUBE,
    SPOTIFY,
    WEB_PAGE,
    ERROR
}

sealed class ResolvedMediaSource {
    data class DirectAudio(
        val originalUrl: String,
        val resolvedUrl: String,
        val mimeType: String,
        val sourceDomain: String,
        val derivedTitle: String
    ) : ResolvedMediaSource()

    data class HlsStream(
        val originalUrl: String,
        val resolvedUrl: String,
        val mimeType: String,
        val sourceDomain: String,
        val derivedTitle: String
    ) : ResolvedMediaSource()

    data class YouTubeMedia(
        val url: String,
        val videoId: String?,
        val sourceDomain: String,
        val message: String
    ) : ResolvedMediaSource()

    data class SpotifyMedia(
        val url: String,
        val sourceDomain: String,
        val message: String
    ) : ResolvedMediaSource()

    data class UnsupportedWebPage(
        val url: String,
        val resolvedUrl: String,
        val sourceDomain: String,
        val contentType: String?,
        val reason: String
    ) : ResolvedMediaSource()

    data class AuthRequired(
        val url: String,
        val statusCode: Int,
        val sourceDomain: String,
        val reason: String
    ) : ResolvedMediaSource()

    data class HttpError(
        val url: String,
        val statusCode: Int,
        val sourceDomain: String,
        val reason: String
    ) : ResolvedMediaSource()

    data class NetworkFailure(
        val url: String,
        val sourceDomain: String,
        val reason: String
    ) : ResolvedMediaSource()

    data class InvalidInput(
        val reason: String
    ) : ResolvedMediaSource()
}

class SmartMediaUrlResolver(
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    companion object {
        private val YOUTUBE_HOSTS = setOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be"
        )

        private val SPOTIFY_HOSTS = setOf(
            "open.spotify.com",
            "spotify.com",
            "play.spotify.com",
            "spotify.link"
        )

        private val HLS_MIME_TYPES = setOf(
            "application/vnd.apple.mpegurl",
            "application/x-mpegurl",
            "audio/mpegurl",
            "audio/x-mpegurl"
        )

        private val DIRECT_AUDIO_MIME_EXACT = setOf(
            "application/ogg",
            "application/x-ogg",
            "video/mp4",
            "video/webm"
        )

        private val WEB_PAGE_MIME_TYPES = setOf(
            "text/html",
            "application/xhtml+xml",
            "text/plain",
            "application/json",
            "application/xml",
            "text/xml",
            "application/pdf"
        )

        private val SUPPORTED_SCHEMES = setOf("http", "https", "content", "file", "rtsp", "icy")

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun isYouTubeHost(host: String): Boolean {
            val clean = host.lowercase()
            return clean in YOUTUBE_HOSTS || clean.endsWith(".youtube.com")
        }

        fun isSpotifyHost(host: String): Boolean {
            val clean = host.lowercase()
            return clean in SPOTIFY_HOSTS || clean.endsWith(".spotify.com")
        }

        fun extractYouTubeVideoId(uri: URI): String? {
            val host = uri.host?.lowercase() ?: ""
            val path = uri.path ?: ""
            if (host == "youtu.be") {
                return path.trim('/').substringBefore('/').takeIf { it.isNotBlank() }
            }
            val query = uri.rawQuery ?: ""
            query.split("&").forEach { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0 && pair.substring(0, idx) == "v") {
                    return pair.substring(idx + 1).takeIf { it.isNotBlank() }
                }
            }
            if (path.startsWith("/shorts/") || path.startsWith("/embed/") || path.startsWith("/live/")) {
                return path.trim('/').substringAfter('/').substringBefore('/').takeIf { it.isNotBlank() }
            }
            return null
        }

        fun deriveTitle(url: String, host: String): String {
            val parsed = try {
                URI(url)
            } catch (_: Exception) {
                null
            }
            val lastSegment = (parsed?.path ?: "")
                .substringAfterLast("/")
                .substringBefore("?")
                .trim()
            return lastSegment.takeIf { it.isNotBlank() && it != "/" } ?: host.ifBlank { "Audio Stream" }
        }
    }

    /**
     * Quick synchronous classification for saved URL previews / offline checks (no network probe).
     */
    fun quickClassify(rawUrl: String): ResolvedMediaSource {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return ResolvedMediaSource.InvalidInput("Masukkan URL audio atau media terlebih dahulu.")
        }
        if (trimmed.startsWith("spotify:", ignoreCase = true)) {
            return ResolvedMediaSource.SpotifyMedia(
                url = trimmed,
                sourceDomain = "Spotify App",
                message = "Integrasi pemutaran Spotify embedded tidak tersedia. Gunakan tombol Buka di Spotify untuk memutar resmi."
            )
        }

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return ResolvedMediaSource.InvalidInput("Format URL tidak valid.")
        }

        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme !in SUPPORTED_SCHEMES) {
            return ResolvedMediaSource.InvalidInput("Gunakan URL diawali http://, https://, atau URI yang didukung.")
        }

        val host = uri.host?.lowercase() ?: ""
        if (scheme in setOf("http", "https") && host.isBlank()) {
            return ResolvedMediaSource.InvalidInput("Domain/host URL tidak valid.")
        }

        if (isYouTubeHost(host)) {
            return ResolvedMediaSource.YouTubeMedia(
                url = trimmed,
                videoId = extractYouTubeVideoId(uri),
                sourceDomain = host,
                message = "YouTube tidak mengizinkan pemutaran background atau ekstraksi stream tersembunyi di ExoPlayer. Buka di YouTube resmi."
            )
        }

        if (isSpotifyHost(host)) {
            return ResolvedMediaSource.SpotifyMedia(
                url = trimmed,
                sourceDomain = host,
                message = "Pemutaran langsung Spotify di dalam aplikasi memerlukan Spotify App/SDK resmi. Buka tautan ini langsung di Spotify."
            )
        }

        val pathLower = (uri.path ?: "").lowercase()
        val title = deriveTitle(trimmed, host)
        return if (pathLower.endsWith(".m3u8")) {
            ResolvedMediaSource.HlsStream(
                originalUrl = trimmed,
                resolvedUrl = trimmed,
                mimeType = "application/vnd.apple.mpegurl",
                sourceDomain = host.ifBlank { scheme.uppercase() },
                derivedTitle = title
            )
        } else {
            ResolvedMediaSource.DirectAudio(
                originalUrl = trimmed,
                resolvedUrl = trimmed,
                mimeType = "audio/*",
                sourceDomain = host.ifBlank { scheme.uppercase() },
                derivedTitle = title
            )
        }
    }

    /**
     * Full Smart URL Resolver with HTTP probing on IO dispatcher:
     * - Follows redirects
     * - Preserves query parameters
     * - Inspects real Content-Type MIME instead of trusting ".mp3" extension
     * - Detects auth-protected (401/403), 404, 429, 5xx, HTML pages, timeouts, and disconnects
     */
    suspend fun resolve(rawUrl: String): ResolvedMediaSource {
        val quick = quickClassify(rawUrl)
        if (quick is ResolvedMediaSource.InvalidInput ||
            quick is ResolvedMediaSource.YouTubeMedia ||
            quick is ResolvedMediaSource.SpotifyMedia
        ) {
            return quick
        }

        val trimmed = rawUrl.trim()
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return ResolvedMediaSource.InvalidInput("Format URL tidak valid.")
        }
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme !in setOf("http", "https")) {
            return quick
        }
        val host = uri.host?.lowercase() ?: ""

        return withContext(Dispatchers.IO) {
            probeHttpUrl(trimmed, host)
        }
    }

    private fun probeHttpUrl(url: String, originalHost: String): ResolvedMediaSource {
        try {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "XSD-BOOST-MediaResolver/1.0 (Android)")
                .build()

            httpClient.newCall(headRequest).execute().use { headResp ->
                val code = headResp.code
                val resolvedUrl = headResp.request.url.toString()
                val resolvedHost = headResp.request.url.host.lowercase()
                val headerContentType = cleanMimeType(headResp.header("Content-Type"))
                val hasIcyHeader = headResp.header("icy-name") != null || headResp.header("icy-metaint") != null

                if (isYouTubeHost(resolvedHost)) {
                    return ResolvedMediaSource.YouTubeMedia(
                        url = resolvedUrl,
                        videoId = extractYouTubeVideoId(URI(resolvedUrl)),
                        sourceDomain = resolvedHost,
                        message = "Redirect menuju YouTube. Pemutaran background dibatasi layanan; buka di aplikasi YouTube resmi."
                    )
                }
                if (isSpotifyHost(resolvedHost)) {
                    return ResolvedMediaSource.SpotifyMedia(
                        url = resolvedUrl,
                        sourceDomain = resolvedHost,
                        message = "Redirect menuju Spotify. Gunakan tombol Buka Spotify untuk pemutaran resmi."
                    )
                }

                if (code == 405 || code == 501 || (headResp.isSuccessful && headerContentType == null && !hasIcyHeader)) {
                    return probeViaRangeGet(url, originalHost)
                }

                return evaluateHttpResponse(
                    originalUrl = url,
                    resolvedUrl = resolvedUrl,
                    sourceDomain = resolvedHost.ifBlank { originalHost },
                    code = code,
                    isSuccessful = headResp.isSuccessful,
                    contentType = headerContentType,
                    icyName = headResp.header("icy-name"),
                    hasIcyHeader = hasIcyHeader
                )
            }
        } catch (e: SocketTimeoutException) {
            return ResolvedMediaSource.NetworkFailure(
                url = url,
                sourceDomain = originalHost,
                reason = "Koneksi timeout saat memeriksa URL media (${e.localizedMessage ?: "Timeout"})."
            )
        } catch (e: UnknownHostException) {
            return ResolvedMediaSource.NetworkFailure(
                url = url,
                sourceDomain = originalHost,
                reason = "Domain tidak dapat dijangkau atau offline ($originalHost)."
            )
        } catch (e: IOException) {
            return ResolvedMediaSource.NetworkFailure(
                url = url,
                sourceDomain = originalHost,
                reason = "Koneksi terputus saat memeriksa URL: ${e.localizedMessage ?: "Network I/O error"}"
            )
        } catch (e: Exception) {
            return ResolvedMediaSource.NetworkFailure(
                url = url,
                sourceDomain = originalHost,
                reason = "Gagal memvalidasi URL: ${e.localizedMessage ?: "Error"}"
            )
        }
    }

    private fun probeViaRangeGet(url: String, originalHost: String): ResolvedMediaSource {
        val getRequest = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-511")
            .header("User-Agent", "XSD-BOOST-MediaResolver/1.0 (Android)")
            .build()

        httpClient.newCall(getRequest).execute().use { resp ->
            val resolvedUrl = resp.request.url.toString()
            val resolvedHost = resp.request.url.host.lowercase()
            val contentType = cleanMimeType(resp.header("Content-Type"))
            val hasIcyHeader = resp.header("icy-name") != null || resp.header("icy-metaint") != null

            return evaluateHttpResponse(
                originalUrl = url,
                resolvedUrl = resolvedUrl,
                sourceDomain = resolvedHost.ifBlank { originalHost },
                code = resp.code,
                isSuccessful = resp.isSuccessful || resp.code == 206,
                contentType = contentType,
                icyName = resp.header("icy-name"),
                hasIcyHeader = hasIcyHeader
            )
        }
    }

    private fun evaluateHttpResponse(
        originalUrl: String,
        resolvedUrl: String,
        sourceDomain: String,
        code: Int,
        isSuccessful: Boolean,
        contentType: String?,
        icyName: String?,
        hasIcyHeader: Boolean
    ): ResolvedMediaSource {
        if (code == 401 || code == 403) {
            return ResolvedMediaSource.AuthRequired(
                url = resolvedUrl,
                statusCode = code,
                sourceDomain = sourceDomain,
                reason = "URL dilindungi autentikasi atau akses ditolak (HTTP $code). Token/login diperlukan oleh server sumber."
            )
        }
        if (code == 404) {
            return ResolvedMediaSource.HttpError(
                url = resolvedUrl,
                statusCode = code,
                sourceDomain = sourceDomain,
                reason = "Media tidak ditemukan pada server (HTTP 404)."
            )
        }
        if (code == 429) {
            return ResolvedMediaSource.HttpError(
                url = resolvedUrl,
                statusCode = code,
                sourceDomain = sourceDomain,
                reason = "Server membatasi permintaan terlalu sering (HTTP 429 Rate Limited). Coba beberapa saat lagi."
            )
        }
        if (code in 500..599) {
            return ResolvedMediaSource.HttpError(
                url = resolvedUrl,
                statusCode = code,
                sourceDomain = sourceDomain,
                reason = "Server sumber sedang gangguan (HTTP $code)."
            )
        }
        if (!isSuccessful) {
            return ResolvedMediaSource.HttpError(
                url = resolvedUrl,
                statusCode = code,
                sourceDomain = sourceDomain,
                reason = "Server mengembalikan status HTTP $code."
            )
        }

        val mime = contentType?.lowercase()
        val pathLower = try {
            URI(resolvedUrl).path?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }

        // Explicitly reject HTML/web documents even if URL path ends with .mp3 or .m3u8
        if (mime != null && (mime in WEB_PAGE_MIME_TYPES || mime.startsWith("text/") || mime.startsWith("image/"))) {
            return ResolvedMediaSource.UnsupportedWebPage(
                url = originalUrl,
                resolvedUrl = resolvedUrl,
                sourceDomain = sourceDomain,
                contentType = mime,
                reason = "URL mengembalikan halaman web/dokumen ($mime), bukan file stream audio langsung."
            )
        }

        val title = icyName?.takeIf { it.isNotBlank() } ?: deriveTitle(resolvedUrl, sourceDomain)

        if (mime in HLS_MIME_TYPES ||
            ((mime == null || mime == "application/octet-stream") && pathLower.endsWith(".m3u8"))
        ) {
            return ResolvedMediaSource.HlsStream(
                originalUrl = originalUrl,
                resolvedUrl = resolvedUrl,
                mimeType = mime ?: "application/vnd.apple.mpegurl",
                sourceDomain = sourceDomain,
                derivedTitle = title
            )
        }

        if (hasIcyHeader ||
            (mime != null && (mime.startsWith("audio/") || mime in DIRECT_AUDIO_MIME_EXACT)) ||
            (mime == "application/octet-stream" && isLikelyAudioExtension(pathLower)) ||
            (mime == null && isLikelyAudioExtension(pathLower))
        ) {
            return ResolvedMediaSource.DirectAudio(
                originalUrl = originalUrl,
                resolvedUrl = resolvedUrl,
                mimeType = mime ?: "audio/*",
                sourceDomain = sourceDomain,
                derivedTitle = title
            )
        }

        return ResolvedMediaSource.UnsupportedWebPage(
            url = originalUrl,
            resolvedUrl = resolvedUrl,
            sourceDomain = sourceDomain,
            contentType = mime,
            reason = "Tipe konten tidak dikenali sebagai audio stream (${mime ?: "unknown"}). Gunakan tautan audio langsung (.mp3/.m4a/.aac/.ogg/.flac/.wav) atau stream HLS (.m3u8)."
        )
    }

    private fun cleanMimeType(rawHeader: String?): String? {
        return rawHeader?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private fun isLikelyAudioExtension(pathLower: String): Boolean {
        return pathLower.endsWith(".mp3") ||
            pathLower.endsWith(".m4a") ||
            pathLower.endsWith(".aac") ||
            pathLower.endsWith(".ogg") ||
            pathLower.endsWith(".opus") ||
            pathLower.endsWith(".flac") ||
            pathLower.endsWith(".wav")
    }
}
