package com.fischerlabs.camscore.model

/**
 * Represents each phase of the 10-second camera diagnostic benchmark.
 *
 * The three scored phases (AUTOFOCUS, OIS_STABILITY, LENS_SWITCH) have precise
 * timing windows and weighted contributions to the overall health percentage.
 *
 * @property displayName Human-readable label shown in the UI.
 * @property startTimeSeconds Phase start offset within the 10-second run.
 * @property endTimeSeconds Phase end offset within the 10-second run.
 * @property healthWeight Fractional weight towards overall health (0.0–1.0).
 * @property description Short explanation of what this phase measures.
 */
enum class DiagnosticPhase(
    val displayName: String,
    val startTimeSeconds: Float,
    val endTimeSeconds: Float,
    val healthWeight: Float,
    val description: String
) {
    IDLE(
        displayName = "Idle",
        startTimeSeconds = 0.0f,
        endTimeSeconds = 0.0f,
        healthWeight = 0.0f,
        description = "Waiting to start diagnostic."
    ),
    PRE_CHECK(
        displayName = "Pre-Check",
        startTimeSeconds = 0.0f,
        endTimeSeconds = 0.0f,
        healthWeight = 0.0f,
        description = "Verifying ambient lighting and target texture."
    ),
    AUTOFOCUS(
        displayName = "Autofocus",
        startTimeSeconds = 0.0f,
        endTimeSeconds = 3.5f,
        healthWeight = 0.35f,
        description = "Measures AF lock time and peak Laplacian sharpness variance."
    ),
    OIS_STABILITY(
        displayName = "OIS Stability",
        startTimeSeconds = 3.5f,
        endTimeSeconds = 7.0f,
        healthWeight = 0.40f,
        description = "Evaluates optical stabilisation dampening via PyrLK and gyroscope fusion."
    ),
    LENS_SWITCH(
        displayName = "Lens Switch",
        startTimeSeconds = 7.0f,
        endTimeSeconds = 10.0f,
        healthWeight = 0.25f,
        description = "Measures colour-shift ΔE and switching latency between lenses."
    ),
    COMPLETED(
        displayName = "Completed",
        startTimeSeconds = 10.0f,
        endTimeSeconds = 10.0f,
        healthWeight = 0.0f,
        description = "Diagnostic finished successfully."
    ),
    ERROR(
        displayName = "Error",
        startTimeSeconds = 0.0f,
        endTimeSeconds = 0.0f,
        healthWeight = 0.0f,
        description = "An error occurred during the diagnostic."
    );

    /** Duration of this phase in seconds. */
    val durationSeconds: Float get() = endTimeSeconds - startTimeSeconds
}
