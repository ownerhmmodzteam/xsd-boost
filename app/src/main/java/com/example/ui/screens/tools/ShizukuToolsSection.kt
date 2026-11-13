package com.example.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.shizuku.ShizukuStatus
import com.example.core.system.shizuku.ShizukuSystemBridge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

internal val ToolCardBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF222222), Color(0xFF151515))
)

fun LazyListScope.shizukuEngineSection(
    status: ShizukuStatus,
    bridge: ShizukuSystemBridge,
    isExecuting: Boolean,
    executionStatusText: String?,
    onExecuteCommand: (label: String, command: suspend () -> String) -> Unit
) {
    item(key = "shizuku_status_card") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderActive, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Shizuku",
                                tint = PrimaryWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SHIZUKU PRIVILEGED ACCESS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryWhite,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = when {
                                    status.isRunning && status.isPermissionGranted && status.isUserServiceBound ->
                                        "Metrics UserService Bound (Rootless)"
                                    status.isRunning && status.isPermissionGranted ->
                                        "ADB Binder Authorized (Rootless)"
                                    status.isRunning ->
                                        "Shizuku Active — Permission Required"
                                    status.isInstalled ->
                                        "Shizuku Installed (Start Service)"
                                    else ->
                                        "Standalone Mode (Shizuku Optional)"
                                },
                                fontSize = 11.sp,
                                color = if (status.isRunning && status.isPermissionGranted) AccentGreen else AccentAmber
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (status.isRunning && status.isPermissionGranted) AccentGreen.copy(alpha = 0.2f) else ElevatedSurface)
                            .border(
                                1.dp,
                                if (status.isRunning && status.isPermissionGranted) AccentGreen.copy(alpha = 0.4f) else BorderColor,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                status.isRunning && status.isPermissionGranted -> "ACTIVE"
                                status.isRunning -> "AUTH"
                                else -> "STANDALONE"
                            },
                            color = if (status.isRunning && status.isPermissionGranted) AccentGreen else SecondaryWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = "Shizuku enables XSD BOOST to safely read system-level CPU & thermal metrics and run gaming optimizations without root.",
                    fontSize = 12.sp,
                    color = SecondaryWhite,
                    lineHeight = 17.sp
                )

                if (status.lastError != null) {
                    Text(
                        text = status.lastError,
                        fontSize = 11.sp,
                        color = AccentAmber,
                        lineHeight = 15.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (status.isRunning && !status.isPermissionGranted) {
                                bridge.requestShizukuPermission()
                            } else {
                                bridge.openShizukuApp()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryWhite,
                            contentColor = BgPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Shizuku Action",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (status.isRunning && !status.isPermissionGranted) "Grant Access" else "Open Shizuku",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { bridge.checkShizukuState() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElevatedSurface,
                            contentColor = PrimaryWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check Shizuku Status",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Check Status", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    item(key = "shizuku_actions_header") {
        Text(
            text = "PRIVILEGED GAMING ACTIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }

    item(key = "shizuku_action_governor") {
        ShizukuActionCard(
            title = "Fixed Performance Mode (CPU / GPU)",
            subtitle = "cmd power set-fixed-performance-mode-enabled true",
            description = "Locks CPU cores to sustained gaming frequencies and minimizes dynamic throttling during Free Fire matches.",
            isExecuting = isExecuting,
            onExecute = {
                onExecuteCommand("Applying performance mode...") {
                    bridge.applyGamingGovernorProfile()
                }
            }
        )
    }

    item(key = "shizuku_action_trim") {
        ShizukuActionCard(
            title = "System Trim-Caches (4096 MB)",
            subtitle = "pm trim-caches 4096M",
            description = "Calls Android PackageManager system cache flush to free unpinned system memory before heavy scrims.",
            isExecuting = isExecuting,
            onExecute = {
                onExecuteCommand("Trimming system caches...") {
                    bridge.clearAllGameBackgroundCaches()
                }
            }
        )
    }

    item(key = "shizuku_action_touch") {
        ShizukuActionCard(
            title = "Touch & Display Latency Sync",
            subtitle = "cmd display set-user-preferred-display-mode",
            description = "Enforces high-priority compositor touch event dispatching to eliminate input lag.",
            isExecuting = isExecuting,
            onExecute = {
                onExecuteCommand("Optimizing touch pipeline...") {
                    bridge.optimizeTouchSamplingRate()
                }
            }
        )
    }

    if (executionStatusText != null || status.lastCommandOutput != null) {
        item(key = "shizuku_console_output") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, BorderActive, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SHIZUKU CONSOLE OUTPUT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = executionStatusText ?: (status.lastCommandOutput ?: "Ready"),
                        fontSize = 11.sp,
                        color = PrimaryWhite,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShizukuActionCard(
    title: String,
    subtitle: String,
    description: String,
    isExecuting: Boolean,
    onExecute: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ToolCardBrush)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Button(
                    onClick = onExecute,
                    enabled = !isExecuting,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryWhite,
                        contentColor = BgPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = description,
                fontSize = 11.sp,
                color = MutedText,
                lineHeight = 16.sp
            )
        }
    }
}
