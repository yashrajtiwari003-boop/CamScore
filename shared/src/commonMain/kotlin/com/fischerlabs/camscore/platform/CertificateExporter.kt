package com.fischerlabs.camscore.platform

import com.fischerlabs.camscore.model.Certificate

/**
 * Interface for exporting and sharing CamScore certificates
 * as PNG images or PDF documents.
 */
interface CertificateExporter {
    /**
     * Render the certificate as a PNG image.
     * @param certificate The certificate data to render
     * @return PNG-encoded byte array
     */
    suspend fun exportAsPng(certificate: Certificate): ByteArray

    /**
     * Render the certificate as a PDF document.
     * @param certificate The certificate data to render
     * @return PDF-encoded byte array
     */
    suspend fun exportAsPdf(certificate: Certificate): ByteArray

    /**
     * Share a file using the platform's native share sheet.
     * @param fileBytes The raw file bytes to share
     * @param fileName Suggested file name (e.g., "CamScore_Certificate.png")
     * @param mimeType MIME type (e.g., "image/png" or "application/pdf")
     */
    suspend fun shareFile(fileBytes: ByteArray, fileName: String, mimeType: String)
}

expect fun createCertificateExporter(context: PlatformContext): CertificateExporter
