package com.fischerlabs.camscore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.fischerlabs.camscore.model.DiagnosticPhase
import com.fischerlabs.camscore.ui.components.*
import com.fischerlabs.camscore.viewmodel.DiagnosticViewModel

/**
 * Screen 02/06 — Camera Test Screen.
 * Faithfully matches Figure 3.1:
 *  - "Camera Test" title with "10-Second Diagnostic" subtitle
 *  - Circular countdown ring showing "6.8s" and camera aperture icon
 *  - "Scanning your camera hardware..." prompt
 *  - 3 Diagnostic Phases with real-time progress bars & status indicators
 *  - 100Hz Gyroscope vs Optical Flow waveform graph footer
 */
@Composable
fun DiagnosticScreen(
    uiState: DiagnosticViewModel.DiagnosticUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar matching Figure 3.1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Camera Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "10-Second Diagnostic",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF29B6F6),
                    fontSize = 11.sp
                )
            }

            // Info or Flash icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Non-OIS notification banner if applicable
        if (!uiState.hasOis && uiState.currentPhase == DiagnosticPhase.OIS_STABILITY) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1976D2).copy(alpha = 0.15f)
                )
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ", fontSize = 16.sp, color = Color(0xFF29B6F6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Non-OIS Fair Mode: Checking Digital Frame Stability (No OIS Penalty Applied).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF29B6F6),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Circular Countdown Ring Gauge with Aperture Icon & "Scanning your camera hardware..."
        CountdownRing(
            totalSeconds = 10f,
            elapsedSeconds = uiState.elapsedSeconds,
            currentPhase = "Scanning your camera hardware...",
            isRunning = uiState.isRunning
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Diagnostic Phases Checklist matching Figure 3.1
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Diagnostic Phases",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                PhaseChecklist(
                    currentPhase = uiState.currentPhase,
                    phaseProgress = uiState.phaseProgress,
                    hasOis = uiState.hasOis
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Live Sinusoidal Waveform Graph Footer matching Figure 3.1
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            WaveformGraph(
                gyroData = uiState.gyroData,
                opticalFlowData = uiState.opticalFlowData,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
