package com.fischerlabs.camscore.model

import kotlinx.serialization.Serializable

/**
 * Shareable diagnostic certificate proving a device's camera health.
 *
 * @property testId UUID of the originating test run.
 * @property deviceModel Marketing name of the device.
 * @property platform Platform string ("Android" / "iOS").
 * @property testDate ISO-8601 date string (YYYY-MM-DD).
 * @property camScore Uncapped benchmark score.
 * @property healthPercent Overall weighted health percentage.
 * @property healthRating Categorical rating (MINT / MINOR_WEAR / FAIL).
 * @property afHealthPercent Autofocus health percentage.
 * @property oisHealthPercent OIS health percentage, or null for non-OIS devices.
 * @property lensHealthPercent Lens-switch health percentage.
 * @property monthlyRank Device rank in the monthly leaderboard, null if not yet ranked.
 * @property verificationHash SHA-256 hash for certificate verification.
 * @property verificationUrl Full URL where the certificate can be verified online.
 * @property isOisDevice Whether the tested device has OIS.
 * @property monthKey YYYY-MM leaderboard period key.
 */
@Serializable
data class Certificate(
    val testId: String,
    val deviceModel: String,
    val platform: String,
    val testDate: String,
    val camScore: Int,
    val healthPercent: Float,
    val healthRating: HealthRating,
    val afHealthPercent: Float,
    val oisHealthPercent: Float?,
    val lensHealthPercent: Float,
    val monthlyRank: Int?,
    val verificationHash: String,
    val verificationUrl: String,
    val isOisDevice: Boolean,
    val monthKey: String
)
