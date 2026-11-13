package com.example.core.system.crosshair

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun CrosshairRenderer(
    config: CrosshairConfig,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(config.sizeDp.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val mainColor = Color(config.colorArgb).copy(alpha = config.opacity)
        val outlineColor = Color(config.outlineColorArgb).copy(alpha = config.opacity)
        val strokeWidthPx = config.thicknessDp.dp.toPx()
        val outlineStrokePx = strokeWidthPx + 2.dp.toPx()
        val gapPx = config.gapDp.dp.toPx()
        val lineLengthPx = (size.width / 2f) - gapPx

        rotate(config.rotationDegrees, pivot = Offset(centerX, centerY)) {
            when (config.style) {
                CrosshairStyle.CLASSIC_CROSS, CrosshairStyle.MINIMALIST, CrosshairStyle.CUSTOM -> {
                    drawCrossLines(
                        centerX, centerY, gapPx, lineLengthPx,
                        mainColor, outlineColor, strokeWidthPx, outlineStrokePx, config.hasOutline
                    )
                }
                CrosshairStyle.MINIMAL_DOT -> {
                    val dotRadius = (config.centerDotRadiusDp + 2f).dp.toPx()
                    if (config.hasOutline) {
                        drawCircle(outlineColor, radius = dotRadius + 1.5.dp.toPx(), center = Offset(centerX, centerY))
                    }
                    drawCircle(mainColor, radius = dotRadius, center = Offset(centerX, centerY))
                }
                CrosshairStyle.CIRCLE -> {
                    val circleRadius = (size.width / 3f)
                    if (config.hasOutline) {
                        drawCircle(outlineColor, radius = circleRadius, style = Stroke(outlineStrokePx))
                    }
                    drawCircle(mainColor, radius = circleRadius, style = Stroke(strokeWidthPx))
                }
                CrosshairStyle.PRECISION -> {
                    // Circle + Fine Cross
                    val circleRadius = (size.width / 3f)
                    if (config.hasOutline) {
                        drawCircle(outlineColor, radius = circleRadius, style = Stroke(outlineStrokePx))
                    }
                    drawCircle(mainColor, radius = circleRadius, style = Stroke(strokeWidthPx))
                    drawCrossLines(
                        centerX, centerY, gapPx + 4f, lineLengthPx,
                        mainColor, outlineColor, strokeWidthPx, outlineStrokePx, config.hasOutline
                    )
                }
                CrosshairStyle.TACTICAL -> {
                    // T-shape (no top bar)
                    drawTacticalT(
                        centerX, centerY, gapPx, lineLengthPx,
                        mainColor, outlineColor, strokeWidthPx, outlineStrokePx, config.hasOutline
                    )
                }
                CrosshairStyle.DYNAMIC_CROSS -> {
                    // Rotated chevron corners
                    drawCrossLines(
                        centerX, centerY, gapPx + 2f, lineLengthPx * 0.8f,
                        mainColor, outlineColor, strokeWidthPx, outlineStrokePx, config.hasOutline
                    )
                    drawCircle(mainColor, radius = (gapPx * 0.8f), style = Stroke(strokeWidthPx))
                }
                CrosshairStyle.NEON_STYLE, CrosshairStyle.FUTURISTIC -> {
                    // Angular Diamond
                    val r = size.width / 2.6f
                    if (config.hasOutline) {
                        drawCircle(outlineColor, radius = r, style = Stroke(outlineStrokePx))
                    }
                    drawCircle(mainColor, radius = r, style = Stroke(strokeWidthPx))
                    drawLine(mainColor, Offset(centerX - r, centerY), Offset(centerX + r, centerY), strokeWidth = strokeWidthPx)
                    drawLine(mainColor, Offset(centerX, centerY - r), Offset(centerX, centerY + r), strokeWidth = strokeWidthPx)
                }
            }

            // Draw center dot if enabled
            if (config.hasCenterDot) {
                val dotRadius = config.centerDotRadiusDp.dp.toPx()
                if (config.hasOutline) {
                    drawCircle(outlineColor, radius = dotRadius + 1.dp.toPx(), center = Offset(centerX, centerY))
                }
                drawCircle(mainColor, radius = dotRadius, center = Offset(centerX, centerY))
            }
        }
    }
}

private fun DrawScope.drawCrossLines(
    centerX: Float, centerY: Float,
    gap: Float, length: Float,
    mainColor: Color, outlineColor: Color,
    stroke: Float, outlineStroke: Float,
    hasOutline: Boolean
) {
    val directions = listOf(
        Pair(Offset(centerX, centerY - gap - length), Offset(centerX, centerY - gap)), // Top
        Pair(Offset(centerX, centerY + gap), Offset(centerX, centerY + gap + length)), // Bottom
        Pair(Offset(centerX - gap - length, centerY), Offset(centerX - gap, centerY)), // Left
        Pair(Offset(centerX + gap, centerY), Offset(centerX + gap + length, centerY))  // Right
    )

    if (hasOutline) {
        for ((start, end) in directions) {
            drawLine(outlineColor, start, end, strokeWidth = outlineStroke)
        }
    }
    for ((start, end) in directions) {
        drawLine(mainColor, start, end, strokeWidth = stroke)
    }
}

private fun DrawScope.drawTacticalT(
    centerX: Float, centerY: Float,
    gap: Float, length: Float,
    mainColor: Color, outlineColor: Color,
    stroke: Float, outlineStroke: Float,
    hasOutline: Boolean
) {
    val directions = listOf(
        Pair(Offset(centerX, centerY + gap), Offset(centerX, centerY + gap + length)), // Bottom
        Pair(Offset(centerX - gap - length, centerY), Offset(centerX - gap, centerY)), // Left
        Pair(Offset(centerX + gap, centerY), Offset(centerX + gap + length, centerY))  // Right
    )

    if (hasOutline) {
        for ((start, end) in directions) {
            drawLine(outlineColor, start, end, strokeWidth = outlineStroke)
        }
    }
    for ((start, end) in directions) {
        drawLine(mainColor, start, end, strokeWidth = stroke)
    }
}
