package com.example.core.system.sensitivity

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.provider.Settings
import android.view.InputDevice
import com.example.core.system.shizuku.ShizukuStatus

enum class SensitivityCompatibilityTier(val title: String) {
    OFFICIAL_GAME_API("Official Android Game Sensitivity API"),
    OEM_INPUT_API("Official OEM / Shizuku Input API"),
    ANDROID_INPUT_SETTINGS("Official Android System Input API"),
    LOCAL_CALIBRATION("Local XSD Calibration Profile & Manual Guide")
}

data class InputCompatibilityReport(
    val activeTier: SensitivityCompatibilityTier,
    val officialGameApiStatus: String,
    val oemInputApiStatus: String,
    val systemPointerSpeed: Int?,
    val touchDeviceNames: List<String>,
    val honestExplanation: String
)

/**
 * Inspects available Android input/sensitivity capabilities in strict priority order without
 * performing touch injection, process hooking, memory patching, APK/OBB modding, or DPI tampering.
 */
object InputCompatibilityLayer {

    fun inspect(
        context: Context,
        shizukuStatus: ShizukuStatus? = null
    ): InputCompatibilityReport {
        val officialGameApiStatus = "Tidak Tersedia (Android API ${Build.VERSION.SDK_INT} tidak memiliki API publik untuk mengubah sensitivitas internal game)"

        val oemStatus = when {
            shizukuStatus?.isPermissionGranted == true ->
                "Tersedia via Shizuku (Read-only telemetry / touch info, tanpa injeksi sentuhan ke game)"
            shizukuStatus?.isInstalled == true ->
                "Shizuku terpasang namun izin belum diberikan (fallback ke konfigurasi standar)"
            else ->
                "Tidak tersedia (OEM game input API tertutup)"
        }

        val pointerSpeed = try {
            Settings.System.getInt(context.contentResolver, "pointer_speed")
        } catch (_: Exception) {
            null
        }

        val touchDevices: List<String> = try {
            val im = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
            val ids = im?.inputDeviceIds ?: IntArray(0)
            ids.toList()
                .mapNotNull { id -> im?.getInputDevice(id) }
                .filter { device ->
                    (device.sources and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN ||
                        (device.sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
                }
                .mapNotNull { device -> device.name }
                .distinct()
                .take(3)
        } catch (_: Exception) {
            emptyList()
        }

        val tier = when {
            pointerSpeed != null || touchDevices.isNotEmpty() -> SensitivityCompatibilityTier.ANDROID_INPUT_SETTINGS
            else -> SensitivityCompatibilityTier.LOCAL_CALIBRATION
        }

        val explanation = buildString {
            append("XSD BOOST menyimpan profil Sensitivity X/Y per-game secara lokal dan menyediakan pad simulasi visual. ")
            append("Nilai slider tidak disuntikkan ke Free Fire atau game lain karena Android membatasi akses input antar-aplikasi demi keamanan & integritas anti-cheat.")
        }

        return InputCompatibilityReport(
            activeTier = tier,
            officialGameApiStatus = officialGameApiStatus,
            oemInputApiStatus = oemStatus,
            systemPointerSpeed = pointerSpeed,
            touchDeviceNames = touchDevices,
            honestExplanation = explanation
        )
    }

    /**
     * Pure helper for interactive simulation pad scaling: maps local touch drag deltas
     * into visual reticle displacement using Sensitivity X/Y (1..26).
     */
    fun computeSimulatedReticleDelta(
        rawDx: Float,
        rawDy: Float,
        sensX: Int,
        sensY: Int
    ): Pair<Float, Float> {
        // Smooth normalized gain for the local calibration preview only.
        fun gain(value: Int): Float {
            val normalized = (value.coerceIn(1, 26) - 1) / 25f
            return 0.50f + normalized * normalized * 2.0f
        }
        val factorX = gain(sensX)
        val factorY = gain(sensY)
        return (rawDx * factorX) to (rawDy * factorY)
    }
}
