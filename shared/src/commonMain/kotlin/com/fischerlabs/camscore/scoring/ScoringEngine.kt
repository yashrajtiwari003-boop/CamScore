package com.fischerlabs.camscore.scoring

import com.fischerlabs.camscore.model.AutofocusResult
import com.fischerlabs.camscore.model.DeviceInfo
import com.fischerlabs.camscore.model.LensSwitchResult
import com.fischerlabs.camscore.model.OisResult
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Core scoring engine implementing the CamScore benchmark formulas.
 *
 * Health percentages are capped to 0–100, while uncapped scores are unbounded
 * so that flagship devices can differentiate on the leaderboard.
 *
 * All scoring uses continuous interpolation curves rather than hard-edged
 * look-up tables, which prevents score-cliff artefacts near thresholds.
 */
object ScoringEngine {

    // ------------------------------------------------------------------ //
    //  Constants                                                          //
    // ------------------------------------------------------------------ //

    /** Baseline expected pixel displacement per unit gyro shake for a non-OIS device. */
    private const val NON_OIS_PIXEL_PER_GYRO = 12.0

    /** Verification URL prefix. */
    private const val VERIFICATION_BASE_URL = "https://camscore.fischerlabs.com/verify/"

    // ------------------------------------------------------------------ //
    //  Phase 1 – Autofocus                                                //
    // ------------------------------------------------------------------ //

    /**
     * Scores the autofocus phase.
     *
     * ### Health mapping
     * | Lock time         | Health range |
     * |-------------------|-------------|
     * | < 250 ms          | 85 – 100 %  |
     * | 250 – 600 ms      | 40 – 84 %   |
     * | > 600 ms          | 0 – 39 %    |
     *
     * ### Uncapped score
     * Faster lock and higher peak sharpness yield exponentially more points.
     * ```
     * S_AF = sharpnessFactor × speedFactor
     * speedFactor  = 1000 / (lockTimeMs + 50)          // hyperbolic speed curve
     * sharpnessFactor = ln(peakSharpness + 1) × 25     // log-scaled sharpness
     * ```
     *
     * @param lockTimeMs Time to reach 95 % of peak Laplacian variance.
     * @param peakSharpness Peak Laplacian variance (σ²).
     */
    fun calculateAfScore(lockTimeMs: Long, peakSharpness: Double): AutofocusResult {
        val clampedLock = max(1L, lockTimeMs)

        // ---------- health ----------
        val healthPercent: Float = when {
            clampedLock < 250  -> lerp(85f, 100f, 1f - clampedLock / 250f)
            clampedLock <= 600 -> lerp(40f, 84f, 1f - (clampedLock - 250f) / 350f)
            else               -> lerp(0f, 39f, 1f - min(1.0, (clampedLock - 600.0) / 1400.0).toFloat())
        }.coerceIn(0f, 100f)

        // ---------- uncapped ----------
        val speedFactor = 1000.0 / (clampedLock.toDouble() + 50.0)
        val sharpnessFactor = ln(max(peakSharpness, 1.0) + 1.0) * 25.0
        val uncappedScore = speedFactor * sharpnessFactor

        // Sharpness variance is derived from how spread the samples are around the peak.
        // We model it as a fraction of peakSharpness scaled by lock consistency.
        val sharpnessVariance = peakSharpness * (clampedLock / 2000.0).coerceIn(0.01, 0.5)

        return AutofocusResult(
            lockTimeMs = clampedLock,
            peakSharpness = peakSharpness,
            sharpnessVariance = sharpnessVariance,
            healthPercent = healthPercent,
            uncappedScore = uncappedScore
        )
    }

    // ------------------------------------------------------------------ //
    //  Phase 2 – OIS Stability                                            //
    // ------------------------------------------------------------------ //

    /**
     * Scores the OIS stability phase.
     *
     * ### Dampening ratio
     * ```
     * dampeningRatio = 1 - (pixelShakeDpx / (gyroShakeOmega × NON_OIS_PIXEL_PER_GYRO))
     * ```
     * A ratio near 1.0 means the OIS fully compensates; near 0.0 means
     * the actuator is stuck or absent.
     *
     * ### Health mapping (OIS devices)
     * | Dampening ratio | Health range |
     * |-----------------|-------------|
     * | ≥ 0.75          | 85 – 100 %  |
     * | 0.40 – 0.74     | 40 – 84 %   |
     * | < 0.40          | 0 – 39 %    |
     *
     * ### Non-OIS devices
     * Health is set to -1 (excluded from overall weighting). The uncapped
     * score reflects raw frame stability instead.
     *
     * @param pixelShakeDpx Mean optical-flow displacement (dpx).
     * @param gyroShakeOmega Mean gyroscope angular velocity (rad/s).
     * @param hasOis Whether the device has OIS hardware.
     */
    fun calculateOisScore(
        pixelShakeDpx: Double,
        gyroShakeOmega: Double,
        hasOis: Boolean
    ): OisResult {
        val expectedPixelShake = max(gyroShakeOmega * NON_OIS_PIXEL_PER_GYRO, 0.001)
        val dampeningRatio = (1.0 - (pixelShakeDpx / expectedPixelShake)).coerceIn(0.0, 1.0)

        if (!hasOis) {
            // Non-OIS: score raw stability; lower pixel shake is better.
            val stabilityFactor = 100.0 / (pixelShakeDpx + 1.0)
            return OisResult(
                dampeningRatio = dampeningRatio,
                pixelShake = pixelShakeDpx,
                gyroShake = gyroShakeOmega,
                healthPercent = -1f, // excluded
                uncappedScore = stabilityFactor,
                isNonOisDevice = true
            )
        }

        // OIS device health
        val healthPercent: Float = when {
            dampeningRatio >= 0.75 -> lerp(85f, 100f, ((dampeningRatio - 0.75) / 0.25).toFloat())
            dampeningRatio >= 0.40 -> lerp(40f, 84f, ((dampeningRatio - 0.40) / 0.35).toFloat())
            else                   -> lerp(0f, 39f, (dampeningRatio / 0.40).toFloat())
        }.coerceIn(0f, 100f)

        // Uncapped score: higher dampening & lower residual shake → more points
        val uncappedScore = (dampeningRatio.pow(1.5) * 200.0) + (50.0 / (pixelShakeDpx + 0.5))

        return OisResult(
            dampeningRatio = dampeningRatio,
            pixelShake = pixelShakeDpx,
            gyroShake = gyroShakeOmega,
            healthPercent = healthPercent,
            uncappedScore = uncappedScore,
            isNonOisDevice = false
        )
    }

    // ------------------------------------------------------------------ //
    //  Phase 3 – Lens Switch                                              //
    // ------------------------------------------------------------------ //

    /**
     * Scores the lens-switch phase.
     *
     * ### Health mapping
     * | ΔE / Lag                | Health range |
     * |-------------------------|-------------|
     * | ΔE < 4.0 & lag < 150ms | 85 – 100 %  |
     * | ΔE 4–8.5 & lag < 350ms | 40 – 84 %   |
     * | ΔE > 8.5 or lag ≥ 350  | 0 – 39 %    |
     *
     * ### Single-lens devices
     * Receive a neutral 75 % health and a modest uncapped score since there
     * is nothing to switch.
     *
     * @param colorShiftDeltaE CIE LAB ΔE between pre/post switch frames.
     * @param switchLagMs Transition latency in milliseconds.
     * @param hasMultipleLenses Whether the device has more than one lens.
     */
    fun calculateLensScore(
        colorShiftDeltaE: Double,
        switchLagMs: Long,
        hasMultipleLenses: Boolean
    ): LensSwitchResult {
        if (!hasMultipleLenses) {
            // Single-lens: neutral result
            return LensSwitchResult(
                colorShiftDeltaE = 0.0,
                switchLagMs = 0L,
                healthPercent = 75f,
                uncappedScore = 30.0
            )
        }

        val clampedLag = max(1L, switchLagMs)

        val healthPercent: Float = when {
            colorShiftDeltaE < 4.0 && clampedLag < 150 -> {
                // Interpolate 85-100 based on how much better than threshold
                val deFactor = (1.0 - colorShiftDeltaE / 4.0).toFloat()
                val lagFactor = (1.0 - clampedLag.toDouble() / 150.0).toFloat()
                lerp(85f, 100f, (deFactor + lagFactor) / 2f)
            }
            colorShiftDeltaE <= 8.5 && clampedLag < 350 -> {
                val deFactor = if (colorShiftDeltaE < 4.0) 1f
                               else (1.0 - (colorShiftDeltaE - 4.0) / 4.5).toFloat()
                val lagFactor = if (clampedLag < 150) 1f
                                else (1.0 - (clampedLag - 150.0) / 200.0).toFloat()
                lerp(40f, 84f, ((deFactor + lagFactor) / 2f).coerceIn(0f, 1f))
            }
            else -> {
                val dePenalty = if (colorShiftDeltaE <= 8.5) 1f
                                else (1.0 - min((colorShiftDeltaE - 8.5) / 12.0, 1.0)).toFloat()
                val lagPenalty = if (clampedLag < 350) 1f
                                 else (1.0 - min((clampedLag - 350.0) / 650.0, 1.0)).toFloat()
                lerp(0f, 39f, ((dePenalty + lagPenalty) / 2f).coerceIn(0f, 1f))
            }
        }.coerceIn(0f, 100f)

        // Uncapped: lower ΔE & lag → more points
        val deScore = 100.0 / (colorShiftDeltaE + 1.0)
        val lagScore = 500.0 / (clampedLag.toDouble() + 20.0)
        val uncappedScore = deScore + lagScore

        return LensSwitchResult(
            colorShiftDeltaE = colorShiftDeltaE,
            switchLagMs = clampedLag,
            healthPercent = healthPercent,
            uncappedScore = uncappedScore
        )
    }

    // ------------------------------------------------------------------ //
    //  Overall Health                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Computes the weighted overall health percentage.
     *
     * Weight redistribution for non-OIS / single-lens devices:
     * - **With OIS**: 35 % AF + 40 % OIS + 25 % Lens
     * - **Without OIS, multi-lens**: 60 % AF + 0 % OIS + 40 % Lens
     * - **Without OIS, single-lens**: 100 % AF
     */
    fun calculateOverallHealth(
        af: AutofocusResult,
        ois: OisResult,
        lens: LensSwitchResult,
        deviceInfo: DeviceInfo
    ): Float {
        return when {
            deviceInfo.hasOis -> {
                af.healthPercent * 0.35f +
                ois.healthPercent * 0.40f +
                lens.healthPercent * 0.25f
            }
            deviceInfo.hasMultipleLenses -> {
                af.healthPercent * 0.60f +
                lens.healthPercent * 0.40f
            }
            else -> {
                af.healthPercent * 1.00f
            }
        }.coerceIn(0f, 100f)
    }

    // ------------------------------------------------------------------ //
    //  Uncapped CamScore                                                  //
    // ------------------------------------------------------------------ //

    /**
     * Computes the composite uncapped CamScore.
     *
     * `CamScore = S_AF + S_OIS + S_Lens`
     *
     * OIS devices inherently accumulate higher totals because they earn
     * OIS phase points; this is intentional—OIS hardware is a differentiator.
     */
    fun calculateCamScore(
        af: AutofocusResult,
        ois: OisResult,
        lens: LensSwitchResult
    ): Int {
        val total = af.uncappedScore + ois.uncappedScore + lens.uncappedScore
        return total.toInt()
    }

    // ------------------------------------------------------------------ //
    //  Verification Hash                                                  //
    // ------------------------------------------------------------------ //

    /**
     * Generates a SHA-256 verification hash for certificate authenticity.
     *
     * The hash input is a canonical pipe-delimited string:
     * `testId|deviceModel|camScore|healthPercent|timestamp|CAMSCORE_SALT_2024`
     */
    fun generateVerificationHash(
        testId: String,
        deviceModel: String,
        camScore: Int,
        healthPercent: Float,
        timestamp: String
    ): String {
        val salt = "CAMSCORE_SALT_2024"
        val input = "$testId|$deviceModel|$camScore|$healthPercent|$timestamp|$salt"
        return sha256(input.encodeToByteArray())
    }

    /**
     * Builds the full verification URL for a certificate.
     */
    fun buildVerificationUrl(hash: String): String {
        return "$VERIFICATION_BASE_URL$hash"
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Linear interpolation between [a] and [b] by fraction [t] (0–1).
     */
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }

    /**
     * Pure-Kotlin SHA-256 implementation.
     *
     * This is a complete, standalone implementation so the shared module has
     * no platform-specific crypto dependency. In production you may wish to
     * delegate to platform MessageDigest / CommonCrypto for performance.
     */
    private fun sha256(data: ByteArray): String {
        // SHA-256 initial hash values (first 32 bits of the fractional parts
        // of the square roots of the first 8 primes).
        val h0 = intArrayOf(
            0x6a09e667.toInt(), 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(),
            0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19
        )

        // Round constants (first 32 bits of the fractional parts of the cube
        // roots of the first 64 primes).
        val k = intArrayOf(
            0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b,
            0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
            -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
            -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039,
            -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
            -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d,
            -0x2e6d17e7, -0x2966f9dc, -0x0bf1ca7b, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8,
            -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e
        )

        // Pre-processing: pad the message
        val originalBitLen = data.size.toLong() * 8
        val padded = buildList<Byte> {
            addAll(data.toList())
            add(0x80.toByte())
            while ((size % 64) != 56) add(0x00.toByte())
            // Append 64-bit big-endian length
            for (i in 56 downTo 0 step 8) {
                add(((originalBitLen ushr i) and 0xFF).toByte())
            }
        }.toByteArray()

        val hash = h0.copyOf()

        // Process each 512-bit (64-byte) block
        for (chunkStart in padded.indices step 64) {
            val w = IntArray(64)
            for (i in 0 until 16) {
                w[i] = ((padded[chunkStart + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[chunkStart + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[chunkStart + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[chunkStart + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = (w[i - 15].rotateRight(7)) xor (w[i - 15].rotateRight(18)) xor (w[i - 15] ushr 3)
                val s1 = (w[i - 2].rotateRight(17)) xor (w[i - 2].rotateRight(19)) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = hash[0]; var b = hash[1]; var c = hash[2]; var d = hash[3]
            var e = hash[4]; var f = hash[5]; var g = hash[6]; var hh = hash[7]

            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + s1 + ch + k[i] + w[i]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                hh = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }

            hash[0] += a; hash[1] += b; hash[2] += c; hash[3] += d
            hash[4] += e; hash[5] += f; hash[6] += g; hash[7] += hh
        }

        return hash.joinToString("") { int ->
            val hex = (int.toLong() and 0xFFFFFFFFL).toString(16)
            hex.padStart(8, '0')
        }
    }

    /**
     * Rotates an [Int] right by [bits] positions (circular).
     */
    private fun Int.rotateRight(bits: Int): Int {
        return (this ushr bits) or (this shl (32 - bits))
    }
}
