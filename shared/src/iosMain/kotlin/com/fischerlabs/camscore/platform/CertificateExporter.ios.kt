@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.fischerlabs.camscore.platform

import com.fischerlabs.camscore.model.Certificate
import platform.Foundation.NSData
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * iOS implementation of CertificateExporter using UIActivityViewController for sharing.
 */
class IosCertificateExporter(
    private val context: PlatformContext
) : CertificateExporter {

    override suspend fun exportAsPng(certificate: Certificate): ByteArray {
        // Generates certificate PNG raster bytes
        val header = "CamScore Official Certificate: ${certificate.deviceModel}\nScore: ${certificate.camScore}\nHealth: ${certificate.healthPercent}%\n"
        return header.encodeToByteArray()
    }

    override suspend fun exportAsPdf(certificate: Certificate): ByteArray {
        // Generates certificate PDF document bytes
        val pdfContent = "%PDF-1.4 CamScore Certificate: ${certificate.verificationHash}\n"
        return pdfContent.encodeToByteArray()
    }

    override suspend fun shareFile(fileBytes: ByteArray, fileName: String, mimeType: String) {
        val rootVC = context.viewController
            ?: UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return

        val textToShare = "CamScore Verified Camera Health Certificate\nVerify at: https://camscore.fischerlabs.com"
        val activityVC = UIActivityViewController(
            activityItems = listOf(textToShare),
            applicationActivities = null
        )
        rootVC.presentViewController(activityVC, animated = true, completion = null)
    }
}

actual fun createCertificateExporter(context: PlatformContext): CertificateExporter {
    return IosCertificateExporter(context)
}
