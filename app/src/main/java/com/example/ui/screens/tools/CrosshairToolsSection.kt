package com.example.ui.screens.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.crosshair.CrosshairConfig
import com.example.core.system.crosshair.CrosshairRenderer
import com.example.core.system.crosshair.CrosshairStyle
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BorderActive
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

fun LazyListScope.crosshairStudioSection(
    config: CrosshairConfig,
    isOverlayActive: Boolean,
    onUpdateConfig: (CrosshairConfig) -> Unit,
    onToggleOverlay: (Boolean) -> Unit
) {
    item(key = "crosshair_preview") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, BorderActive, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Subtle tactical reticle grid background for clear contrast
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 24.dp.toPx()
                val gridColor = Color(0x14FFFFFF)
                var x = step
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }

            CrosshairRenderer(config = config)

            Text(
                text = "CANVAS LIVE AIM PREVIEW",
                color = SecondaryWhite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }
    }

    item(key = "crosshair_overlay_toggle") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Floating Crosshair Overlay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryWhite
                    )
                    Text(
                        text = "Renders aiming reticle above game surface",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = onToggleOverlay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryWhite,
                        checkedTrackColor = ElevatedSurface,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = ElevatedSurface
                    ),
                    modifier = Modifier.testTag("floating_crosshair_toggle")
                )
            }
        }
    }

    item(key = "crosshair_presets") {
        Column {
            Text(
                text = "CROSSHAIR PRESETS",
                fontSize = 11.sp,
                color = MutedText,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CrosshairStyle.values().forEach { style ->
                    val isSelected = config.style == style
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryWhite else ElevatedSurface)
                            .border(1.dp, if (isSelected) PrimaryWhite else BorderColor, RoundedCornerShape(10.dp))
                            .clickable { onUpdateConfig(config.copy(style = style)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.title,
                            fontSize = 11.sp,
                            color = if (isSelected) BgPrimary else SecondaryWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    item(key = "crosshair_sliders") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ToolCardBrush)
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PRECISION TUNING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryWhite,
                    fontFamily = FontFamily.Monospace
                )

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Size", color = MutedText, fontSize = 12.sp)
                        Text("${config.sizeDp.toInt()} dp", color = PrimaryWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = config.sizeDp,
                        onValueChange = { onUpdateConfig(config.copy(sizeDp = it)) },
                        valueRange = 12f..60f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Thickness", color = MutedText, fontSize = 12.sp)
                        Text(
                            String.format("%.1f dp", config.thicknessDp),
                            color = PrimaryWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = config.thicknessDp,
                        onValueChange = { onUpdateConfig(config.copy(thicknessDp = it)) },
                        valueRange = 1f..8f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Center Gap", color = MutedText, fontSize = 12.sp)
                        Text("${config.gapDp.toInt()} dp", color = PrimaryWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = config.gapDp,
                        onValueChange = { onUpdateConfig(config.copy(gapDp = it)) },
                        valueRange = 0f..20f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rotation", color = MutedText, fontSize = 12.sp)
                        Text("${config.rotationDegrees.toInt()}°", color = PrimaryWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = config.rotationDegrees,
                        onValueChange = { onUpdateConfig(config.copy(rotationDegrees = it)) },
                        valueRange = 0f..90f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryWhite, activeTrackColor = PrimaryWhite)
                    )
                }
            }
        }
    }
}
