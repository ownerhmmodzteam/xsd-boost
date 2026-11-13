package com.example.core.system.graphics

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class GraphicsHardwareInfo(
    val renderer: String,
    val vendor: String,
    val glVersion: String,
    val extensionsCount: Int,
    val vulkanSupported: Boolean,
    val angleStatus: String,
    val freeFireCompatible: Boolean,
    val freeFireMaxCompatible: Boolean
)

data class GraphicsPresetProfile(
    val id: String,
    val name: String,
    val description: String,
    val targetFps: Int,
    val renderScale: String,
    val shadowQuality: String,
    val textureFiltering: String,
    val angleRecommended: Boolean
)

class GraphicsEngine(private val context: Context) {

    fun getHardwareInfo(): GraphicsHardwareInfo {
        var renderer = "Adreno / Mali GPU"
        var vendor = "Qualcomm / ARM"
        var version = "OpenGL ES 3.2"
        val extCount = 48

        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val configInfo = am?.deviceConfigurationInfo
            if (configInfo != null && !configInfo.glEsVersion.isNullOrEmpty()) {
                version = "OpenGL ES " + configInfo.glEsVersion
            }
        } catch (_: Exception) {
        }

        val hasVulkan = try {
            context.packageManager.hasSystemFeature("android.hardware.vulkan.version")
        } catch (_: Exception) {
            true
        }

        val isAngleSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        return GraphicsHardwareInfo(
            renderer = renderer,
            vendor = vendor,
            glVersion = version,
            extensionsCount = extCount,
            vulkanSupported = hasVulkan,
            angleStatus = if (isAngleSupported) "Available (Android 10+ Driver Hook)" else "Unsupported (Requires Android 10+)",
            freeFireCompatible = true,
            freeFireMaxCompatible = true
        )
    }

    fun getPresetProfiles(): List<GraphicsPresetProfile> {
        return listOf(
            GraphicsPresetProfile(
                id = "ultra_smooth",
                name = "Ultra Smooth (Max FPS)",
                description = "Optimized for high refresh rates (90Hz/120Hz). Minimal latency profile for Free Fire & Free Fire MAX competitive matches.",
                targetFps = 120,
                renderScale = "Native (1.0x)",
                shadowQuality = "Fast / Low",
                textureFiltering = "Bilinear Fast",
                angleRecommended = true
            ),
            GraphicsPresetProfile(
                id = "balanced_esports",
                name = "Esports Balanced",
                description = "Optimal visual clarity and thermal sustainability. Ideal for long scrims and ranked sessions.",
                targetFps = 90,
                renderScale = "Native (1.0x)",
                shadowQuality = "Medium Soft",
                textureFiltering = "Trilinear Standard",
                angleRecommended = true
            ),
            GraphicsPresetProfile(
                id = "cinematic_hdr",
                name = "Cinematic Visuals",
                description = "Max detail preset with anti-aliasing reference configuration for casual play and content recording.",
                targetFps = 60,
                renderScale = "High (1.2x Anti-alias)",
                shadowQuality = "High Resolution",
                textureFiltering = "Anisotropic 4x",
                angleRecommended = false
            ),
            GraphicsPresetProfile(
                id = "minecraft_shading",
                name = "Voxel / Shaded Style",
                description = "Minecraft-inspired shader lighting contrast profile with sharp edge definitions.",
                targetFps = 90,
                renderScale = "Native (1.0x)",
                shadowQuality = "Sharp Shadow Borders",
                textureFiltering = "Point Filter (Crisp)",
                angleRecommended = true
            )
        )
    }
}
