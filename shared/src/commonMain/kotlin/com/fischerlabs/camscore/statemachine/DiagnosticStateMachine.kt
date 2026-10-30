package com.fischerlabs.camscore.statemachine

import com.fischerlabs.camscore.model.*
import com.fischerlabs.camscore.platform.CameraController
import com.fischerlabs.camscore.platform.OpenCVProcessor
import com.fischerlabs.camscore.platform.SensorController
import com.fischerlabs.camscore.scoring.ScoringEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/**
 * Core state machine orchestrating the 10-second cross-platform camera diagnostic.
 *
 * Phase timeline:
 *   0.0s – 3.5s  → Autofocus Speed (35% health weight)
 *   3.5s – 7.0s  → OIS / Sensor Stability (40% health weight)
 *   7.0s – 10.0s → Lens Switch & Color Shift (25% health weight)
 */
class DiagnosticStateMachine(
    private val cameraController: CameraController,
    private val sensorController: SensorController,
    private val openCVProcessor: OpenCVProcessor
) {
    private val _state = MutableStateFlow<TestState>(TestState.Idle)
    val state: StateFlow<TestState> = _state.asStateFlow()

    private val _waveformData = MutableStateFlow(WaveformData(emptyList(), emptyList()))
    val waveformData: StateFlow<WaveformData> = _waveformData.asStateFlow()

    private var diagnosticJob: Job? = null

    companion object {
        const val TOTAL_DURATION_MS = 10_000L
        const val PHASE1_END_MS = 3_500L
        const val PHASE2_END_MS = 7_000L
        const val PHASE3_END_MS = 10_000L
        const val TICK_INTERVAL_MS = 100L
        const val LENS_SWITCH_TIMEOUT_MS = 2_000L
    }

    data class WaveformData(
        val gyroPoints: List<Float>,
        val opticalFlowPoints: List<Float>
    )

    fun startDiagnostic(deviceInfo: DeviceInfo, scope: CoroutineScope) {
        diagnosticJob?.cancel()
        diagnosticJob = scope.launch {
            try {
                runDiagnostic(deviceInfo)
            } catch (e: CancellationException) {
                _state.value = TestState.Idle
                throw e
            } catch (e: Exception) {
                _state.value = TestState.Error(
                    message = e.message ?: "Unknown diagnostic error",
                    phase = getCurrentPhase(0L)
                )
            }
        }
    }

    fun cancelDiagnostic() {
        diagnosticJob?.cancel()
        diagnosticJob = null
        _state.value = TestState.Idle
        sensorController.stopGyroscopeLogging()
        sensorController.stopAccelerometerLogging()
        sensorController.clearHistory()
        _waveformData.value = WaveformData(emptyList(), emptyList())
    }

    private suspend fun runDiagnostic(deviceInfo: DeviceInfo) {
        // Initialize processors
        openCVProcessor.initialize()
        sensorController.startGyroscopeLogging()
        sensorController.startAccelerometerLogging()

        val gyroWaveform = mutableListOf<Float>()
        val opticalWaveform = mutableListOf<Float>()

        // ──── Phase 1: Autofocus Speed (0.0s – 3.5s) ────
        val afResult = runPhase1Autofocus(deviceInfo, gyroWaveform, opticalWaveform)

        // ──── Phase 2: OIS / Sensor Stability (3.5s – 7.0s) ────
        val oisResult = runPhase2Ois(deviceInfo, gyroWaveform, opticalWaveform)

        // ──── Phase 3: Lens Switch & Color Shift (7.0s – 10.0s) ────
        val lensResult = runPhase3LensSwitch(deviceInfo, gyroWaveform, opticalWaveform)

        // ──── Compute Final Scores ────
        sensorController.stopGyroscopeLogging()
        sensorController.stopAccelerometerLogging()

        val overallHealth = ScoringEngine.calculateOverallHealth(afResult, oisResult, lensResult, deviceInfo)
        val camScore = ScoringEngine.calculateCamScore(afResult, oisResult, lensResult)
        val healthRating = HealthRating.fromPercent(overallHealth)
        val testId = generateTestId()
        val timestamp = Clock.System.now()
        val monthKey = timestamp.toString().substring(0, 7) // YYYY-MM

        val verificationHash = ScoringEngine.generateVerificationHash(
            testId = testId,
            deviceModel = deviceInfo.deviceModel,
            camScore = camScore,
            healthPercent = overallHealth,
            timestamp = timestamp.toString()
        )

        val testResult = TestResult(
            testId = testId,
            deviceInfo = deviceInfo,
            timestamp = timestamp,
            afResult = afResult,
            oisResult = oisResult,
            lensResult = lensResult,
            overallHealthPercent = overallHealth,
            camScore = camScore,
            healthRating = healthRating,
            monthKey = monthKey,
            verificationHash = verificationHash
        )

        openCVProcessor.release()
        _state.value = TestState.Completed(testResult)
    }

    // ══════════════════════════════════════════════════════════════
    //  Phase 1 — Autofocus Speed (0.0 – 3.5s)
    // ══════════════════════════════════════════════════════════════
    private suspend fun runPhase1Autofocus(
        deviceInfo: DeviceInfo,
        gyroWaveform: MutableList<Float>,
        opticalWaveform: MutableList<Float>
    ): AutofocusResult {
        val phaseStartMs = 0L
        val phaseDurationMs = PHASE1_END_MS

        // Defocus lens to near-macro
        cameraController.defocusToMacro()
        delay(200)

        // Start refocus and track sharpness
        cameraController.triggerAutofocus()
        val focusStartTime = System.currentTimeMillis()

        var peakSharpness = 0.0
        var sharpnessAtLock = 0.0
        var lockTimeMs = phaseDurationMs // default to full phase if never locked
        var locked = false
        val sharpnessReadings = mutableListOf<Double>()

        val tickCount = (phaseDurationMs / TICK_INTERVAL_MS).toInt()
        for (tick in 0 until tickCount) {
            val elapsed = tick * TICK_INTERVAL_MS
            val phaseProgress = elapsed.toFloat() / phaseDurationMs.toFloat()

            _state.value = TestState.Running(
                phase = DiagnosticPhase.AUTOFOCUS,
                elapsedSeconds = elapsed / 1000f,
                phaseProgress = phaseProgress
            )

            // Measure Laplacian sharpness variance (σ²)
            val frameData = cameraController.captureAnalysisFrame()
            val sharpness = openCVProcessor.calculateLaplacianVariance(frameData, 1920, 1080)
            sharpnessReadings.add(sharpness)

            if (sharpness > peakSharpness) {
                peakSharpness = sharpness
            }

            // Check for 95% of peak lock condition
            if (!locked && peakSharpness > 0.0 && sharpness >= peakSharpness * 0.95) {
                lockTimeMs = System.currentTimeMillis() - focusStartTime
                sharpnessAtLock = sharpness
                locked = true
            }

            // Record waveform data
            val gyroMag = sensorController.getAngularVelocityMagnitude().toFloat()
            gyroWaveform.add(gyroMag)
            opticalWaveform.add((sharpness / 1000.0).toFloat().coerceIn(0f, 5f))
            _waveformData.value = WaveformData(gyroWaveform.toList(), opticalWaveform.toList())

            delay(TICK_INTERVAL_MS)
        }

        // Compute sharpness variance across all readings
        val mean = sharpnessReadings.average()
        val variance = sharpnessReadings.map { (it - mean) * (it - mean) }.average()

        return ScoringEngine.calculateAfScore(lockTimeMs, peakSharpness)
    }

    // ══════════════════════════════════════════════════════════════
    //  Phase 2 — OIS / Sensor Stability (3.5s – 7.0s)
    // ══════════════════════════════════════════════════════════════
    private suspend fun runPhase2Ois(
        deviceInfo: DeviceInfo,
        gyroWaveform: MutableList<Float>,
        opticalWaveform: MutableList<Float>
    ): OisResult {
        val phaseDurationMs = PHASE2_END_MS - PHASE1_END_MS

        // Lock zoom at 4x for hand-tremor sensitivity
        cameraController.setZoomRatio(4.0f)
        delay(300)

        // Initialize optical flow tracking with current frame
        val initFrame = cameraController.captureAnalysisFrame()
        openCVProcessor.initOpticalFlowTracking(initFrame, 1920, 1080, 20)

        var totalPixelShake = 0.0
        var totalGyroShake = 0.0
        var sampleCount = 0

        val tickCount = (phaseDurationMs / TICK_INTERVAL_MS).toInt()
        for (tick in 0 until tickCount) {
            val elapsed = PHASE1_END_MS + tick * TICK_INTERVAL_MS
            val phaseProgress = (tick * TICK_INTERVAL_MS).toFloat() / phaseDurationMs.toFloat()

            _state.value = TestState.Running(
                phase = DiagnosticPhase.OIS_STABILITY,
                elapsedSeconds = elapsed / 1000f,
                phaseProgress = phaseProgress
            )

            // Capture frame and compute optical flow pixel displacement
            val frameData = cameraController.captureAnalysisFrame()
            val pixelDisplacement = openCVProcessor.calculateOpticalFlow(frameData, 1920, 1080)

            // Read gyroscope angular velocity magnitude
            val gyroMagnitude = sensorController.getAngularVelocityMagnitude()

            totalPixelShake += pixelDisplacement
            totalGyroShake += gyroMagnitude
            sampleCount++

            // Waveform data
            gyroWaveform.add(gyroMagnitude.toFloat())
            opticalWaveform.add(pixelDisplacement.toFloat().coerceIn(0f, 10f))
            _waveformData.value = WaveformData(gyroWaveform.toList(), opticalWaveform.toList())

            delay(TICK_INTERVAL_MS)
        }

        val avgPixelShake = if (sampleCount > 0) totalPixelShake / sampleCount else 0.0
        val avgGyroShake = if (sampleCount > 0) totalGyroShake / sampleCount else 0.001

        return ScoringEngine.calculateOisScore(avgPixelShake, avgGyroShake, deviceInfo.hasOis)
    }

    // ══════════════════════════════════════════════════════════════
    //  Phase 3 — Lens Switch & Color Shift (7.0s – 10.0s)
    // ══════════════════════════════════════════════════════════════
    private suspend fun runPhase3LensSwitch(
        deviceInfo: DeviceInfo,
        gyroWaveform: MutableList<Float>,
        opticalWaveform: MutableList<Float>
    ): LensSwitchResult {
        val phaseDurationMs = PHASE3_END_MS - PHASE2_END_MS
        val hasMultipleLenses = deviceInfo.lensCount > 1

        if (!hasMultipleLenses) {
            // Single-lens device: just measure frame stability during zoom sweep
            val tickCount = (phaseDurationMs / TICK_INTERVAL_MS).toInt()
            for (tick in 0 until tickCount) {
                val elapsed = PHASE2_END_MS + tick * TICK_INTERVAL_MS
                val phaseProgress = (tick * TICK_INTERVAL_MS).toFloat() / phaseDurationMs.toFloat()
                _state.value = TestState.Running(
                    phase = DiagnosticPhase.LENS_SWITCH,
                    elapsedSeconds = elapsed / 1000f,
                    phaseProgress = phaseProgress
                )

                val gyroMag = sensorController.getAngularVelocityMagnitude().toFloat()
                gyroWaveform.add(gyroMag)
                opticalWaveform.add(0f)
                _waveformData.value = WaveformData(gyroWaveform.toList(), opticalWaveform.toList())

                delay(TICK_INTERVAL_MS)
            }
            return ScoringEngine.calculateLensScore(0.0, 0L, false)
        }

        // Multi-lens: Capture main lens reference
        cameraController.setZoomRatio(1.0f)
        delay(500)

        val mainFrame = cameraController.captureAnalysisFrame()
        val mainLab = openCVProcessor.extractCenterPatchLab(mainFrame, 1920, 1080)

        var maxDeltaE = 0.0
        var totalSwitchLagMs = 0L

        val lensIds = cameraController.getAvailableLensIds()
        val zoomTargets = mutableListOf<Float>()
        if (deviceInfo.hasUltrawide) zoomTargets.add(0.6f)
        if (deviceInfo.hasTelephoto) zoomTargets.add(3.0f)
        if (zoomTargets.isEmpty()) zoomTargets.add(2.0f)

        for ((index, zoom) in zoomTargets.withIndex()) {
            val switchStart = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

            // Switch lens with timeout guard
            cameraController.setZoomRatio(zoom)
            delay(500L)

            val switchLag = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - switchStart
            totalSwitchLagMs += switchLag

            // Capture and measure CIE LAB color shift
            val lensFrame = cameraController.captureAnalysisFrame()
            val lensLab = openCVProcessor.extractCenterPatchLab(lensFrame, 1920, 1080)
            val deltaE = openCVProcessor.calculateDeltaE(mainLab, lensLab)

            if (deltaE > maxDeltaE) maxDeltaE = deltaE

            // Update state
            val elapsed = PHASE2_END_MS + ((index + 1).toFloat() / zoomTargets.size * phaseDurationMs).toLong()
            val phaseProgress = (index + 1).toFloat() / zoomTargets.size
            _state.value = TestState.Running(
                phase = DiagnosticPhase.LENS_SWITCH,
                elapsedSeconds = elapsed / 1000f,
                phaseProgress = phaseProgress
            )

            val gyroMag = sensorController.getAngularVelocityMagnitude().toFloat()
            gyroWaveform.add(gyroMag)
            opticalWaveform.add(deltaE.toFloat())
            _waveformData.value = WaveformData(gyroWaveform.toList(), opticalWaveform.toList())
        }

        // Reset zoom to 1x
        cameraController.setZoomRatio(1.0f)

        val avgSwitchLag = if (zoomTargets.isNotEmpty()) totalSwitchLagMs / zoomTargets.size else 0L

        // Fill remaining time with status updates
        val remainingMs = PHASE3_END_MS - (PHASE2_END_MS + 1500L)
        if (remainingMs > 0) {
            val remainingTicks = (remainingMs / TICK_INTERVAL_MS).toInt()
            for (tick in 0 until remainingTicks) {
                _state.value = TestState.Running(
                    phase = DiagnosticPhase.LENS_SWITCH,
                    elapsedSeconds = (PHASE3_END_MS - remainingMs + tick * TICK_INTERVAL_MS) / 1000f,
                    phaseProgress = 0.5f + (tick.toFloat() / remainingTicks * 0.5f)
                )
                delay(TICK_INTERVAL_MS)
            }
        }

        return ScoringEngine.calculateLensScore(maxDeltaE, avgSwitchLag, true)
    }

    private fun getCurrentPhase(elapsedMs: Long): DiagnosticPhase {
        return when {
            elapsedMs < PHASE1_END_MS -> DiagnosticPhase.AUTOFOCUS
            elapsedMs < PHASE2_END_MS -> DiagnosticPhase.OIS_STABILITY
            elapsedMs < PHASE3_END_MS -> DiagnosticPhase.LENS_SWITCH
            else -> DiagnosticPhase.COMPLETED
        }
    }

    private fun generateTestId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars.random() }.joinToString("")
    }

    /**
     * Expect platform implementation for currentTimeMillis().
     * In common code we use a simple wrapper.
     */
    private object System {
        fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    }
}
