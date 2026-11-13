package com.example.core.system.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanStatus {
    SAFE,
    UNKNOWN,
    SUSPICIOUS,
    NEEDS_REVIEW
}

data class AppSecurityReport(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val apkSizeMb: Float,
    val scanStatus: ScanStatus,
    val riskSummary: String,
    val signatureHash: String,
    val scanTime: String,
    val isSystemApp: Boolean,
    val suspiciousFlags: List<String>
)

class AppSecurityScanner(private val context: Context) {

    fun scanInstalledApps(): List<AppSecurityReport> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val reports = mutableListOf<AppSecurityReport>()
        val dateFormat = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
        val scanTime = dateFormat.format(Date())

        for (pkg in packages) {
            // Skip own package from scan review
            if (pkg.packageName == context.packageName) continue

            try {
                val appInfo = pkg.applicationInfo ?: continue
                val appName = pm.getApplicationLabel(appInfo).toString()
                val sourceDir = appInfo.sourceDir
                val file = if (sourceDir != null) File(sourceDir) else null
                val sizeMb = if (file != null && file.exists()) file.length() / (1024f * 1024f) else 0f
                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                val requestedPerms = pkg.requestedPermissions?.toList() ?: emptyList()
                val suspiciousFlags = mutableListOf<String>()

                // Heuristic inspection
                val dangerousPerms = listOf(
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.RECEIVE_BOOT_COMPLETED",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.CAMERA",
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.READ_CONTACTS",
                    "android.permission.SEND_SMS",
                    "android.permission.READ_SMS"
                )

                val matchedDangerous = requestedPerms.intersect(dangerousPerms.toSet())
                if (matchedDangerous.size >= 5 && !isSystem) {
                    suspiciousFlags.add("Requests ${matchedDangerous.size} sensitive hardware & privacy permissions")
                }

                if (pkg.packageName.startsWith("com.google.") || pkg.packageName.startsWith("com.android.")) {
                    // System ecosystem
                } else if (!isSystem && matchedDangerous.contains("android.permission.SEND_SMS")) {
                    suspiciousFlags.add("Third-party app has SMS send capability")
                }

                val sigHash = computeSignatureFingerprint(pkg)

                val status = when {
                    suspiciousFlags.isNotEmpty() -> ScanStatus.SUSPICIOUS
                    matchedDangerous.size in 3..4 && !isSystem -> ScanStatus.NEEDS_REVIEW
                    isSystem -> ScanStatus.SAFE
                    requestedPerms.isEmpty() -> ScanStatus.SAFE
                    else -> ScanStatus.SAFE
                }

                val riskSummary = when (status) {
                    ScanStatus.SAFE -> "No anomalous signatures or over-privileged triggers detected."
                    ScanStatus.NEEDS_REVIEW -> "Has multiple elevated permissions (${matchedDangerous.size}). Normal for utilities."
                    ScanStatus.SUSPICIOUS -> "Potential high-privilege risk: ${suspiciousFlags.joinToString(", ")}"
                    ScanStatus.UNKNOWN -> "Signature could not be fully verified."
                }

                reports.add(
                    AppSecurityReport(
                        appName = appName,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        apkSizeMb = sizeMb,
                        scanStatus = status,
                        riskSummary = riskSummary,
                        signatureHash = sigHash,
                        scanTime = scanTime,
                        isSystemApp = isSystem,
                        suspiciousFlags = suspiciousFlags
                    )
                )
            } catch (_: Exception) {
                // Ignore transient errors
            }
        }

        // Sort suspicious first, then needs review, then safe
        return reports.sortedWith(
            compareByDescending<AppSecurityReport> { it.scanStatus == ScanStatus.SUSPICIOUS }
                .thenByDescending { it.scanStatus == ScanStatus.NEEDS_REVIEW }
                .thenBy { it.appName.lowercase() }
        )
    }

    private fun computeSignatureFingerprint(pkg: PackageInfo): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = context.packageManager.getPackageInfo(
                    pkg.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo
                signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val pInfo = context.packageManager.getPackageInfo(pkg.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pInfo.signatures
            }

            if (signatures != null && signatures.isNotEmpty()) {
                val certBytes = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(certBytes)
                digest.joinToString("") { "%02X".format(it) }.take(16) + "..."
            } else {
                "UNSIGNED_OR_SYSTEM"
            }
        } catch (_: Exception) {
            "UNAVAILABLE"
        }
    }
}
