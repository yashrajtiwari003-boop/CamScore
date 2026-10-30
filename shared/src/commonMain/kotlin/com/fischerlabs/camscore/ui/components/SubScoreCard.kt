package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sub-score breakdown card for AF, OIS, or Lens results.
 * Shows a horizontal progress bar with percentage and handles N/A state for non-OIS devices.
 */
@Composable
fun SubScoreCard(
    title: String,
    subtitle: String,
    healthPercent: Float,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isNotApplicable: Boolean = false,
    naMessage: String = "N/A — Non-OIS Device (Factory Spec)"
) {
    val barColor = when {
        isNotApplicable -> Color(0xFF64748B) // Slate gray for N/A
        healthPercent >= 85f -> Color(0xFF10B981) // Emerald green
        healthPercent >= 65f -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(barColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isNotApplicable) naMessage else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNotApplicable) Color(0xFF64748B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                if (!isNotApplicable) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (healthPercent / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Percentage or N/A badge
            if (isNotApplicable) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF64748B).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "N/A",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Text(
                    text = "${healthPercent.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
        }
    }
}
