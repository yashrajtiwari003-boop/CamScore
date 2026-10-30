package com.fischerlabs.camscore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.PreCheckStatus
import com.fischerlabs.camscore.ui.components.*

/**
 * Screen 01/06 — Home Screen & Ambient Light Guard.
 * Faithfully matches Figure 3.1:
 *  - "Know Your Camera. In 10 Seconds." headline
 *  - High-fidelity camera viewfinder preview
 *  - 3-column readiness badges (Lighting, Texture, Readiness)
 *  - Prominent "Start Test →" royal blue button
 */
@Composable
fun HomeScreen(
    preCheckStatus: PreCheckStatus,
    onStartTest: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onDismissLowLight: () -> Unit = {},
    showLowLightDialog: Boolean = false,
    isDarkMode: Boolean = true,
    onDarkModeToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Headline & Subtitle matching Figure 3.1
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { -it / 4 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Know Your Camera.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "In 10 Seconds.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1976D2),
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Get an instant hardware health check with a 0-100% CamScore and a tamper-proof certificate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Realistic Room Camera Viewfinder Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF112240)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Stylized Living Room Viewfinder Graphic (sofa, painting, warm lighting)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Room wall background with subtle texture gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2A3B53), Color(0xFF1A2639))
                            )
                        )

                        // Warm wall light glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFE082).copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(w * 0.5f, h * 0.25f),
                                radius = w * 0.45f
                            )
                        )

                        // Framed painting on textured wall
                        val frameW = w * 0.32f
                        val frameH = h * 0.36f
                        val frameX = (w - frameW) / 2
                        val frameY = h * 0.12f
                        drawRect(
                            color = Color(0xFFD4AF37),
                            topLeft = Offset(frameX, frameY),
                            size = androidx.compose.ui.geometry.Size(frameW, frameH)
                        )
                        drawRect(
                            color = Color(0xFF1E3A5F),
                            topLeft = Offset(frameX + 3, frameY + 3),
                            size = androidx.compose.ui.geometry.Size(frameW - 6, frameH - 6)
                        )

                        // Modern textured sofa
                        val sofaY = h * 0.58f
                        drawRoundRect(
                            color = Color(0xFF4A6572),
                            topLeft = Offset(w * 0.12f, sofaY),
                            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.35f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                        )
                        // Sofa cushions
                        drawRoundRect(
                            color = Color(0xFF5A7582),
                            topLeft = Offset(w * 0.16f, sofaY + 12),
                            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.22f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = Color(0xFF5A7582),
                            topLeft = Offset(w * 0.52f, sofaY + 12),
                            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.22f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                    }

                    // Viewfinder Framing Reticle Corners
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Top-Right OIS Badge
                        Box(modifier = Modifier.align(Alignment.TopEnd)) {
                            OisBadge(hasOis = preCheckStatus.hasOis)
                        }

                        // Bottom Center Instruction Pill
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "Point at textured scene",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Readiness Indicators matching Figure 3.1
            ReadinessIndicator(
                lightingOk = preCheckStatus.lightingOk,
                textureOk = preCheckStatus.textureOk,
                isReady = preCheckStatus.isReady
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Start Test Button (Royal Blue pill button)
            Button(
                onClick = onStartTest,
                enabled = preCheckStatus.isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
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
                    text = "Start Test →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instructions Footer
            Text(
                text = "Hold phone steady • Point at textured wall • Ensure good lighting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }

        // Low-light dialog
        if (showLowLightDialog) {
            AlertDialog(
                onDismissRequest = onDismissLowLight,
                title = { Text("Low Light Detected", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Current ambient light is too low. Move to a well-lit area for accurate hardware benchmarking.")
                },
                confirmButton = {
                    TextButton(onClick = onDismissLowLight) { Text("Got it") }
                }
            )
        }
    }
}
