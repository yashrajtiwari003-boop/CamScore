package com.fischerlabs.camscore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fischerlabs.camscore.model.Certificate
import com.fischerlabs.camscore.ui.components.QRCodeView
import com.fischerlabs.camscore.ui.components.StatusBadge
import com.fischerlabs.camscore.ui.components.BadgeType

/**
 * Screen 04/04 — Hardware Health & Flex Certificate View.
 * Official exportable certificate card with QR code and share buttons.
 */
@Composable
fun CertificateScreen(
    certificate: Certificate,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    isExporting: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Certificate Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                // Royal Blue Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1565C0), Color(0xFF1976D2), Color(0xFF29B6F6))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📷",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CamScore Hardware Health Certificate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "by Fischer Labs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Certificate Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Device Model
                    Text(
                        text = certificate.deviceModel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D233A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "${certificate.platform} • ${certificate.testDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Test ID: ${certificate.testId.take(12)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(20.dp))

                    // CamScore
                    Text(
                        text = "CamScore",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${certificate.camScore}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1976D2),
                        fontSize = 48.sp
                    )
                    Text(
                        text = "pts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Health Status
                    val (healthColor, healthText) = when {
                        certificate.healthPercent >= 85f -> Color(0xFF10B981) to "CERTIFIED PASS (MINT)"
                        certificate.healthPercent >= 65f -> Color(0xFFF59E0B) to "MINOR WEAR (PASS)"
                        else -> Color(0xFFEF4444) to "HARDWARE FAIL"
                    }

                    Text(
                        text = "${certificate.healthPercent.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = healthColor
                    )

                    val badgeType = when {
                        certificate.healthPercent >= 85f -> BadgeType.SUCCESS
                        certificate.healthPercent >= 65f -> BadgeType.WARNING
                        else -> BadgeType.ERROR
                    }
                    StatusBadge(text = healthText, type = badgeType, large = true)

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sub-score Breakdown
                    CertificateScoreRow("Autofocus Health", "${certificate.afHealthPercent.toInt()}%")

                    if (certificate.oisHealthPercent != null) {
                        CertificateScoreRow("OIS Health", "${certificate.oisHealthPercent.toInt()}%")
                    } else {
                        CertificateScoreRow("OIS Health", "N/A — Non-OIS Model")
                    }

                    CertificateScoreRow("Lens Health", "${certificate.lensHealthPercent.toInt()}%")

                    Spacer(modifier = Modifier.height(12.dp))

                    // OIS Tag
                    val oisTag = if (certificate.isOisDevice)
                        "OIS Equipped ✓"
                    else
                        "Non-OIS Verified ✓ — Fair Diagnostic Protocol"

                    StatusBadge(
                        text = oisTag,
                        type = if (certificate.isOisDevice) BadgeType.SUCCESS else BadgeType.INFO
                    )

                    // Monthly Rank
                    certificate.monthlyRank?.let { rank ->
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusBadge(
                            text = "Monthly Global Rank #$rank",
                            type = BadgeType.NEUTRAL
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(20.dp))

                    // QR Code
                    QRCodeView(
                        data = certificate.verificationUrl,
                        size = 140.dp,
                        label = "Scan QR to verify this certificate — Powered by Firebase"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer
                    Text(
                        text = "Verified by CamScore • fischerlabs.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onExportPng,
                enabled = !isExporting,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("📷 PNG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = onExportPdf,
                enabled = !isExporting,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("📄 PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onShare,
                enabled = !isExporting,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (isExporting) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun CertificateScoreRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D233A)
        )
    }
}
