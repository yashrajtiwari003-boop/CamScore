package com.fischerlabs.camscore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.DiagnosticPhase

/**
 * 3-row diagnostic phase checklist with real-time progress.
 * Shows spinner (running), checkmark (done), or pending for each phase.
 */
@Composable
fun PhaseChecklist(
    currentPhase: DiagnosticPhase,
    phaseProgress: Float,
    hasOis: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PhaseRow(
            phaseName = "Autofocus Speed",
            phaseTime = "0.0s – 3.5s",
            weight = "35%",
            status = getPhaseStatus(DiagnosticPhase.AUTOFOCUS, currentPhase),
            progress = if (currentPhase == DiagnosticPhase.AUTOFOCUS) phaseProgress else
                if (currentPhase.ordinal > DiagnosticPhase.AUTOFOCUS.ordinal) 1f else 0f,
            accentColor = Color(0xFF1976D2)
        )

        PhaseRow(
            phaseName = if (hasOis) "OIS Stabilization" else "Sensor Stability (Non-OIS)",
            phaseTime = "3.5s – 7.0s",
            weight = if (hasOis) "40%" else "Skip",
            status = getPhaseStatus(DiagnosticPhase.OIS_STABILITY, currentPhase),
            progress = if (currentPhase == DiagnosticPhase.OIS_STABILITY) phaseProgress else
                if (currentPhase.ordinal > DiagnosticPhase.OIS_STABILITY.ordinal) 1f else 0f,
            accentColor = Color(0xFF29B6F6)
        )

        PhaseRow(
            phaseName = "Lens Switch & Color",
            phaseTime = "7.0s – 10.0s",
            weight = "25%",
            status = getPhaseStatus(DiagnosticPhase.LENS_SWITCH, currentPhase),
            progress = if (currentPhase == DiagnosticPhase.LENS_SWITCH) phaseProgress else
                if (currentPhase.ordinal > DiagnosticPhase.LENS_SWITCH.ordinal) 1f else 0f,
            accentColor = Color(0xFF1E88E5)
        )
    }
}

private enum class PhaseStatus { PENDING, RUNNING, DONE }

private fun getPhaseStatus(phase: DiagnosticPhase, currentPhase: DiagnosticPhase): PhaseStatus {
    return when {
        currentPhase == DiagnosticPhase.COMPLETED -> PhaseStatus.DONE
        currentPhase.ordinal > phase.ordinal -> PhaseStatus.DONE
        currentPhase == phase -> PhaseStatus.RUNNING
        else -> PhaseStatus.PENDING
    }
}

@Composable
private fun PhaseRow(
    phaseName: String,
    phaseTime: String,
    weight: String,
    status: PhaseStatus,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when (status) {
                    PhaseStatus.RUNNING -> accentColor.copy(alpha = 0.1f)
                    PhaseStatus.DONE -> Color(0xFF10B981).copy(alpha = 0.08f)
                    PhaseStatus.PENDING -> Color.Transparent
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                PhaseStatus.DONE -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                PhaseStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                }
                PhaseStatus.PENDING -> {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64748B).copy(alpha = 0.3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = phaseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = weight,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (status == PhaseStatus.DONE) Color(0xFF10B981) else accentColor
                        )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = phaseTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}
