package com.fischerlabs.camscore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.HealthRating
import com.fischerlabs.camscore.model.TestResult
import com.fischerlabs.camscore.ui.components.*
import kotlinx.coroutines.delay

/**
 * Screen 03/04 — Results, Health % & Leaderboard Screen.
 * Animated CamScore counter, health gauge, sub-score cards, and monthly rank.
 */
@Composable
fun ResultScreen(
    result: TestResult,
    monthlyRank: Int?,
    onViewCertificate: () -> Unit,
    onShareResults: () -> Unit,
    onTestAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Uncapped CamScore Counter
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { -it / 2 }
        ) {
            ScoreCounter(
                score = result.camScore,
                animate = true,
                monthlyRank = monthlyRank
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Health Gauge
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HealthGauge(
                    healthPercent = result.overallHealthPercent,
                    animationDurationMs = 1500,
                    size = 180.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Health Rating Badge
                val (badgeText, badgeType) = when (result.healthRating) {
                    HealthRating.MINT -> "PASS — Camera Hardware Healthy" to BadgeType.SUCCESS
                    HealthRating.MINOR_WEAR -> "MINOR WEAR — Slight Degradation" to BadgeType.WARNING
                    HealthRating.FAIL -> "FAIL — Hardware Issue Detected" to BadgeType.ERROR
                }
                StatusBadge(text = badgeText, type = badgeType, large = true)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sub-Score Cards
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // AF Health
                SubScoreCard(
                    title = "Autofocus Speed",
                    subtitle = "Lock: ${result.afResult.lockTimeMs}ms | Score: ${result.afResult.uncappedScore} pts",
                    healthPercent = result.afResult.healthPercent,
                    icon = { Text("🎯", fontSize = 22.sp) }
                )

                // OIS Health
                SubScoreCard(
                    title = "OIS Stabilization",
                    subtitle = if (result.oisResult.isNonOisDevice)
                        "Your phone model does not come with physical OIS hardware from the factory."
                    else
                        "Dampening: ${String.format("%.0f", result.oisResult.dampeningRatio * 100)}% | Score: ${result.oisResult.uncappedScore} pts",
                    healthPercent = result.oisResult.healthPercent,
                    icon = { Text("📐", fontSize = 22.sp) },
                    isNotApplicable = result.oisResult.isNonOisDevice,
                    naMessage = "N/A — Non-OIS Device (Factory Spec)\nYour 0%–100% Camera Health is evaluated fairly based on your Autofocus Motor & Lens Sensors without counting OIS against you!"
                )

                // Lens Health
                SubScoreCard(
                    title = "Lens Switch & Color",
                    subtitle = "ΔE: ${String.format("%.1f", result.lensResult.colorShiftDeltaE)} | Lag: ${result.lensResult.switchLagMs}ms | Score: ${result.lensResult.uncappedScore} pts",
                    healthPercent = result.lensResult.healthPercent,
                    icon = { Text("🔍", fontSize = 22.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Monthly Global Rank Banner
        if (monthlyRank != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1976D2).copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🏆", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Monthly Global Rank",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "#$monthlyRank",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = result.deviceInfo.deviceModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // OIS Badge
        if (result.oisResult.isNonOisDevice) {
            Spacer(modifier = Modifier.height(12.dp))
            OisBadge(hasOis = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onViewCertificate,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("View Certificate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onShareResults,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Share Results", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Test Again
        TextButton(
            onClick = onTestAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "🔄  Run New Test",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1976D2)
            )
        }
    }
}
