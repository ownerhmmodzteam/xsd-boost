package com.example.ui.screens

import android.app.Application
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.hardware.SystemStats
import com.example.ui.components.MetricSparklineCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun DashboardScreen(
    stats: SystemStats,
    fps: Float,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    tempHistory: List<Float>,
    fpsHistory: List<Float>,
    onSmartBoostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero System Status Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceGradient)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "XSD BOOST ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stats.deviceModel,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryWhite
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen)
                                )
                                Text(
                                    text = "ACTIVE",
                                    color = PrimaryWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Key Specs Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecPill(label = "REFRESH RATE", value = "${stats.refreshRate.toInt()} Hz")
                        SpecPill(label = "CORES", value = "${stats.cpuCoreCount} Cores")
                        SpecPill(label = "ARCH", value = stats.cpuArch.substringBefore("-"))
                        SpecPill(label = "OS", value = stats.androidVersion.substringBefore(" ("))
                    }

                    // Smart Boost Action Button
                    Button(
                        onClick = onSmartBoostClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dashboard_smart_boost_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryWhite,
                            contentColor = BgPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Boost",
                                tint = BgPrimary
                            )
                            Text(
                                text = "RUN SMART BOOST",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Telemetry Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSparklineCard(
                    title = "Compositor FPS",
                    currentValue = "${fps.toInt()}",
                    unit = "FPS",
                    history = fpsHistory,
                    minVal = 0f,
                    maxVal = 144f,
                    modifier = Modifier.weight(1f)
                )
                MetricSparklineCard(
                    title = "CPU Load",
                    currentValue = stats.rawCpuUsagePercent?.let { "${it.toInt()}" } ?: "N/A",
                    unit = if (stats.isCpuMeasurementAvailable) "%" else "",
                    history = cpuHistory,
                    minVal = 0f,
                    maxVal = 100f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val usedRamGb = String.format("%.1f", stats.ramUsedBytes / (1024f * 1024f * 1024f))
                val totalRamGb = String.format("%.1f", stats.ramTotalBytes / (1024f * 1024f * 1024f))
                MetricSparklineCard(
                    title = "RAM Usage",
                    currentValue = "$usedRamGb/$totalRamGb",
                    unit = "GB",
                    history = ramHistory,
                    minVal = 0f,
                    maxVal = 100f,
                    modifier = Modifier.weight(1f)
                )
                MetricSparklineCard(
                    title = "Thermals",
                    currentValue = stats.effectiveTempCelsius?.let { "${it.toInt()}" } ?: "N/A",
                    unit = if (stats.effectiveTempCelsius != null) "°C" else "",
                    history = tempHistory,
                    minVal = 20f,
                    maxVal = 90f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Detailed Hardware Section
        item {
            Text(
                text = "HARDWARE & STORAGE METRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }

        item {
            val availStorageGb = String.format("%.1f", stats.storageAvailableBytes / (1024f * 1024f * 1024f))
            val totalStorageGb = String.format("%.1f", stats.storageTotalBytes / (1024f * 1024f * 1024f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HardwareDetailRow(
                    icon = Icons.Default.Storage,
                    title = "Internal Storage",
                    subtitle = "$availStorageGb GB Free of $totalStorageGb GB",
                    trailing = "${stats.storageUsagePercent.toInt()}% Used"
                )
                HardwareDetailRow(
                    icon = Icons.Default.Memory,
                    title = "Memory Bandwidth",
                    subtitle = "LPDDR Virtual Swap & Active Caches",
                    trailing = "${stats.ramAvailableBytes / (1024 * 1024)} MB Free"
                )
                HardwareDetailRow(
                    icon = Icons.Default.Thermostat,
                    title = "Battery & Health",
                    subtitle = "${stats.batteryLevel}% ${if (stats.isCharging) "(Fast Charging)" else "(Discharging)"}",
                    trailing = stats.batteryTempCelsius?.let { "${it.toInt()}°C" } ?: "Normal"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun SpecPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = MutedText, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, color = PrimaryWhite, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HardwareDetailRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceGradient)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = PrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryWhite)
                    Text(text = subtitle, fontSize = 11.sp, color = MutedText)
                }
            }
            Text(
                text = trailing,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryWhite,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
