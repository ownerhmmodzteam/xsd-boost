package com.example.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun PermissionsCenterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    val hasUsageStats = checkUsageStatsPermission(context)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryWhite)
                }
                Text(
                    text = "PERMISSIONS CENTER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        item {
            PermissionCard(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps (Overlay)",
                description = "Required for Floating Gaming HUD and Precision Crosshair Aiming overlay across game sessions.",
                isGranted = hasOverlayPermission,
                actionLabel = "Configure Overlay",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                }
            )
        }

        item {
            PermissionCard(
                icon = Icons.Default.QueryStats,
                title = "Usage Access (PACKAGE_USAGE_STATS)",
                description = "Enables real-time foreground game detection and frame time tracking.",
                isGranted = hasUsageStats,
                actionLabel = "Grant Usage Stats",
                onAction = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        item {
            val shizukuBridge = androidx.compose.runtime.remember {
                (context.applicationContext as com.example.XsdBoostApplication).container.shizukuBridge
            }
            val shizukuState = shizukuBridge.status.collectAsState().value
            PermissionCard(
                icon = Icons.Default.Security,
                title = "Shizuku Privileged System Access",
                description = "Mengakses metrik CPU & suhu sistem serta optimasi kernel secara aman tanpa root melalui Shizuku Manager.",
                isGranted = shizukuState.isRunning && shizukuState.isPermissionGranted,
                actionLabel = if (shizukuState.isRunning) "Request Shizuku Permission" else "Open Shizuku",
                onAction = {
                    if (shizukuState.isRunning) {
                        shizukuBridge.requestShizukuPermission()
                    } else {
                        shizukuBridge.openShizukuApp()
                    }
                }
            )
        }

        item {
            val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            var notifGranted by androidx.compose.runtime.remember(hasNotificationPermission) {
                androidx.compose.runtime.mutableStateOf(hasNotificationPermission)
            }
            val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted ->
                notifGranted = granted
            }

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Foreground Service Notifications",
                description = "Diperlukan untuk menampilkan notifikasi status layanan In-Game Booster Overlay agar tidak dihentikan OS.",
                isGranted = notifGranted,
                actionLabel = "Izinkan Notifikasi",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceGradient)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = PrimaryWhite, modifier = Modifier.size(20.dp))
                    }
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryWhite)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGranted) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "OPTIONAL",
                        color = if (isGranted) AccentGreen else AccentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(text = description, fontSize = 11.sp, color = MutedText, lineHeight = 16.sp)

            if (!isGranted) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("grant_permission_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryWhite, contentColor = BgPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
