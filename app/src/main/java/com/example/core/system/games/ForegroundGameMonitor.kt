package com.example.core.system.games

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process

data class ForegroundDetectionState(
    val foregroundPackage: String?,
    val hasUsagePermission: Boolean,
    val isScreenInteractive: Boolean
)

/**
 * Monitors foreground application changes using Android's official UsageStatsManager.queryEvents API.
 * Does NOT use AccessibilityService and does NOT infer foreground status from background processes.
 */
class ForegroundGameMonitor(private val context: Context) {

    private var lastForegroundPackage: String? = null
    private var lastEventTimestampMs: Long = 0L

    fun hasUsageStatsPermission(): Boolean {
        return try {
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
            when (mode) {
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_DEFAULT -> {
                    context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun pollForegroundPackage(nowMs: Long = System.currentTimeMillis()): ForegroundDetectionState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val interactive = try {
            powerManager?.isInteractive ?: true
        } catch (_: Exception) {
            true
        }

        if (!interactive) {
            lastForegroundPackage = null
            return ForegroundDetectionState(
                foregroundPackage = null,
                hasUsagePermission = hasUsageStatsPermission(),
                isScreenInteractive = false
            )
        }

        if (!hasUsageStatsPermission()) {
            lastForegroundPackage = null
            return ForegroundDetectionState(
                foregroundPackage = null,
                hasUsagePermission = false,
                isScreenInteractive = true
            )
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ForegroundDetectionState(lastForegroundPackage, true, true)

        val lookbackStart = if (lastEventTimestampMs == 0L) {
            nowMs - INITIAL_LOOKBACK_MS
        } else {
            (lastEventTimestampMs - EVENT_OVERLAP_MS).coerceAtLeast(nowMs - MAX_LOOKBACK_MS)
        }

        try {
            val events = usm.queryEvents(lookbackStart, nowMs)
            val event = UsageEvents.Event()
            var latestResumedPkg: String? = null
            var latestResumedTime = 0L
            var latestPausedPkg: String? = null
            var latestPausedTime = 0L
            var maxSeenTimestamp = lastEventTimestampMs

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.timeStamp > maxSeenTimestamp) {
                    maxSeenTimestamp = event.timeStamp
                }
                val pkg = event.packageName ?: continue
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (event.timeStamp >= latestResumedTime) {
                            latestResumedTime = event.timeStamp
                            latestResumedPkg = pkg
                        }
                    }
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && event.timeStamp >= latestResumedTime) {
                            latestResumedTime = event.timeStamp
                            latestResumedPkg = pkg
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        if (event.timeStamp >= latestPausedTime) {
                            latestPausedTime = event.timeStamp
                            latestPausedPkg = pkg
                        }
                    }
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && event.timeStamp >= latestPausedTime) {
                            latestPausedTime = event.timeStamp
                            latestPausedPkg = pkg
                        }
                    }
                }
            }

            if (maxSeenTimestamp > lastEventTimestampMs) {
                lastEventTimestampMs = maxSeenTimestamp
            } else if (lastEventTimestampMs == 0L) {
                lastEventTimestampMs = nowMs
            }

            if (latestResumedPkg != null && latestResumedTime >= latestPausedTime) {
                lastForegroundPackage = latestResumedPkg
            } else if (latestResumedPkg != null && latestPausedPkg != latestResumedPkg) {
                lastForegroundPackage = latestResumedPkg
            } else if (latestPausedPkg != null && latestPausedPkg == lastForegroundPackage && latestPausedTime > latestResumedTime) {
                // Active target paused with no newer resumed app in window (e.g., transitional state)
                lastForegroundPackage = null
            }
        } catch (_: Exception) {
            // Keep safe state without crashing on vendor restrictions
        }

        return ForegroundDetectionState(
            foregroundPackage = lastForegroundPackage,
            hasUsagePermission = true,
            isScreenInteractive = true
        )
    }

    fun reset() {
        lastForegroundPackage = null
        lastEventTimestampMs = 0L
    }

    companion object {
        private const val INITIAL_LOOKBACK_MS = 180_000L
        private const val MAX_LOOKBACK_MS = 60_000L
        private const val EVENT_OVERLAP_MS = 1_500L

        fun isYouTubePackage(pkg: String): Boolean {
            return pkg.contains("youtube", ignoreCase = true)
        }
    }
}
