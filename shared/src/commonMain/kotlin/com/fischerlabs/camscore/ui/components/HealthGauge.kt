package com.fischerlabs.camscore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.ui.model.BadgeType
import com.fischerlabs.camscore.ui.theme.CamScoreTheme

@Composable
fun HealthGauge(
    healthPercent: Float,
    animationDurationMs: Int = 1500,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val colors = CamScoreTheme.colors

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    val animatedPercent by animateFloatAsState(
        targetValue = if (animationTriggered) healthPercent else 0f,
        animationSpec = tween(durationMillis = animationDurationMs),
        label = "gaugeAnimation"
    )

    val gaugeColor = when {
        animatedPercent >= 85f -> colors.gaugeFillGreen
        animatedPercent >= 65f -> colors.gaugeFillAmber
        else -> colors.gaugeFillRed
    }

    val statusText = when {
        healthPercent >= 85f -> "PASS"
        healthPercent >= 65f -> "MINOR WEAR"
        else -> "FAIL"
    }

    val badgeType = when {
        healthPercent >= 85f -> BadgeType.SUCCESS
        healthPercent >= 65f -> BadgeType.WARNING
        else -> BadgeType.ERROR
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = this.size.width - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Background track ring
                drawArc(
                    color = colors.gaugeTrack,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Filled arc
                val sweepAngle = (animatedPercent / 100f) * 270f
                drawArc(
                    color = gaugeColor,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center percentage text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${animatedPercent.toInt()}%",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = (size.value * 0.2f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // Status badge below gauge
        StatusBadge(
            text = statusText,
            type = badgeType,
            icon = true,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
