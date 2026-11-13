package com.example.core.system.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ScreenshotFeedback
import com.example.core.database.MusicHistoryEntity
import com.example.core.database.SensitivityPresetEntity
import com.example.core.system.booster.RamAnalysisReport
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.graphics.GraphicsHardwareInfo
import com.example.core.system.graphics.GraphicsPresetProfile
import com.example.core.system.hardware.SystemStats
import com.example.core.system.media.MusicPlayerState
import com.example.core.system.sensitivity.InputCompatibilityReport
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

enum class OverlayToolTab(val label: String, val icon: ImageVector) {
    PERFORMANCE("Perf", Icons.Default.Speed),
    CROSSHAIR("Aim", Icons.Default.GpsFixed),
    SENSITIVITY("Sens", Icons.Default.Tune),
    GRAPHICS("GPU", Icons.Default.AutoAwesome),
    RAM_SYSTEM("RAM", Icons.Default.Memory),
    MUSIC("Music", Icons.Default.MusicNote),
    SCREENSHOT("Shot", Icons.Default.CameraAlt)
}

@Composable
fun InGameBoosterPanel(
    visible: Boolean,
    activeGameName: String,
    overlaySide: EdgeSide = EdgeSide.RIGHT,
    handleSizeDp: Float = 76f,
    handleOpacity: Float = 0.90f,
    longPressDurationMs: Long = 420L,
    selectedTab: OverlayToolTab,
    onSelectTab: (OverlayToolTab) -> Unit,
    panelWidthDp: Dp,
    panelMaxHeightDp: Dp,
    opacity: Float,
    stats: SystemStats,
    fps: Float,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    intervalMs: Long,
    isHandleHidden: Boolean,
    onChangeInterval: (Long) -> Unit,
    onChangeOverlaySide: (EdgeSide) -> Unit = {},
    onChangeHandleSize: (Float) -> Unit = {},
    onChangeHandleOpacity: (Float) -> Unit = {},
    onChangeLongPressMs: (Long) -> Unit = {},
    onToggleHandleHidden: (Boolean) -> Unit,
    onResetProfile: () -> Unit,
    crosshairEnabled: Boolean,
    crosshairConfig: CrosshairConfig,
    onToggleCrosshair: (Boolean) -> Unit,
    onUpdateCrosshair: (CrosshairConfig) -> Unit,
    sensX: Int,
    sensY: Int,
    sensitivityPresets: List<SensitivityPresetEntity> = emptyList(),
    compatibilityReport: InputCompatibilityReport? = null,
    onSaveSensitivity: (Int, Int) -> Unit,
    onSaveSensitivityPreset: (String, Int, Int) -> Unit = { _, _, _ -> },
    onDeleteSensitivityPreset: (Long) -> Unit = {},
    graphicsInfo: GraphicsHardwareInfo,
    graphicsProfiles: List<GraphicsPresetProfile>,
    selectedGraphicsProfileId: String,
    onSelectGraphicsProfile: (String) -> Unit,
    lastBoosterReport: RamAnalysisReport?,
    onCleanCache: () -> Unit,
    musicState: MusicPlayerState,
    savedMusicUrl: String,
    dynamicIslandEnabled: Boolean,
    musicHistory: List<MusicHistoryEntity>,
    onLoadMusicUrl: (String) -> Unit,
    onMusicPlay: () -> Unit,
    onMusicPause: () -> Unit,
    onMusicStop: () -> Unit,
    onMusicNext: () -> Unit,
    onMusicPrev: () -> Unit,
    onMusicRepeat: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMusicSeekTo: (Long) -> Unit,
    onToggleDynamicIsland: (Boolean) -> Unit,
    onPlayHistoryItem: (MusicHistoryEntity) -> Unit,
    onOpenYouTube: (String) -> Unit,
    onOpenSpotify: (String) -> Unit,
    onTextInputFocusChanged: (Boolean) -> Unit,
    screenshotFeedback: ScreenshotFeedback?,
    onCaptureScreenshot: () -> Unit,
    onMinimizeToHandle: () -> Unit,
    onCloseForSession: () -> Unit
) {
    var swipeDragSum by remember { mutableFloatStateOf(0f) }
    val isLeft = overlaySide == EdgeSide.LEFT

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(animationSpec = tween(200)) { if (isLeft) -it else it } + fadeIn(tween(160)),
        exit = slideOutHorizontally(animationSpec = tween(160)) { if (isLeft) -it else it } + fadeOut(tween(130))
    ) {
        val surfaceGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1F1F1F).copy(alpha = opacity.coerceIn(0.68f, 0.96f)),
                Color(0xFF070707).copy(alpha = opacity.coerceIn(0.78f, 0.98f))
            )
        )

        Box(
            modifier = Modifier
                .width(panelWidthDp)
                .heightIn(max = panelMaxHeightDp)
                .clip(RoundedCornerShape(22.dp))
                .background(surfaceGradient)
                .border(1.dp, BorderActive, RoundedCornerShape(22.dp))
                .pointerInput(overlaySide) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeDragSum = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeDragSum += dragAmount
                            val shouldMinimize = if (isLeft) swipeDragSum < -85f else swipeDragSum > 85f
                            if (shouldMinimize) {
                                swipeDragSum = 0f
                                onMinimizeToHandle()
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Column {
                            Text(
                                text = "XSD BOOST",
                                color = PrimaryWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (activeGameName.isNotBlank()) "GAME BOOSTER ACTIVE • $activeGameName" else "GAME BOOSTER ACTIVE",
                                color = AccentGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMinimizeToHandle,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isLeft) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                                contentDescription = if (isLeft) "Minimize to left handle" else "Minimize to right handle",
                                tint = SecondaryWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onCloseForSession,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close overlay for session",
                                tint = MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Real-Time Dashboard Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg.copy(alpha = 0.9f))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DashboardMetricCell(
                                label = "HUD FPS",
                                value = "${fps.toInt()}",
                                valueColor = PrimaryWhite,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "GAME FPS",
                                value = "N/A",
                                valueColor = AccentAmber,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "REFRESH",
                                value = "${stats.refreshRate.toInt()}Hz",
                                valueColor = PrimaryWhite,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DashboardMetricCell(
                                label = "CPU",
                                value = stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "N/A",
                                valueColor = if (stats.isCpuMeasurementAvailable) PrimaryWhite else AccentAmber,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "RAM",
                                value = "${stats.ramUsagePercent.toInt()}%",
                                valueColor = PrimaryWhite,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "TEMP",
                                value = stats.effectiveTempCelsius?.let { "${it.toInt()}°C" } ?: "N/A",
                                valueColor = if (stats.effectiveTempCelsius != null) PrimaryWhite else AccentAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 7 Quick Game Tools Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OverlayToolTab.entries.forEach { tab ->
                        val active = selectedTab == tab
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) PrimaryWhite else ElevatedSurface)
                                .border(1.dp, if (active) PrimaryWhite else BorderColor, RoundedCornerShape(10.dp))
                                .clickable { onSelectTab(tab) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (active) BgPrimary else SecondaryWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.label,
                                color = if (active) BgPrimary else SecondaryWhite,
                                fontSize = 9.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Active Tool Content Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        OverlayToolTab.PERFORMANCE -> OverlayPerfMonitorTool(
                            stats = stats,
                            fps = fps,
                            cpuHistory = cpuHistory,
                            ramHistory = ramHistory,
                            intervalMs = intervalMs,
                            overlaySide = overlaySide,
                            handleSizeDp = handleSizeDp,
                            handleOpacity = handleOpacity,
                            longPressDurationMs = longPressDurationMs,
                            isHandleHidden = isHandleHidden,
                            onChangeInterval = onChangeInterval,
                            onChangeOverlaySide = onChangeOverlaySide,
                            onChangeHandleSize = onChangeHandleSize,
                            onChangeHandleOpacity = onChangeHandleOpacity,
                            onChangeLongPressMs = onChangeLongPressMs,
                            onToggleHandleHidden = onToggleHandleHidden,
                            onResetProfile = onResetProfile
                        )
                        OverlayToolTab.CROSSHAIR -> OverlayCrosshairTool(
                            enabled = crosshairEnabled,
                            config = crosshairConfig,
                            onToggleCrosshair = onToggleCrosshair,
                            onUpdateConfig = onUpdateCrosshair
                        )
                        OverlayToolTab.SENSITIVITY -> OverlaySensitivityTool(
                            sensX = sensX,
                            sensY = sensY,
                            savedPresets = sensitivityPresets,
                            compatibilityReport = compatibilityReport,
                            onSaveSensitivity = onSaveSensitivity,
                            onSavePreset = onSaveSensitivityPreset,
                            onDeletePreset = onDeleteSensitivityPreset
                        )
                        OverlayToolTab.GRAPHICS -> OverlayGraphicsTool(
                            hardwareInfo = graphicsInfo,
                            profiles = graphicsProfiles,
                            selectedProfileId = selectedGraphicsProfileId,
                            onSelectProfile = onSelectGraphicsProfile
                        )
                        OverlayToolTab.RAM_SYSTEM -> OverlayRamSystemTool(
                            stats = stats,
                            lastReport = lastBoosterReport,
                            onCleanCache = onCleanCache
                        )
                        OverlayToolTab.MUSIC -> OverlayMusicPlayerTool(
                            musicState = musicState,
                            savedUrl = savedMusicUrl,
                            dynamicIslandEnabled = dynamicIslandEnabled,
                            history = musicHistory,
                            onLoadUrl = onLoadMusicUrl,
                            onPlay = onMusicPlay,
                            onPause = onMusicPause,
                            onStop = onMusicStop,
                            onNext = onMusicNext,
                            onPrevious = onMusicPrev,
                            onToggleRepeat = onMusicRepeat,
                            onVolumeChange = onMusicVolumeChange,
                            onSeekTo = onMusicSeekTo,
                            onToggleDynamicIsland = onToggleDynamicIsland,
                            onPlayHistoryItem = onPlayHistoryItem,
                            onOpenYouTube = onOpenYouTube,
                            onOpenSpotify = onOpenSpotify,
                            onTextInputFocusChanged = onTextInputFocusChanged
                        )
                        OverlayToolTab.SCREENSHOT -> OverlayScreenshotTool(
                            feedback = screenshotFeedback,
                            onCaptureScreenshot = onCaptureScreenshot
                        )
                    }
                }

                Text(
                    text = if (isLeft) {
                        "Geser panel ke kiri atau ketuk luar panel untuk kembali ke game"
                    } else {
                        "Geser panel ke kanan atau ketuk luar panel untuk kembali ke game"
                    },
                    color = MutedText,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = MutedText,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
