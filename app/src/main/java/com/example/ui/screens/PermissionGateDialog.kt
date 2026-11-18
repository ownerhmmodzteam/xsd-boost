package com.example.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.XsdBoostApplication

private data class GatePermission(val title: String, val detail: String, val granted: Boolean, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)

@Composable
fun PermissionGateDialog() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    var dismissedThisSession by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh++
                dismissedThisSession = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val refreshToken = refresh
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val shizuku = remember { (context.applicationContext as XsdBoostApplication).container.shizukuBridge }
    val shizukuStatus by shizuku.status.collectAsState()
    val overlayGranted = remember(refreshToken) { Settings.canDrawOverlays(context) }
    val usageGranted = remember(refreshToken) { hasUsageAccess(context) }
    val audioGranted = remember(refreshToken) { ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED }
    val notificationGranted = remember(refreshToken) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val shizukuGranted = shizukuStatus.isRunning && shizukuStatus.isPermissionGranted
    val permissions = listOf(
        GatePermission("Display Over Apps", "Untuk floating HUD dan crosshair di atas game.", overlayGranted, Icons.Default.Layers) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        },
        GatePermission("Usage Access", "Untuk mendeteksi game yang sedang berjalan.", usageGranted, Icons.Default.QueryStats) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        GatePermission("Music Library", "Untuk membaca audio lokal yang dipilih pengguna.", audioGranted, Icons.Default.MusicNote) {
            audioLauncher.launch(audioPermission)
        },
        GatePermission("Notifications", "Untuk notifikasi layanan booster yang aktif.", notificationGranted, Icons.Default.Notifications) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        },
        GatePermission("Shizuku Access", "Untuk fitur sistem lanjutan yang memerlukan Shizuku.", shizukuGranted, Icons.Default.Security) {
            if (shizukuStatus.isRunning) shizuku.requestShizukuPermission() else shizuku.openShizukuApp()
        }
    )
    val missing = permissions.count { !it.granted }
    LaunchedEffect(missing) { if (missing == 0) dismissedThisSession = false }
    if (missing > 0 && !dismissedThisSession) {
        Dialog(
            onDismissRequest = { dismissedThisSession = true },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false)
        ) {
            Column(Modifier.fillMaxWidth(0.90f).clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF151B17)).border(1.dp, Color(0x3339F58A), RoundedCornerShape(28.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x2239F58A)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, null, tint = Color(0xFF39F58A), modifier = Modifier.size(25.dp))
                    }
                }
                Text("PERMISSIONS", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("$missing izin belum aktif. Aktifkan izin yang diperlukan agar fitur XSD BOOST dapat bekerja optimal.",
                    color = Color(0xFFABB8AF), fontSize = 12.sp, lineHeight = 17.sp)
                permissions.forEach { permission ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF202922)).padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(permission.icon, null, tint = if (permission.granted) Color(0xFF39F58A) else Color(0xFFB7C5BA), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(permission.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(permission.detail, color = Color(0xFF9AA89E), fontSize = 9.sp, lineHeight = 12.sp)
                        }
                        if (permission.granted) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF39F58A), modifier = Modifier.size(19.dp))
                        else TextButton(onClick = permission.action, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text("SET", color = Color(0xFF39F58A), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text("Status izin diperiksa kembali saat kembali dari Pengaturan. Jika semua sudah aktif, jendela ini tertutup otomatis.",
                    color = Color(0xFF7F8E83), fontSize = 9.sp, lineHeight = 13.sp)
                TextButton(onClick = { dismissedThisSession = true }, modifier = Modifier.align(Alignment.End)) {
                    Text("NANTI", color = Color(0xFFB7C5BA), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
