package com.fischerlabs.camscore.platform

/**
 * Represents a color in CIE L*a*b* color space.
 * @param l Lightness (0-100)
 * @param a Green-red component
 * @param b Blue-yellow component
 */
data class LabColor(val l: Double, val a: Double, val b: Double)

/**
 * Interface for OpenCV-based image processing operations used
 * across all three scoring phases of CamScore.
 */
interface OpenCVProcessor {
    /** Initialize the OpenCV library. Must be called before any operations. */
    fun initialize()

    /** Release all OpenCV resources. */
    fun release()

    /**
     * Phase 1: Autofocus — Calculate Laplacian sharpness variance.
     * Converts frame to grayscale, applies Laplacian operator, then
     * computes variance (σ²) of the result.
     *
     * @param frameData Raw frame bytes (NV21 / YUV420)
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @return Laplacian variance σ² — higher means sharper
     */
    fun calculateLaplacianVariance(frameData: ByteArray, width: Int, height: Int): Double

    /**
     * Phase 2: OIS — Initialize optical flow tracking by detecting
     * good features to track (Shi-Tomasi corners) in the reference frame.
     *
     * @param frameData Raw reference frame bytes
     * @param width Frame width
     * @param height Frame height
     * @param numCorners Number of corners to detect (default 20)
     */
    fun initOpticalFlowTracking(
        frameData: ByteArray,
        width: Int,
        height: Int,
        numCorners: Int = 20
    )

    /**
     * Phase 2: OIS — Calculate Lucas-Kanade optical flow between
     * the previously initialized reference frame and this new frame.
     *
     * @param newFrameData Raw new frame bytes
     * @param width Frame width
     * @param height Frame height
     * @return Mean pixel displacement (dpx) across tracked points
     */
    fun calculateOpticalFlow(
        newFrameData: ByteArray,
        width: Int,
        height: Int
    ): Double

    /**
     * Phase 3: Lens Switch — Extract CIE L*a*b* color values from
     * the center patch of a frame.
     *
     * @param frameData Raw frame bytes
     * @param width Frame width
     * @param height Frame height
     * @param patchSize Size of the center square patch (default 200px)
     * @return Mean L*a*b* color of the center patch
     */
    fun extractCenterPatchLab(
        frameData: ByteArray,
        width: Int,
        height: Int,
        patchSize: Int = 200
    ): LabColor

    /**
     * Phase 3: Lens Switch — Calculate CIE76 ΔE* color difference
     * between two L*a*b* colors.
     *
     * @return √((L₁-L₂)² + (a₁-a₂)² + (b₁-b₂)²)
     */
    fun calculateDeltaE(lab1: LabColor, lab2: LabColor): Double

    /**
     * Pre-check: Calculate texture/edge density of a frame.
     * Uses Canny edge detection and counts edge pixels as a fraction
     * of total pixels. Minimum 0.05 required before benchmarking.
     *
     * @return Edge density ratio (0.0 to 1.0)
     */
    fun calculateTextureScore(frameData: ByteArray, width: Int, height: Int): Float

    /** Release any cached frame data (previous frame for optical flow, etc.). */
    fun releaseFrame()
}

expect fun createOpenCVProcessor(): OpenCVProcessor
