package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * Three-column pre-test readiness indicators for the Home Screen.
 * Shows: Lighting | Texture | Readiness with green/red status dots.
 */
@Composable
fun ReadinessIndicator(
    lightingOk: Boolean,
    textureOk: Boolean,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReadinessItem(
            label = "Lighting",
            statusText = if (lightingOk) "Good" else "Low",
            isOk = lightingOk,
            emoji = "☀\uFE0F",
            modifier = Modifier.weight(1f)
        )
        ReadinessItem(
            label = "Texture",
            statusText = if (textureOk) "Good" else "Poor",
            isOk = textureOk,
            emoji = "🧱",
            modifier = Modifier.weight(1f)
        )
        ReadinessItem(
            label = "Readiness",
            statusText = if (isReady) "Ready" else "Not Ready",
            isOk = isReady,
            emoji = "✅",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReadinessItem(
    label: String,
    statusText: String,
    isOk: Boolean,
    emoji: String,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isOk) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = dotColor
                )
            }
        }
    }
}
