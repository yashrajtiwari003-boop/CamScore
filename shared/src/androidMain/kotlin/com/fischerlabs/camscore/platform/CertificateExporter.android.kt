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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android CertificateExporter implementation.
 *
 * Renders certificates using Android Canvas/Paint for PNG,
 * PdfDocument for PDF, and Intent.ACTION_SEND with FileProvider for sharing.
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
            // Write the file to the app's cache directory for FileProvider access
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
                putExtra(Intent.EXTRA_SUBJECT, "CamScore Certificate")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out my CamScore camera benchmark results!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Certificate")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }

    // ---------- Private rendering helpers ----------

    /**
     * Render the full certificate as an Android Bitmap.
     */
    private fun renderCertificateBitmap(certificate: Certificate): Bitmap {
        val bitmap = Bitmap.createBitmap(CERT_WIDTH, CERT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCertificateContent(canvas, certificate)
        return bitmap
    }

    /**
     * Draw all certificate content onto a Canvas.
     * Used by both PNG (via Bitmap Canvas) and PDF (via PdfDocument Page Canvas).
     */
    private fun drawCertificateContent(canvas: Canvas, certificate: Certificate) {
        // Background
        canvas.drawColor(Color.WHITE)

        // Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#1A237E") // Deep indigo
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRect(
            MARGIN / 2, MARGIN / 2,
            CERT_WIDTH - MARGIN / 2, CERT_HEIGHT - MARGIN / 2,
            borderPaint
        )

        // Inner border
        val innerBorderPaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
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

        // App title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("CamScore", CERT_WIDTH / 2f, yPos, titlePaint)
        yPos += 40f

        // Subtitle
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#5C6BC0")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("by Fischer Labs", CERT_WIDTH / 2f, yPos, subtitlePaint)
        yPos += 60f

        // Divider line
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#C5CAE9")
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 50f

        // "Camera Benchmark Certificate"
        val certTitlePaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Camera Benchmark Certificate", CERT_WIDTH / 2f, yPos, certTitlePaint)
        yPos += 80f

        // Device info
        val labelPaint = Paint().apply {
            color = Color.parseColor("#616161")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 26f
            isAntiAlias = true
        }

        // Device
        canvas.drawText("Device:", MARGIN + 40f, yPos, labelPaint)
        canvas.drawText(certificate.deviceModel, MARGIN + 220f, yPos, valuePaint)
        yPos += 45f

        // OS Version
        canvas.drawText("OS:", MARGIN + 40f, yPos, labelPaint)
        canvas.drawText(certificate.osVersion, MARGIN + 220f, yPos, valuePaint)
        yPos += 45f

        // Date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(certificate.timestamp))
        canvas.drawText("Date:", MARGIN + 40f, yPos, labelPaint)
        canvas.drawText(dateStr, MARGIN + 220f, yPos, valuePaint)
        yPos += 80f

        // Divider
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 60f

        // Overall Score - large centered
        val scoreHeaderPaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("OVERALL SCORE", CERT_WIDTH / 2f, yPos, scoreHeaderPaint)
        yPos += 70f

        val scorePaint = Paint().apply {
            color = getScoreColor(certificate.overallScore)
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            String.format(Locale.US, "%.1f", certificate.overallScore),
            CERT_WIDTH / 2f, yPos, scorePaint
        )
        yPos += 30f

        // Score label (e.g., "/ 100")
        val maxScorePaint = Paint().apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("/ 100", CERT_WIDTH / 2f, yPos, maxScorePaint)
        yPos += 80f

        // Divider
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 50f

        // Sub-scores section
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("COMPONENT SCORES", CERT_WIDTH / 2f, yPos, sectionPaint)
        yPos += 50f

        // Draw each score bar
        val scores = listOf(
            "Autofocus Speed" to certificate.autofocusScore,
            "OIS Effectiveness" to certificate.oisScore,
            "Lens Consistency" to certificate.lensScore
        )

        for ((label, score) in scores) {
            yPos = drawScoreBar(canvas, label, score, yPos)
            yPos += 30f
        }

        yPos += 30f

        // Divider
        canvas.drawLine(MARGIN + 40f, yPos, CERT_WIDTH - MARGIN - 40f, yPos, dividerPaint)
        yPos += 50f

        // Hash / ID
        val hashPaint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("ID: ${certificate.id}", CERT_WIDTH / 2f, yPos, hashPaint)
        yPos += 30f

        // App version
        val versionPaint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "CamScore v${certificate.appVersion} • Fischer Labs",
            CERT_WIDTH / 2f, yPos, versionPaint
        )
    }

    /**
     * Draw a horizontal score bar with label and value.
     * Returns the updated Y position after drawing.
     */
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

        // Label
        val labelPaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText(label, barLeft, yPos, labelPaint)

        val barTop = yPos + 10f

        // Background bar
        val bgPaint = Paint().apply {
            color = Color.parseColor("#E8EAF6")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            barLeft, barTop, barRight, barTop + barHeight,
            barHeight / 2, barHeight / 2, bgPaint
        )

        // Filled bar
        val fillWidth = (barWidth * (score / 100f)).coerceIn(0f, barWidth)
        val fillPaint = Paint().apply {
            color = getScoreColor(score)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            barLeft, barTop, barLeft + fillWidth, barTop + barHeight,
            barHeight / 2, barHeight / 2, fillPaint
        )

        // Score value
        val scoreTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(
            String.format(Locale.US, "%.1f", score),
            barRight + 15f, barTop + barHeight - 4f,
            scoreTextPaint
        )

        return barTop + barHeight
    }

    /**
     * Get a color based on the score value.
     * Green for high scores, amber for medium, red for low.
     */
    private fun getScoreColor(score: Float): Int {
        return when {
            score >= 80f -> Color.parseColor("#2E7D32") // Green
            score >= 60f -> Color.parseColor("#F9A825") // Amber
            score >= 40f -> Color.parseColor("#EF6C00") // Orange
            else -> Color.parseColor("#C62828") // Red
        }
    }
}

actual fun createCertificateExporter(context: PlatformContext): CertificateExporter {
    return AndroidCertificateExporter(context)
}
