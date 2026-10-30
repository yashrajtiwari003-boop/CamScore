package com.fischerlabs.camscore.model

import kotlinx.serialization.Serializable

/**
 * Overall camera health classification derived from the weighted health percentage.
 *
 * @property displayName User-facing label.
 * @property minPercent Inclusive lower bound of the percentage range.
 * @property maxPercent Inclusive upper bound of the percentage range.
 * @property colorHex Brand colour used in UI badges and charts.
 */
@Serializable
enum class HealthRating(
    val displayName: String,
    val minPercent: Int,
    val maxPercent: Int,
    val colorHex: String
) {
    MINT(
        displayName = "Mint",
        minPercent = 85,
        maxPercent = 100,
        colorHex = "#10B981"
    ),
    MINOR_WEAR(
        displayName = "Minor Wear",
        minPercent = 65,
        maxPercent = 84,
        colorHex = "#F59E0B"
    ),
    FAIL(
        displayName = "Fail",
        minPercent = 0,
        maxPercent = 64,
        colorHex = "#EF4444"
    );

    companion object {
        /**
         * Maps a health percentage (0–100) to the appropriate [HealthRating].
         * Values are clamped to [0, 100] before classification.
         */
        fun fromPercent(percent: Float): HealthRating {
            val clamped = percent.coerceIn(0f, 100f)
            return when {
                clamped >= MINT.minPercent -> MINT
                clamped >= MINOR_WEAR.minPercent -> MINOR_WEAR
                else -> FAIL
            }
        }
    }
}
