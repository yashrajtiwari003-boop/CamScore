package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Real-time waveform graph for sensor fusion visualization.
 * Overlaps gyroscope (blue) and optical flow (cyan) data as scrolling line charts.
 */
@Composable
fun WaveformGraph(
    gyroData: List<Float>,
    opticalFlowData: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    gyroColor: Color = Color(0xFF1976D2),
    opticalFlowColor: Color = Color(0xFF29B6F6),
    backgroundColor: Color = Color(0xFF0A192F).copy(alpha = 0.6f)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Background
        drawRect(backgroundColor)

        // Center line
        drawLine(
            color = Color.White.copy(alpha = 0.1f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1f
        )

        // Draw waveform helper
        fun drawWaveform(data: List<Float>, color: Color, maxValue: Float = 5f) {
            if (data.size < 2) return

            val visiblePoints = minOf(data.size, (w / 3f).toInt().coerceAtLeast(50))
            val startIndex = (data.size - visiblePoints).coerceAtLeast(0)
            val visibleData = data.subList(startIndex, data.size)

            val stepX = w / (visiblePoints - 1).toFloat().coerceAtLeast(1f)

            // Draw filled area
            val fillPath = Path().apply {
                moveTo(0f, midY)
                visibleData.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalized = (value / maxValue).coerceIn(-1f, 1f)
                    val y = midY - (normalized * h * 0.4f)
                    lineTo(x, y)
                }
                lineTo((visibleData.size - 1) * stepX, midY)
                close()
            }
            drawPath(fillPath, color.copy(alpha = 0.08f))

            // Draw line
            val linePath = Path().apply {
                visibleData.forEachIndexed { index, value ->
                    val x = index * stepX
                    val normalized = (value / maxValue).coerceIn(-1f, 1f)
                    val y = midY - (normalized * h * 0.4f)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(linePath, color, style = Stroke(width = 2f))
        }

        drawWaveform(gyroData, gyroColor, 3f)
        drawWaveform(opticalFlowData, opticalFlowColor, 5f)
    }
}
