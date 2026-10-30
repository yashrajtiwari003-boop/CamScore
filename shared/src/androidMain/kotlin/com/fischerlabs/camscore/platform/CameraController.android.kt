package com.fischerlabs.camscore.platform

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

/**
 * Android CameraController implementation using CameraX with Camera2 interop.
 *
 * Provides preview control, zoom, autofocus, lens switching, frame analysis
 * and ambient lux reading for the CamScore benchmarking engine.
 */
class AndroidCameraController(
    private val context: Context
) : CameraController {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val latestFrame = AtomicReference<FrameData?>(null)
    private val latestLux = AtomicReference(0f)
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // Map of logical lens IDs: 0=main, 1=ultrawide, 2=telephoto
    private var availableLensMap: Map<Int, String> = emptyMap()
    private var currentLensId: Int = 0

    /**
     * Holds a captured analysis frame for processing.
     */
    private data class FrameData(
        val data: ByteArray,
        val width: Int,
        val height: Int,
        val timestamp: Long
    )

    @SuppressLint("RestrictedApi")
    override fun startPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            buildAvailableLensMap()

            preview = Preview.Builder()
                .setTargetResolution(Size(1920, 1080))
                .build()

            val analysisBuilder = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)

            // Use Camera2 interop to read AE metering data for lux
            val camera2Extender = Camera2Interop.Extender(analysisBuilder)
            camera2Extender.setCaptureRequestOption(
                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
            )

            imageAnalysis = analysisBuilder.build().apply {
                setAnalyzer(analysisExecutor) { imageProxy ->
                    processFrame(imageProxy)
                }
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            provider.unbindAll()

            val lifecycleOwner = context as? LifecycleOwner
            if (lifecycleOwner != null) {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun stopPreview() {
        cameraProvider?.unbindAll()
        camera = null
        cameraProvider = null
        latestFrame.set(null)
    }

    override fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(1.0f, 10.0f))
    }

    override fun triggerAutofocus() {
        val cam = camera ?: return
        // Create a metering point at the center of the frame
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val centerPoint = factory.createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
            .disableAutoCancel()
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    override fun defocusToMacro() {
        // Use Camera2 interop to set focus distance to minimum (macro)
        val cam = camera ?: return
        try {
            @SuppressLint("RestrictedApi")
            val camera2Info = Camera2CameraInfo.from(cam.cameraInfo)
            val cameraId = camera2Info.cameraId
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val minFocusDistance = characteristics.get(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
            ) ?: return

            // Set zoom to 1x and trigger AF at macro distance
            // CameraX doesn't directly expose focus distance, so we use a near metering point
            // The closest we can get is triggering AF on a very close metering region
            val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
            val centerPoint = factory.createPoint(0.5f, 0.5f, 0.01f) // Very small region
            val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
                .disableAutoCancel()
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            // Fallback: just trigger normal AF
            triggerAutofocus()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun switchToLens(lensId: Int) {
        if (lensId == currentLensId) return
        val targetCameraId = availableLensMap[lensId] ?: return

        currentLensId = lensId
        val provider = cameraProvider ?: return

        provider.unbindAll()

        // Build a camera selector for the specific physical camera
        val cameraSelector = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Try to select by physical camera ID through logical camera
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        } else {
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        }

        val lifecycleOwner = context as? LifecycleOwner ?: return

        camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )

        // Use zoom ratio to simulate lens switch on logical camera
        // Ultrawide: ~0.5x zoom, Main: 1.0x, Telephoto: ~2.0x or more
        val zoomRatio = when (lensId) {
            1 -> 0.5f  // ultrawide
            2 -> {
                // Calculate telephoto zoom ratio based on focal length ratio
                val mainFL = getMainFocalLength()
                val teleFL = getTelephotoFocalLength()
                if (mainFL > 0 && teleFL > 0) teleFL / mainFL else 2.0f
            }
            else -> 1.0f // main
        }
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    override fun getCurrentFrameSharpness(): Double {
        val frame = latestFrame.get() ?: return 0.0
        // Calculate Laplacian variance on the Y channel (luminance)
        return calculateSimpleLaplacianVariance(frame.data, frame.width, frame.height)
    }

    override fun getAvailableLensIds(): List<Int> {
        return availableLensMap.keys.sorted()
    }

    @SuppressLint("RestrictedApi")
    override fun isOisSupported(): Boolean {
        return try {
            val cam = camera ?: return false
            val camera2Info = Camera2CameraInfo.from(cam.cameraInfo)
            val cameraId = camera2Info.cameraId
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val oisModes = characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
            )
            oisModes != null && oisModes.any {
                it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun getAmbientLux(): Float {
        return latestLux.get()
    }

    override fun getTextureScore(): Float {
        val frame = latestFrame.get() ?: return 0f
        return calculateSimpleEdgeDensity(frame.data, frame.width, frame.height)
    }

    override suspend fun captureAnalysisFrame(): ByteArray {
        return withContext(Dispatchers.IO) {
            // Wait for a fresh frame with a timeout
            val deferred = CompletableDeferred<ByteArray>()
            val startTime = System.currentTimeMillis()

            while (!deferred.isCompleted) {
                val frame = latestFrame.get()
                if (frame != null && frame.timestamp > startTime) {
                    deferred.complete(frame.data)
                    break
                }
                if (System.currentTimeMillis() - startTime > 2000L) {
                    // Timeout: return whatever frame we have, or empty
                    val currentFrame = latestFrame.get()
                    deferred.complete(currentFrame?.data ?: ByteArray(0))
                    break
                }
                kotlinx.coroutines.delay(10)
            }

            deferred.await()
        }
    }

    // ---------- Private helpers ----------

    /**
     * Process each frame from the ImageAnalysis pipeline.
     * Extracts YUV data, estimates ambient lux from Y-channel mean.
     */
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val yuvData = imageProxyToNv21(imageProxy)
            val width = imageProxy.width
            val height = imageProxy.height

            latestFrame.set(
                FrameData(
                    data = yuvData,
                    width = width,
                    height = height,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Estimate ambient lux from mean luminance of Y channel
            // This is a rough estimate: Lux ≈ (meanY / 255) * 1000
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val ySize = yBuffer.remaining()
            var sum = 0L
            val tempArray = ByteArray(ySize)
            yBuffer.get(tempArray)
            for (byte in tempArray) {
                sum += (byte.toInt() and 0xFF)
            }
            val meanY = sum.toFloat() / ySize
            val estimatedLux = (meanY / 255f) * 1000f
            latestLux.set(estimatedLux)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Convert an ImageProxy (YUV_420_888) to NV21 byte array.
     */
    private fun imageProxyToNv21(imageProxy: ImageProxy): ByteArray {
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize)

        // NV21 format: VUVU... interleaved
        val vBytes = ByteArray(vSize)
        val uBytes = ByteArray(uSize)
        vBuffer.get(vBytes)
        uBuffer.get(uBytes)

        val uvPixelStride = uPlane.pixelStride
        var uvIndex = ySize

        if (uvPixelStride == 2) {
            // Already interleaved (most devices)
            // V plane in NV21 comes first
            for (i in 0 until minOf(vSize, uSize)) {
                nv21[uvIndex++] = vBytes[i]
                if (uvIndex < nv21.size && i < uSize) {
                    nv21[uvIndex++] = uBytes[i]
                }
            }
        } else {
            // Not interleaved, manually interleave
            val chromaWidth = imageProxy.width / 2
            val chromaHeight = imageProxy.height / 2
            for (row in 0 until chromaHeight) {
                for (col in 0 until chromaWidth) {
                    if (uvIndex + 1 < nv21.size) {
                        val srcIndex = row * chromaWidth + col
                        nv21[uvIndex++] = if (srcIndex < vBytes.size) vBytes[srcIndex] else 0
                        nv21[uvIndex++] = if (srcIndex < uBytes.size) uBytes[srcIndex] else 0
                    }
                }
            }
        }

        return nv21
    }

    /**
     * Simple Laplacian variance calculation on Y channel only.
     * Uses a 3x3 Laplacian kernel: [0, 1, 0; 1, -4, 1; 0, 1, 0]
     */
    private fun calculateSimpleLaplacianVariance(
        yuvData: ByteArray,
        width: Int,
        height: Int
    ): Double {
        // Only process Y channel (first width*height bytes)
        val pixelCount = width * height
        if (yuvData.size < pixelCount) return 0.0

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        // Apply 3x3 Laplacian kernel, skip border pixels
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = (yuvData[y * width + x].toInt() and 0xFF).toDouble()
                val top = (yuvData[(y - 1) * width + x].toInt() and 0xFF).toDouble()
                val bottom = (yuvData[(y + 1) * width + x].toInt() and 0xFF).toDouble()
                val left = (yuvData[y * width + (x - 1)].toInt() and 0xFF).toDouble()
                val right = (yuvData[y * width + (x + 1)].toInt() and 0xFF).toDouble()

                val laplacian = -4.0 * center + top + bottom + left + right
                sum += laplacian
                sumSq += laplacian * laplacian
                count++
            }
        }

        if (count == 0) return 0.0

        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return variance
    }

    /**
     * Simple edge density calculation using Sobel-like gradient magnitude.
     * Returns ratio of "edge pixels" to total pixels (0.0 to 1.0).
     */
    private fun calculateSimpleEdgeDensity(
        yuvData: ByteArray,
        width: Int,
        height: Int
    ): Float {
        val pixelCount = width * height
        if (yuvData.size < pixelCount) return 0f

        var edgePixels = 0
        var totalPixels = 0
        val threshold = 30 // Gradient magnitude threshold for "edge"

        // Simple horizontal + vertical gradient, skip borders
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = (yuvData[y * width + (x + 1)].toInt() and 0xFF) -
                        (yuvData[y * width + (x - 1)].toInt() and 0xFF)
                val gy = (yuvData[(y + 1) * width + x].toInt() and 0xFF) -
                        (yuvData[(y - 1) * width + x].toInt() and 0xFF)
                val magnitude = sqrt((gx * gx + gy * gy).toDouble())
                if (magnitude > threshold) edgePixels++
                totalPixels++
            }
        }

        return if (totalPixels > 0) edgePixels.toFloat() / totalPixels else 0f
    }

    /**
     * Build the map of available back-facing lens IDs.
     * 0 = main, 1 = ultrawide, 2 = telephoto
     */
    private fun buildAvailableLensMap() {
        val result = mutableMapOf<Int, String>()
        try {
            val backCameraIds = cameraManager.cameraIdList.filter { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK
            }

            // Classify cameras by focal length
            data class CameraInfo(val id: String, val focalLength: Float)

            val cameraInfos = backCameraIds.mapNotNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val focalLengths = characteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )
                val fl = focalLengths?.firstOrNull() ?: return@mapNotNull null
                CameraInfo(id, fl)
            }.sortedBy { it.focalLength }

            when {
                cameraInfos.size >= 3 -> {
                    result[1] = cameraInfos[0].id // ultrawide (shortest focal length)
                    result[0] = cameraInfos[1].id // main
                    result[2] = cameraInfos[2].id // telephoto (longest focal length)
                }
                cameraInfos.size == 2 -> {
                    // Determine if second is ultrawide or telephoto
                    val ratio = cameraInfos[1].focalLength / cameraInfos[0].focalLength
                    if (ratio > 1.5f) {
                        // First is main/ultrawide, second is telephoto
                        result[0] = cameraInfos[0].id
                        result[2] = cameraInfos[1].id
                    } else {
                        // First is ultrawide, second is main
                        result[1] = cameraInfos[0].id
                        result[0] = cameraInfos[1].id
                    }
                }
                cameraInfos.size == 1 -> {
                    result[0] = cameraInfos[0].id
                }
            }
        } catch (e: Exception) {
            // Fallback: just use first back camera as main
            val backId = cameraManager.cameraIdList.firstOrNull()
            if (backId != null) {
                result[0] = backId
            }
        }

        availableLensMap = result
    }

    private fun getMainFocalLength(): Float {
        return try {
            val cameraId = availableLensMap[0] ?: return 4.5f
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.firstOrNull() ?: 4.5f
        } catch (e: Exception) {
            4.5f
        }
    }

    private fun getTelephotoFocalLength(): Float {
        return try {
            val cameraId = availableLensMap[2] ?: return 9.0f
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.firstOrNull() ?: 9.0f
        } catch (e: Exception) {
            9.0f
        }
    }
}

actual fun createCameraController(context: PlatformContext): CameraController {
    return AndroidCameraController(context)
}
