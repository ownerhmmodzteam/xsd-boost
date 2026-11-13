package com.example.core.system.screenshot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ScreenshotFeedback
import com.example.XsdBoostApplication

/**
 * Translucent proxy activity to request standard Android MediaProjection consent
 * without disrupting the underlying game session.
 */
class ScreenshotActivity : ComponentActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val container = (application as? XsdBoostApplication)?.container
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ScreenshotService.startCapture(this, result.resultCode, data)
        } else {
            container?.screenshotFeedback?.value = ScreenshotFeedback(
                message = "Screenshot dibatalkan atau izin MediaProjection ditolak.",
                isSuccess = false
            )
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager == null) {
            (application as? XsdBoostApplication)?.container?.screenshotFeedback?.value = ScreenshotFeedback(
                message = "MediaProjection tidak didukung pada perangkat ini.",
                isSuccess = false
            )
            finish()
            return
        }
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, ScreenshotActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
