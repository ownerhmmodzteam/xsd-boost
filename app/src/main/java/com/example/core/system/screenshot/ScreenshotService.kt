package com.example.core.system.screenshot

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ScreenshotFeedback
import com.example.XsdBoostApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class ScreenshotService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val hasCaptured = AtomicBoolean(false)

    companion object {
        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val NOTIFICATION_ID = 1003

        fun startCapture(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenshotService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMediaProjectionForeground()

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            postFeedback("Izin MediaProjection tidak tersedia.", isSuccess = false)
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            // Brief wait so consent dialog / translucent activity finishes closing cleanly
            delay(320)
            beginCapture(resultCode, resultData)
        }
        return START_NOT_STICKY
    }

    private fun startMediaProjectionForeground() {
        val notification: Notification = NotificationCompat.Builder(this, XsdBoostApplication.SCREENSHOT_CHANNEL_ID)
            .setContentTitle("XSD BOOST Capture")
            .setContentText("Capturing screen frame via MediaProjection...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginCapture(resultCode: Int, resultData: Intent) {
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, resultData)
            if (projection == null) {
                postFeedback("Gagal memulai MediaProjection.", isSuccess = false)
                stopSelf()
                return
            }
            mediaProjection = projection

            val handler = Handler(Looper.getMainLooper())
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    cleanupCaptureResources()
                }
            }, handler)

            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay?.getRealMetrics(metrics)
            val width = metrics.widthPixels.coerceAtLeast(720)
            val height = metrics.heightPixels.coerceAtLeast(1280)
            val density = metrics.densityDpi.coerceAtLeast(DisplayMetrics.DENSITY_DEFAULT)

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            reader.setOnImageAvailableListener({ r ->
                if (!hasCaptured.compareAndSet(false, true)) return@setOnImageAvailableListener
                val image = try {
                    r.acquireLatestImage()
                } catch (_: Exception) {
                    null
                }
                if (image != null) {
                    serviceScope.launch {
                        val savedLocation = withContext(Dispatchers.IO) {
                            processAndSaveImage(image, width, height)
                        }
                        image.close()
                        cleanupCaptureResources()
                        if (savedLocation != null) {
                            postFeedback("Screenshot tersimpan: $savedLocation", isSuccess = true)
                        } else {
                            postFeedback("Layar diproteksi (FLAG_SECURE) atau gagal menyimpan frame.", isSuccess = false)
                        }
                        stopSelf()
                    }
                }
            }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "XsdBoostCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            // Safety timeout if OS blocks frame (e.g., FLAG_SECURE content)
            serviceScope.launch {
                delay(2500)
                if (hasCaptured.compareAndSet(false, true)) {
                    cleanupCaptureResources()
                    postFeedback("Capture timeout — kemungkinan aplikasi dilindungi FLAG_SECURE.", isSuccess = false)
                    stopSelf()
                }
            }
        } catch (e: SecurityException) {
            cleanupCaptureResources()
            postFeedback("Akses screenshot dibatasi oleh sistem: ${e.localizedMessage}", isSuccess = false)
            stopSelf()
        } catch (e: Exception) {
            cleanupCaptureResources()
            postFeedback("Gagal menangkap layar: ${e.localizedMessage}", isSuccess = false)
            stopSelf()
        }
    }

    private fun processAndSaveImage(image: Image, width: Int, height: Int): String? {
        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val paddedBitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            paddedBitmap.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
            if (cropped !== paddedBitmap) {
                paddedBitmap.recycle()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "XSD_BOOST_$timestamp.png"

            val savedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/XSD BOOST")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                    "Pictures/XSD BOOST/$fileName"
                } else {
                    null
                }
            } else {
                val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "XSD BOOST").apply { mkdirs() }
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                outFile.name
            }

            cropped.recycle()
            savedPath
        } catch (_: Exception) {
            null
        }
    }

    private fun postFeedback(msg: String, isSuccess: Boolean) {
        (application as? XsdBoostApplication)?.container?.screenshotFeedback?.value =
            ScreenshotFeedback(message = msg, isSuccess = isSuccess)
    }

    private fun cleanupCaptureResources() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        cleanupCaptureResources()
    }
}
