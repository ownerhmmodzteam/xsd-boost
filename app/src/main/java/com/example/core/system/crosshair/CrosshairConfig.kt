package com.example.core.system.crosshair

import androidx.compose.ui.graphics.Color

enum class CrosshairStyle(val title: String) {
    CLASSIC_CROSS("Classic Cross"),
    MINIMAL_DOT("Minimal Dot"),
    CIRCLE("Circle Holo"),
    PRECISION("Precision Scope"),
    TACTICAL("Tactical Cross"),
    DYNAMIC_CROSS("Dynamic Cross"),
    NEON_STYLE("Cyber Neon"),
    FUTURISTIC("Futuristic HUD"),
    MINIMALIST("Minimalist Plus"),
    CUSTOM("Custom Aim")
}

data class CrosshairConfig(
    val style: CrosshairStyle = CrosshairStyle.CLASSIC_CROSS,
    val colorArgb: Long = 0xFFFFFFFF,
    val sizeDp: Float = 28f,
    val thicknessDp: Float = 2.5f,
    val opacity: Float = 0.95f,
    val hasCenterDot: Boolean = true,
    val centerDotRadiusDp: Float = 2f,
    val gapDp: Float = 6f,
    val hasOutline: Boolean = true,
    val outlineColorArgb: Long = 0xFF000000,
    val rotationDegrees: Float = 0f
)
