package com.fischerlabs.camscore.statemachine

import com.fischerlabs.camscore.model.DiagnosticPhase
import com.fischerlabs.camscore.model.PreCheckStatus
import com.fischerlabs.camscore.model.TestResult

/**
 * Sealed hierarchy representing every possible state of a diagnostic run.
 *
 * The UI layer observes a `StateFlow<TestState>` to drive screen transitions,
 * progress bars, and waveform visualisations.
 */
sealed class TestState {

    /** No test in progress; the user has not started a run. */
    data object Idle : TestState()

    /**
     * Pre-flight checks are running (lighting, texture, OIS detection).
     *
     * @property preCheckStatus Current readiness snapshot.
     */
    data class PreCheck(val preCheckStatus: PreCheckStatus) : TestState()

    /**
     * The 10-second diagnostic is actively running.
     *
     * @property phase Current diagnostic phase.
     * @property elapsedSeconds Seconds elapsed since the diagnostic started.
     * @property phaseProgress Progress within the current phase (0.0–1.0).
     */
    data class Running(
        val phase: DiagnosticPhase,
        val elapsedSeconds: Float,
        val phaseProgress: Float
    ) : TestState()

    /**
     * The diagnostic completed successfully and results are available.
     *
     * @property result Full test result with per-phase scores and CamScore.
     */
    data class Completed(val result: TestResult) : TestState()

    /**
     * An error occurred during the diagnostic.
     *
     * @property message Human-readable error description.
     * @property phase The phase during which the error occurred, or null if
     *                 the error happened outside a scored phase.
     */
    data class Error(
        val message: String,
        val phase: DiagnosticPhase? = null
    ) : TestState()
}
