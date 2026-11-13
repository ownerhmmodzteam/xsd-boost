package com.example.core.system.media

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.core.database.MusicHistoryDao
import com.example.core.database.MusicHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MediaUrlValidationResult {
    data class DirectStream(val url: String, val derivedTitle: String) : MediaUrlValidationResult()
    data class YouTubeLink(val url: String) : MediaUrlValidationResult()
    data class SpotifyLink(val url: String) : MediaUrlValidationResult()
    data class Invalid(val reason: String) : MediaUrlValidationResult()
}

object MediaUrlValidator {
    private val quickResolver = SmartMediaUrlResolver()

    fun validate(rawUrl: String): MediaUrlValidationResult {
        return when (val quick = quickResolver.quickClassify(rawUrl)) {
            is ResolvedMediaSource.DirectAudio -> MediaUrlValidationResult.DirectStream(quick.resolvedUrl, quick.derivedTitle)
            is ResolvedMediaSource.HlsStream -> MediaUrlValidationResult.DirectStream(quick.resolvedUrl, quick.derivedTitle)
            is ResolvedMediaSource.YouTubeMedia -> MediaUrlValidationResult.YouTubeLink(quick.url)
            is ResolvedMediaSource.SpotifyMedia -> MediaUrlValidationResult.SpotifyLink(quick.url)
            is ResolvedMediaSource.InvalidInput -> MediaUrlValidationResult.Invalid(quick.reason)
            else -> MediaUrlValidationResult.Invalid("URL media tidak didukung.")
        }
    }
}

data class MusicPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasLoadedMedia: Boolean = false,
    val playbackStatus: String = "Idle",
    val currentTitle: String = "No Media Loaded",
    val sourceName: String = "XSD Audio",
    val sourceKind: MediaSourceKind = MediaSourceKind.DIRECT_AUDIO,
    val artworkUri: String? = null,
    val artworkBytes: ByteArray? = null,
    val currentUrl: String = "",
    val activeGamePackage: String = "",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.85f,
    val isLooping: Boolean = false,
    val hasHistoryQueue: Boolean = false,
    val externalMediaActive: Boolean = false,
    val isYouTubeUrl: Boolean = false,
    val youtubeUrlToOpen: String? = null,
    val isSpotifyUrl: Boolean = false,
    val spotifyUrlToOpen: String? = null,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MusicPlayerState) return false
        return isPlaying == other.isPlaying &&
            isBuffering == other.isBuffering &&
            hasLoadedMedia == other.hasLoadedMedia &&
            playbackStatus == other.playbackStatus &&
            currentTitle == other.currentTitle &&
            sourceName == other.sourceName &&
            sourceKind == other.sourceKind &&
            artworkUri == other.artworkUri &&
            artworkBytes.contentEqualsNullable(other.artworkBytes) &&
            currentUrl == other.currentUrl &&
            activeGamePackage == other.activeGamePackage &&
            currentPositionMs == other.currentPositionMs &&
            durationMs == other.durationMs &&
            volume == other.volume &&
            isLooping == other.isLooping &&
            hasHistoryQueue == other.hasHistoryQueue &&
            externalMediaActive == other.externalMediaActive &&
            isYouTubeUrl == other.isYouTubeUrl &&
            youtubeUrlToOpen == other.youtubeUrlToOpen &&
            isSpotifyUrl == other.isSpotifyUrl &&
            spotifyUrlToOpen == other.spotifyUrlToOpen &&
            errorMessage == other.errorMessage
    }

    override fun hashCode(): Int {
        var result = isPlaying.hashCode()
        result = 31 * result + isBuffering.hashCode()
        result = 31 * result + hasLoadedMedia.hashCode()
        result = 31 * result + playbackStatus.hashCode()
        result = 31 * result + currentTitle.hashCode()
        result = 31 * result + sourceName.hashCode()
        result = 31 * result + sourceKind.hashCode()
        result = 31 * result + (artworkUri?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        result = 31 * result + currentUrl.hashCode()
        result = 31 * result + activeGamePackage.hashCode()
        result = 31 * result + currentPositionMs.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + isLooping.hashCode()
        result = 31 * result + hasHistoryQueue.hashCode()
        result = 31 * result + externalMediaActive.hashCode()
        result = 31 * result + isYouTubeUrl.hashCode()
        result = 31 * result + (youtubeUrlToOpen?.hashCode() ?: 0)
        result = 31 * result + isSpotifyUrl.hashCode()
        result = 31 * result + (spotifyUrlToOpen?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    return this.contentEquals(other)
}

class MusicPlayerEngine(
    private val context: Context,
    private val musicHistoryDao: MusicHistoryDao? = null,
    private val urlResolver: SmartMediaUrlResolver = SmartMediaUrlResolver()
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // Single lifecycle-managed ExoPlayer + MediaSession instance
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var resolveJob: Job? = null

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private val _historyList = MutableStateFlow<List<MusicHistoryEntity>>(emptyList())
    val historyList: StateFlow<List<MusicHistoryEntity>> = _historyList.asStateFlow()

    private var currentQueueIndex: Int = -1
    private var lastLoadedGamePackage: String = ""
    private var pendingHistorySave: Triple<String, String, String>? = null // url, title, gamePkg

    init {
        if (musicHistoryDao != null) {
            ioScope.launch {
                musicHistoryDao.getAllHistory().collect { items ->
                    _historyList.value = items
                    _state.value = _state.value.copy(hasHistoryQueue = items.isNotEmpty())
                }
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.repeatMode = if (_state.value.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.volume = _state.value.volume.coerceIn(0f, 1f)
        player.addListener(playerListener)

        val session = try {
            MediaSession.Builder(context.applicationContext, player)
                .setId("xsd_boost_media_session")
                .build()
        } catch (_: Exception) {
            null
        }

        exoPlayer = player
        mediaSession = session
        return player
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = exoPlayer ?: return
            val buffering = player.playbackState == Player.STATE_BUFFERING
            val statusText = when {
                isPlaying -> "Playing"
                buffering -> "Buffering..."
                player.playbackState == Player.STATE_ENDED -> "Stopped"
                _state.value.hasLoadedMedia -> "Paused"
                else -> "Idle"
            }
            _state.value = _state.value.copy(
                isPlaying = isPlaying,
                isBuffering = buffering,
                playbackStatus = statusText,
                durationMs = player.duration.takeIf { it > 0 } ?: _state.value.durationMs,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.value = _state.value.copy(
                        isBuffering = true,
                        playbackStatus = "Buffering...",
                        errorMessage = null
                    )
                }
                Player.STATE_READY -> {
                    val duration = player.duration.takeIf { it > 0 } ?: 0L
                    _state.value = _state.value.copy(
                        hasLoadedMedia = true,
                        isBuffering = false,
                        durationMs = duration,
                        isPlaying = player.isPlaying,
                        playbackStatus = if (player.isPlaying) "Playing" else "Ready • Loaded",
                        errorMessage = null
                    )
                    pendingHistorySave?.let { (url, title, gamePkg) ->
                        pendingHistorySave = null
                        persistHistory(url, title, gamePkg, duration / 1000L)
                    }
                }
                Player.STATE_ENDED -> {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = 0L,
                        playbackStatus = "Stopped"
                    )
                }
                Player.STATE_IDLE -> {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        playbackStatus = when {
                            _state.value.isYouTubeUrl -> "YouTube App Handoff"
                            _state.value.isSpotifyUrl -> "Spotify App Handoff"
                            else -> _state.value.playbackStatus
                        }
                    )
                }
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val metaTitle = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
            val metaArtist = mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() }
            val metaArtworkUri = mediaMetadata.artworkUri?.toString()?.takeIf { it.isNotBlank() }
            val metaArtworkData = mediaMetadata.artworkData

            _state.value = _state.value.copy(
                currentTitle = metaTitle ?: _state.value.currentTitle,
                sourceName = metaArtist ?: _state.value.sourceName,
                artworkUri = metaArtworkUri ?: _state.value.artworkUri,
                artworkBytes = metaArtworkData ?: _state.value.artworkBytes
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            pendingHistorySave = null
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                hasLoadedMedia = false,
                playbackStatus = "Error",
                errorMessage = "Playback gagal (${error.errorCodeName}). Pastikan server mendukung format audio MP3/M4A/AAC/OGG/FLAC/WAV atau HLS M3U8."
            )
        }
    }

    fun syncGameContext(gamePackageName: String, savedUrl: String, savedTitle: String) {
        if (lastLoadedGamePackage == gamePackageName) return
        lastLoadedGamePackage = gamePackageName

        val current = _state.value
        if (!current.isPlaying && savedUrl.isNotBlank()) {
            when (val quick = urlResolver.quickClassify(savedUrl)) {
                is ResolvedMediaSource.DirectAudio -> {
                    _state.value = current.copy(
                        activeGamePackage = gamePackageName,
                        currentUrl = quick.resolvedUrl,
                        currentTitle = savedTitle.ifBlank { quick.derivedTitle },
                        sourceName = "${quick.sourceDomain} • Audio",
                        sourceKind = MediaSourceKind.DIRECT_AUDIO,
                        playbackStatus = "Saved • Ready to Load",
                        isYouTubeUrl = false,
                        youtubeUrlToOpen = null,
                        isSpotifyUrl = false,
                        spotifyUrlToOpen = null,
                        errorMessage = null
                    )
                }
                is ResolvedMediaSource.HlsStream -> {
                    _state.value = current.copy(
                        activeGamePackage = gamePackageName,
                        currentUrl = quick.resolvedUrl,
                        currentTitle = savedTitle.ifBlank { quick.derivedTitle },
                        sourceName = "${quick.sourceDomain} • HLS",
                        sourceKind = MediaSourceKind.HLS_STREAM,
                        playbackStatus = "Saved • Ready to Load",
                        isYouTubeUrl = false,
                        youtubeUrlToOpen = null,
                        isSpotifyUrl = false,
                        spotifyUrlToOpen = null,
                        errorMessage = null
                    )
                }
                is ResolvedMediaSource.YouTubeMedia -> {
                    _state.value = current.copy(
                        activeGamePackage = gamePackageName,
                        currentUrl = quick.url,
                        currentTitle = savedTitle.ifBlank { "YouTube Link Saved" },
                        sourceName = quick.sourceDomain,
                        sourceKind = MediaSourceKind.YOUTUBE,
                        playbackStatus = "YouTube App Handoff",
                        isYouTubeUrl = true,
                        youtubeUrlToOpen = quick.url,
                        isSpotifyUrl = false,
                        spotifyUrlToOpen = null,
                        errorMessage = quick.message
                    )
                }
                is ResolvedMediaSource.SpotifyMedia -> {
                    _state.value = current.copy(
                        activeGamePackage = gamePackageName,
                        currentUrl = quick.url,
                        currentTitle = savedTitle.ifBlank { "Spotify Link Saved" },
                        sourceName = quick.sourceDomain,
                        sourceKind = MediaSourceKind.SPOTIFY,
                        playbackStatus = "Spotify App Handoff",
                        isYouTubeUrl = false,
                        youtubeUrlToOpen = null,
                        isSpotifyUrl = true,
                        spotifyUrlToOpen = quick.url,
                        errorMessage = quick.message
                    )
                }
                else -> {
                    _state.value = current.copy(
                        activeGamePackage = gamePackageName,
                        currentUrl = savedUrl,
                        currentTitle = savedTitle.ifBlank { "Saved URL" }
                    )
                }
            }
        } else {
            _state.value = current.copy(activeGamePackage = gamePackageName)
        }
    }

    /**
     * Resolves any media URL using [SmartMediaUrlResolver] (following redirects, checking real
     * Content-Type MIME headers, classifying Direct Audio / HLS / YouTube / Spotify / Web Pages / HTTP errors)
     * before preparing ExoPlayer.
     */
    fun loadUrl(
        url: String,
        title: String? = null,
        gamePackageName: String = _state.value.activeGamePackage,
        autoPlay: Boolean = false,
        saveToHistory: Boolean = true
    ) {
        resolveJob?.cancel()
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(
                isBuffering = false,
                isYouTubeUrl = false,
                youtubeUrlToOpen = null,
                isSpotifyUrl = false,
                spotifyUrlToOpen = null,
                playbackStatus = "Invalid URL",
                errorMessage = "Masukkan URL audio atau media terlebih dahulu."
            )
            return
        }

        _state.value = _state.value.copy(
            currentUrl = trimmed,
            activeGamePackage = gamePackageName,
            isBuffering = true,
            isYouTubeUrl = false,
            youtubeUrlToOpen = null,
            isSpotifyUrl = false,
            spotifyUrlToOpen = null,
            playbackStatus = "Resolving URL...",
            errorMessage = null
        )

        resolveJob = ioScope.launch {
            val resolved = urlResolver.resolve(trimmed)
            applyResolvedSource(
                resolved = resolved,
                customTitle = title,
                gamePackageName = gamePackageName,
                autoPlay = autoPlay,
                saveToHistory = saveToHistory
            )
        }
    }

    private fun applyResolvedSource(
        resolved: ResolvedMediaSource,
        customTitle: String?,
        gamePackageName: String,
        autoPlay: Boolean,
        saveToHistory: Boolean
    ) {
        when (resolved) {
            is ResolvedMediaSource.DirectAudio -> {
                val finalTitle = customTitle?.takeIf { it.isNotBlank() } ?: resolved.derivedTitle
                _state.value = _state.value.copy(
                    currentUrl = resolved.resolvedUrl,
                    currentTitle = finalTitle,
                    sourceName = "${resolved.sourceDomain} • ${resolved.mimeType.uppercase()}",
                    sourceKind = MediaSourceKind.DIRECT_AUDIO,
                    artworkUri = null,
                    artworkBytes = null,
                    activeGamePackage = gamePackageName,
                    isBuffering = true,
                    isYouTubeUrl = false,
                    youtubeUrlToOpen = null,
                    isSpotifyUrl = false,
                    spotifyUrlToOpen = null,
                    playbackStatus = "Loading Audio...",
                    errorMessage = null
                )
                prepareInExoPlayer(
                    streamUrl = resolved.resolvedUrl,
                    title = finalTitle,
                    sourceName = resolved.sourceDomain,
                    mimeType = null,
                    gamePackageName = gamePackageName,
                    autoPlay = autoPlay,
                    saveToHistory = saveToHistory
                )
            }
            is ResolvedMediaSource.HlsStream -> {
                val finalTitle = customTitle?.takeIf { it.isNotBlank() } ?: resolved.derivedTitle
                _state.value = _state.value.copy(
                    currentUrl = resolved.resolvedUrl,
                    currentTitle = finalTitle,
                    sourceName = "${resolved.sourceDomain} • HLS M3U8",
                    sourceKind = MediaSourceKind.HLS_STREAM,
                    artworkUri = null,
                    artworkBytes = null,
                    activeGamePackage = gamePackageName,
                    isBuffering = true,
                    isYouTubeUrl = false,
                    youtubeUrlToOpen = null,
                    isSpotifyUrl = false,
                    spotifyUrlToOpen = null,
                    playbackStatus = "Loading HLS Stream...",
                    errorMessage = null
                )
                prepareInExoPlayer(
                    streamUrl = resolved.resolvedUrl,
                    title = finalTitle,
                    sourceName = "${resolved.sourceDomain} (HLS)",
                    mimeType = MimeTypes.APPLICATION_M3U8,
                    gamePackageName = gamePackageName,
                    autoPlay = autoPlay,
                    saveToHistory = saveToHistory
                )
            }
            is ResolvedMediaSource.YouTubeMedia -> {
                runOnMain { exoPlayer?.stop(); exoPlayer?.clearMediaItems() }
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    currentUrl = resolved.url,
                    currentTitle = customTitle?.takeIf { it.isNotBlank() } ?: "YouTube Audio Link",
                    sourceName = resolved.sourceDomain,
                    sourceKind = MediaSourceKind.YOUTUBE,
                    artworkUri = null,
                    artworkBytes = null,
                    activeGamePackage = gamePackageName,
                    isYouTubeUrl = true,
                    youtubeUrlToOpen = resolved.url,
                    isSpotifyUrl = false,
                    spotifyUrlToOpen = null,
                    playbackStatus = "YouTube App Handoff",
                    errorMessage = resolved.message
                )
            }
            is ResolvedMediaSource.SpotifyMedia -> {
                runOnMain { exoPlayer?.stop(); exoPlayer?.clearMediaItems() }
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    currentUrl = resolved.url,
                    currentTitle = customTitle?.takeIf { it.isNotBlank() } ?: "Spotify Track / Playlist",
                    sourceName = resolved.sourceDomain,
                    sourceKind = MediaSourceKind.SPOTIFY,
                    artworkUri = null,
                    artworkBytes = null,
                    activeGamePackage = gamePackageName,
                    isYouTubeUrl = false,
                    youtubeUrlToOpen = null,
                    isSpotifyUrl = true,
                    spotifyUrlToOpen = resolved.url,
                    playbackStatus = "Spotify App Handoff",
                    errorMessage = resolved.message
                )
            }
            is ResolvedMediaSource.UnsupportedWebPage -> {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    currentUrl = resolved.resolvedUrl,
                    sourceName = resolved.sourceDomain,
                    sourceKind = MediaSourceKind.WEB_PAGE,
                    playbackStatus = "Unsupported Web Page",
                    errorMessage = resolved.reason
                )
            }
            is ResolvedMediaSource.AuthRequired -> {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    currentUrl = resolved.url,
                    sourceName = resolved.sourceDomain,
                    sourceKind = MediaSourceKind.ERROR,
                    playbackStatus = "Auth Required (${resolved.statusCode})",
                    errorMessage = resolved.reason
                )
            }
            is ResolvedMediaSource.HttpError -> {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    currentUrl = resolved.url,
                    sourceName = resolved.sourceDomain,
                    sourceKind = MediaSourceKind.ERROR,
                    playbackStatus = "HTTP ${resolved.statusCode}",
                    errorMessage = resolved.reason
                )
            }
            is ResolvedMediaSource.NetworkFailure -> {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    currentUrl = resolved.url,
                    sourceName = resolved.sourceDomain,
                    sourceKind = MediaSourceKind.ERROR,
                    playbackStatus = "Network Error",
                    errorMessage = resolved.reason
                )
            }
            is ResolvedMediaSource.InvalidInput -> {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    sourceKind = MediaSourceKind.ERROR,
                    playbackStatus = "Invalid URL",
                    errorMessage = resolved.reason
                )
            }
        }
    }

    private fun prepareInExoPlayer(
        streamUrl: String,
        title: String,
        sourceName: String,
        mimeType: String?,
        gamePackageName: String,
        autoPlay: Boolean,
        saveToHistory: Boolean
    ) {
        pendingHistorySave = if (saveToHistory) Triple(streamUrl, title, gamePackageName) else null
        runOnMain {
            try {
                val player = ensurePlayer()
                val itemBuilder = MediaItem.Builder()
                    .setUri(Uri.parse(streamUrl))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(sourceName)
                            .build()
                    )
                if (mimeType != null) {
                    itemBuilder.setMimeType(mimeType)
                }
                player.setMediaItem(itemBuilder.build())
                player.prepare()
                player.playWhenReady = autoPlay
            } catch (e: Exception) {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    playbackStatus = "Error",
                    errorMessage = "Gagal memuat stream Media3: ${e.localizedMessage}"
                )
            }
        }
    }

    fun playUrl(
        url: String,
        title: String? = null,
        gamePackageName: String = _state.value.activeGamePackage,
        saveToHistory: Boolean = true
    ) {
        val parsed = Uri.parse(url)
        if (parsed.scheme.equals("content", ignoreCase = true) ||
            parsed.scheme.equals("file", ignoreCase = true)
        ) {
            playLocalUri(parsed, title)
            return
        }
        loadUrl(
            url = url,
            title = title,
            gamePackageName = gamePackageName,
            autoPlay = true,
            saveToHistory = saveToHistory
        )
    }

    /**
     * Plays a user-selected local audio document. The URI permission is persisted by SAF
     * so the selected track remains available after the app is restarted.
     */
    fun playLocalUri(uri: Uri, displayName: String? = null) {
        resolveJob?.cancel()
        runOnMain {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some document providers do not offer persistable grants; current grant may still work.
                } catch (_: IllegalArgumentException) {
                    // Provider did not issue a persistable grant.
                }

                val title = displayName?.takeIf { it.isNotBlank() }
                    ?: queryDisplayName(uri)
                    ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                    ?: "Local Audio"

                val player = ensurePlayer()
                pendingHistorySave = Triple(uri.toString(), title, _state.value.activeGamePackage)
                _state.value = _state.value.copy(
                    currentUrl = uri.toString(),
                    currentTitle = title,
                    sourceName = "Device • Local Music",
                    sourceKind = MediaSourceKind.DIRECT_AUDIO,
                    isYouTubeUrl = false,
                    youtubeUrlToOpen = null,
                    isSpotifyUrl = false,
                    spotifyUrlToOpen = null,
                    isBuffering = true,
                    hasLoadedMedia = false,
                    playbackStatus = "Loading local audio...",
                    errorMessage = null
                )
                val metadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("Device • Local Music")
                    .build()
                val item = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(metadata)
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.playWhenReady = true
            } catch (e: Exception) {
                pendingHistorySave = null
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    hasLoadedMedia = false,
                    playbackStatus = "Local file error",
                    errorMessage = e.localizedMessage ?: "Tidak dapat membuka file audio ini."
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    fun play() {
        runOnMain {
            val player = exoPlayer
            val current = _state.value
            if (player != null && current.hasLoadedMedia) {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
                return@runOnMain
            }
            if (current.currentUrl.isNotBlank() && !current.isYouTubeUrl && !current.isSpotifyUrl) {
                loadUrl(
                    url = current.currentUrl,
                    title = current.currentTitle,
                    gamePackageName = current.activeGamePackage,
                    autoPlay = true,
                    saveToHistory = false
                )
                return@runOnMain
            }
            dispatchSystemMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    fun togglePlayPause() {
        runOnMain {
            val player = exoPlayer
            val current = _state.value
            if (player != null && current.hasLoadedMedia) {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0)
                    }
                    player.play()
                }
                return@runOnMain
            }

            if (current.currentUrl.isNotBlank() && !current.isYouTubeUrl && !current.isSpotifyUrl) {
                loadUrl(
                    url = current.currentUrl,
                    title = current.currentTitle,
                    gamePackageName = current.activeGamePackage,
                    autoPlay = true,
                    saveToHistory = false
                )
                return@runOnMain
            }

            dispatchSystemMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            val items = _historyList.value
            if (items.isNotEmpty() && audioManager?.isMusicActive != true) {
                currentQueueIndex = 0
                val track = items[0]
                playUrl(track.url, track.title, current.activeGamePackage, saveToHistory = false)
            } else {
                _state.value = current.copy(
                    currentTitle = if (audioManager?.isMusicActive == true) "System Media Active" else current.currentTitle,
                    playbackStatus = if (audioManager?.isMusicActive == true) "System Media Key Sent" else "Idle",
                    errorMessage = null
                )
            }
        }
    }

    fun pause() {
        runOnMain {
            val player = exoPlayer
            if (player != null && player.isPlaying) {
                player.pause()
            } else {
                dispatchSystemMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
            }
        }
    }

    fun stopPlayback() {
        resolveJob?.cancel()
        runOnMain {
            exoPlayer?.let {
                it.stop()
                it.clearMediaItems()
            }
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                hasLoadedMedia = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                playbackStatus = "Stopped"
            )
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        runOnMain {
            exoPlayer?.volume = clamped
        }
    }

    fun skipToNext() {
        val list = queueForActiveGame()
        if (list.isNotEmpty() && (exoPlayer != null || _state.value.hasLoadedMedia)) {
            currentQueueIndex = (currentQueueIndex + 1).mod(list.size)
            val next = list[currentQueueIndex]
            playUrl(next.url, next.title, _state.value.activeGamePackage, saveToHistory = false)
        } else {
            dispatchSystemMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun skipToPrevious() {
        val list = queueForActiveGame()
        if (list.isNotEmpty() && (exoPlayer != null || _state.value.hasLoadedMedia)) {
            currentQueueIndex = (currentQueueIndex - 1).mod(list.size)
            val prev = list[currentQueueIndex]
            playUrl(prev.url, prev.title, _state.value.activeGamePackage, saveToHistory = false)
        } else {
            dispatchSystemMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun toggleRepeat() {
        val newLoop = !_state.value.isLooping
        _state.value = _state.value.copy(isLooping = newLoop)
        runOnMain {
            exoPlayer?.repeatMode = if (newLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    fun seekTo(positionMs: Long) {
        runOnMain {
            exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
            _state.value = _state.value.copy(currentPositionMs = positionMs.coerceAtLeast(0L))
        }
    }

    fun updateProgress() {
        val externalActive = try {
            audioManager?.isMusicActive == true && !_state.value.isPlaying
        } catch (_: Exception) {
            false
        }

        mainScope.launch {
            val player = exoPlayer
            if (player != null && player.isPlaying) {
                _state.value = _state.value.copy(
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.takeIf { it > 0 } ?: _state.value.durationMs,
                    externalMediaActive = false
                )
            } else if (_state.value.externalMediaActive != externalActive) {
                _state.value = _state.value.copy(
                    externalMediaActive = externalActive
                )
            }
        }
    }

    fun openInYouTube(callerContext: Context, url: String) {
        launchExternalUrl(callerContext, url)
    }

    fun openInSpotify(callerContext: Context, url: String) {
        launchExternalUrl(callerContext, url)
    }

    private fun launchExternalUrl(callerContext: Context, url: String) {
        val normalized = normalizeExternalMediaUrl(url)
        val targetPackage = when {
            normalized.contains("youtube.com", true) || normalized.contains("youtu.be", true) ->
                "com.google.android.youtube"
            normalized.startsWith("spotify:", true) || normalized.contains("spotify.com", true) ||
                normalized.contains("spotify.link", true) -> "com.spotify.music"
            else -> null
        }

        fun makeIntent(packageName: String? = null) = Intent(
            Intent.ACTION_VIEW, Uri.parse(normalized)
        ).apply {
            if (packageName != null) setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val appIntent = targetPackage?.let(::makeIntent)
            if (appIntent != null && appIntent.resolveActivity(callerContext.packageManager) != null) {
                callerContext.startActivity(appIntent)
            } else {
                val browserIntent = makeIntent()
                if (browserIntent.resolveActivity(callerContext.packageManager) == null) {
                    throw IllegalStateException("Tidak ada aplikasi untuk membuka tautan ini.")
                }
                callerContext.startActivity(browserIntent)
            }
            runOnMain {
                _state.value = _state.value.copy(
                    playbackStatus = if (targetPackage == "com.spotify.music") "Opened in Spotify" else "Opened in YouTube",
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            runOnMain {
                _state.value = _state.value.copy(
                    playbackStatus = "Unable to Open Media App",
                    errorMessage = e.localizedMessage ?: "Tidak dapat membuka aplikasi media."
                )
            }
        }
    }

    private fun normalizeExternalMediaUrl(raw: String): String {
        val value = raw.trim()
        if (value.startsWith("spotify:", true)) return value
        if (value.matches(Regex("^[A-Za-z0-9_-]{11}$"))) {
            return "https://www.youtube.com/watch?v=$value"
        }
        return when {
            value.startsWith("www.", true) || value.startsWith("youtube.com/", true) ||
                value.startsWith("youtu.be/", true) || value.startsWith("open.spotify.com/", true) ||
                value.startsWith("spotify.link/", true) -> "https://$value"
            else -> value
        }
    }

    fun deleteHistoryItem(id: Long) {
        if (musicHistoryDao == null) return
        ioScope.launch {
            musicHistoryDao.deleteById(id)
        }
    }

    private fun queueForActiveGame(): List<MusicHistoryEntity> {
        val pkg = _state.value.activeGamePackage
        val all = _historyList.value
        if (pkg.isBlank()) return all
        val gameSpecific = all.filter { it.gamePackageName == pkg || it.gamePackageName.isBlank() }
        return gameSpecific.ifEmpty { all }
    }

    private fun persistHistory(url: String, title: String, gamePackageName: String, durationSeconds: Long) {
        if (musicHistoryDao == null) return
        ioScope.launch {
            musicHistoryDao.insert(
                MusicHistoryEntity(
                    title = title,
                    url = url,
                    gamePackageName = gamePackageName,
                    durationSeconds = durationSeconds
                )
            )
        }
    }

    private fun dispatchSystemMediaKey(keyCode: Int) {
        try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
        } catch (_: Exception) {}
    }

    fun release() {
        resolveJob?.cancel()
        runOnMain {
            try {
                mediaSession?.release()
                mediaSession = null
                exoPlayer?.removeListener(playerListener)
                exoPlayer?.release()
                exoPlayer = null
            } catch (_: Exception) {}
            _state.value = MusicPlayerState()
        }
    }
}
