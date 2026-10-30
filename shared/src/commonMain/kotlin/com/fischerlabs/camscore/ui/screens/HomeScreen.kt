package com.fischerlabs.camscore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.PreCheckStatus
import com.fischerlabs.camscore.ui.components.*

/**
 * Screen 01/04 — Home Screen & Ambient Light Guard.
 * Live camera preview, Start Test button, readiness indicators, and OIS badge.
 */
@Composable
fun HomeScreen(
    preCheckStatus: PreCheckStatus,
    onStartTest: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onDismissLowLight: () -> Unit = {},
    showLowLightDialog: Boolean = false,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Preview Card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { -it / 3 }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A192F)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Camera preview placeholder — replaced by native camera view
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Camera Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Point at a well-lit textured scene",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        // OIS Badge overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            OisBadge(hasOis = preCheckStatus.hasOis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Readiness Indicators
            ReadinessIndicator(
                lightingOk = preCheckStatus.lightingOk,
                textureOk = preCheckStatus.textureOk,
                isReady = preCheckStatus.isReady
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start Test Button
            Button(
                onClick = onStartTest,
                enabled = preCheckStatus.isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    disabledContainerColor = Color(0xFF64748B).copy(alpha = 0.3f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = if (preCheckStatus.isReady) "▶  Start Test" else "Checking Environment...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error message
            preCheckStatus.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF59E0B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Instructions
            Text(
                text = "Hold phone steady • Point at textured wall • Ensure good lighting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Monthly Global Ranking Quick Access
            Card(
                onClick = onNavigateToLeaderboard,
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Global Ranking",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = "Compare your CamScore with phones worldwide",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Text("→", fontSize = 18.sp, color = Color(0xFF1976D2))
                }
            }
        }

        // Low-light dialog
        if (showLowLightDialog) {
            AlertDialog(
                onDismissRequest = onDismissLowLight,
                title = {
                    Text("⚠\uFE0F Low Light Detected", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Current ambient light is too low (${preCheckStatus.ambientLux.toInt()} lux). " +
                            "Move to a well-lit area for accurate results. Minimum recommended: 50 lux."
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismissLowLight) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}
