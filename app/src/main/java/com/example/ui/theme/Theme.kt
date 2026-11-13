package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XsdBoostColorScheme = darkColorScheme(
    primary = PrimaryWhite,
    onPrimary = BgPrimary,
    primaryContainer = ElevatedSurface,
    onPrimaryContainer = PrimaryWhite,
    secondary = SecondaryWhite,
    onSecondary = BgPrimary,
    secondaryContainer = CardBg,
    onSecondaryContainer = SecondaryWhite,
    tertiary = MutedText,
    onTertiary = PrimaryWhite,
    background = BgPrimary,
    onBackground = PrimaryWhite,
    surface = CardBg,
    onSurface = PrimaryWhite,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = SecondaryWhite,
    outline = DarkMuted,
    outlineVariant = BorderColor,
    error = AccentRed,
    onError = PrimaryWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Pure Black & White theme as requested
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = XsdBoostColorScheme,
        typography = Typography,
        content = content
    )
}
