package com.fischerlabs.camscore.platform

/**
 * Interface for camera operations used by the CamScore benchmarking engine.
 * Provides access to camera preview, zoom, autofocus, lens switching,
 * and frame analysis capabilities.
 */
interface CameraController {
    /** Start the camera preview session. */
    fun startPreview()

    /** Stop the camera preview session and release resources. */
    fun stopPreview()

    /** Set the zoom ratio. 1.0 = no zoom. */
    fun setZoomRatio(ratio: Float)

    /** Trigger a single autofocus cycle. */
    fun triggerAutofocus()

    /** Defocus lens to macro (closest focus distance) for AF speed testing. */
    fun defocusToMacro()

    /**
     * Switch to a specific lens.
     * @param lensId 0 = main/wide, 1 = ultrawide, 2 = telephoto
     */
    fun switchToLens(lensId: Int)

    /**
     * Calculate the sharpness of the current preview frame using
     * Laplacian variance (σ²). Higher values indicate sharper image.
     */
    fun getCurrentFrameSharpness(): Double

    /** Get list of available lens IDs on this device. */
    fun getAvailableLensIds(): List<Int>

    /** Check if the current lens supports optical image stabilization. */
    fun isOisSupported(): Boolean

    /** Get the current ambient light level in lux from AE metering. */
    fun getAmbientLux(): Float

    /** Get edge density / texture score of the current frame (0.0 to 1.0). */
    fun getTextureScore(): Float

    /** Capture a raw analysis frame (NV21/YUV bytes) for OpenCV processing. */
    suspend fun captureAnalysisFrame(): ByteArray
}

expect fun createCameraController(context: PlatformContext): CameraController
