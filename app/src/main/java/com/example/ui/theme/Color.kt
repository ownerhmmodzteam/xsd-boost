package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// XSD BOOST - Premium Black & White Gradient Palette
val BgPrimary = Color(0xFF050505)
val BgSecondary = Color(0xFF101010)
val CardBg = Color(0xFF171717)
val ElevatedSurface = Color(0xFF202020)
val BorderColor = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)
val BorderActive = Color(0x40FFFFFF)

val PrimaryWhite = Color(0xFFFFFFFF)
val SecondaryWhite = Color(0xFFD4D4D4)
val MutedText = Color(0xFF858585)
val DarkMuted = Color(0xFF404040)

// Accent cyber indicators
val AccentGreen = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)
val AccentCyan = Color(0xFF00E5FF)

// Gradients
val MetallicLightGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFAFAFAF), Color(0xFF404040))
)

val DarkSurfaceGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF262626), Color(0xFF080808))
)

val GlassBorderGradient = Brush.linearGradient(
    colors = listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF), Color(0x05FFFFFF))
)

val CardGlowGradient = Brush.radialGradient(
    colors = listOf(Color(0x1AFFFFFF), Color(0x00000000))
)
