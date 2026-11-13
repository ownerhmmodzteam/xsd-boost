package com.example.ui.viewmodels

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.XsdBoostApplication
import com.example.core.database.GameProfileEntity
import com.example.core.database.SensitivityPresetEntity
import com.example.core.system.booster.RamAnalysisReport
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.fps.FpsEstimator
import com.example.core.system.games.GameApp
import com.example.core.system.games.GameDetectionForegroundService
import com.example.core.system.games.GameDetector
import com.example.core.system.graphics.GraphicsHardwareInfo
import com.example.core.system.graphics.GraphicsPresetProfile
import com.example.core.system.hardware.SystemStats
import com.example.core.system.overlay.EdgeSide
import com.example.core.system.overlay.FloatingCrosshairService
import com.example.core.system.overlay.FloatingOverlayService
import com.example.core.system.prefs.EdgeSwipeConfig
import com.example.core.system.scanner.AppSecurityReport
import com.example.core.system.scanner.AppSecurityScanner
import com.example.core.system.sensitivity.InputCompatibilityLayer
import com.example.core.system.sensitivity.InputCompatibilityReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as XsdBoostApplication).container
    private val hardwareMonitor = container.hardwareMonitor
    private val fpsEstimator = FpsEstimator()
    private val gameDetector = GameDetector(application)
    private val scanner = AppSecurityScanner(application)
    private val graphicsEngine = container.graphicsEngine
    private val ramBooster = container.ramBooster
    private val gameProfileRepo = container.gameProfileRepo
    private val foregroundMonitor = container.foregroundMonitor
    val musicPlayer = container.musicPlayer
    val prefsRepo = container.prefsRepo
    val shizukuBridge = container.shizukuBridge

    val shizukuStatus = shizukuBridge.status

    // Telemetry State
    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()

    private val _currentFps = MutableStateFlow(60f)
    val currentFps: StateFlow<Float> = _currentFps.asStateFlow()

    // History for Sparklines
    private val _cpuHistory = MutableStateFlow(List(16) { 0f })
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _ramHistory = MutableStateFlow(List(16) { 50f })
    val ramHistory: StateFlow<List<Float>> = _ramHistory.asStateFlow()

    private val _tempHistory = MutableStateFlow(List(16) { 34f })
    val tempHistory: StateFlow<List<Float>> = _tempHistory.asStateFlow()

    private val _fpsHistory = MutableStateFlow(List(16) { 60f })
    val fpsHistory: StateFlow<List<Float>> = _fpsHistory.asStateFlow()

    // Games & Per-Game Profiles
    private val _games = MutableStateFlow<List<GameApp>>(emptyList())
    val games: StateFlow<List<GameApp>> = _games.asStateFlow()

    val gameProfiles: StateFlow<Map<String, GameProfileEntity>> = gameProfileRepo.allProfiles
        .map { list -> list.associateBy { it.packageName } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val sensitivityPresets: StateFlow<List<SensitivityPresetEntity>> =
        (gameProfileRepo.sensitivityPresets ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _hasUsageStatsPermission = MutableStateFlow(foregroundMonitor.hasUsageStatsPermission())
    val hasUsageStatsPermission: StateFlow<Boolean> = _hasUsageStatsPermission.asStateFlow()

    private val _inputCompatibilityReport = MutableStateFlow(
        InputCompatibilityLayer.inspect(application, shizukuStatus.value)
    )
    val inputCompatibilityReport: StateFlow<InputCompatibilityReport> = _inputCompatibilityReport.asStateFlow()

    // Booster State
    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    private val _lastBoosterReport = MutableStateFlow<RamAnalysisReport?>(null)
    val lastBoosterReport: StateFlow<RamAnalysisReport?> = _lastBoosterReport.asStateFlow()

    // Scanner State
    private val _scanReports = MutableStateFlow<List<AppSecurityReport>>(emptyList())
    val scanReports: StateFlow<List<AppSecurityReport>> = _scanReports.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Graphics Info
    private val _graphicsInfo = MutableStateFlow(graphicsEngine.getHardwareInfo())
    val graphicsInfo: StateFlow<GraphicsHardwareInfo> = _graphicsInfo.asStateFlow()

    val graphicsProfiles: List<GraphicsPresetProfile> = graphicsEngine.getPresetProfiles()

    val sensitivityX = prefsRepo.sensitivityX.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val sensitivityY = prefsRepo.sensitivityY.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val sensitivityBaselineX = prefsRepo.sensitivityBaselineX.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val sensitivityBaselineY = prefsRepo.sensitivityBaselineY.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val isOverlayEnabled = prefsRepo.isOverlayEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isCrosshairEnabled = prefsRepo.isCrosshairEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val overlayOpacity = prefsRepo.overlayOpacity.stateIn(viewModelScope, SharingStarted.Eagerly, 0.9f)
    val overlayCompact = prefsRepo.overlayCompact.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val edgeSwipeConfig = prefsRepo.edgeSwipeConfig.stateIn(viewModelScope, SharingStarted.Eagerly, EdgeSwipeConfig())
    val monitorIntervalMs = prefsRepo.monitorIntervalMs.stateIn(viewModelScope, SharingStarted.Eagerly, 1000L)
    val crosshairConfig = prefsRepo.crosshairConfig.stateIn(viewModelScope, SharingStarted.Eagerly, CrosshairConfig())
    val selectedGraphicsProfileId = prefsRepo.selectedGraphicsProfile.stateIn(viewModelScope, SharingStarted.Eagerly, "ultra_smooth")
    val isSmartWatcherEnabled = prefsRepo.isSmartWatcherEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isDefaultHandleHidden = prefsRepo.isDefaultHandleHidden.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val defaultOverlaySide = prefsRepo.defaultOverlaySide
        .map { EdgeSide.fromString(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EdgeSide.RIGHT)
    val defaultHandleSizeDp = prefsRepo.defaultHandleSizeDp.stateIn(viewModelScope, SharingStarted.Eagerly, 76f)
    val defaultHandleOpacity = prefsRepo.defaultHandleOpacity.stateIn(viewModelScope, SharingStarted.Eagerly, 0.90f)
    val defaultLongPressMs = prefsRepo.defaultLongPressMs.stateIn(viewModelScope, SharingStarted.Eagerly, 420L)

    val musicHistory = musicPlayer.historyList

    init {
        try {
            fpsEstimator.start()
        } catch (_: Exception) {}

        startTelemetryLoop()
        refreshInstalledGames()
        refreshPermissions()

        viewModelScope.launch {
            shizukuStatus.collect { status ->
                _inputCompatibilityReport.value = InputCompatibilityLayer.inspect(getApplication(), status)
            }
        }

        viewModelScope.launch {
            val enabledGames = gameProfileRepo.enabledPackageSet.first()
            val watcherOn = prefsRepo.isSmartWatcherEnabled.first()
            val app = getApplication<Application>()
            val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)
            if (watcherOn && canDraw && foregroundMonitor.hasUsageStatsPermission() && enabledGames.isNotEmpty()) {
                GameDetectionForegroundService.start(app)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            runApkSecurityScan()
        }
    }

    fun refreshPermissions() {
        _hasUsageStatsPermission.value = foregroundMonitor.hasUsageStatsPermission()
        _inputCompatibilityReport.value = InputCompatibilityLayer.inspect(getApplication(), shizukuStatus.value)
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val stats = hardwareMonitor.getSystemStats()
                    val fps = fpsEstimator.getCurrentFps()

                    _systemStats.value = stats
                    _currentFps.value = fps

                    _cpuHistory.value = _cpuHistory.value.drop(1) + (stats.rawCpuUsagePercent ?: 0f)
                    _ramHistory.value = _ramHistory.value.drop(1) + stats.ramUsagePercent
                    _tempHistory.value = _tempHistory.value.drop(1) + (stats.effectiveTempCelsius ?: 34f)
                    _fpsHistory.value = _fpsHistory.value.drop(1) + fps

                    musicPlayer.updateProgress()
                } catch (_: Exception) {}

                delay(monitorIntervalMs.value)
            }
        }
    }

    fun refreshInstalledGames() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favs = prefsRepo.favoriteGames.first()
                val detected = gameDetector.detectInstalledGames(favs)
                _games.value = detected
            } catch (_: Exception) {
                _games.value = emptyList()
            }
        }
    }

    fun toggleGameBooster(game: GameApp, enabled: Boolean) {
        viewModelScope.launch {
            val defaultHidden = prefsRepo.isDefaultHandleHidden.first()
            val defSide = prefsRepo.defaultOverlaySide.first()
            val defSize = prefsRepo.defaultHandleSizeDp.first()
            val defOp = prefsRepo.defaultHandleOpacity.first()
            val defLp = prefsRepo.defaultLongPressMs.first()
            val updated = gameProfileRepo.setBoosterEnabled(
                packageName = game.packageName,
                appName = game.appName,
                enabled = enabled,
                defaultOverlaySide = defSide,
                defaultHandleSizeDp = defSize,
                defaultHandleOpacity = defOp,
                defaultLongPressMs = defLp
            )
            if (enabled && defaultHidden && !updated.isHandleHidden) {
                gameProfileRepo.updateProfile(game.packageName, game.appName) {
                    it.copy(isHandleHidden = true)
                }
            }
            if (enabled) {
                val app = getApplication<Application>()
                val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)
                if (canDraw) {
                    prefsRepo.setOverlayEnabled(true)
                    GameDetectionForegroundService.start(app)
                }
            }
        }
    }

    fun updateGameSensitivity(game: GameApp, x: Int, y: Int) {
        viewModelScope.launch {
            gameProfileRepo.updateProfile(game.packageName, game.appName) {
                it.copy(sensitivityX = x.coerceIn(1, 26), sensitivityY = y.coerceIn(1, 26))
            }
        }
    }

    fun updateGameOverlayConfig(
        game: GameApp,
        side: EdgeSide,
        handleSizeDp: Float,
        handleOpacity: Float,
        longPressMs: Long
    ) {
        viewModelScope.launch {
            gameProfileRepo.updateProfile(game.packageName, game.appName) {
                it.copy(
                    overlaySide = side.storageValue,
                    handleSizeDp = handleSizeDp.coerceIn(56f, 120f),
                    handleOpacity = handleOpacity.coerceIn(0.25f, 1.0f),
                    longPressDurationMs = longPressMs.coerceIn(250L, 800L)
                )
            }
        }
    }

    fun setGameHandleHidden(game: GameApp, hidden: Boolean) {
        viewModelScope.launch {
            gameProfileRepo.updateProfile(game.packageName, game.appName) {
                it.copy(isHandleHidden = hidden)
            }
        }
    }

    fun restoreGameHandle(game: GameApp) {
        viewModelScope.launch {
            gameProfileRepo.updateProfile(game.packageName, game.appName) {
                it.copy(isHandleHidden = false, handleYFraction = 0.42f, overlayEnabled = true)
            }
        }
    }

    fun setGameDynamicIslandEnabled(game: GameApp, enabled: Boolean) {
        viewModelScope.launch {
            gameProfileRepo.updateProfile(game.packageName, game.appName) {
                it.copy(dynamicIslandEnabled = enabled)
            }
        }
    }

    fun saveAndLoadGameMusicUrl(game: GameApp, url: String) {
        val trimmed = url.trim()
        musicPlayer.loadUrl(url = trimmed, gamePackageName = game.packageName, autoPlay = false)
        viewModelScope.launch {
            gameProfileRepo.updateProfile(game.packageName, game.appName) {
                it.copy(
                    musicUrl = trimmed,
                    musicTitle = musicPlayer.state.value.currentTitle
                )
            }
        }
    }

    fun resetGameProfile(packageName: String) {
        viewModelScope.launch {
            gameProfileRepo.resetProfile(packageName)
        }
    }

    fun restoreAllHiddenHandles() {
        viewModelScope.launch {
            val all = gameProfiles.value.values
            all.filter { it.isHandleHidden }.forEach { profile ->
                gameProfileRepo.updateProfile(profile.packageName, profile.appName) {
                    it.copy(isHandleHidden = false, handleYFraction = 0.42f)
                }
            }
            prefsRepo.setDefaultHandleHidden(false)
        }
    }

    fun launchGame(packageName: String): Boolean {
        val app = getApplication<Application>()
        val gameName = _games.value.firstOrNull { it.packageName == packageName }?.appName ?: packageName
        viewModelScope.launch {
            gameProfileRepo.setBoosterEnabled(
                packageName = packageName,
                appName = gameName,
                enabled = true,
                defaultOverlaySide = prefsRepo.defaultOverlaySide.first(),
                defaultHandleSizeDp = prefsRepo.defaultHandleSizeDp.first(),
                defaultHandleOpacity = prefsRepo.defaultHandleOpacity.first(),
                defaultLongPressMs = prefsRepo.defaultLongPressMs.first()
            )
            val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)
            if (canDraw) {
                prefsRepo.setOverlayEnabled(true)
                GameDetectionForegroundService.start(app)
            }
        }
        return gameDetector.launchGame(packageName)
    }

    fun toggleFavoriteGame(packageName: String) {
        viewModelScope.launch {
            prefsRepo.toggleFavoriteGame(packageName)
            refreshInstalledGames()
        }
    }

    fun runSmartBoost() {
        if (_isBoosting.value) return
        viewModelScope.launch {
            _isBoosting.value = true
            delay(1200)
            val report = withContext(Dispatchers.IO) {
                ramBooster.executeSmartBoost()
            }
            _lastBoosterReport.value = report
            _isBoosting.value = false
            _systemStats.value = withContext(Dispatchers.IO) { hardwareMonitor.getSystemStats() }
        }
    }

    fun runApkSecurityScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            val reports = withContext(Dispatchers.IO) {
                try {
                    scanner.scanInstalledApps()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            _scanReports.value = reports
            _isScanning.value = false
        }
    }

    fun setSensitivity(x: Int, y: Int) {
        viewModelScope.launch {
            prefsRepo.setSensitivity(x, y)
        }
    }

    fun saveSensitivityPreset(name: String, x: Int, y: Int) {
        viewModelScope.launch {
            gameProfileRepo.saveSensitivityPreset(name, x, y)
            prefsRepo.setSensitivityBaseline(x, y)
        }
    }

    fun loadSensitivityPreset(preset: SensitivityPresetEntity) {
        viewModelScope.launch {
            prefsRepo.setSensitivity(preset.sensitivityX, preset.sensitivityY)
            prefsRepo.setSensitivityBaseline(preset.sensitivityX, preset.sensitivityY)
        }
    }

    fun deleteSensitivityPreset(id: Long) {
        viewModelScope.launch {
            gameProfileRepo.deleteSensitivityPreset(id)
        }
    }

    fun saveCrosshairConfig(config: CrosshairConfig) {
        viewModelScope.launch {
            prefsRepo.saveCrosshairConfig(config)
        }
    }

    fun toggleOverlay(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setOverlayEnabled(enabled)
            prefsRepo.setSmartWatcherEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                GameDetectionForegroundService.start(app)
            } else {
                GameDetectionForegroundService.stop(app)
                FloatingOverlayService.stop(app)
            }
        }
    }

    fun openInGamePanelPreview() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)
            if (canDraw) {
                prefsRepo.setOverlayEnabled(true)
                FloatingOverlayService.start(app, openPanelImmediately = true)
            }
        }
    }

    fun toggleCrosshairOverlay(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setCrosshairEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                FloatingCrosshairService.start(app)
            } else {
                FloatingCrosshairService.stop(app)
            }
        }
    }

    fun setOverlayOpacity(opacity: Float) {
        viewModelScope.launch {
            prefsRepo.setOverlayOpacity(opacity)
        }
    }

    fun setOverlayCompact(compact: Boolean) {
        viewModelScope.launch {
            prefsRepo.setOverlayCompact(compact)
        }
    }

    fun setDefaultHandleHidden(hidden: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDefaultHandleHidden(hidden)
        }
    }

    fun setDefaultOverlaySide(side: EdgeSide) {
        viewModelScope.launch {
            prefsRepo.setDefaultOverlaySide(side.storageValue)
        }
    }

    fun setDefaultHandleSizeDp(sizeDp: Float) {
        viewModelScope.launch {
            prefsRepo.setDefaultHandleSizeDp(sizeDp)
        }
    }

    fun setDefaultHandleOpacity(opacity: Float) {
        viewModelScope.launch {
            prefsRepo.setDefaultHandleOpacity(opacity)
        }
    }

    fun setDefaultLongPressMs(durationMs: Long) {
        viewModelScope.launch {
            prefsRepo.setDefaultLongPressMs(durationMs)
        }
    }

    fun setEdgeSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setEdgeSwipeEnabled(enabled)
        }
    }

    fun setEdgeSwipeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            prefsRepo.setEdgeSwipeSensitivity(sensitivity)
        }
    }

    fun setEdgeTriggerYFraction(fraction: Float) {
        viewModelScope.launch {
            prefsRepo.setEdgeTriggerYFraction(fraction)
        }
    }

    fun setEdgeTriggerSize(widthDp: Float, heightDp: Float) {
        viewModelScope.launch {
            prefsRepo.setEdgeTriggerSize(widthDp, heightDp)
        }
    }

    fun setMonitorIntervalMs(intervalMs: Long) {
        viewModelScope.launch {
            prefsRepo.setMonitorIntervalMs(intervalMs)
        }
    }

    fun setGraphicsProfile(profileId: String) {
        viewModelScope.launch {
            prefsRepo.setGraphicsProfile(profileId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            fpsEstimator.stop()
        } catch (_: Exception) {}
    }
}
