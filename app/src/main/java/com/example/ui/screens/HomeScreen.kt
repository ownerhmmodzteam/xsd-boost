package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.hardware.SystemStats
import java.util.Locale

private val HomeGreen = Color(0xFF39F58A)
private val HomeBackground = Color(0xFF080B09)
private val HomeCard = Color(0xFF141B16)

@Composable
fun HomeScreen(
    stats: SystemStats,
    fps: Float,
    onOpenGameSpace: () -> Unit,
    onRunSmartBoost: () -> Unit,
    isBoosting: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("xsd_home_display", Context.MODE_PRIVATE) }
    var customDeviceName by remember { mutableStateOf(prefs.getString("device_display_name", "").orEmpty()) }
    var editDeviceName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(customDeviceName) }
    val defaultDeviceName = stats.deviceModel.ifBlank { "Android Device" }
    val visibleDeviceName = customDeviceName.ifBlank { defaultDeviceName }

    Column(
        modifier.fillMaxSize().background(HomeBackground).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("XSD BOOST", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                Text("PERFORMANCE CONTROL CENTER", color = HomeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.35.sp)
            }
            Surface(
                onClick = { nameDraft = visibleDeviceName; editDeviceName = true },
                color = HomeCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3339F58A))
            ) {
                Row(
                    Modifier.widthIn(max = 156.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(Modifier.weight(1f, fill = false)) {
                        Text("DEVICE", color = Color(0xFF84948A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(visibleDeviceName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Ubah nama tampilan perangkat", tint = HomeGreen, modifier = Modifier.size(13.dp))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HomeMetric("CPU", stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "--", "Usage", Modifier.weight(1f))
            HomeMetric("RAM", "${stats.ramUsagePercent.toInt()}%", "${formatBytes(stats.ramUsedBytes)} / ${formatBytes(stats.ramTotalBytes)}", Modifier.weight(1f))
            HomeMetric("STORAGE", if (stats.storageTotalBytes > 0L) "${stats.storageUsagePercent.toInt()}%" else "--", "${formatBytes(stats.storageAvailableBytes)} free", Modifier.weight(1f))
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(HomeCard).border(1.dp, Color(0x2239F58A), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("GAME SPACE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                Text("Gaming environment", color = Color(0xFF9AA89F), fontSize = 10.sp)
            }
            Text(" ${fps.toInt()} FPS ", color = HomeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onOpenGameSpace,
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF20372A), contentColor = HomeGreen)
            ) {
                Icon(Icons.Default.Gamepad, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("OPEN", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF101712)).border(1.dp, Color(0x2239F58A), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text("SYSTEM OVERVIEW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            UsageLine("CPU", stats.rawCpuUsagePercent)
            UsageLine("RAM", stats.ramUsagePercent)
            UsageLine("STORAGE", if (stats.storageTotalBytes > 0L) stats.storageUsagePercent else null)
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onRunSmartBoost,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HomeGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6639F58A)),
            contentPadding = PaddingValues(vertical = 13.dp)
        ) {
            Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isBoosting) "BOOSTING..." else "SMART BOOST", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }

    if (editDeviceName) {
        AlertDialog(
            onDismissRequest = { editDeviceName = false },
            containerColor = Color(0xFF151C18),
            title = { Text("Nama Perangkat", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nama ini hanya mengubah tampilan di XSD BOOST. Nama perangkat Android tetap sama.", color = Color(0xFFADB9B0), fontSize = 12.sp)
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(32) },
                        singleLine = true,
                        label = { Text("Nama tampilan") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HomeGreen,
                            cursorColor = HomeGreen
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    customDeviceName = nameDraft.trim()
                    prefs.edit().putString("device_display_name", customDeviceName).apply()
                    editDeviceName = false
                }) { Text("SIMPAN", color = HomeGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    nameDraft = ""
                    customDeviceName = ""
                    prefs.edit().remove("device_display_name").apply()
                    editDeviceName = false
                }) { Text("RESET", color = Color.LightGray) }
            }
        )
    }
}

@Composable
private fun HomeMetric(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(HomeCard)
            .border(1.dp, Color(0x2239F58A), RoundedCornerShape(15.dp)).padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, color = Color(0xFF8D9B91), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(detail, color = Color(0xFF87958B), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun UsageLine(label: String, usage: Float?) {
    val value = usage?.coerceIn(0f, 100f)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFB8C5BC), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(66.dp))
        LinearProgressIndicator(
            progress = { (value ?: 0f) / 100f },
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
            color = HomeGreen,
            trackColor = Color(0xFF29332C)
        )
        Text(value?.let { "${it.toInt()}%" } ?: "--", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 GB"
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 100.0) String.format(Locale.US, "%.0f GB", gb)
    else String.format(Locale.US, "%.1f GB", gb)
}
