package com.example.core.system.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ScreenshotFeedback
import com.example.core.database.MusicHistoryEntity
import com.example.core.database.SensitivityPresetEntity
import com.example.core.system.booster.RamAnalysisReport
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.crosshair.CrosshairStyle
import com.example.core.system.graphics.GraphicsHardwareInfo
import com.example.core.system.graphics.GraphicsPresetProfile
import com.example.core.system.hardware.SystemStats
import com.example.core.system.media.MusicPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.core.system.sensitivity.InputCompatibilityLayer
import com.example.core.system.sensitivity.InputCompatibilityReport
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun OverlayPerfMonitorTool(
    stats: SystemStats,
    fps: Float,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    intervalMs: Long,
    overlaySide: EdgeSide,
    handleSizeDp: Float,
    handleOpacity: Float,
    longPressDurationMs: Long,
    isHandleHidden: Boolean,
    onChangeInterval: (Long) -> Unit,
    onChangeOverlaySide: (EdgeSide) -> Unit,
    onChangeHandleSize: (Float) -> Unit,
    onChangeHandleOpacity: (Float) -> Unit,
    onChangeLongPressMs: (Long) -> Unit,
    onToggleHandleHidden: (Boolean) -> Unit,
    onResetProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "PERFORMANCE MONITOR",
            color = PrimaryWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniOverlayGraph(
                label = "CPU LOAD",
                valueText = stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "N/A",
                history = cpuHistory,
                isAvailable = stats.isCpuMeasurementAvailable,
                modifier = Modifier.weight(1f)
            )
            MiniOverlayGraph(
                label = "RAM LOAD",
                valueText = "${stats.ramUsagePercent.toInt()}%",
                history = ramHistory,
                isAvailable = true,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OverlayInfoRow("Refresh Rate", "${stats.refreshRate.toInt()} Hz")
                OverlayInfoRow("Compositor FPS (App HUD)", "${fps.toInt()} FPS")
                OverlayInfoRow("Foreground Game FPS", "N/A (Android Sandbox)")
                OverlayInfoRow(
                    "Thermal Sensor",
                    stats.effectiveTempCelsius?.let { "${it.toInt()}°C" } ?: "N/A"
                )
            }
        }

        // Overlay Position (Left Edge / Right Edge) + Handle Customization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OVERLAY POSITION & HANDLE",
                    color = PrimaryWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EdgeSide.entries.forEach { side ->
                        val selected = overlaySide == side
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrimaryWhite else ElevatedSurface)
                                .border(1.dp, if (selected) PrimaryWhite else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { onChangeOverlaySide(side) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = side.label,
                                color = if (selected) BgPrimary else SecondaryWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OverlayInfoRow("Ukuran Handle", "${handleSizeDp.toInt()} dp")
                Slider(
                    value = handleSizeDp,
                    onValueChange = onChangeHandleSize,
                    valueRange = 56f..120f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )

                OverlayInfoRow("Transparansi Handle", "${(handleOpacity * 100).toInt()}%")
                Slider(
                    value = handleOpacity,
                    onValueChange = onChangeHandleOpacity,
                    valueRange = 0.25f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )

                OverlayInfoRow("Durasi Tahan (Long Press)", "${longPressDurationMs} ms")
                Slider(
                    value = longPressDurationMs.toFloat(),
                    onValueChange = { onChangeLongPressMs(it.toLong()) },
                    valueRange = 250f..800f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Polling Interval", color = MutedText, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1000L to "1s", 2000L to "2s", 3000L to "3s").forEach { (ms, label) ->
                    val selected = intervalMs == ms
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) PrimaryWhite else ElevatedSurface)
                            .clickable { onChangeInterval(ms) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) BgPrimary else SecondaryWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Hidden Handle Mode", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (overlaySide == EdgeSide.LEFT) {
                        "Garis tipis di tepi kiri (tahan lama untuk buka)"
                    } else {
                        "Garis tipis di tepi kanan (tahan lama untuk buka)"
                    },
                    color = MutedText,
                    fontSize = 9.sp
                )
            }
            Switch(
                checked = isHandleHidden,
                onCheckedChange = onToggleHandleHidden,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryWhite,
                    checkedTrackColor = ElevatedSurface,
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = ElevatedSurface
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ElevatedSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .clickable(onClick = onResetProfile)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Profile", tint = SecondaryWhite, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("RESET PROFIL GAME INI", color = SecondaryWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniOverlayGraph(
    label: String,
    valueText: String,
    history: List<Float>,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    valueText,
                    color = if (isAvailable) PrimaryWhite else AccentAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                if (history.size < 2) return@Canvas
                val linePath = Path()
                val fillPath = Path()
                val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                history.forEachIndexed { idx, value ->
                    val norm = (value / 100f).coerceIn(0f, 1f)
                    val x = idx * stepX
                    val y = size.height - (norm * size.height)
                    if (idx == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (idx == history.lastIndex) {
                        fillPath.lineTo(x, size.height)
                        fillPath.close()
                    }
                }
                val strokeColor = if (isAvailable) Color.White else Color(0xFF737373)
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(strokeColor.copy(alpha = 0.24f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = strokeColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun OverlayCrosshairTool(
    enabled: Boolean,
    config: CrosshairConfig,
    onToggleCrosshair: (Boolean) -> Unit,
    onUpdateConfig: (CrosshairConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "CROSSHAIR OVERLAY",
                    color = PrimaryWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    if (enabled) "Aktif di tengah layar" else "Nonaktif",
                    color = if (enabled) AccentGreen else MutedText,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggleCrosshair,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryWhite,
                    checkedTrackColor = ElevatedSurface,
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = ElevatedSurface
                )
            )
        }

        Text("Desain Crosshair", color = MutedText, fontSize = 10.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CrosshairStyle.entries.forEach { style ->
                val selected = config.style == style
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) PrimaryWhite else ElevatedSurface)
                        .clickable { onUpdateConfig(config.copy(style = style)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = style.title,
                        color = if (selected) BgPrimary else SecondaryWhite,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Text("Warna", color = MutedText, fontSize = 10.sp)
        val palette = listOf(0xFFFFFFFFL, 0xFF10B981L, 0xFF00E5FFL, 0xFFEF4444L, 0xFFF59E0BL)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            palette.forEach { argb ->
                val selected = config.colorArgb == argb
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) PrimaryWhite else BorderColor,
                            shape = CircleShape
                        )
                        .clickable { onUpdateConfig(config.copy(colorArgb = argb)) }
                )
            }
        }

        Column {
            OverlayInfoRow("Ukuran", "${config.sizeDp.toInt()} dp")
            Slider(
                value = config.sizeDp,
                onValueChange = { onUpdateConfig(config.copy(sizeDp = it)) },
                valueRange = 14f..56f,
                colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
            )
            OverlayInfoRow("Transparansi", "${(config.opacity * 100).toInt()}%")
            Slider(
                value = config.opacity,
                onValueChange = { onUpdateConfig(config.copy(opacity = it)) },
                valueRange = 0.25f..1.0f,
                colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
            )
        }
    }
}

@Composable
fun OverlaySensitivityTool(
    sensX: Int,
    sensY: Int,
    savedPresets: List<SensitivityPresetEntity> = emptyList(),
    compatibilityReport: InputCompatibilityReport? = null,
    onSaveSensitivity: (Int, Int) -> Unit,
    onSavePreset: (String, Int, Int) -> Unit = { _, _, _ -> },
    onDeletePreset: (Long) -> Unit = {}
) {
    var baselineX by remember { mutableIntStateOf(sensX) }
    var baselineY by remember { mutableIntStateOf(sensY) }
    var reticleOffsetX by remember { mutableFloatStateOf(0f) }
    var reticleOffsetY by remember { mutableFloatStateOf(0f) }
    var showPresets by remember { mutableStateOf(false) }

    val deltaX = sensX - baselineX
    val deltaY = sensY - baselineY
    val deltaXText = if (deltaX == 0) "0" else if (deltaX > 0) "+$deltaX" else "$deltaX"
    val deltaYText = if (deltaY == 0) "0" else if (deltaY > 0) "+$deltaY" else "$deltaY"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "RESPONSIVE SENSITIVITY (1 - 26)",
            color = PrimaryWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OverlayInfoRow("Sensitivity X", "$baselineX → $sensX ($deltaXText)")
                Slider(
                    value = sensX.toFloat(),
                    onValueChange = { onSaveSensitivity(it.toInt().coerceIn(1, 26), sensY) },
                    valueRange = 1f..26f,
                    steps = 24,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )
                OverlayInfoRow("Sensitivity Y", "$baselineY → $sensY ($deltaYText)")
                Slider(
                    value = sensY.toFloat(),
                    onValueChange = { onSaveSensitivity(sensX, it.toInt().coerceIn(1, 26)) },
                    valueRange = 1f..26f,
                    steps = 24,
                    colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                )
            }
        }

        // Save Profile / Load Profile / Reset Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    baselineX = sensX
                    baselineY = sensY
                    onSavePreset("Profil X$sensX Y$sensY", sensX, sensY)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Save", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showPresets = !showPresets },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = PrimaryWhite),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("Load (${savedPresets.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    onSaveSensitivity(1, 1)
                    reticleOffsetX = 0f
                    reticleOffsetY = 0f
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = SecondaryWhite),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("Reset 1/1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showPresets) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSurface)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (savedPresets.isEmpty()) {
                    Text("Belum ada preset tersimpan. Tekan Save untuk menyimpan.", color = MutedText, fontSize = 10.sp)
                } else {
                    savedPresets.take(4).forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardBg)
                                .clickable {
                                    baselineX = preset.sensitivityX
                                    baselineY = preset.sensitivityY
                                    onSaveSensitivity(preset.sensitivityX, preset.sensitivityY)
                                    showPresets = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${preset.name} (X:${preset.sensitivityX} Y:${preset.sensitivityY})",
                                color = PrimaryWhite,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { onDeletePreset(preset.id) }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete preset", tint = MutedText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Interactive Sensitivity Simulation Pad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INTERACTIVE SIMULATION PAD", color = PrimaryWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("X:$sensX Y:$sensY", color = AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF050505))
                    .border(1.dp, BorderActive, RoundedCornerShape(10.dp))
                    .pointerInput(sensX, sensY) {
                        detectDragGestures(
                            onDragEnd = {
                                reticleOffsetX = 0f
                                reticleOffsetY = 0f
                            },
                            onDragCancel = {
                                reticleOffsetX = 0f
                                reticleOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val (simDx, simDy) = InputCompatibilityLayer.computeSimulatedReticleDelta(
                                    dragAmount.x,
                                    dragAmount.y,
                                    sensX,
                                    sensY
                                )
                                val maxW = (size.width / 2f - 10f).coerceAtLeast(10f)
                                val maxH = (size.height / 2f - 10f).coerceAtLeast(10f)
                                reticleOffsetX = (reticleOffsetX + simDx).coerceIn(-maxW, maxW)
                                reticleOffsetY = (reticleOffsetY + simDy).coerceIn(-maxH, maxH)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(Color.White.copy(alpha = 0.14f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
                    drawLine(Color.White.copy(alpha = 0.14f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
                    val target = Offset(cx + reticleOffsetX, cy + reticleOffsetY)
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(cx, cy), target, strokeWidth = 1.5f)
                    drawCircle(Color.White, radius = 9f, center = target, style = Stroke(width = 2f))
                    drawCircle(AccentCyan, radius = 3.5f, center = target)
                }
            }
            Text(
                text = compatibilityReport?.activeTier?.title?.let { "Layer: $it • " }.orEmpty() +
                    "Simulasi visual lokal — tidak memodifikasi/menyuntikkan input internal Free Fire atau game lain.",
                color = MutedText,
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun OverlayGraphicsTool(
    hardwareInfo: GraphicsHardwareInfo,
    profiles: List<GraphicsPresetProfile>,
    selectedProfileId: String,
    onSelectProfile: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "GRAPHICS PROFILE",
            color = PrimaryWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                OverlayInfoRow("API", hardwareInfo.glVersion)
                OverlayInfoRow("Vulkan", if (hardwareInfo.vulkanSupported) "Supported" else "Unavailable")
                OverlayInfoRow("ANGLE Hook", hardwareInfo.angleStatus.substringBefore(" ("))
            }
        }

        profiles.forEach { profile ->
            val selected = profile.id == selectedProfileId
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) ElevatedSurface else CardBg)
                    .border(1.dp, if (selected) BorderActive else BorderColor, RoundedCornerShape(10.dp))
                    .clickable { onSelectProfile(profile.id) }
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = profile.name,
                            color = PrimaryWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile.targetFps} FPS",
                            color = if (selected) AccentGreen else MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${profile.renderScale} • Shadow: ${profile.shadowQuality}",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Text(
            text = "Preset ini bersifat panduan profil perangkat dan tidak memodifikasi driver GPU atau file APK/OBB game.",
            color = MutedText,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun OverlayRamSystemTool(
    stats: SystemStats,
    lastReport: RamAnalysisReport?,
    onCleanCache: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "RAM & SYSTEM INFO",
            color = PrimaryWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val usedMb = stats.ramUsedBytes / (1024 * 1024)
                val totalMb = stats.ramTotalBytes / (1024 * 1024)
                OverlayInfoRow("RAM Terpakai", "$usedMb / $totalMb MB (${stats.ramUsagePercent.toInt()}%)")
                LinearProgressIndicator(
                    progress = { (stats.ramUsagePercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryWhite,
                    trackColor = ElevatedSurface
                )

                val freeGb = String.format("%.1f", stats.storageAvailableBytes / (1024f * 1024f * 1024f))
                val totalGb = String.format("%.1f", stats.storageTotalBytes / (1024f * 1024f * 1024f))
                OverlayInfoRow("Storage Bebas", "$freeGb GB / $totalGb GB")
                OverlayInfoRow("Baterai", "${stats.batteryLevel}% ${if (stats.isCharging) "(Charging)" else ""}")
            }
        }

        Button(
            onClick = onCleanCache,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("BERSIHKAN CACHE XSD BOOST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (lastReport != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSurface)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Cache internal dibersihkan: ${lastReport.appCacheSizeBytes / 1024} KB • Free RAM: ${lastReport.initialFreeBytes / (1024 * 1024)} MB\n${lastReport.recommendation}",
                    color = SecondaryWhite,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun OverlayMusicPlayerTool(
    musicState: MusicPlayerState,
    savedUrl: String,
    dynamicIslandEnabled: Boolean,
    history: List<MusicHistoryEntity>,
    onLoadUrl: (String) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleDynamicIsland: (Boolean) -> Unit,
    onPlayHistoryItem: (MusicHistoryEntity) -> Unit,
    onOpenYouTube: (String) -> Unit,
    onOpenSpotify: (String) -> Unit,
    onTextInputFocusChanged: (Boolean) -> Unit,
    onPlayLocalUri: (Uri, String?) -> Unit = { _, _ -> }
) {
    val clipboard = LocalClipboardManager.current
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl.ifBlank { musicState.currentUrl }) }
    var showHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val musicScope = rememberCoroutineScope()
    var localMusic by remember { mutableStateOf<List<Pair<Uri, String>>>(emptyList()) }
    var localMusicStatus by remember { mutableStateOf("Folder /storage/emulated/0/music_hmmodz belum dipindai.") }
    var localMusicQuery by remember { mutableStateOf("") }
    var scanningLocalMusic by remember { mutableStateOf(false) }

    suspend fun scanDefaultMusicFolder() {
        scanningLocalMusic = true
        localMusicStatus = "Memindai folder musik..."
        val result = withContext(Dispatchers.IO) {
            val rows = mutableListOf<Pair<Uri, String>>()
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA
            )
            val allowed = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "3gp", "amr", "mka")
            val base = "/storage/emulated/0/music_hmmodz/"
            try {
                context.contentResolver.query(collection, projection, null, null,
                    "${MediaStore.Audio.Media.DISPLAY_NAME} COLLATE NOCASE ASC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol) ?: continue
                        val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (!path.isNullOrBlank() && path.startsWith(base, ignoreCase = true) && ext in allowed) {
                            val id = cursor.getLong(idCol)
                            rows.add(ContentUris.withAppendedId(collection, id) to name)
                        }
                    }
                }
            } catch (_: SecurityException) { }
              catch (_: Exception) { }
            rows
        }
        localMusic = result
        localMusicStatus = when {
            result.isNotEmpty() -> "${result.size} file audio ditemukan, termasuk subfolder."
            android.os.Build.VERSION.SDK_INT >= 33 &&
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED ->
                "Izin musik belum diberikan. Buka Pengaturan aplikasi lalu izinkan Musik dan audio."
            else -> "Tidak ada audio ditemukan di /storage/emulated/0/music_hmmodz. Periksa nama folder dan izin media."
        }
        scanningLocalMusic = false
    }

    LaunchedEffect(Unit) { scanDefaultMusicFolder() }

    LaunchedEffect(musicState.currentUrl) {
        if (musicState.currentUrl.isNotBlank() && urlInput.isBlank()) {
            urlInput = musicState.currentUrl
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "ADVANCED MUSIC PLAYER",
            color = PrimaryWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // URL Input row with exact placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = { Text("Paste music or media URL here...", fontSize = 10.sp, color = MutedText) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onTextInputFocusChanged(it.isFocused) }
                    .testTag("music_url_input"),
                singleLine = true,
                textStyle = TextStyle(fontSize = 11.sp, color = PrimaryWhite),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryWhite,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = PrimaryWhite,
                    unfocusedTextColor = SecondaryWhite,
                    cursorColor = PrimaryWhite
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val clip = clipboard.getText()?.text?.trim()
                            if (!clip.isNullOrEmpty()) {
                                urlInput = clip
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste URL",
                            tint = SecondaryWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
            Button(
                onClick = { onLoadUrl(urlInput) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("music_load_btn")
            ) {
                Text("Load", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!musicState.artworkUri.isNullOrBlank() || musicState.artworkBytes != null) {
                            AsyncImage(
                                model = musicState.artworkBytes ?: musicState.artworkUri,
                                contentDescription = "Track Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Track",
                                tint = PrimaryWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = musicState.currentTitle,
                            color = PrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${musicState.sourceName} • ${if (musicState.isBuffering) "Buffering..." else musicState.playbackStatus}",
                            color = when {
                                musicState.isPlaying -> AccentGreen
                                musicState.isBuffering -> AccentCyan
                                musicState.errorMessage != null -> AccentAmber
                                else -> MutedText
                            },
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Playback Progress & Duration
                val durationMs = musicState.durationMs.coerceAtLeast(0L)
                val posMs = musicState.currentPositionMs.coerceAtLeast(0L)
                if (durationMs > 0L) {
                    Slider(
                        value = posMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDurationMs(posMs), color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(formatDurationMs(durationMs), color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                } else if (musicState.isBuffering) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryWhite,
                        trackColor = ElevatedSurface
                    )
                }

                // Required Buttons: Previous, Play, Pause, Stop, Next, Repeat, History
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = PrimaryWhite)
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryWhite)
                            .clickable(onClick = onPlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = BgPrimary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = PrimaryWhite)
                    }
                    IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = SecondaryWhite)
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = PrimaryWhite)
                    }
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (musicState.isLooping) AccentGreen else MutedText
                        )
                    }
                    IconButton(onClick = { showHistory = !showHistory }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = if (showHistory) AccentCyan else SecondaryWhite
                        )
                    }
                }

                // Volume Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = SecondaryWhite, modifier = Modifier.size(16.dp))
                    Slider(
                        value = musicState.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                    )
                    Text("${(musicState.volume * 100).toInt()}%", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                if (musicState.errorMessage != null) {
                    Text(
                        text = musicState.errorMessage,
                        color = AccentAmber,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }

                if (musicState.isYouTubeUrl && musicState.youtubeUrlToOpen != null) {
                    Button(
                        onClick = { onOpenYouTube(musicState.youtubeUrlToOpen) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = PrimaryWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Link di YouTube Resmi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (musicState.isSpotifyUrl && musicState.spotifyUrlToOpen != null) {
                    Button(
                        onClick = { onOpenSpotify(musicState.spotifyUrlToOpen) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface, contentColor = PrimaryWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Link di Spotify Resmi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Local audio library: reads the requested folder recursively through MediaStore.
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(ElevatedSurface).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LOCAL MUSIC LIBRARY", color = PrimaryWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("${localMusic.size} TRACKS", color = AccentCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Text("/storage/emulated/0/music_hmmodz", color = MutedText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { musicScope.launch { scanDefaultMusicFolder() } }, enabled = !scanningLocalMusic) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SecondaryWhite, modifier = Modifier.size(14.dp))
                    Text(if (scanningLocalMusic) "Memindai..." else "Scan Ulang", color = SecondaryWhite, fontSize = 10.sp)
                }
            }
            OutlinedTextField(
                value = localMusicQuery, onValueChange = { localMusicQuery = it },
                modifier = Modifier.fillMaxWidth().height(48.dp), singleLine = true,
                placeholder = { Text("Cari judul lagu...", color = MutedText, fontSize = 10.sp) },
                textStyle = TextStyle(fontSize = 11.sp, color = PrimaryWhite),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryWhite, unfocusedBorderColor = BorderColor, focusedTextColor = PrimaryWhite, unfocusedTextColor = SecondaryWhite)
            )
            Text(localMusicStatus, color = if (localMusic.isEmpty()) MutedText else AccentGreen, fontSize = 9.sp)
            val filteredLocalMusic = localMusic.filter { it.second.contains(localMusicQuery, ignoreCase = true) }
            if (filteredLocalMusic.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp).verticalScroll(rememberScrollState())) {
                    filteredLocalMusic.forEach { (uri, title) ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onPlayLocalUri(uri, title) }.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(17.dp))
                            Text(title, color = PrimaryWhite, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play $title", tint = SecondaryWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Per-Game Dynamic Island Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Music Dynamic Island", color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Tampilkan kapsul musik mini di atas game", color = MutedText, fontSize = 9.sp)
            }
            Switch(
                checked = dynamicIslandEnabled,
                onCheckedChange = onToggleDynamicIsland,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryWhite,
                    checkedTrackColor = ElevatedSurface,
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = ElevatedSurface
                )
            )
        }

        if (showHistory || history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History (${history.size})", color = MutedText, fontSize = 10.sp)
            }
            if (history.isEmpty()) {
                Text("Belum ada riwayat audio yang berhasil diputar.", color = MutedText, fontSize = 10.sp)
            } else {
                history.take(5).forEach { track ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElevatedSurface)
                            .clickable {
                                urlInput = track.url
                                onPlayHistoryItem(track)
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = track.title,
                            color = SecondaryWhite,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayScreenshotTool(
    feedback: ScreenshotFeedback?,
    onCaptureScreenshot: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "SCREENSHOT SHORTCUT",
            color = PrimaryWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Menangkap layar game melalui Android MediaProjection API standar. Panel akan otomatis tersembunyi sesaat sebelum capture.",
                    color = SecondaryWhite,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Button(
                    onClick = onCaptureScreenshot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AMBIL SCREENSHOT LAYAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (feedback != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (feedback.isSuccess) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (feedback.isSuccess) AccentGreen.copy(alpha = 0.4f) else AccentRed.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = feedback.message,
                    color = PrimaryWhite,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun OverlayInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedText, fontSize = 10.sp)
        Text(
            value,
            color = PrimaryWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d", mins, secs)
}
