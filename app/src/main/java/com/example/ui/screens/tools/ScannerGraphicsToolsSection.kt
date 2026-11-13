package com.example.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.graphics.GraphicsHardwareInfo
import com.example.core.system.graphics.GraphicsPresetProfile
import com.example.core.system.scanner.AppSecurityReport
import com.example.core.system.scanner.ScanStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

fun LazyListScope.apkScannerSection(
    reports: List<AppSecurityReport>,
    isScanning: Boolean,
    onScan: () -> Unit
) {
    item(key = "scanner_header_card") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderActive, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "APK & SYSTEM INTEGRITY SCANNER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Inspects installed application packages, verifies cryptographic certificates, and flags suspicious excessive permission requests using transparent heuristics.",
                    fontSize = 12.sp,
                    color = SecondaryWhite,
                    lineHeight = 17.sp
                )
                Button(
                    onClick = onScan,
                    enabled = !isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_apk_scan_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryWhite,
                        contentColor = BgPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BgPrimary)
                    } else {
                        Text("SCAN INSTALLED PACKAGES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    items(reports, key = { "scan_${it.packageName}" }) { report ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryWhite,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    val (statusColor, statusText) = when (report.scanStatus) {
                        ScanStatus.SAFE -> Pair(AccentGreen, "SAFE")
                        ScanStatus.NEEDS_REVIEW -> Pair(AccentAmber, "REVIEW")
                        ScanStatus.SUSPICIOUS -> Pair(AccentRed, "SUSPICIOUS")
                        ScanStatus.UNKNOWN -> Pair(MutedText, "UNKNOWN")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "${report.packageName} • v${report.versionName} • SHA: ${report.signatureHash}",
                    fontSize = 10.sp,
                    color = MutedText,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = report.riskSummary,
                    fontSize = 11.sp,
                    color = SecondaryWhite
                )
            }
        }
    }
}

fun LazyListScope.graphicsDriverSection(
    info: GraphicsHardwareInfo,
    profiles: List<GraphicsPresetProfile>,
    selectedId: String,
    applyResult: String?,
    onSelectProfile: (String) -> Unit
) {
    item(key = "graphics_info_card") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderActive, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GPU & DRIVER SPECIFICATIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Renderer:", color = MutedText, fontSize = 11.sp)
                    Text(info.renderer, color = PrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vendor:", color = MutedText, fontSize = 11.sp)
                    Text(info.vendor, color = PrimaryWhite, fontSize = 11.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("OpenGL ES:", color = MutedText, fontSize = 11.sp)
                    Text(info.glVersion, color = PrimaryWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vulkan Support:", color = MutedText, fontSize = 11.sp)
                    Text(
                        if (info.vulkanSupported) "Supported" else "Unsupported",
                        color = if (info.vulkanSupported) AccentGreen else AccentRed,
                        fontSize = 11.sp
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ANGLE Status:", color = MutedText, fontSize = 11.sp)
                    Text(info.angleStatus, color = PrimaryWhite, fontSize = 11.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Free Fire & FF MAX:", color = MutedText, fontSize = 11.sp)
                    Text("Fully Compatible", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    item(key = "graphics_profiles_header") {
        Text(
            text = "GRAPHICS PRESET PROFILES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }

    items(profiles, key = { "gfx_${it.id}" }) { profile ->
        val isSelected = profile.id == selectedId
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ToolCardBrush)
                .border(1.dp, if (isSelected) PrimaryWhite else BorderColor, RoundedCornerShape(16.dp))
                .clickable { onSelectProfile(profile.id) }
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = profile.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryWhite)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryWhite)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", color = BgPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Text(text = profile.description, fontSize = 11.sp, color = SecondaryWhite)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Target: ${profile.targetFps} FPS",
                        fontSize = 10.sp,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Filtering: ${profile.textureFiltering}",
                        fontSize = 10.sp,
                        color = MutedText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    if (applyResult != null) {
        item(key = "graphics_apply_result") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Shizuku Governor: $applyResult",
                    color = SecondaryWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
