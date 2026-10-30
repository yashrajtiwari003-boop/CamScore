package com.fischerlabs.camscore.viewmodel

import com.fischerlabs.camscore.model.DeviceInfo
import com.fischerlabs.camscore.model.PreCheckStatus
import com.fischerlabs.camscore.platform.CameraController
import com.fischerlabs.camscore.platform.DeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home Screen.
 * Manages pre-test ambient light, texture guard, and OIS hardware detection.
 */
class HomeViewModel(
    private val cameraController: CameraController,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _preCheckStatus = MutableStateFlow(
        PreCheckStatus(
            lightingOk = false,
            textureOk = false,
            isReady = false,
            ambientLux = 0f,
            textureScore = 0f,
            hasOis = false,
            errorMessage = null
        )
    )
    val preCheckStatus: StateFlow<PreCheckStatus> = _preCheckStatus.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _showLowLightDialog = MutableStateFlow(false)
    val showLowLightDialog: StateFlow<Boolean> = _showLowLightDialog.asStateFlow()

    companion object {
        /** Minimum ambient light in lux for a valid test */
        const val MIN_LUX = 50f
        /** Minimum texture/edge density score for anti-blank-wall guard */
        const val MIN_TEXTURE_SCORE = 0.15f
    }

    /**
     * Start continuous pre-check monitoring of ambient light and texture.
     */
    fun startPreCheck() {
        scope.launch {
            // Detect device hardware capabilities
            val info = deviceInfoProvider.getDeviceInfo()
            _deviceInfo.value = info

            _preCheckStatus.update { it.copy(hasOis = info.hasOis) }
        }
    }

    /**
     * Called continuously from the camera preview with current ambient light level.
     */
    fun updateLightLevel(lux: Float) {
        val lightingOk = lux >= MIN_LUX
        _preCheckStatus.update { current ->
            val isReady = lightingOk && current.textureOk
            current.copy(
                lightingOk = lightingOk,
                ambientLux = lux,
                isReady = isReady,
                errorMessage = when {
                    !lightingOk -> "Move to a well-lit area (current: ${lux.toInt()} lux)"
                    !current.textureOk -> "Point at a textured surface (avoid blank walls)"
                    else -> null
                }
            )
        }

        // Show low-light popup when light drops below threshold
        if (lux < MIN_LUX && !_showLowLightDialog.value) {
            _showLowLightDialog.value = true
        }
    }

    /**
     * Called with the texture/edge density score from frame analysis.
     */
    fun updateTextureScore(score: Float) {
        val textureOk = score >= MIN_TEXTURE_SCORE
        _preCheckStatus.update { current ->
            val isReady = current.lightingOk && textureOk
            current.copy(
                textureOk = textureOk,
                textureScore = score,
                isReady = isReady,
                errorMessage = when {
                    !current.lightingOk -> "Move to a well-lit area (current: ${current.ambientLux.toInt()} lux)"
                    !textureOk -> "Point at a textured surface (avoid blank walls)"
                    else -> null
                }
            )
        }
    }

    /**
     * Check OIS hardware support for the current device.
     */
    fun checkOisSupport(): Boolean {
        val hasOis = cameraController.isOisSupported()
        _preCheckStatus.update { it.copy(hasOis = hasOis) }
        return hasOis
    }

    fun dismissLowLightDialog() {
        _showLowLightDialog.value = false
    }
}
