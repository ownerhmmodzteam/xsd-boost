package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.hardware.SystemStats

@Composable
fun HomeScreen(
    stats: SystemStats,
    fps: Float,
    onOpenGameSpace: () -> Unit,
    onRunSmartBoost: () -> Unit,
    isBoosting: Boolean,
    modifier: Modifier = Modifier
) {
    val green = Color(0xFF39F58A)
    Column(modifier.fillMaxSize().background(Color(0xFF080B09)).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("XSD BOOST", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text("PERFORMANCE CONTROL CENTER", color = green, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            HomeMetric("CPU", stats.rawCpuUsagePercent?.let { "${it.toInt()}%" } ?: "--", Modifier.weight(1f))
            HomeMetric("RAM", "${stats.ramUsagePercent.toInt()}%", Modifier.weight(1f))
            HomeMetric("FPS", "${fps.toInt()}", Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF18241D), Color(0xFF0D1510))))
            .border(1.dp, Color(0x4439F58A), RoundedCornerShape(26.dp))
            .clickable(onClick = onOpenGameSpace).padding(24.dp)) {
            Column(Modifier.fillMaxWidth().align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Gamepad, null, tint = green, modifier = Modifier.size(42.dp))
                Text("GAME SPACE", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Enter your gaming environment", color = Color(0xFFB8C5BC), fontSize = 12.sp)
                Button(onClick = onOpenGameSpace, colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color(0xFF07110A))) {
                    Text("OPEN GAME SPACE", fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
        OutlinedButton(onClick = onRunSmartBoost, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = green)) {
            Icon(Icons.Default.Bolt, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isBoosting) "BOOSTING..." else "SMART BOOST", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(Color(0xFF141B16))
        .border(1.dp, Color(0x2239F58A), RoundedCornerShape(15.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color(0xFF8D9B91), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
