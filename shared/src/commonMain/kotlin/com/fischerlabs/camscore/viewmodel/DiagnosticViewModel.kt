package com.fischerlabs.camscore.viewmodel

import com.fischerlabs.camscore.model.*
import com.fischerlabs.camscore.platform.CameraController
import com.fischerlabs.camscore.platform.OpenCVProcessor
import com.fischerlabs.camscore.platform.SensorController
import com.fischerlabs.camscore.repository.TestRepository
import com.fischerlabs.camscore.statemachine.DiagnosticStateMachine
import com.fischerlabs.camscore.statemachine.TestState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the 10-second diagnostic screen.
 * Orchestrates the DiagnosticStateMachine and emits real-time waveform data.
 */
class DiagnosticViewModel(
    private val cameraController: CameraController,
    private val sensorController: SensorController,
    private val openCVProcessor: OpenCVProcessor,
    private val testRepository: TestRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val stateMachine = DiagnosticStateMachine(
        cameraController = cameraController,
        sensorController = sensorController,
        openCVProcessor = openCVProcessor
    )

    /** Current diagnostic state (idle, running phases, completed, error) */
    val testState: StateFlow<TestState> = stateMachine.state

    /** Real-time waveform data for the sensor fusion graph */
    val waveformData: StateFlow<DiagnosticStateMachine.WaveformData> = stateMachine.waveformData

    /** UI state derived from test state */
    data class DiagnosticUiState(
        val currentPhase: DiagnosticPhase = DiagnosticPhase.IDLE,
        val elapsedSeconds: Float = 0f,
        val phaseProgress: Float = 0f,
        val totalProgress: Float = 0f,
        val hasOis: Boolean = true,
        val isRunning: Boolean = false,
        val statusMessage: String = "",
        val gyroData: List<Float> = emptyList(),
        val opticalFlowData: List<Float> = emptyList()
    )

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    init {
        // Transform state machine events into UI state
        scope.launch {
            combine(testState, waveformData) { state, waveform ->
                when (state) {
                    is TestState.Idle -> DiagnosticUiState()
                    is TestState.PreCheck -> DiagnosticUiState(
                        currentPhase = DiagnosticPhase.PRE_CHECK,
                        statusMessage = "Preparing diagnostic..."
                    )
                    is TestState.Running -> DiagnosticUiState(
                        currentPhase = state.phase,
                        elapsedSeconds = state.elapsedSeconds,
                        phaseProgress = state.phaseProgress,
                        totalProgress = state.elapsedSeconds / 10f,
                        hasOis = true,
                        isRunning = true,
                        statusMessage = getPhaseMessage(state.phase, _uiState.value.hasOis),
                        gyroData = waveform.gyroPoints,
                        opticalFlowData = waveform.opticalFlowPoints
                    )
                    is TestState.Completed -> DiagnosticUiState(
                        currentPhase = DiagnosticPhase.COMPLETED,
                        elapsedSeconds = 10f,
                        totalProgress = 1f,
                        isRunning = false,
                        statusMessage = "Diagnostic complete!"
                    )
                    is TestState.Error -> DiagnosticUiState(
                        currentPhase = state.phase ?: DiagnosticPhase.ERROR,
                        isRunning = false,
                        statusMessage = "Error: ${state.message}"
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    /**
     * Start the full 10-second diagnostic.
     */
    fun startDiagnostic(deviceInfo: DeviceInfo) {
        _uiState.update { it.copy(hasOis = deviceInfo.hasOis) }
        stateMachine.startDiagnostic(deviceInfo, scope)
    }

    /**
     * Cancel a running diagnostic.
     */
    fun cancelDiagnostic() {
        stateMachine.cancelDiagnostic()
    }

    /**
     * Get the completed test result (if diagnostic finished).
     */
    fun getCompletedResult(): TestResult? {
        val state = testState.value
        return if (state is TestState.Completed) state.result else null
    }

    /**
     * Save the completed result to local database.
     */
    fun saveResult(result: TestResult) {
        scope.launch {
            testRepository.saveTestResult(result)
        }
    }

    private fun getPhaseMessage(phase: DiagnosticPhase, hasOis: Boolean): String {
        return when (phase) {
            DiagnosticPhase.AUTOFOCUS -> "Testing Autofocus Speed..."
            DiagnosticPhase.OIS_STABILITY -> {
                if (hasOis) "Testing OIS Stabilization..."
                else "Testing Digital Sensor Stability (Non-OIS Mode)..."
            }
            DiagnosticPhase.LENS_SWITCH -> "Testing Lens Switching & Color Accuracy..."
            DiagnosticPhase.COMPLETED -> "Diagnostic Complete!"
            DiagnosticPhase.PRE_CHECK -> "Pre-check in progress..."
            else -> ""
        }
    }
}
