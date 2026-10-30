package com.fischerlabs.camscore.ui.screens

import androidx.compose.foundation.background
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
import com.fischerlabs.camscore.model.DiagnosticPhase
import com.fischerlabs.camscore.ui.components.*
import com.fischerlabs.camscore.viewmodel.DiagnosticViewModel

/**
 * Screen 02/04 — The 10-Second Live Testing Screen (Scanner View).
 * Shows countdown ring, phase checklist, sensor waveform, and Non-OIS banner.
 */
@Composable
fun DiagnosticScreen(
    uiState: DiagnosticViewModel.DiagnosticUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A192F))
    ) {
        // Camera feed background placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A192F),
                            Color(0xFF112240),
                            Color(0xFF0A192F)
                        )
                    )
                )
        )

        // Semi-transparent overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Cancel button + Non-OIS banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Phase label
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF29B6F6),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            // Non-OIS info banner
            if (!uiState.hasOis && uiState.currentPhase == DiagnosticPhase.OIS_STABILITY) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1976D2).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("ℹ\uFE0F", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hardware Note: No physical OIS motor detected. " +
                                "Running Digital Sensor & Frame Stability Check (No OIS Penalty Applied).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF29B6F6),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Countdown Ring (centered)
            CountdownRing(
                totalSeconds = 10f,
                elapsedSeconds = uiState.elapsedSeconds,
                currentPhase = when (uiState.currentPhase) {
                    DiagnosticPhase.AUTOFOCUS -> "Autofocus"
                    DiagnosticPhase.OIS_STABILITY -> if (uiState.hasOis) "OIS Check" else "Stability"
                    DiagnosticPhase.LENS_SWITCH -> "Lens Switch"
                    DiagnosticPhase.COMPLETED -> "Done!"
                    else -> "Preparing..."
                },
                isRunning = uiState.isRunning
            )

            Spacer(modifier = Modifier.weight(1f))

            // Phase Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF172A45).copy(alpha = 0.9f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Diagnostic Phases",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PhaseChecklist(
                        currentPhase = uiState.currentPhase,
                        phaseProgress = uiState.phaseProgress,
                        hasOis = uiState.hasOis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Waveform Graph
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF172A45)
                )
            ) {
                WaveformGraph(
                    gyroData = uiState.gyroData,
                    opticalFlowData = uiState.opticalFlowData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
