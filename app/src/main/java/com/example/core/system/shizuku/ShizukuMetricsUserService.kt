package com.example.core.system.shizuku

import android.os.Process
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile
import kotlin.system.exitProcess

/**
 * Privileged UserService hosted inside the Shizuku server process (ADB shell / rootless or root).
 * Reads system-level CPU and thermal metrics that SELinux blocks from normal app sandboxes.
 */
class ShizukuMetricsUserService : IShizukuMetricsService.Stub() {

    private var lastTotalTime: Long = 0L
    private var lastIdleTime: Long = 0L

    override fun destroy() {
        exitProcess(0)
    }

    override fun readCpuUsagePercent(): Float {
        val firstSample = readProcStatLine() ?: return -1f
        if (lastTotalTime == 0L) {
            lastTotalTime = firstSample.first
            lastIdleTime = firstSample.second
            try {
                Thread.sleep(90L)
            } catch (_: InterruptedException) {
                return -1f
            }
            val secondSample = readProcStatLine() ?: return -1f
            return computeDelta(secondSample.first, secondSample.second)
        }
        return computeDelta(firstSample.first, firstSample.second)
    }

    private fun computeDelta(total: Long, idle: Long): Float {
        val totalDelta = total - lastTotalTime
        val idleDelta = idle - lastIdleTime
        lastTotalTime = total
        lastIdleTime = idle
        if (totalDelta <= 0L) return -1f
        return (((totalDelta - idleDelta).toFloat() / totalDelta.toFloat()) * 100f)
            .coerceIn(0f, 100f)
    }

    private fun readProcStatLine(): Pair<Long, Long>? {
        return try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val line = reader.readLine() ?: return null
                val tokens = line.trim().split("\\s+".toRegex())
                if (tokens.size < 8) return null
                val user = tokens[1].toLong()
                val nice = tokens[2].toLong()
                val system = tokens[3].toLong()
                val idle = tokens[4].toLong()
                val iowait = tokens[5].toLong()
                val irq = tokens[6].toLong()
                val softirq = tokens[7].toLong()
                val total = user + nice + system + idle + iowait + irq + softirq
                Pair(total, idle)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun readThermalCelsius(): Float {
        return try {
            val thermalRoot = File("/sys/class/thermal")
            val zoneDirs = thermalRoot.listFiles { file ->
                file.isDirectory && file.name.startsWith("thermal_zone")
            }?.sortedBy { it.name } ?: emptyList()

            val preferredKeywords = listOf("cpu", "soc", "ap", "tsens", "cluster", "big", "little")
            var fallbackTemp: Float? = null

            for (zone in zoneDirs) {
                val typeName = readTrimmedFile(File(zone, "type"))?.lowercase() ?: ""
                val tempValue = parseThermalFile(File(zone, "temp")) ?: continue
                if (preferredKeywords.any { typeName.contains(it) }) {
                    return tempValue
                }
                if (fallbackTemp == null) {
                    fallbackTemp = tempValue
                }
            }

            if (fallbackTemp != null) return fallbackTemp

            val legacyPaths = listOf(
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/class/hwmon/hwmon0/temp1_input"
            )
            for (path in legacyPaths) {
                val temp = parseThermalFile(File(path))
                if (temp != null) return temp
            }
            -1f
        } catch (_: Exception) {
            -1f
        }
    }

    private fun readTrimmedFile(file: File): String? {
        return try {
            if (file.exists() && file.canRead()) file.readText().trim() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseThermalFile(file: File): Float? {
        val text = readTrimmedFile(file) ?: return null
        val raw = text.toDoubleOrNull() ?: return null
        val celsius = when {
            raw > 1000.0 -> raw / 1000.0
            raw > 200.0 -> raw / 10.0
            else -> raw
        }
        return if (celsius in 10.0..115.0) celsius.toFloat() else null
    }

    override fun executeShellCommand(command: String?): String {
        val cmd = command?.trim().orEmpty()
        if (cmd.isEmpty()) return "Empty command"

        return try {
            val process = ProcessBuilder("sh", "-c", cmd).start()
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }.trim()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }.trim()
            val exitCode = process.waitFor()
            val uid = Process.myUid()
            when {
                exitCode == 0 && stdout.isNotEmpty() -> stdout
                exitCode == 0 -> "Command executed successfully via Shizuku [UID=$uid, Exit=0]"
                stderr.isNotEmpty() -> "Shizuku Exit $exitCode [UID=$uid]: $stderr"
                else -> "Shizuku Exit $exitCode [UID=$uid]"
            }
        } catch (e: Exception) {
            "Shizuku command error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}
