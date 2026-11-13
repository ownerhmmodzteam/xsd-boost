package com.example.core.system.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.XsdBoostApplication
import com.example.core.database.GameProfileEntity
import com.example.core.database.toCrosshairConfig
import com.example.core.system.booster.RamAnalysisReport
import com.example.core.system.fps.FpsEstimator
import com.example.core.system.hardware.SystemStats
import com.example.core.system.media.MusicPlayerState
import com.example.core.system.screenshot.ScreenshotActivity
import com.example.core.system.sensitivity.InputCompatibilityLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lifecycleOwner = OverlayLifecycleOwner()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var monitoringJob: Job? = null
    private var foregroundWatcherJob: Job? = null
    private var activeProfileJob: Job? = null

    private val container by lazy { (application as XsdBoostApplication).container }
    private val hardwareMonitor by lazy { container.hardwareMonitor }
    private val prefsRepo by lazy { container.prefsRepo }
    private val gameProfileRepo by lazy { container.gameProfileRepo }
    private val foregroundMonitor by lazy { container.foregroundMonitor }
    private val ramBooster by lazy { container.ramBooster }
    private val graphicsEngine by lazy { container.graphicsEngine }
    private val musicPlayer by lazy { container.musicPlayer }
    private val shizukuClient by lazy { container.shizukuClient }
    private val fpsEstimator = FpsEstimator()

    // Window views & layout params
    private var handleView: View? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var islandView: View? = null
    private var islandParams: WindowManager.LayoutParams? = null

    // Telemetry state (only polled when booster panel is actively open)
    private val statsFlow = MutableStateFlow(SystemStats())
    private val fpsFlow = MutableStateFlow(60f)
    private val cpuHistoryFlow = MutableStateFlow(List(16) { 0f })
    private val ramHistoryFlow = MutableStateFlow(List(16) { 0f })
    private val lastBoosterReportFlow = MutableStateFlow<RamAnalysisReport?>(null)

    // Smart game detection & per-game state
    private var enabledProfilesMap: Map<String, GameProfileEntity> = emptyMap()
    private var activeGamePackage: String? = null
    private val activeProfileFlow = MutableStateFlow<GameProfileEntity?>(null)

    // Transient session UI flags (always reset on exiting game)
    private var panelVisibleState by mutableStateOf(false)
    private var selectedTabState by mutableStateOf(OverlayToolTab.PERFORMANCE)
    private var isIslandExpandedState by mutableStateOf(false)
    private var isClosedForSession = false
    private var isIslandHiddenForSession = false
    private var isPreviewSession = false
    private var isTextInputFocused = false

    companion object {
        const val ACTION_START_WATCHER = "com.example.ACTION_START_WATCHER"
        const val ACTION_START_EDGE = "com.example.ACTION_START_EDGE"
        const val ACTION_OPEN_PANEL = "com.example.ACTION_OPEN_PANEL"
        const val ACTION_SHOW_FOR_GAME = "com.example.ACTION_SHOW_FOR_GAME"
        const val ACTION_HIDE_FOR_NON_GAME = "com.example.ACTION_HIDE_FOR_NON_GAME"
        const val ACTION_STOP = "com.example.ACTION_STOP_OVERLAY"
        const val EXTRA_GAME_PACKAGE = "extra_game_package"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, openPanelImmediately: Boolean = false) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = if (openPanelImmediately) ACTION_OPEN_PANEL else ACTION_START_WATCHER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun showForGame(context: Context, gamePackageName: String) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_SHOW_FOR_GAME
                putExtra(EXTRA_GAME_PACKAGE, gamePackageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hideForNonGame(context: Context) {
            if (!isRunning) return
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_HIDE_FOR_NON_GAME
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(1001, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreateAndResume()
        observeEnabledProfiles()
        observeMusicForDynamicIsland()
        observeDetectedGameOrPollFallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    prefsRepo.setOverlayEnabled(false)
                    com.example.core.system.games.GameDetectionForegroundService.stop(this@FloatingOverlayService)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_SHOW_FOR_GAME -> {
                val targetPkg = intent.getStringExtra(EXTRA_GAME_PACKAGE)
                if (!targetPkg.isNullOrBlank()) {
                    serviceScope.launch {
                        val profile = enabledProfilesMap[targetPkg]
                            ?: gameProfileRepo.getOrCreateProfile(
                                packageName = targetPkg,
                                appName = targetPkg,
                                defaultOverlaySide = prefsRepo.defaultOverlaySide.first(),
                                defaultHandleSizeDp = prefsRepo.defaultHandleSizeDp.first(),
                                defaultHandleOpacity = prefsRepo.defaultHandleOpacity.first(),
                                defaultLongPressMs = prefsRepo.defaultLongPressMs.first()
                            )
                        if (profile.boosterEnabled && activeGamePackage != targetPkg) {
                            onEnterTargetGame(profile)
                        }
                    }
                }
            }
            ACTION_HIDE_FOR_NON_GAME -> {
                if (!isPreviewSession && activeGamePackage != null) {
                    onExitTargetGame()
                }
            }
            ACTION_OPEN_PANEL -> {
                serviceScope.launch {
                    if (activeGamePackage == null) {
                        isPreviewSession = true
                        if (activeProfileFlow.value == null) {
                            activeProfileFlow.value = GameProfileEntity(
                                packageName = packageName,
                                appName = "Preview Mode",
                                boosterEnabled = true,
                                overlaySide = prefsRepo.defaultOverlaySide.first(),
                                handleSizeDp = prefsRepo.defaultHandleSizeDp.first(),
                                handleOpacity = prefsRepo.defaultHandleOpacity.first(),
                                longPressDurationMs = prefsRepo.defaultLongPressMs.first()
                            )
                        }
                    }
                    isClosedForSession = false
                    openBoosterPanel()
                }
            }
            else -> {}
        }
        return START_STICKY
    }

    private fun observeEnabledProfiles() {
        serviceScope.launch {
            gameProfileRepo.enabledProfiles.collectLatest { profiles ->
                enabledProfilesMap = profiles.associateBy { it.packageName }
                val activePkg = activeGamePackage
                if (activePkg != null && !enabledProfilesMap.containsKey(activePkg) && !isPreviewSession) {
                    onExitTargetGame()
                }
            }
        }
    }

    private fun observeMusicForDynamicIsland() {
        serviceScope.launch {
            musicPlayer.state.collectLatest { musicState ->
                syncDynamicIslandWindow(musicState)
            }
        }
    }

    private fun observeDetectedGameOrPollFallback() {
        if (foregroundWatcherJob?.isActive == true) return
        foregroundWatcherJob = serviceScope.launch {
            while (isActive) {
                if (com.example.core.system.games.GameDetectionForegroundService.isRunning) {
                    val detected = container.detectedForegroundGame.value
                    val matched = if (detected != null) enabledProfilesMap[detected] else null
                    if (matched != null && activeGamePackage != matched.packageName) {
                        onEnterTargetGame(matched)
                    } else if (matched == null && activeGamePackage != null && !isPreviewSession) {
                        onExitTargetGame()
                    }
                } else {
                    val detection = withContext(Dispatchers.IO) {
                        foregroundMonitor.pollForegroundPackage()
                    }

                    val fgPkg = detection.foregroundPackage
                    val matchedProfile = if (
                        fgPkg != null &&
                        fgPkg != packageName &&
                        detection.isScreenInteractive
                    ) {
                        enabledProfilesMap[fgPkg]
                    } else {
                        null
                    }

                    if (matchedProfile != null) {
                        if (activeGamePackage != matchedProfile.packageName) {
                            onEnterTargetGame(matchedProfile)
                        }
                    } else if (activeGamePackage != null) {
                        onExitTargetGame()
                    } else if (isPreviewSession && fgPkg != null && fgPkg != packageName) {
                        onExitTargetGame()
                    }
                }

                delay(1100L)
            }
        }
    }

    private fun onEnterTargetGame(profile: GameProfileEntity) {
        isPreviewSession = false
        activeGamePackage = profile.packageName
        isClosedForSession = false
        isIslandHiddenForSession = false
        isIslandExpandedState = false
        selectedTabState = OverlayToolTab.PERFORMANCE
        panelVisibleState = false

        activeProfileJob?.cancel()
        activeProfileJob = serviceScope.launch {
            gameProfileRepo.observeProfile(profile.packageName).collectLatest { updated ->
                val resolved = updated ?: profile
                activeProfileFlow.value = resolved
                syncGameCrosshair(resolved)
                syncEdgeHandleForProfile(resolved)
                updatePanelSideIfVisible(resolved)
                syncDynamicIslandWindow(musicPlayer.state.value)
            }
        }

        activeProfileFlow.value = profile
        musicPlayer.syncGameContext(profile.packageName, profile.musicUrl, profile.musicTitle)
        syncGameCrosshair(profile)
        syncEdgeHandleForProfile(profile)
        syncDynamicIslandWindow(musicPlayer.state.value)
    }

    private fun onExitTargetGame() {
        activeProfileJob?.cancel()
        activeProfileJob = null
        activeGamePackage = null
        isPreviewSession = false

        // Reset ONLY transient session UI state — never mutate saved Room GameProfileEntity
        panelVisibleState = false
        selectedTabState = OverlayToolTab.PERFORMANCE
        isIslandExpandedState = false
        isClosedForSession = false
        isIslandHiddenForSession = false
        isTextInputFocused = false

        removePanelView()
        removeHandleView()
        removeDynamicIslandView()
        pauseTelemetryLoop()

        container.activeGameCrosshairConfig.value = null
        FloatingCrosshairService.stop(this)
        activeProfileFlow.value = null
    }

    private fun syncGameCrosshair(profile: GameProfileEntity) {
        if (profile.crosshairEnabled && activeGamePackage == profile.packageName) {
            container.activeGameCrosshairConfig.value = profile.toCrosshairConfig()
            FloatingCrosshairService.start(this)
        } else {
            container.activeGameCrosshairConfig.value = null
            FloatingCrosshairService.stop(this)
        }
    }

    private fun syncEdgeHandleForProfile(profile: GameProfileEntity) {
        if (isClosedForSession || !profile.overlayEnabled) {
            removeHandleView()
            return
        }
        if (panelView != null) {
            removeHandleView()
            return
        }
        if (handleView == null) {
            attachEdgeHandleView(profile)
        } else {
            updateEdgeHandleLayout(profile)
        }
    }

    private fun syncDynamicIslandWindow(musicState: MusicPlayerState) {
        val profile = activeProfileFlow.value
        val inActiveTargetGame = activeGamePackage != null && !isPreviewSession
        val shouldShow = inActiveTargetGame &&
            profile != null &&
            profile.dynamicIslandEnabled &&
            !isIslandHiddenForSession &&
            (musicState.isPlaying || (musicState.hasLoadedMedia && musicState.playbackStatus == "Paused"))

        if (shouldShow) {
            if (islandView == null) {
                attachDynamicIslandView()
            }
        } else {
            isIslandExpandedState = false
            removeDynamicIslandView()
        }
    }

    private fun edgeGravity(side: EdgeSide): Int {
        return if (side == EdgeSide.LEFT) {
            Gravity.TOP or Gravity.START
        } else {
            Gravity.TOP or Gravity.END
        }
    }

    private fun attachEdgeHandleView(profile: GameProfileEntity) {
        removeHandleView()
        val dm = resources.displayMetrics
        val density = dm.density
        val side = EdgeSide.fromString(profile.overlaySide)
        val widthPx = ((if (profile.isHandleHidden) 14f else 22f) * density).toInt().coerceAtLeast(12)
        val heightDp = (profile.handleSizeDp + 16f).coerceIn(72f, 136f)
        val heightPx = (heightDp * density).toInt().coerceAtLeast(64)
        val maxY = (dm.heightPixels - heightPx).coerceAtLeast(0)
        val yPx = (maxY * profile.handleYFraction.coerceIn(0.08f, 0.88f)).toInt().coerceIn(0, maxY)

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            overlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = edgeGravity(side)
            x = 0
            y = yPx
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                val liveProfile by activeProfileFlow.collectAsState()
                val activeProf = liveProfile ?: profile
                val currentSide = EdgeSide.fromString(activeProf.overlaySide)
                FloatingEdgeHandle(
                    side = currentSide,
                    handleSizeDp = activeProf.handleSizeDp,
                    handleOpacity = activeProf.handleOpacity,
                    longPressDurationMs = activeProf.longPressDurationMs,
                    isHiddenMode = activeProf.isHandleHidden,
                    onLongPress = {
                        if (activeGamePackage != null) {
                            triggerLightHaptic()
                            val pkg = activeGamePackage
                            if (pkg != null && activeProfileFlow.value?.isHandleHidden == true) {
                                serviceScope.launch {
                                    gameProfileRepo.updateProfile(pkg) { it.copy(isHandleHidden = false) }
                                }
                            }
                            openBoosterPanel()
                        }
                    },
                    onDragDeltaY = { deltaY ->
                        val currentDm = resources.displayMetrics
                        val maxDragY = (currentDm.heightPixels - params.height).coerceAtLeast(1)
                        params.y = (params.y + kotlin.math.round(deltaY).toInt()).coerceIn(0, maxDragY)
                        handleView?.let { v ->
                            try {
                                windowManager?.updateViewLayout(v, params)
                            } catch (_: Exception) {}
                        }
                    },
                    onDragEnd = {
                        val currentDm = resources.displayMetrics
                        val maxDragY = (currentDm.heightPixels - params.height).coerceAtLeast(1)
                        val newFraction = (params.y.toFloat() / maxDragY.toFloat()).coerceIn(0.08f, 0.88f)
                        val pkg = activeGamePackage
                        if (pkg != null) {
                            serviceScope.launch {
                                gameProfileRepo.updateProfile(pkg) { it.copy(handleYFraction = newFraction) }
                            }
                        }
                    }
                )
            }
        }

        handleParams = params
        handleView = composeView
        try {
            windowManager?.addView(composeView, params)
        } catch (_: Exception) {}
    }

    private fun updateEdgeHandleLayout(profile: GameProfileEntity) {
        val view = handleView ?: return
        val params = handleParams ?: return
        val dm = resources.displayMetrics
        val density = dm.density
        val side = EdgeSide.fromString(profile.overlaySide)
        val widthPx = ((if (profile.isHandleHidden) 14f else 22f) * density).toInt().coerceAtLeast(12)
        val heightDp = (profile.handleSizeDp + 16f).coerceIn(72f, 136f)
        val heightPx = (heightDp * density).toInt().coerceAtLeast(64)
        val maxY = (dm.heightPixels - heightPx).coerceAtLeast(0)
        val yPx = (maxY * profile.handleYFraction.coerceIn(0.08f, 0.88f)).toInt().coerceIn(0, maxY)

        params.gravity = edgeGravity(side)
        params.width = widthPx
        params.height = heightPx
        params.y = yPx
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun updatePanelSideIfVisible(profile: GameProfileEntity) {
        val view = panelView ?: return
        val params = panelParams ?: return
        val side = EdgeSide.fromString(profile.overlaySide)
        val desiredGravity = edgeGravity(side)
        if (params.gravity != desiredGravity) {
            params.gravity = desiredGravity
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }

    private fun openBoosterPanel() {
        removeHandleView()
        removePanelView()
        startTelemetryLoop()

        val dm = resources.displayMetrics
        val density = dm.density
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val screenWidthDp = dm.widthPixels / density
        val screenHeightDp = dm.heightPixels / density

        val panelWidthDp = if (isLandscape) {
            (screenWidthDp * 0.54f).coerceIn(280f, 430f).dp
        } else {
            (screenWidthDp * 0.86f).coerceIn(280f, 356f).dp
        }
        val panelMaxHeightDp = (screenHeightDp * (if (isLandscape) 0.86f else 0.76f)).coerceAtLeast(260f).dp

        val currentSide = EdgeSide.fromString(activeProfileFlow.value?.overlaySide)
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayLayoutType(),
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = edgeGravity(currentSide)
            x = (10 * density).toInt()
            y = (if (isLandscape) 14 * density else 46 * density).toInt()
        }

        panelVisibleState = false
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                val profile by activeProfileFlow.collectAsState()
                val resolvedProfile = profile ?: GameProfileEntity(packageName = packageName, appName = "Preview")
                val stats by statsFlow.collectAsState()
                val fps by fpsFlow.collectAsState()
                val cpuHistory by cpuHistoryFlow.collectAsState()
                val ramHistory by ramHistoryFlow.collectAsState()
                val opacity by prefsRepo.overlayOpacity.collectAsState(initial = 0.90f)
                val lastReport by lastBoosterReportFlow.collectAsState()
                val musicState by musicPlayer.state.collectAsState()
                val allMusicHistory by musicPlayer.historyList.collectAsState()
                val sensitivityPresets by (gameProfileRepo.sensitivityPresets ?: flowOf(emptyList())).collectAsState(initial = emptyList())
                val compatibilityReport = remember {
                    InputCompatibilityLayer.inspect(this@FloatingOverlayService, container.shizukuBridge.status.value)
                }
                val gameFilteredHistory = remember(allMusicHistory, resolvedProfile.packageName) {
                    val exact = allMusicHistory.filter { it.gamePackageName == resolvedProfile.packageName }
                    exact.ifEmpty { allMusicHistory }
                }
                val screenshotFeedback by container.screenshotFeedback.collectAsState()
                val graphicsInfo = remember { graphicsEngine.getHardwareInfo() }
                val graphicsProfiles = remember { graphicsEngine.getPresetProfiles() }

                InGameBoosterPanel(
                    visible = panelVisibleState,
                    activeGameName = resolvedProfile.appName,
                    overlaySide = EdgeSide.fromString(resolvedProfile.overlaySide),
                    handleSizeDp = resolvedProfile.handleSizeDp,
                    handleOpacity = resolvedProfile.handleOpacity,
                    longPressDurationMs = resolvedProfile.longPressDurationMs,
                    selectedTab = selectedTabState,
                    onSelectTab = { selectedTabState = it },
                    panelWidthDp = panelWidthDp,
                    panelMaxHeightDp = panelMaxHeightDp,
                    opacity = opacity,
                    stats = stats,
                    fps = fps,
                    cpuHistory = cpuHistory,
                    ramHistory = ramHistory,
                    intervalMs = resolvedProfile.monitorIntervalMs,
                    isHandleHidden = resolvedProfile.isHandleHidden,
                    onChangeInterval = { ms ->
                        updateActiveGameOrPreview { it.copy(monitorIntervalMs = ms) }
                    },
                    onChangeOverlaySide = { side ->
                        serviceScope.launch { prefsRepo.setDefaultOverlaySide(side.storageValue) }
                        updateActiveGameOrPreview { it.copy(overlaySide = side.storageValue) }
                    },
                    onChangeHandleSize = { sizeDp ->
                        serviceScope.launch { prefsRepo.setDefaultHandleSizeDp(sizeDp) }
                        updateActiveGameOrPreview { it.copy(handleSizeDp = sizeDp.coerceIn(56f, 120f)) }
                    },
                    onChangeHandleOpacity = { op ->
                        serviceScope.launch { prefsRepo.setDefaultHandleOpacity(op) }
                        updateActiveGameOrPreview { it.copy(handleOpacity = op.coerceIn(0.25f, 1.0f)) }
                    },
                    onChangeLongPressMs = { ms ->
                        serviceScope.launch { prefsRepo.setDefaultLongPressMs(ms) }
                        updateActiveGameOrPreview { it.copy(longPressDurationMs = ms.coerceIn(250L, 800L)) }
                    },
                    onToggleHandleHidden = { hidden ->
                        updateActiveGameOrPreview { it.copy(isHandleHidden = hidden) }
                    },
                    onResetProfile = {
                        val pkg = activeGamePackage
                        if (pkg != null) {
                            serviceScope.launch {
                                val reset = gameProfileRepo.resetProfile(pkg)
                                if (reset != null) {
                                    activeProfileFlow.value = reset
                                    syncGameCrosshair(reset)
                                }
                            }
                        }
                    },
                    crosshairEnabled = resolvedProfile.crosshairEnabled,
                    crosshairConfig = resolvedProfile.toCrosshairConfig(),
                    onToggleCrosshair = { enabled ->
                        updateActiveGameOrPreview { it.copy(crosshairEnabled = enabled) }
                    },
                    onUpdateCrosshair = { cfg ->
                        updateActiveGameOrPreview {
                            it.copy(
                                crosshairStyle = cfg.style.name,
                                crosshairColorArgb = cfg.colorArgb,
                                crosshairSizeDp = cfg.sizeDp,
                                crosshairOpacity = cfg.opacity
                            )
                        }
                    },
                    sensX = resolvedProfile.sensitivityX,
                    sensY = resolvedProfile.sensitivityY,
                    sensitivityPresets = sensitivityPresets,
                    compatibilityReport = compatibilityReport,
                    onSaveSensitivity = { x, y ->
                        updateActiveGameOrPreview { it.copy(sensitivityX = x, sensitivityY = y) }
                    },
                    onSaveSensitivityPreset = { name, x, y ->
                        serviceScope.launch {
                            gameProfileRepo.saveSensitivityPreset(name, x, y, resolvedProfile.packageName)
                        }
                    },
                    onDeleteSensitivityPreset = { id ->
                        serviceScope.launch { gameProfileRepo.deleteSensitivityPreset(id) }
                    },
                    graphicsInfo = graphicsInfo,
                    graphicsProfiles = graphicsProfiles,
                    selectedGraphicsProfileId = resolvedProfile.graphicsProfileId,
                    onSelectGraphicsProfile = { id ->
                        updateActiveGameOrPreview { it.copy(graphicsProfileId = id) }
                    },
                    lastBoosterReport = lastReport,
                    onCleanCache = {
                        serviceScope.launch {
                            val report = withContext(Dispatchers.IO) { ramBooster.executeSmartBoost() }
                            lastBoosterReportFlow.value = report
                            statsFlow.value = withContext(Dispatchers.IO) { hardwareMonitor.getSystemStats() }
                        }
                    },
                    musicState = musicState,
                    savedMusicUrl = resolvedProfile.musicUrl,
                    dynamicIslandEnabled = resolvedProfile.dynamicIslandEnabled,
                    musicHistory = gameFilteredHistory,
                    onLoadMusicUrl = { url ->
                        val pkg = resolvedProfile.packageName
                        musicPlayer.loadUrl(url = url, gamePackageName = pkg, autoPlay = false)
                        updateActiveGameOrPreview {
                            it.copy(
                                musicUrl = url.trim(),
                                musicTitle = musicPlayer.state.value.currentTitle
                            )
                        }
                    },
                    onMusicPlay = { musicPlayer.play() },
                    onMusicPause = { musicPlayer.pause() },
                    onMusicStop = { musicPlayer.stopPlayback() },
                    onMusicNext = { musicPlayer.skipToNext() },
                    onMusicPrev = { musicPlayer.skipToPrevious() },
                    onMusicRepeat = {
                        musicPlayer.toggleRepeat()
                        updateActiveGameOrPreview { it.copy(musicLooping = musicPlayer.state.value.isLooping) }
                    },
                    onMusicVolumeChange = { vol -> musicPlayer.setVolume(vol) },
                    onMusicSeekTo = { posMs -> musicPlayer.seekTo(posMs) },
                    onToggleDynamicIsland = { enabled ->
                        if (enabled) isIslandHiddenForSession = false
                        updateActiveGameOrPreview { it.copy(dynamicIslandEnabled = enabled) }
                    },
                    onPlayHistoryItem = { item ->
                        musicPlayer.playUrl(item.url, item.title, resolvedProfile.packageName, saveToHistory = false)
                        updateActiveGameOrPreview { it.copy(musicUrl = item.url, musicTitle = item.title) }
                    },
                    onOpenYouTube = { ytUrl ->
                        musicPlayer.openInYouTube(this@FloatingOverlayService, ytUrl)
                    },
                    onOpenSpotify = { spUrl ->
                        musicPlayer.openInSpotify(this@FloatingOverlayService, spUrl)
                    },
                    onTextInputFocusChanged = { focused ->
                        updatePanelImeFocusability(focused)
                    },
                    screenshotFeedback = screenshotFeedback,
                    onCaptureScreenshot = {
                        serviceScope.launch {
                            minimizePanelToHandle()
                            delay(220)
                            ScreenshotActivity.launch(this@FloatingOverlayService)
                        }
                    },
                    onMinimizeToHandle = { minimizePanelToHandle() },
                    onCloseForSession = { closeOverlayForSession() }
                )
            }
        }

        composeView.setOnTouchListener { _, event ->
            if (event?.actionMasked == MotionEvent.ACTION_OUTSIDE && !isTextInputFocused) {
                minimizePanelToHandle()
                true
            } else {
                false
            }
        }

        panelParams = params
        panelView = composeView
        try {
            windowManager?.addView(composeView, params)
            panelVisibleState = true
        } catch (_: Exception) {}
    }

    private fun updatePanelImeFocusability(focused: Boolean) {
        isTextInputFocused = focused
        val view = panelView ?: return
        val params = panelParams ?: return
        params.flags = if (focused) {
            (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun attachDynamicIslandView() {
        removeDynamicIslandView()
        val density = resources.displayMetrics.density
        val statusBarPx = getStatusBarHeightPx()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = statusBarPx + (8 * density).toInt()
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                val musicState by musicPlayer.state.collectAsState()
                FloatingMusicDynamicIsland(
                    musicState = musicState,
                    isExpanded = isIslandExpandedState,
                    onToggleExpand = { isIslandExpandedState = !isIslandExpandedState },
                    onPlayPause = { musicPlayer.togglePlayPause() },
                    onNext = { musicPlayer.skipToNext() },
                    onPrevious = { musicPlayer.skipToPrevious() },
                    onToggleRepeat = { musicPlayer.toggleRepeat() },
                    onHideIsland = {
                        isIslandHiddenForSession = true
                        isIslandExpandedState = false
                        removeDynamicIslandView()
                    }
                )
            }
        }

        islandParams = params
        islandView = composeView
        try {
            windowManager?.addView(composeView, params)
        } catch (_: Exception) {}
    }

    private fun minimizePanelToHandle() {
        serviceScope.launch {
            isTextInputFocused = false
            panelVisibleState = false
            delay(170)
            removePanelView()
            pauseTelemetryLoop()
            if (isPreviewSession) {
                isPreviewSession = false
                activeProfileFlow.value = null
            } else {
                activeProfileFlow.value?.let { syncEdgeHandleForProfile(it) }
            }
        }
    }

    private fun closeOverlayForSession() {
        serviceScope.launch {
            isTextInputFocused = false
            panelVisibleState = false
            delay(160)
            removePanelView()
            removeHandleView()
            pauseTelemetryLoop()
            if (isPreviewSession) {
                isPreviewSession = false
                activeProfileFlow.value = null
            } else {
                isClosedForSession = true
            }
        }
    }

    private fun updateActiveGameOrPreview(transform: (GameProfileEntity) -> GameProfileEntity) {
        val currentPreview = activeProfileFlow.value
        if (isPreviewSession && currentPreview != null) {
            val updated = transform(currentPreview)
            activeProfileFlow.value = updated
            updatePanelSideIfVisible(updated)
            return
        }
        val pkg = activeGamePackage ?: return
        serviceScope.launch {
            val updated = gameProfileRepo.updateProfile(pkg, transform = transform)
            activeProfileFlow.value = updated
            updatePanelSideIfVisible(updated)
        }
    }

    private fun startTelemetryLoop() {
        if (monitoringJob?.isActive == true) return
        try {
            fpsEstimator.start()
        } catch (_: Exception) {}

        monitoringJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val stats = hardwareMonitor.getSystemStats()
                    val fps = fpsEstimator.getCurrentFps()
                    statsFlow.value = stats
                    fpsFlow.value = fps
                    cpuHistoryFlow.value = cpuHistoryFlow.value.drop(1) + (stats.rawCpuUsagePercent ?: 0f)
                    ramHistoryFlow.value = ramHistoryFlow.value.drop(1) + stats.ramUsagePercent
                    musicPlayer.updateProgress()
                } catch (_: Exception) {}
                val interval = activeProfileFlow.value?.monitorIntervalMs ?: 1000L
                delay(interval.coerceIn(500L, 5000L))
            }
        }
    }

    private fun pauseTelemetryLoop() {
        monitoringJob?.cancel()
        monitoringJob = null
        try {
            fpsEstimator.stop()
        } catch (_: Exception) {}
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, XsdBoostApplication.OVERLAY_CHANNEL_ID)
            .setContentTitle("XSD BOOST Smart Game Watcher")
            .setContentText("Aktif otomatis hanya saat game target berjalan di foreground")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(0, "Nonaktifkan", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun overlayLayoutType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun getStatusBarHeightPx(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else (24 * resources.displayMetrics.density).toInt()
    }

    private fun triggerLightHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18L)
            }
        } catch (_: Exception) {}
    }

    private fun removeHandleView() {
        handleView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
            } catch (_: Exception) {}
        }
        handleView = null
        handleParams = null
    }

    private fun removePanelView() {
        panelView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
            } catch (_: Exception) {}
        }
        panelView = null
        panelParams = null
    }

    private fun removeDynamicIslandView() {
        islandView?.let { view ->
            try {
                windowManager?.removeViewImmediate(view)
            } catch (_: Exception) {}
        }
        islandView = null
        islandParams = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (panelView != null) {
            openBoosterPanel()
        } else if (handleView != null) {
            activeProfileFlow.value?.let { updateEdgeHandleLayout(it) }
        }
        if (islandView != null) {
            attachDynamicIslandView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        foregroundWatcherJob?.cancel()
        activeProfileJob?.cancel()
        pauseTelemetryLoop()
        mainHandler.removeCallbacksAndMessages(null)
        serviceJob.cancel()
        removePanelView()
        removeHandleView()
        removeDynamicIslandView()
        container.activeGameCrosshairConfig.value = null
        FloatingCrosshairService.stop(this)
        lifecycleOwner.destroy()
    }
}
