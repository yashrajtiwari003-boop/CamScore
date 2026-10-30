package com.fischerlabs.camscore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.LeaderboardEntry
import com.fischerlabs.camscore.model.Platform
import com.fischerlabs.camscore.ui.components.BadgeType
import com.fischerlabs.camscore.ui.components.StatusBadge
import com.fischerlabs.camscore.viewmodel.LeaderboardViewModel.LeaderboardFilter

/**
 * Screen — Monthly Global Mobile Ranking Leaderboard.
 * Ranks Android & iOS devices worldwide with monthly rollover and fair Non-OIS filtering.
 */
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    currentMonth: String,
    selectedFilter: LeaderboardFilter,
    onMonthChange: (String) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onFilterChange: (LeaderboardFilter) -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Global Ranking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Uncapped CamScores worldwide",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRefresh) {
                Text("🔄", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Text("◀", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentMonth,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Text("▶", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeaderboardFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            text = filter.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1976D2),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Loading or List View
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1976D2))
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Rankings Yet for This Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run a benchmark test to submit your device score!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    LeaderboardCard(entry = entry, position = index + 1)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCard(
    entry: LeaderboardEntry,
    position: Int
) {
    val medalColor = when (position) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.rank == 1) Color(0xFF1976D2).copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number / Medal
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        medalColor?.copy(alpha = 0.2f)
                            ?: MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (position in 1..3) {
                    val medalEmoji = when (position) {
                        1 -> "🥇"
                        2 -> "🥈"
                        else -> "🥉"
                    }
                    Text(medalEmoji, fontSize = 18.sp)
                } else {
                    Text(
                        text = "#$position",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Device Name & Platform Badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.deviceModel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val platformIcon = if (entry.platform == Platform.ANDROID) "🤖 Android" else "🍎 iOS"
                    Text(
                        text = platformIcon,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (entry.isOisDevice) {
                        StatusBadge(text = "OIS", type = BadgeType.SUCCESS, showIcon = false)
                    } else {
                        StatusBadge(text = "Non-OIS", type = BadgeType.INFO, showIcon = false)
                    }
                }
            }

            // CamScore points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.camScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
