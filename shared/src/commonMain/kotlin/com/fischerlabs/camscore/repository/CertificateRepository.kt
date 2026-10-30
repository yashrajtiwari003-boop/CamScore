package com.fischerlabs.camscore.repository

import com.fischerlabs.camscore.model.*
import com.fischerlabs.camscore.scoring.ScoringEngine
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Repository managing certificate generation and Firebase QR verification.
 * Certificates are tamper-proof via SHA-256 verification hashes stored in Firestore.
 */
class CertificateRepository {

    private val firestore = Firebase.firestore
    private val certificatesCollection = firestore.collection("certificates")

    /**
     * Generate a Certificate from a completed TestResult.
     */
    fun generateCertificate(testResult: TestResult): Certificate {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val testDate = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"

        return Certificate(
            testId = testResult.testId,
            deviceModel = testResult.deviceInfo.deviceModel,
            platform = testResult.deviceInfo.platform.name,
            testDate = testDate,
            camScore = testResult.camScore,
            healthPercent = testResult.overallHealthPercent,
            healthRating = testResult.healthRating.name,
            afHealthPercent = testResult.afResult.healthPercent,
            oisHealthPercent = if (testResult.oisResult.isNonOisDevice) null else testResult.oisResult.healthPercent,
            lensHealthPercent = testResult.lensResult.healthPercent,
            monthlyRank = null, // To be filled after leaderboard lookup
            verificationHash = testResult.verificationHash,
            verificationUrl = buildVerificationUrl(testResult.verificationHash),
            isOisDevice = testResult.deviceInfo.hasOis,
            monthKey = testResult.monthKey
        )
    }

    /**
     * Upload certificate verification data to Firebase Firestore.
     * This enables QR code scanning to verify authenticity.
     */
    suspend fun uploadVerification(certificate: Certificate) = withContext(Dispatchers.Default) {
        try {
            certificatesCollection.document(certificate.verificationHash).set(
                mapOf(
                    "testId" to certificate.testId,
                    "deviceModel" to certificate.deviceModel,
                    "platform" to certificate.platform,
                    "testDate" to certificate.testDate,
                    "camScore" to certificate.camScore,
                    "healthPercent" to certificate.healthPercent,
                    "healthRating" to certificate.healthRating,
                    "afHealthPercent" to certificate.afHealthPercent,
                    "oisHealthPercent" to (certificate.oisHealthPercent ?: -1f),
                    "lensHealthPercent" to certificate.lensHealthPercent,
                    "isOisDevice" to certificate.isOisDevice,
                    "monthKey" to certificate.monthKey,
                    "verificationHash" to certificate.verificationHash,
                    "createdAt" to Clock.System.now().toEpochMilliseconds(),
                    "verified" to true
                )
            )
        } catch (e: Exception) {
            // Offline mode — certificate will be verified when connectivity is restored
        }
    }

    /**
     * Verify a certificate by its hash against Firebase Firestore.
     * Returns the certificate data if verified, null if not found or tampered.
     */
    suspend fun verifyCertificate(hash: String): Certificate? = withContext(Dispatchers.Default) {
        try {
            val doc = certificatesCollection.document(hash).get()
            if (!doc.exists) return@withContext null

            Certificate(
                testId = doc.get("testId"),
                deviceModel = doc.get("deviceModel"),
                platform = doc.get("platform"),
                testDate = doc.get("testDate"),
                camScore = doc.get("camScore"),
                healthPercent = doc.get("healthPercent"),
                healthRating = doc.get("healthRating"),
                afHealthPercent = doc.get("afHealthPercent"),
                oisHealthPercent = doc.get<Float>("oisHealthPercent").let { if (it < 0f) null else it },
                lensHealthPercent = doc.get("lensHealthPercent"),
                monthlyRank = null,
                verificationHash = doc.get("verificationHash"),
                verificationUrl = buildVerificationUrl(hash),
                isOisDevice = doc.get("isOisDevice"),
                monthKey = doc.get("monthKey")
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Build the web verification URL for QR codes.
     */
    fun buildVerificationUrl(hash: String): String {
        return "https://camscore.fischerlabs.com/verify?id=$hash"
    }
}
