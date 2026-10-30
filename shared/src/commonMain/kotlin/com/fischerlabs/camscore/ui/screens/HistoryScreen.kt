package com.fischerlabs.camscore.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.HealthRating
import com.fischerlabs.camscore.model.TestResult
import com.fischerlabs.camscore.ui.components.BadgeType
import com.fischerlabs.camscore.ui.components.StatusBadge

/**
 * Screen — Test History & Long-Term 0%–100% Health Trend Log.
 * Graphs hardware health degradation over time and provides access to past benchmark certificates.
 */
@Composable
fun HistoryScreen(
    results: List<TestResult>,
    onResultClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Test History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hardware health degradation over time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (results.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Clear All", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Tests Recorded Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Execute your first 10-second camera benchmark to begin tracking hardware health!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Health Trend Line Graph Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Health Trend (0%–100%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val latest = results.firstOrNull()?.overallHealthPercent ?: 100f
                        Text(
                            text = "Latest: ${latest.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (latest >= 85f) Color(0xFF10B981) else if (latest >= 65f) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HealthTrendChart(
                        healthValues = results.reversed().map { it.overallHealthPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Past Diagnostic Logs (${results.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results) { item ->
                    HistoryCard(result = item, onClick = { onResultClick(item.testId) })
                }
            }
        }
    }
}

@Composable
private fun HealthTrendChart(
    healthValues: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Horizontal guide lines (100%, 85%, 65%)
        val line100Y = 0f
        val line85Y = h * (1f - 0.85f)
        val line65Y = h * (1f - 0.65f)

        drawLine(
            color = Color(0xFF10B981).copy(alpha = 0.2f),
            start = Offset(0f, line85Y),
            end = Offset(w, line85Y),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
            start = Offset(0f, line65Y),
            end = Offset(w, line65Y),
            strokeWidth = 1f
        )

        if (healthValues.isEmpty()) return@Canvas

        val stepX = if (healthValues.size > 1) w / (healthValues.size - 1) else w / 2

        val points = healthValues.mapIndexed { index, health ->
            val x = if (healthValues.size > 1) index * stepX else w / 2
            val y = h * (1f - (health / 100f).coerceIn(0f, 1f))
            Offset(x, y)
        }

        // Fill path
        if (points.size > 1) {
            val fillPath = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(fillPath, Color(0xFF1976D2).copy(alpha = 0.1f))

            // Stroke line
            val strokePath = Path().apply {
                points.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }
            drawPath(strokePath, Color(0xFF1976D2), style = Stroke(width = 2.5f))
        }

        // Draw points
        points.forEach { pt ->
            drawCircle(Color.White, radius = 4f, center = pt)
            drawCircle(Color(0xFF1976D2), radius = 2.5f, center = pt)
        }
    }
}

@Composable
private fun HistoryCard(
    result: TestResult,
    onClick: () -> Unit
) {
    val ratingBadgeType = when (result.healthRating) {
        HealthRating.MINT -> BadgeType.SUCCESS
        HealthRating.MINOR_WEAR -> BadgeType.WARNING
        HealthRating.FAIL -> BadgeType.ERROR
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health percentage circle
            val circleColor = when (result.healthRating) {
                HealthRating.MINT -> Color(0xFF10B981)
                HealthRating.MINOR_WEAR -> Color(0xFFF59E0B)
                HealthRating.FAIL -> Color(0xFFEF4444)
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${result.overallHealthPercent.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = circleColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.deviceInfo.deviceModel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${result.monthKey} • ${if (result.deviceInfo.hasOis) "OIS" else "Non-OIS"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${result.camScore} pts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1976D2)
                )

                Spacer(modifier = Modifier.height(4.dp))

                StatusBadge(
                    text = result.healthRating.name,
                    type = ratingBadgeType,
                    showIcon = false
                )
            }
        }
    }
}
