package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardBg
import com.example.ui.theme.DarkSurfaceGradient
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SecondaryWhite

@Composable
fun MetricSparklineCard(
    title: String,
    currentValue: String,
    history: List<Float>,
    unit: String = "",
    minVal: Float = 0f,
    maxVal: Float = 100f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceGradient)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    color = MutedText,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$currentValue $unit",
                    fontSize = 14.sp,
                    color = PrimaryWhite,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Real Canvas Sparkline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                if (history.size < 2) return@Canvas

                val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                val path = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, size.height)

                history.forEachIndexed { i, value ->
                    val normalized = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val y = size.height - (normalized * size.height)
                    val x = i * stepX

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(size.width, size.height)
                fillPath.close()

                // Draw gradient under graph
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x02FFFFFF))
                    )
                )

                // Draw line
                drawPath(
                    path = path,
                    color = PrimaryWhite,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
