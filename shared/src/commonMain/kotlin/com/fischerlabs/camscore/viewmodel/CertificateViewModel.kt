package com.fischerlabs.camscore.viewmodel

import com.fischerlabs.camscore.model.Certificate
import com.fischerlabs.camscore.model.TestResult
import com.fischerlabs.camscore.platform.CertificateExporter
import com.fischerlabs.camscore.repository.CertificateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Certificate screen.
 * Manages certificate generation, QR code data, and PNG/PDF export.
 */
class CertificateViewModel(
    private val certificateRepository: CertificateRepository,
    private val certificateExporter: CertificateExporter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _certificate = MutableStateFlow<Certificate?>(null)
    val certificate: StateFlow<Certificate?> = _certificate.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportSuccess = MutableStateFlow<String?>(null)
    val exportSuccess: StateFlow<String?> = _exportSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Generate a certificate from a test result and upload verification to Firebase.
     */
    fun generateCertificate(testResult: TestResult, monthlyRank: Int? = null) {
        scope.launch {
            try {
                val cert = certificateRepository.generateCertificate(testResult)
                val certWithRank = if (monthlyRank != null) {
                    cert.copy(monthlyRank = monthlyRank)
                } else cert

                _certificate.value = certWithRank

                // Upload verification hash to Firebase for QR scanning
                certificateRepository.uploadVerification(certWithRank)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to generate certificate: ${e.message}"
            }
        }
    }

    /**
     * Export the certificate as a PNG image.
     */
    fun exportAsPng() {
        val cert = _certificate.value ?: return
        scope.launch {
            _isExporting.value = true
            try {
                val pngBytes = certificateExporter.exportAsPng(cert)
                val fileName = "CamScore_${cert.deviceModel.replace(" ", "_")}_${cert.testDate}.png"
                certificateExporter.shareFile(pngBytes, fileName, "image/png")
                _exportSuccess.value = "Certificate exported as PNG"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to export PNG: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * Export the certificate as a PDF document.
     */
    fun exportAsPdf() {
        val cert = _certificate.value ?: return
        scope.launch {
            _isExporting.value = true
            try {
                val pdfBytes = certificateExporter.exportAsPdf(cert)
                val fileName = "CamScore_${cert.deviceModel.replace(" ", "_")}_${cert.testDate}.pdf"
                certificateExporter.shareFile(pdfBytes, fileName, "application/pdf")
                _exportSuccess.value = "Certificate exported as PDF"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to export PDF: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * Share the certificate via native share sheet.
     */
    fun shareCertificate() {
        exportAsPng() // Default share format is PNG
    }

    fun clearMessages() {
        _exportSuccess.value = null
        _errorMessage.value = null
    }
}
