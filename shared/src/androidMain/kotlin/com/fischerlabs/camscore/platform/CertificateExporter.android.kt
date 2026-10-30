package com.fischerlabs.camscore.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.fischerlabs.camscore.model.Certificate
import com.fischerlabs.camscore.model.HealthRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

/**
 * Android CertificateExporter implementation.
 *
 * Renders official certificates matching Figure 3.1:
 *   - Royal blue header banner ("CamScore by Fischer Labs")
 *   - Device model, platform, date, and hash ID
 *   - Big CamScore number (e.g. 862 / 1000) and CERTIFIED PASS / MINOR WEAR / FAIL badge
 *   - Component scores (Autofocus, OIS, Lens)
 *   - Export as PNG / PDF, and share via Android Intent with FileProvider.
 */
class AndroidCertificateExporter(
    private val context: Context
) : CertificateExporter {

    companion object {
        private const val CERT_WIDTH = 1200
        private const val CERT_HEIGHT = 1600
        private const val MARGIN = 80f
        private const val FILE_PROVIDER_AUTHORITY = "com.fischerlabs.camscore.fileprovider"
    }

    override suspend fun exportAsPng(certificate: Certificate): ByteArray {
        return withContext(Dispatchers.Default) {
            val bitmap = renderCertificateBitmap(certificate)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        }
    }

    override suspend fun exportAsPdf(certificate: Certificate): ByteArray {
        return withContext(Dispatchers.Default) {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(CERT_WIDTH, CERT_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)

            drawCertificateContent(page.canvas, certificate)

            document.finishPage(page)

            val outputStream = ByteArrayOutputStream()
            document.writeTo(outputStream)
            document.close()

            outputStream.toByteArray()
        }
    }

    override suspend fun shareFile(fileBytes: ByteArray, fileName: String, mimeType: String) {
        withContext(Dispatchers.IO) {
            val shareDir = File(context.cacheDir, "shared_certificates")
            shareDir.mkdirs()

            val shareFile = File(shareDir, fileName)
            shareFile.writeBytes(fileBytes)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                shareFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "CamScore Certificate: ${shareFile.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Verified CamScore Camera Health Certificate by Fischer Labs."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Certificate")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }

    private fun renderCertificateBitmap(certificate: Certificate): Bitmap {
        val bitmap = Bitmap.createBitmap(CERT_WIDTH, CERT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCertificateContent(canvas, certificate)
        return bitmap
    }

    private fun drawCertificateContent(canvas: Canvas, certificate: Certificate) {
        // Background
        canvas.drawColor(Color.WHITE)

        // Outer border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRect(
            MARGIN / 2, MARGIN / 2,
            CERT_WIDTH - MARGIN / 2, CERT_HEIGHT - MARGIN / 2,
            borderPaint
        )

        // Inner frame
        val innerBorderPaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(
            MARGIN, MARGIN,
            CERT_WIDTH.toFloat() - MARGIN, CERT_HEIGHT.toFloat() - MARGIN,
            innerBorderPaint
        )

        var yPos = MARGIN + 80f

        // Brand Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D233A")
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("CamScore", CERT_WIDTH / 2f, yPos, titlePaint)
        yPos += 40f

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("BY FISCHER LABS", CERT_WIDTH / 2f, yPos, subtitlePaint)
        yPos += 50f

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 50f

        // Certificate Header
        val certTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Camera Hardware Health Certificate", CERT_WIDTH / 2f, yPos, certTitlePaint)
        yPos += 70f

        // Metadata labels
        val labelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 26f
            isAntiAlias = true
        }

        // Device
        canvas.drawText("Device:", MARGIN + 40f, yPos, labelPaint)
        canvas.drawText(certificate.deviceModel, MARGIN + 220f, yPos, valuePaint)
        yPos += 45f

        // Platform
        canvas.drawText("Platform:", MARGIN + 40f, yPos, labelPaint)
        canvas.drawText(certificate.platform, MARGIN + 220f, yPos, valuePaint)
        yPos += 45f

        // Date
        canvas.drawText("Test Date:", MARGIN + 40f, yPos, labelPaint)
        canvas.drawText(certificate.testDate, MARGIN + 220f, yPos, valuePaint)
        yPos += 70f

        // Divider
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 60f

        // Score Section
        val scoreHeaderPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("UNCAPPED CAMSCORE", CERT_WIDTH / 2f, yPos, scoreHeaderPaint)
        yPos += 65f

        val scorePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            textSize = 88f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("${certificate.camScore}", CERT_WIDTH / 2f, yPos, scorePaint)
        yPos += 30f

        val healthTextPaint = Paint().apply {
            color = when (certificate.healthRating) {
                HealthRating.MINT -> Color.parseColor("#10B981")
                HealthRating.MINOR_WEAR -> Color.parseColor("#F59E0B")
                HealthRating.FAIL -> Color.parseColor("#EF4444")
            }
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val ratingBadge = when (certificate.healthRating) {
            HealthRating.MINT -> "✓ CERTIFIED PASS (${certificate.healthPercent.toInt()}%)"
            HealthRating.MINOR_WEAR -> "⚠ MINOR WEAR (${certificate.healthPercent.toInt()}%)"
            HealthRating.FAIL -> "✗ HARDWARE FAIL (${certificate.healthPercent.toInt()}%)"
        }
        canvas.drawText(ratingBadge, CERT_WIDTH / 2f, yPos, healthTextPaint)
        yPos += 60f

        // Divider
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 50f

        // Subscores
        val scores = listOf(
            "Autofocus Speed" to certificate.afHealthPercent,
            (if (certificate.isOisDevice) "OIS Optical Stabilization" else "Non-OIS Fair Stability") to (certificate.oisHealthPercent ?: 100f),
            "Lens Transition & Color" to certificate.lensHealthPercent
        )

        for ((label, score) in scores) {
            yPos = drawScoreBar(canvas, label, score, yPos)
            yPos += 35f
        }

        yPos += 30f
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 50f

        // Verification Hash
        val hashPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Verification: ${certificate.verificationHash.take(32)}...", CERT_WIDTH / 2f, yPos, hashPaint)
        yPos += 35f

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "Verified by CamScore • Fischer Labs On-Device Diagnostic Engine",
            CERT_WIDTH / 2f, yPos, footerPaint
        )
    }

    private fun drawScoreBar(
        canvas: Canvas,
        label: String,
        score: Float,
        yPos: Float
    ): Float {
        val barLeft = MARGIN + 40f
        val barRight = CERT_WIDTH - MARGIN - 140f
        val barHeight = 28f
        val barWidth = barRight - barLeft

        val labelPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText(label, barLeft, yPos, labelPaint)

        val barTop = yPos + 12f
        val bgPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            barLeft, barTop, barRight, barTop + barHeight,
            barHeight / 2, barHeight / 2, bgPaint
        )

        val fillWidth = (barWidth * (score / 100f)).coerceIn(0f, barWidth)
        val fillPaint = Paint().apply {
            color = getScoreColor(score)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            barLeft, barTop, barLeft + fillWidth, barTop + barHeight,
            barHeight / 2, barHeight / 2, fillPaint
        )

        val scoreTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(
            String.format(Locale.US, "%.1f%%", score),
            barRight + 15f, barTop + barHeight - 4f,
            scoreTextPaint
        )

        return barTop + barHeight
    }

    private fun getScoreColor(score: Float): Int {
        return when {
            score >= 85f -> Color.parseColor("#10B981")
            score >= 65f -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#EF4444")
        }
    }
}

actual fun createCertificateExporter(context: PlatformContext): CertificateExporter {
    return AndroidCertificateExporter(context)
}
