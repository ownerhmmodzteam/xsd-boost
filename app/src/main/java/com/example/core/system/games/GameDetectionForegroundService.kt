package com.example.core.system.games

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.XsdBoostApplication
import com.example.core.database.GameProfileEntity
import com.example.core.system.overlay.FloatingOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated ForegroundService that polls Android UsageStatsManager to detect which package
 * is currently in the foreground. When the foreground package matches a registered game
 * in Room DB (boosterEnabled = true), it triggers FloatingOverlayService to display the
 * right-edge floating handle. When switching away to non-game apps or Home, it hides overlays.
 */
class GameDetectionForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var detectionLoopJob: Job? = null

    private val container by lazy { (application as XsdBoostApplication).container }
    private val foregroundMonitor by lazy { container.foregroundMonitor }
    private val gameProfileRepo by lazy { container.gameProfileRepo }

    private var registeredGamesMap: Map<String, GameProfileEntity> = emptyMap()
    private var lastTriggeredGamePackage: String? = null

    companion object {
        const val NOTIFICATION_ID = 1004
        const val ACTION_START_DETECTION = "com.example.ACTION_START_GAME_DETECTION"
        const val ACTION_STOP_DETECTION = "com.example.ACTION_STOP_GAME_DETECTION"
        private const val POLL_INTERVAL_MS = 1000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, GameDetectionForegroundService::class.java).apply {
                action = ACTION_START_DETECTION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GameDetectionForegroundService::class.java)
            context.stopService(intent)
        }

        fun evaluateTargetPackage(
            foregroundPackage: String?,
            registeredPackages: Set<String>,
            selfPackageName: String,
            isScreenInteractive: Boolean
        ): String? {
            if (!isScreenInteractive || foregroundPackage.isNullOrBlank()) return null
            if (foregroundPackage == selfPackageName) return null
            return if (foregroundPackage in registeredPackages) foregroundPackage else null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification(activeGameName = null))
        observeRegisteredGames()
        startDetectionLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_DETECTION -> {
                FloatingOverlayService.hideForNonGame(this)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startDetectionLoop()
            }
        }
        return START_STICKY
    }

    private fun observeRegisteredGames() {
        serviceScope.launch {
            gameProfileRepo.enabledProfiles.collectLatest { profiles ->
                registeredGamesMap = profiles.associateBy { it.packageName }
                val active = lastTriggeredGamePackage
                if (active != null && !registeredGamesMap.containsKey(active)) {
                    onTargetGameExited()
                }
            }
        }
    }

    private fun startDetectionLoop() {
        if (detectionLoopJob?.isActive == true) return
        detectionLoopJob = serviceScope.launch {
            while (isActive) {
                evaluateOnce()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    internal suspend fun evaluateOnce() {
        if (registeredGamesMap.isEmpty()) {
            registeredGamesMap = gameProfileRepo.enabledProfiles.firstOrNull()?.associateBy { it.packageName } ?: emptyMap()
        }
        val detectionState = withContext(Dispatchers.IO) {
            foregroundMonitor.pollForegroundPackage()
        }

        val targetPkg = evaluateTargetPackage(
            foregroundPackage = detectionState.foregroundPackage,
            registeredPackages = registeredGamesMap.keys,
            selfPackageName = packageName,
            isScreenInteractive = detectionState.isScreenInteractive
        )

        if (targetPkg != null) {
            if (lastTriggeredGamePackage != targetPkg) {
                lastTriggeredGamePackage = targetPkg
                container.detectedForegroundGame.value = targetPkg
                val gameName = registeredGamesMap[targetPkg]?.appName ?: targetPkg
                updateNotification(gameName)
                FloatingOverlayService.showForGame(this, targetPkg)
            }
        } else if (lastTriggeredGamePackage != null) {
            onTargetGameExited()
        }
    }

    private fun onTargetGameExited() {
        lastTriggeredGamePackage = null
        container.detectedForegroundGame.value = null
        updateNotification(activeGameName = null)
        FloatingOverlayService.hideForNonGame(this)
    }

    private fun updateNotification(activeGameName: String?) {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.notify(NOTIFICATION_ID, createNotification(activeGameName))
        } catch (_: Exception) {}
    }

    private fun createNotification(activeGameName: String?): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, GameDetectionForegroundService::class.java).apply {
            action = ACTION_STOP_DETECTION
        }
        val stopPending = PendingIntent.getService(
            this,
            11,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (activeGameName != null) {
            "Game aktif terdeteksi: $activeGameName • Floating Handle aktif"
        } else {
            "Memantau foreground via UsageStats untuk game terdaftar"
        }

        return NotificationCompat.Builder(this, XsdBoostApplication.OVERLAY_CHANNEL_ID)
            .setContentTitle("XSD BOOST Smart Game Detector")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPending)
            .addAction(0, "Hentikan", stopPending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        detectionLoopJob?.cancel()
        serviceJob.cancel()
        lastTriggeredGamePackage = null
        container.detectedForegroundGame.value = null
        FloatingOverlayService.hideForNonGame(this)
    }
}
