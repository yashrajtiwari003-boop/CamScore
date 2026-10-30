package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.ui.theme.CamScoreTheme

@Composable
fun CountdownRing(
    totalSeconds: Float = 10f,
    elapsedSeconds: Float,
    currentPhase: String,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = CamScoreTheme.colors
    val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0f)
    val progress = if (totalSeconds > 0f) (remaining / totalSeconds) else 0f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 10.dp.toPx()
                val arcSize = size.width - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Background track
                drawArc(
                    color = colors.gaugeTrack.copy(alpha = 0.4f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Depleting arc
                if (isRunning) {
                    drawArc(
                        color = colors.accentBlue,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📷",
                    fontSize = 24.sp
                )
                val timeStr = "${((remaining * 10).toInt() / 10.0)}s"
                Text(
                    text = if (isRunning) timeStr else "Ready",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Phase label
        Text(
            text = currentPhase,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
