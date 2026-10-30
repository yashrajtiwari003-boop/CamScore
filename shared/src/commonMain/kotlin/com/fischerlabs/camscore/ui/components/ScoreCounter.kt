package com.fischerlabs.camscore.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.ui.theme.CamScoreTheme

@Composable
fun ScoreCounter(
    score: Int,
    animate: Boolean = true,
    monthlyRank: Int? = null,
    modifier: Modifier = Modifier
) {
    val colors = CamScoreTheme.colors

    var animationTriggered by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { animationTriggered = true }

    val animatedScore by animateIntAsState(
        targetValue = if (animationTriggered) score else 0,
        animationSpec = tween(durationMillis = 2000),
        label = "scoreCountUp"
    )

    val displayScore = if (animate) animatedScore else score
    val formattedScore = formatWithCommas(displayScore)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CamScore",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp
            ),
            color = colors.textSecondary
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = formattedScore,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary
            )
            Text(
                text = " pts",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (monthlyRank != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "🏆",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "Monthly Rank #$monthlyRank",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.accentBlue
                )
            }
        }
    }
}

private fun formatWithCommas(number: Int): String {
    val str = number.toString()
    val result = StringBuilder()
    var count = 0
    for (i in str.length - 1 downTo 0) {
        result.append(str[i])
        count++
        if (count % 3 == 0 && i > 0) {
            result.append(',')
        }
    }
    return result.reverse().toString()
}
