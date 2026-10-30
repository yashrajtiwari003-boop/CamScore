package com.fischerlabs.camscore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.HealthRating
import com.fischerlabs.camscore.model.TestResult
import com.fischerlabs.camscore.ui.components.*

/**
 * Screen 03/06 — Test Results Screen.
 * Faithfully matches Figure 3.1:
 *  - "Test Results" top bar with share action
 *  - Large circular CamScore gauge with "862 / 1000" readout
 *  - "✓ PASS" emerald pill badge & "Camera Hardware Healthy" verdict
 *  - "Diagnostic Subscores" card section (Autofocus, OIS, Lens)
 *  - Full-width "View Certificate" royal blue pill button
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onTestAgain, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Test Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = onShareResults, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Circular CamScore Gauge matching Figure 3.1 (e.g. 862 / 1000)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Score Display
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Gauge arc representation
                    HealthGauge(
                        healthPercent = result.overallHealthPercent,
                        animationDurationMs = 1200,
                        size = 170.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.camScore}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 44.sp
                        )
                        Text(
                            text = "/ 1000",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CamScore",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1976D2),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Badge matching Figure 3.1
                val (badgeText, badgeType) = when (result.healthRating) {
                    HealthRating.MINT -> "✓ PASS" to BadgeType.SUCCESS
                    HealthRating.MINOR_WEAR -> "⚠ WARN" to BadgeType.WARNING
                    HealthRating.FAIL -> "✗ FAIL" to BadgeType.ERROR
                }
                StatusBadge(text = badgeText, type = badgeType, large = true)

                Spacer(modifier = Modifier.height(6.dp))

                val verdictText = when (result.healthRating) {
                    HealthRating.MINT -> "Camera Hardware Healthy"
                    HealthRating.MINOR_WEAR -> "Minor Mechanical Wear"
                    HealthRating.FAIL -> "Hardware Damage Detected"
                }
                Text(
                    text = verdictText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Diagnostic Subscores Section matching Figure 3.1
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Diagnostic Subscores",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Autofocus Speed Row
                SubScoreCard(
                    title = "Autofocus Speed",
                    subtitle = "Lock Time: ${result.afResult.lockTimeMs}ms • σ² Peak",
                    healthPercent = result.afResult.healthPercent,
                    icon = { Text("🎯", fontSize = 20.sp) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // OIS Stabilization Row
                SubScoreCard(
                    title = "OIS Stabilization",
                    subtitle = if (result.oisResult.isNonOisDevice)
                        "Non-OIS Fair Mode: Excluded from penalty"
                    else
                        "Dampening: ${(result.oisResult.dampeningRatio * 100).toInt()}% vs 100Hz Gyro",
                    healthPercent = result.oisResult.healthPercent,
                    icon = { Text("📐", fontSize = 20.sp) },
                    isNotApplicable = result.oisResult.isNonOisDevice,
                    naMessage = "N/A — Non-OIS Model (Factory Spec)"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lens & Colour Row
                SubScoreCard(
                    title = "Lens & Colour",
                    subtitle = "Color Shift ΔE: ${(result.lensResult.colorShiftDeltaE * 10).toInt() / 10.0} • Lag: ${result.lensResult.switchLagMs}ms",
                    healthPercent = result.lensResult.healthPercent,
                    icon = { Text("🔍", fontSize = 20.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action: View Certificate
        Button(
            onClick = onViewCertificate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text("📜 View Certificate", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Retest Link
        TextButton(onClick = onTestAgain) {
            Text("🔄 Run Another Test", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
        }
    }
}
