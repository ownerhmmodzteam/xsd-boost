package com.example.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.booster.RamAnalysisReport
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

fun LazyListScope.ramBoosterSection(
    report: RamAnalysisReport?,
    isBoosting: Boolean,
    shizukuTrimResult: String?,
    onBoostAndTrim: () -> Unit
) {
    item(key = "ram_booster_hero") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderActive, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = "RAM Booster",
                    tint = PrimaryWhite,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "SMART RAM MANAGEMENT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Analyzes active RAM pressure, compacts internal caches, and recommends system memory optimization. Powered by Shizuku Privileged System Access for deep kernel trim.",
                    fontSize = 12.sp,
                    color = SecondaryWhite,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onBoostAndTrim,
                    enabled = !isBoosting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_smart_ram_boost_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryWhite,
                        contentColor = BgPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isBoosting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BgPrimary)
                    } else {
                        Text("RUN SMART BOOST + SHIZUKU TRIM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (shizukuTrimResult != null) {
        item(key = "ram_booster_shizuku_result") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Shizuku Kernel Trim: $shizukuTrimResult",
                    color = SecondaryWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    if (report != null) {
        item(key = "ram_booster_report") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ToolCardBrush)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DIAGNOSTIC RESULTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Physical RAM:", color = MutedText, fontSize = 12.sp)
                        Text(
                            "${report.totalRamBytes / (1024 * 1024)} MB",
                            color = PrimaryWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Initial Free RAM:", color = MutedText, fontSize = 12.sp)
                        Text(
                            "${report.initialFreeBytes / (1024 * 1024)} MB",
                            color = PrimaryWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recovered Internal Heap:", color = MutedText, fontSize = 12.sp)
                        Text(
                            "${report.cleanedBytes / 1024} KB",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSurface)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = report.recommendation,
                            fontSize = 12.sp,
                            color = SecondaryWhite,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
