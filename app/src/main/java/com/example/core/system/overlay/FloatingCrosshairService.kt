package com.example.core.system.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.XsdBoostApplication
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.crosshair.CrosshairRenderer

class FloatingCrosshairService : Service() {

    private var windowManager: WindowManager? = null
    private var crosshairView: View? = null
    private val lifecycleOwner = OverlayLifecycleOwner()

    private val container by lazy {
        (application as XsdBoostApplication).container
    }
    private val prefsRepo by lazy { container.prefsRepo }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FloatingCrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingCrosshairService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1002, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreateAndResume()
        setupCrosshairView()
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, XsdBoostApplication.OVERLAY_CHANNEL_ID)
            .setContentTitle("XSD Crosshair Active")
            .setContentText("Precision aiming assistant overlay is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupCrosshairView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Initial center; updated from persisted normalized coordinates below.
            x = 0
            y = 0
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                val globalConfig by prefsRepo.crosshairConfig.collectAsState(initial = CrosshairConfig())
                val activeGameConfig by container.activeGameCrosshairConfig.collectAsState()
                val config = activeGameConfig ?: globalConfig
                val density = resources.displayMetrics.density
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val viewSizePx = (config.sizeDp * density).toInt().coerceAtLeast(1)
                val targetX = ((screenWidth - viewSizePx).coerceAtLeast(0) * config.positionX.coerceIn(0f, 1f)).toInt()
                val targetY = ((screenHeight - viewSizePx).coerceAtLeast(0) * config.positionY.coerceIn(0f, 1f)).toInt()
                androidx.compose.runtime.SideEffect {
                    val view = crosshairView
                    val wm = windowManager
                    val lp = view?.layoutParams as? WindowManager.LayoutParams
                    if (view != null && wm != null && lp != null && (lp.x != targetX || lp.y != targetY)) {
                        lp.x = targetX
                        lp.y = targetY
                        try { wm.updateViewLayout(view, lp) } catch (_: Exception) {}
                    }
                }
                CrosshairRenderer(config = config)
            }
        }

        crosshairView = composeView
        windowManager?.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (crosshairView != null) {
            try {
                windowManager?.removeViewImmediate(crosshairView)
            } catch (_: Exception) {}
            crosshairView = null
        }
        lifecycleOwner.destroy()
    }
}
