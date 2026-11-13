package com.example.core.system.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import com.example.core.system.shizuku.ShizukuClient
import java.io.File
import java.io.RandomAccessFile

data class SystemStats(
    val cpuUsagePercent: Float = 0f,
    val rawCpuUsagePercent: Float? = null,
    val ramUsedBytes: Long = 0L,
    val ramAvailableBytes: Long = 0L,
    val ramTotalBytes: Long = 0L,
    val ramUsagePercent: Float = 0f,
    val storageAvailableBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val storageUsagePercent: Float = 0f,
    val cpuTempCelsius: Float? = null,
    val batteryTempCelsius: Float? = null,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val refreshRate: Float = 60f,
    val isShizukuMetricsActive: Boolean = false,
    val deviceModel: String = "${Build.MANUFACTURER ?: "Android"} ${Build.MODEL ?: "Device"}".trim(),
    val androidVersion: String = "Android ${Build.VERSION.RELEASE ?: "Unknown"} (API ${Build.VERSION.SDK_INT})",
    val cpuCoreCount: Int = Runtime.getRuntime().availableProcessors(),
    val cpuArch: String = Build.SUPPORTED_ABIS?.firstOrNull() ?: "Unknown",
    val timestamp: Long = System.currentTimeMillis()
) {
    val isCpuMeasurementAvailable: Boolean get() = rawCpuUsagePercent != null
    val effectiveTempCelsius: Float? get() = cpuTempCelsius ?: batteryTempCelsius
}

class HardwareMonitor(
    private val context: Context,
    private val shizukuClient: ShizukuClient? = null
) {

    private var lastTotalTime: Long = 0
    private var lastIdleTime: Long = 0
    private var lastValidCpuPercent: Float? = null

    fun getSystemStats(): SystemStats {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            activityManager?.getMemoryInfo(memoryInfo)

            val totalRam = memoryInfo.totalMem
            val availRam = memoryInfo.availMem
            val usedRam = (totalRam - availRam).coerceAtLeast(0L)
            val ramPercent = if (totalRam > 0) (usedRam.toFloat() / totalRam.toFloat()) * 100f else 0f

            // Storage Info
            val statFs = try {
                StatFs(Environment.getDataDirectory().path)
            } catch (_: Exception) {
                null
            }
            val totalStorage = statFs?.totalBytes ?: 0L
            val availStorage = statFs?.availableBytes ?: 0L
            val usedStorage = (totalStorage - availStorage).coerceAtLeast(0L)
            val storagePercent = if (totalStorage > 0) (usedStorage.toFloat() / totalStorage.toFloat()) * 100f else 0f

            // Battery Info
            val batteryIntent = try {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            } catch (_: Exception) {
                null
            }
            val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if (batteryLevel >= 0 && batteryScale > 0) (batteryLevel * 100) / batteryScale else 0
            val batteryRawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val batteryTemp = if (batteryRawTemp > 0) batteryRawTemp / 10f else null
            val batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    batteryStatus == BatteryManager.BATTERY_STATUS_FULL

            // Query Shizuku privileged metrics first if permitted & bound; fall back cleanly to local /proc or sysfs
            val shizukuSnapshot = shizukuClient?.readSystemMetrics()
            val usedShizuku = shizukuSnapshot?.cpuUsagePercent != null || shizukuSnapshot?.temperatureCelsius != null

            val rawCpuUsage = shizukuSnapshot?.cpuUsagePercent ?: readCpuUsageRaw()
            val cpuTemp = shizukuSnapshot?.temperatureCelsius ?: readCpuTemperature() ?: batteryTemp

            // Display refresh rate safely obtained from WindowManager
            var refreshRate = 60f
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                val display = windowManager?.defaultDisplay
                val rate = display?.mode?.refreshRate ?: display?.refreshRate
                if (rate != null && rate > 0f) {
                    refreshRate = rate
                }
            } catch (_: Exception) {
                refreshRate = 60f
            }

            SystemStats(
                cpuUsagePercent = rawCpuUsage ?: 0f,
                rawCpuUsagePercent = rawCpuUsage,
                ramUsedBytes = usedRam,
                ramAvailableBytes = availRam,
                ramTotalBytes = totalRam,
                ramUsagePercent = ramPercent,
                storageAvailableBytes = availStorage,
                storageTotalBytes = totalStorage,
                storageUsagePercent = storagePercent,
                cpuTempCelsius = cpuTemp,
                batteryTempCelsius = batteryTemp,
                batteryLevel = batteryPct,
                isCharging = isCharging,
                refreshRate = refreshRate,
                isShizukuMetricsActive = usedShizuku
            )
        } catch (_: Exception) {
            SystemStats()
        }
    }

    private fun readCpuUsageRaw(): Float? {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val tokens = load?.trim()?.split("\\s+".toRegex()) ?: return null
            if (tokens.size >= 8) {
                val user = tokens[1].toLong()
                val nice = tokens[2].toLong()
                val system = tokens[3].toLong()
                val idle = tokens[4].toLong()
                val iowait = tokens[5].toLong()
                val irq = tokens[6].toLong()
                val softirq = tokens[7].toLong()

                val total = user + nice + system + idle + iowait + irq + softirq
                val prevTotal = lastTotalTime
                val prevIdle = lastIdleTime

                lastTotalTime = total
                lastIdleTime = idle

                if (prevTotal > 0L) {
                    val totalDelta = total - prevTotal
                    val idleDelta = idle - prevIdle
                    if (totalDelta > 0) {
                        val usage = ((totalDelta - idleDelta).toFloat() / totalDelta.toFloat() * 100f)
                            .coerceIn(0f, 100f)
                        lastValidCpuPercent = usage
                        usage
                    } else {
                        lastValidCpuPercent
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readCpuTemperature(): Float? {
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/hwmon/hwmon0/temp1_input"
        )
        for (path in thermalPaths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                try {
                    val text = file.readText().trim()
                    val rawVal = text.toDoubleOrNull() ?: continue
                    val temp = if (rawVal > 1000) rawVal / 1000.0 else rawVal
                    if (temp in 10.0..115.0) {
                        return temp.toFloat()
                    }
                } catch (_: Exception) {
                    continue
                }
            }
        }
        return null
    }
}
