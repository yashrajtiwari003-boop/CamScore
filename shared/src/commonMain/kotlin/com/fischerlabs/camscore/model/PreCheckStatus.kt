package com.fischerlabs.camscore.model

/**
 * Snapshot of pre-diagnostic environmental readiness.
 *
 * The benchmark requires adequate ambient lighting and a textured target
 * to produce reliable autofocus and colour-shift measurements.
 *
 * @property lightingOk True when ambient lux is within the acceptable range (≥ 50 lux).
 * @property textureOk True when the scene has enough high-frequency detail (texture score ≥ 0.4).
 * @property isReady Convenience flag: true only when all pre-checks pass.
 * @property ambientLux Current ambient light sensor reading in lux.
 * @property textureScore Normalised texture richness score (0.0–1.0) derived from Laplacian variance.
 * @property hasOis Whether the device reports OIS support for the primary camera.
 * @property errorMessage Optional human-readable error if a pre-check fails.
 */
data class PreCheckStatus(
    val lightingOk: Boolean = false,
    val textureOk: Boolean = false,
    val isReady: Boolean = false,
    val ambientLux: Float = 350f,
    val textureScore: Float = 0.85f,
    val hasOis: Boolean = true,
    val errorMessage: String? = null
) {
    companion object {
        /** Minimum ambient lux required to begin a diagnostic run. */
        const val MIN_LUX = 50f

        /** Minimum normalised texture score required. */
        const val MIN_TEXTURE_SCORE = 0.4f

        /** Creates a default "not-ready" status. */
        fun initial(): PreCheckStatus = PreCheckStatus(
            lightingOk = false,
            textureOk = false,
            isReady = false,
            ambientLux = 0f,
            textureScore = 0f,
            hasOis = false,
            errorMessage = null
        )

        /**
         * Evaluates readiness from raw sensor values.
         */
        fun evaluate(ambientLux: Float, textureScore: Float, hasOis: Boolean): PreCheckStatus {
            val lightingOk = ambientLux >= MIN_LUX
            val textureOk = textureScore >= MIN_TEXTURE_SCORE
            val errorMessage = buildString {
                if (!lightingOk) append("Insufficient lighting (${ambientLux.toInt()} lux, need ≥${MIN_LUX.toInt()}). ")
                if (!textureOk) append("Target lacks texture detail (score ${(textureScore * 100).toInt() / 100.0}, need ≥${MIN_TEXTURE_SCORE}). ")
            }.ifEmpty { null }

            return PreCheckStatus(
                lightingOk = lightingOk,
                textureOk = textureOk,
                isReady = lightingOk && textureOk,
                ambientLux = ambientLux,
                textureScore = textureScore,
                hasOis = hasOis,
                errorMessage = errorMessage
            )
        }
    }
}
