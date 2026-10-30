package com.fischerlabs.camscore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 3-slide illustrated Onboarding Tutorial.
 * Introduces users to the 10-second automated diagnostic, 0%–100% health tracking, and anti-fraud certificates.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlide by remember { mutableStateOf(0) }

    val slides = listOf(
        OnboardingSlide(
            emoji = "⚡",
            title = "10-Second Hardware Benchmark",
            subtitle = "Zero Lab Equipment Required",
            description = "In a single 10-second hands-off test, CamScore sweeps zoom 1x–10x, tests Autofocus speed, and measures OIS motor stability directly on your device."
        ),
        OnboardingSlide(
            emoji = "🛡\uFE0F",
            title = "0%–100% Camera Health",
            subtitle = "Catch Hidden Mechanical Damage",
            description = "Motorcycle handlebar vibrations shatter OIS springs and accidental drops misalign focus motors. Track your camera's true physical condition over time."
        ),
        OnboardingSlide(
            emoji = "📜",
            title = "Anti-Fraud Health Certificate",
            subtitle = "Tamper-Proof Used Phone Proof",
            description = "Generate an official QR-verifiable Hardware Health Certificate backed by Firebase Firestore. Prove your camera is 100% authentic before buying or selling."
        )
    )

    val slide = slides[currentSlide]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onComplete) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Slide Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF1976D2).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(slide.emoji, fontSize = 54.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = slide.subtitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1976D2),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = slide.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Slide Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.indices.forEach { index ->
                val isActive = index == currentSlide
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (isActive) 24.dp else 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isActive) Color(0xFF1976D2)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Button
        Button(
            onClick = {
                if (currentSlide < slides.size - 1) {
                    currentSlide++
                } else {
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Text(
                text = if (currentSlide == slides.size - 1) "Get Started 🚀" else "Continue →",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private data class OnboardingSlide(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String
)
