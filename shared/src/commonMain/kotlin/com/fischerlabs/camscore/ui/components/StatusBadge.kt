package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pill-shaped status badge for PASS/MINOR WEAR/FAIL and info badges.
 */
enum class BadgeType(val backgroundColor: Color, val textColor: Color, val iconText: String) {
    SUCCESS(Color(0xFF10B981), Color.White, "✓"),
    WARNING(Color(0xFFF59E0B), Color.White, "⚠"),
    ERROR(Color(0xFFEF4444), Color.White, "✗"),
    INFO(Color(0xFF1976D2), Color.White, "ℹ"),
    NEUTRAL(Color(0xFF64748B), Color.White, "•")
}

@Composable
fun StatusBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    large: Boolean = false
) {
    val paddingH = if (large) 16.dp else 10.dp
    val paddingV = if (large) 8.dp else 4.dp
    val fontSize = if (large) 14.sp else 11.sp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(type.backgroundColor)
            .padding(horizontal = paddingH, vertical = paddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showIcon) {
            Text(
                text = type.iconText,
                color = type.textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = type.textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * OIS hardware badge for Home and Results screens.
 */
@Composable
fun OisBadge(
    hasOis: Boolean,
    modifier: Modifier = Modifier
) {
    StatusBadge(
        text = if (hasOis) "OIS Supported" else "Non-OIS Fair Mode Active",
        type = if (hasOis) BadgeType.SUCCESS else BadgeType.INFO,
        modifier = modifier,
        showIcon = true
    )
}
