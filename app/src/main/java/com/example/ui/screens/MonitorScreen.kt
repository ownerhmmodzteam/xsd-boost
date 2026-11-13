package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.hardware.SystemStats
import com.example.core.system.overlay.EdgeSide
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun MonitorScreen(
    stats: SystemStats,
    fps: Float,
    isOverlayActive: Boolean,
    hasUsageStatsPermission: Boolean,
    overlayOpacity: Float,
    isDefaultHandleHidden: Boolean,
    defaultOverlaySide: EdgeSide = EdgeSide.RIGHT,
    defaultHandleSizeDp: Float = 76f,
    defaultHandleOpacity: Float = 0.90f,
    defaultLongPressMs: Long = 420L,
    monitorIntervalMs: Long,
    onRefreshPermissions: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onOpenPanelPreview: () -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onToggleDefaultHandleHidden: (Boolean) -> Unit,
    onUpdateDefaultOverlaySide: (EdgeSide) -> Unit = {},
    onUpdateDefaultHandleSizeDp: (Float) -> Unit = {},
    onUpdateDefaultHandleOpacity: (Float) -> Unit = {},
    onUpdateDefaultLongPressMs: (Long) -> Unit = {},
    onRestoreAllHiddenHandles: () -> Unit,
    onUpdateMonitorInterval: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Smart Game Watcher & Overlay Toggle
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMART GAME BOOSTER OVERLAY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryWhite,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Hanya tampil otomatis saat game terdaftar aktif di foreground",
                                fontSize = 11.sp,
                                color = MutedText
                            )
                        }

                        Switch(
                            checked = isOverlayActive,
                            onCheckedChange = { targetState ->
                                if (targetState && !hasOverlayPermission) {
                                    openOverlaySettings(context)
                                } else {
                                    onToggleOverlay(targetState)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryWhite,
                                checkedTrackColor = ElevatedSurface,
                                uncheckedThumbColor = MutedText,
                                uncheckedTrackColor = ElevatedSurface
                            ),
                            modifier = Modifier.testTag("gaming_hud_toggle")
                        )
                    }

                    if (!hasOverlayPermission) {
                        PermissionWarningBanner(
                            title = "Izin Display Over Other Apps",
                            description = "Diperlukan agar Floating Handle & Music Dynamic Island tampil di atas game.",
                            onGrantClick = { openOverlaySettings(context) }
                        )
                    }

                    if (!hasUsageStatsPermission) {
                        PermissionWarningBanner(
                            title = "Izin Usage Access (Foreground Detection)",
                            description = "Diperlukan untuk mendeteksi game yang aktif di foreground tanpa AccessibilityService.",
                            onGrantClick = { openUsageStatsSettings(context) }
                        )
                    }

                    if (hasOverlayPermission) {
                        Button(
                            onClick = onOpenPanelPreview,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("open_overlay_panel_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryWhite,
                                contentColor = BgPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (defaultOverlaySide == EdgeSide.LEFT) Icons.Default.SwipeRight else Icons.Default.SwipeLeft,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "PREVIEW PANEL BOOSTER (${if (defaultOverlaySide == EdgeSide.LEFT) "KIRI" else "KANAN"})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Overlay appearance: only size and transparency are user-customizable.
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "OVERLAY APPEARANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Overlay menggunakan handle bawaan XSD BOOST. Kamu hanya dapat mengatur ukuran dan transparansinya.",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                    OverlayPositionPreview(
                        side = EdgeSide.RIGHT,
                        handleSizeDp = defaultHandleSizeDp,
                        handleOpacity = defaultHandleOpacity,
                        isHidden = false
                    )
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ukuran Overlay", color = MutedText, fontSize = 11.sp)
                            Text("${defaultHandleSizeDp.toInt()} dp", color = PrimaryWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = defaultHandleSizeDp,
                            onValueChange = onUpdateDefaultHandleSizeDp,
                            valueRange = 56f..120f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transparansi Overlay", color = MutedText, fontSize = 11.sp)
                            Text("${(defaultHandleOpacity * 100).toInt()}%", color = PrimaryWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = defaultHandleOpacity,
                            onValueChange = onUpdateDefaultHandleOpacity,
                            valueRange = 0.25f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                        )
                    }
                }
            }
        }

        // Overlay Opacity & Polling Performance Settings
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PANEL APPEARANCE & POLLING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryWhite,
                        fontFamily = FontFamily.Monospace
                    )

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transparansi Panel", color = MutedText, fontSize = 12.sp)
                            Text("${(overlayOpacity * 100).toInt()}%", color = PrimaryWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = overlayOpacity,
                            onValueChange = onUpdateOpacity,
                            valueRange = 0.5f..1f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interval Telemetry", color = PrimaryWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Otomatis dijeda saat panel tersembunyi atau keluar game", color = MutedText, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1000L to "1s", 2000L to "2s", 3000L to "3s").forEach { (ms, label) ->
                                val selected = monitorIntervalMs == ms
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) PrimaryWhite else ElevatedSurface)
                                        .clickable { onUpdateMonitorInterval(ms) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) BgPrimary else SecondaryWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Real-time Telemetry Monitor Table
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTIC METRICS STREAM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    MonitorDataRow("FPS (Compositor HUD):", "${fps.toInt()} FPS")
                    MonitorDataRow("Game Foreground FPS:", "N/A (Android API Restricted)")
                    MonitorDataRow("Display Refresh Rate:", "${stats.refreshRate.toInt()} Hz")
                    MonitorDataRow(
                        "CPU Usage:",
                        stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "N/A (SELinux / No Shizuku)"
                    )
                    MonitorDataRow("RAM Usage:", "${stats.ramUsagePercent.toInt()}%")
                    MonitorDataRow("RAM Available:", "${stats.ramAvailableBytes / (1024 * 1024)} MB")
                    MonitorDataRow("Thermal Sensor:", stats.effectiveTempCelsius?.let { "${it.toInt()}°C" } ?: "N/A")
                    MonitorDataRow("Battery Level:", "${stats.batteryLevel}%")
                }
            }
        }
    }
}

@Composable
private fun OverlayPositionPreview(
    side: EdgeSide,
    handleSizeDp: Float,
    handleOpacity: Float,
    isHidden: Boolean
) {
    val isLeft = side == EdgeSide.LEFT
    val scaledHeight = (handleSizeDp * 0.45f).coerceIn(24f, 54f).dp
    val barWidth = if (isHidden) 3.dp else 7.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF070707))
            .border(1.dp, BorderActive, RoundedCornerShape(14.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .testTag("overlay_position_preview")
    ) {
        // Miniature handle on left or right border
        Box(
            modifier = Modifier
                .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .alpha(handleOpacity.coerceIn(0.25f, 1.0f))
                .width(barWidth)
                .height(scaledHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(PrimaryWhite)
        )

        // Miniature panel direction preview
        Box(
            modifier = Modifier
                .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(start = if (isLeft) 18.dp else 0.dp, end = if (isLeft) 0.dp else 18.dp)
                .width(126.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (isLeft) "LEFT → PANEL" else "PANEL ← RIGHT",
                    color = PrimaryWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Size ${handleSizeDp.toInt()}dp • ${(handleOpacity * 100).toInt()}%",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(
    title: String,
    description: String,
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentRed.copy(alpha = 0.15f))
            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = "Alert", tint = AccentAmber)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryWhite)
                Text(text = description, fontSize = 10.sp, color = SecondaryWhite)
            }
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Izinkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MonitorDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MutedText, fontSize = 11.sp)
        Text(text = value, color = PrimaryWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

private fun openOverlaySettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

private fun openUsageStatsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {}
}
