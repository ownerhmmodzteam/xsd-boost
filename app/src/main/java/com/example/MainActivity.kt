package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.BottomNavTab
import com.example.ui.components.DynamicIsland
import com.example.ui.components.FloatingBottomNavigation
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeveloperProfileScreen
import com.example.ui.screens.GameCenterScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.PermissionsCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.MainViewModel

enum class AppSubScreen {
    MAIN_NAV,
    DEVELOPER_PROFILE,
    PERMISSIONS_CENTER
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var subScreen by remember { mutableStateOf(AppSubScreen.MAIN_NAV) }
    val context = LocalContext.current

    val stats by viewModel.systemStats.collectAsState()
    val fps by viewModel.currentFps.collectAsState()
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val ramHistory by viewModel.ramHistory.collectAsState()
    val tempHistory by viewModel.tempHistory.collectAsState()
    val fpsHistory by viewModel.fpsHistory.collectAsState()

    val games by viewModel.games.collectAsState()
    val gameProfiles by viewModel.gameProfiles.collectAsState()
    val hasUsageStatsPermission by viewModel.hasUsageStatsPermission.collectAsState()
    val isBoosting by viewModel.isBoosting.collectAsState()
    val lastBoosterReport by viewModel.lastBoosterReport.collectAsState()
    val scanReports by viewModel.scanReports.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val graphicsInfo by viewModel.graphicsInfo.collectAsState()
    val selectedGraphicsProfileId by viewModel.selectedGraphicsProfileId.collectAsState()

    val sensitivityX by viewModel.sensitivityX.collectAsState()
    val sensitivityY by viewModel.sensitivityY.collectAsState()
    val sensitivityBaselineX by viewModel.sensitivityBaselineX.collectAsState()
    val sensitivityBaselineY by viewModel.sensitivityBaselineY.collectAsState()
    val sensitivityPresets by viewModel.sensitivityPresets.collectAsState()
    val inputCompatibilityReport by viewModel.inputCompatibilityReport.collectAsState()

    val crosshairConfig by viewModel.crosshairConfig.collectAsState()
    val isCrosshairOverlayActive by viewModel.isCrosshairEnabled.collectAsState()
    val isOverlayActive by viewModel.isOverlayEnabled.collectAsState()
    val overlayOpacity by viewModel.overlayOpacity.collectAsState()
    val isDefaultHandleHidden by viewModel.isDefaultHandleHidden.collectAsState()
    val defaultOverlaySide by viewModel.defaultOverlaySide.collectAsState()
    val defaultHandleSizeDp by viewModel.defaultHandleSizeDp.collectAsState()
    val defaultHandleOpacity by viewModel.defaultHandleOpacity.collectAsState()
    val defaultLongPressMs by viewModel.defaultLongPressMs.collectAsState()
    val monitorIntervalMs by viewModel.monitorIntervalMs.collectAsState()

    val musicState by viewModel.musicPlayer.state.collectAsState()
    val musicHistory by viewModel.musicHistory.collectAsState()

    BackHandler(enabled = subScreen != AppSubScreen.MAIN_NAV) {
        subScreen = AppSubScreen.MAIN_NAV
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Global Dynamic Island Header
                DynamicIsland(
                    stats = stats,
                    fps = fps,
                    musicState = musicState,
                    musicHistory = musicHistory,
                    onMusicPlay = { viewModel.musicPlayer.play() },
                    onMusicPause = { viewModel.musicPlayer.pause() },
                    onMusicStop = { viewModel.musicPlayer.stopPlayback() },
                    onMusicPrevious = { viewModel.musicPlayer.skipToPrevious() },
                    onMusicNext = { viewModel.musicPlayer.skipToNext() },
                    onMusicPlayPause = { viewModel.musicPlayer.togglePlayPause() },
                    onMusicRepeat = { viewModel.musicPlayer.toggleRepeat() },
                    onMusicVolumeChange = { vol -> viewModel.musicPlayer.setVolume(vol) },
                    onPlayUrl = { url -> viewModel.musicPlayer.loadUrl(url, autoPlay = false) },
                    onPlayLocalUri = { uri, title -> viewModel.musicPlayer.playLocalUri(uri, title) },
                    onPlayHistoryItem = { item ->
                        viewModel.musicPlayer.playUrl(item.url, item.title, saveToHistory = false)
                    },
                    onOpenYouTube = { ytUrl -> viewModel.musicPlayer.openInYouTube(context, ytUrl) },
                    onOpenSpotify = { spUrl -> viewModel.musicPlayer.openInSpotify(context, spUrl) },
                    boostStatusMessage = if (isBoosting) "COMPACTING HEAP & OPTIMIZING..." else null
                )

                // Subscreen vs Tab Navigation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (subScreen) {
                        AppSubScreen.DEVELOPER_PROFILE -> {
                            DeveloperProfileScreen(
                                onBack = { subScreen = AppSubScreen.MAIN_NAV }
                            )
                        }
                        AppSubScreen.PERMISSIONS_CENTER -> {
                            PermissionsCenterScreen(
                                onBack = { subScreen = AppSubScreen.MAIN_NAV }
                            )
                        }
                        AppSubScreen.MAIN_NAV -> {
                            when (selectedTab) {
                                BottomNavTab.HOME -> {
                                    DashboardScreen(
                                        stats = stats,
                                        fps = fps,
                                        cpuHistory = cpuHistory,
                                        ramHistory = ramHistory,
                                        tempHistory = tempHistory,
                                        fpsHistory = fpsHistory,
                                        onSmartBoostClick = { viewModel.runSmartBoost() }
                                    )
                                }
                                BottomNavTab.GAMES -> {
                                    GameCenterScreen(
                                        games = games,
                                        gameProfiles = gameProfiles,
                                        hasUsageStatsPermission = hasUsageStatsPermission,
                                        onRefreshPermissions = { viewModel.refreshPermissions() },
                                        onLaunchGame = { pkg -> viewModel.launchGame(pkg) },
                                        onToggleFavorite = { pkg -> viewModel.toggleFavoriteGame(pkg) },
                                        onToggleGameBooster = { g, en -> viewModel.toggleGameBooster(g, en) },
                                        onUpdateGameSensitivity = { g, x, y -> viewModel.updateGameSensitivity(g, x, y) },
                                        onUpdateGameOverlayConfig = { g, side, sizeDp, op, lpMs ->
                                            viewModel.updateGameOverlayConfig(g, side, sizeDp, op, lpMs)
                                        },
                                        onSaveGameMusicUrl = { g, url -> viewModel.saveAndLoadGameMusicUrl(g, url) },
                                        onToggleGameDynamicIsland = { g, en -> viewModel.setGameDynamicIslandEnabled(g, en) },
                                        onSetGameHandleHidden = { g, hidden -> viewModel.setGameHandleHidden(g, hidden) },
                                        onRestoreGameHandle = { g -> viewModel.restoreGameHandle(g) },
                                        onResetGameProfile = { pkg -> viewModel.resetGameProfile(pkg) }
                                    )
                                }
                                BottomNavTab.TOOLS -> {
                                    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
                                    ToolsScreen(
                                        lastBoosterReport = lastBoosterReport,
                                        isBoosting = isBoosting,
                                        onRunSmartBoost = { viewModel.runSmartBoost() },
                                        crosshairConfig = crosshairConfig,
                                        isCrosshairOverlayActive = isCrosshairOverlayActive,
                                        onSaveCrosshairConfig = { cfg -> viewModel.saveCrosshairConfig(cfg) },
                                        onToggleCrosshairOverlay = { en -> viewModel.toggleCrosshairOverlay(en) },
                                        sensitivityX = sensitivityX,
                                        sensitivityY = sensitivityY,
                                        sensitivityBaselineX = sensitivityBaselineX,
                                        sensitivityBaselineY = sensitivityBaselineY,
                                        sensitivityPresets = sensitivityPresets,
                                        inputCompatibilityReport = inputCompatibilityReport,
                                        onSaveSensitivity = { x, y -> viewModel.setSensitivity(x, y) },
                                        onSaveSensitivityPreset = { name, x, y -> viewModel.saveSensitivityPreset(name, x, y) },
                                        onLoadSensitivityPreset = { preset -> viewModel.loadSensitivityPreset(preset) },
                                        onDeleteSensitivityPreset = { id -> viewModel.deleteSensitivityPreset(id) },
                                        scanReports = scanReports,
                                        isScanning = isScanning,
                                        onRunSecurityScan = { viewModel.runApkSecurityScan() },
                                        graphicsInfo = graphicsInfo,
                                        graphicsProfiles = viewModel.graphicsProfiles,
                                        selectedGraphicsProfileId = selectedGraphicsProfileId,
                                        onSelectGraphicsProfile = { id -> viewModel.setGraphicsProfile(id) },
                                        shizukuStatus = shizukuStatus,
                                        shizukuBridge = viewModel.shizukuBridge
                                    )
                                }
                                BottomNavTab.MONITOR -> {
                                    MonitorScreen(
                                        stats = stats,
                                        fps = fps,
                                        isOverlayActive = isOverlayActive,
                                        hasUsageStatsPermission = hasUsageStatsPermission,
                                        overlayOpacity = overlayOpacity,
                                        isDefaultHandleHidden = isDefaultHandleHidden,
                                        defaultOverlaySide = defaultOverlaySide,
                                        defaultHandleSizeDp = defaultHandleSizeDp,
                                        defaultHandleOpacity = defaultHandleOpacity,
                                        defaultLongPressMs = defaultLongPressMs,
                                        monitorIntervalMs = monitorIntervalMs,
                                        onRefreshPermissions = { viewModel.refreshPermissions() },
                                        onToggleOverlay = { en -> viewModel.toggleOverlay(en) },
                                        onOpenPanelPreview = { viewModel.openInGamePanelPreview() },
                                        onUpdateOpacity = { op -> viewModel.setOverlayOpacity(op) },
                                        onToggleDefaultHandleHidden = { hidden -> viewModel.setDefaultHandleHidden(hidden) },
                                        onUpdateDefaultOverlaySide = { side -> viewModel.setDefaultOverlaySide(side) },
                                        onUpdateDefaultHandleSizeDp = { sz -> viewModel.setDefaultHandleSizeDp(sz) },
                                        onUpdateDefaultHandleOpacity = { op -> viewModel.setDefaultHandleOpacity(op) },
                                        onUpdateDefaultLongPressMs = { lp -> viewModel.setDefaultLongPressMs(lp) },
                                        onRestoreAllHiddenHandles = { viewModel.restoreAllHiddenHandles() },
                                        onUpdateMonitorInterval = { ms -> viewModel.setMonitorIntervalMs(ms) }
                                    )
                                }
                                BottomNavTab.SETTINGS -> {
                                    SettingsScreen(
                                        onNavigateToDeveloperProfile = { subScreen = AppSubScreen.DEVELOPER_PROFILE },
                                        onNavigateToPermissions = { subScreen = AppSubScreen.PERMISSIONS_CENTER }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (subScreen == AppSubScreen.MAIN_NAV) {
                FloatingBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
        }
    }
}
