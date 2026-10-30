package com.fischerlabs.camscore.model

import kotlinx.serialization.Serializable

/**
 * Result of the autofocus phase (Phase 1, 0.0–3.5 s).
 *
 * @property lockTimeMs Time in milliseconds to reach 95 % of peak Laplacian variance.
 * @property peakSharpness Peak Laplacian variance (σ²) measured at focus lock.
 * @property sharpnessVariance Variance of sharpness samples over the AF phase window.
 * @property healthPercent Capped health percentage (0–100).
 * @property uncappedScore Unbounded performance score used for leaderboard ranking.
 */
@Serializable
data class AutofocusResult(
    val lockTimeMs: Long,
    val peakSharpness: Double,
    val sharpnessVariance: Double,
    val healthPercent: Float,
    val uncappedScore: Double
)

/**
 * Result of the OIS stability phase (Phase 2, 3.5–7.0 s).
 *
 * @property dampeningRatio Stabilisation effectiveness (1.0 = perfect, 0.0 = no dampening).
 * @property pixelShake Mean pixel displacement measured via PyrLK optical flow (dpx).
 * @property gyroShake Mean angular velocity from the gyroscope (ωgyro, rad/s).
 * @property healthPercent Capped health percentage (0–100), or -1 if device has no OIS.
 * @property uncappedScore Unbounded performance score.
 * @property isNonOisDevice True when the device lacks OIS hardware.
 */
@Serializable
data class OisResult(
    val dampeningRatio: Double,
    val pixelShake: Double,
    val gyroShake: Double,
    val healthPercent: Float,
    val uncappedScore: Double,
    val isNonOisDevice: Boolean
)

/**
 * Result of the lens-switch phase (Phase 3, 7.0–10.0 s).
 *
 * @property colorShiftDeltaE CIE LAB ΔE between pre- and post-switch frames.
 * @property switchLagMs Latency of the lens transition in milliseconds.
 * @property healthPercent Capped health percentage (0–100).
 * @property uncappedScore Unbounded performance score.
 */
@Serializable
data class LensSwitchResult(
    val colorShiftDeltaE: Double,
    val switchLagMs: Long,
    val healthPercent: Float,
    val uncappedScore: Double
)

/**
 * Complete diagnostic test result containing per-phase scores and the overall CamScore.
 *
 * @property testId Unique test identifier (UUID v4 string).
 * @property deviceInfo Hardware description of the tested device.
 * @property timestamp Instant the test completed (kotlinx.datetime).
 * @property afResult Autofocus phase result.
 * @property oisResult OIS stability phase result.
 * @property lensResult Lens switch phase result.
 * @property overallHealthPercent Weighted health percentage (0–100).
 * @property camScore Uncapped composite benchmark score.
 * @property healthRating Categorical health classification.
 * @property monthKey YYYY-MM key for monthly leaderboard grouping.
 * @property verificationHash SHA-256 hash for certificate authenticity verification.
 */
@Serializable
data class TestResult(
    val testId: String,
    val deviceInfo: DeviceInfo,
    val timestamp: kotlinx.datetime.Instant,
    val afResult: AutofocusResult,
    val oisResult: OisResult,
    val lensResult: LensSwitchResult,
    val overallHealthPercent: Float,
    val camScore: Int,
    val healthRating: HealthRating,
    val monthKey: String,
    val verificationHash: String
)
