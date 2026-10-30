package com.fischerlabs.camscore.platform

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.fischerlabs.camscore.model.DeviceInfo
import com.fischerlabs.camscore.model.Platform

actual class DeviceInfoProvider actual constructor(private val context: PlatformContext) {

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    actual fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    actual fun getOsVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    actual fun getPlatform(): Platform {
        return Platform.ANDROID
    }

    actual fun hasOis(): Boolean {
        return try {
            val cameraId = getBackCameraId() ?: return false
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val oisModes = characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
            )
            oisModes != null && oisModes.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON }
        } catch (e: Exception) {
            false
        }
    }

    actual fun hasUltrawide(): Boolean {
        return try {
            val backCameras = getBackCameraIds()
            backCameras.any { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val focalLengths = characteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )
                // Ultrawide lenses typically have focal lengths < 3.0mm (35mm equiv ~16mm)
                focalLengths != null && focalLengths.any { it < 3.0f }
            }
        } catch (e: Exception) {
            false
        }
    }

    actual fun hasTelephoto(): Boolean {
        return try {
            val backCameras = getBackCameraIds()
            // Find the main camera focal length first
            val mainFocalLength = getMainCameraFocalLength()
            backCameras.any { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val focalLengths = characteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )
                // Telephoto has focal length significantly longer than main camera
                focalLengths != null && focalLengths.any { it > mainFocalLength * 1.8f }
            }
        } catch (e: Exception) {
            false
        }
    }

    actual fun getSensorMegapixels(): Float {
        return try {
            val cameraId = getBackCameraId() ?: return 0f
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val sensorSize = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )
            if (sensorSize != null) {
                (sensorSize.width.toLong() * sensorSize.height.toLong()) / 1_000_000f
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    actual fun getLensCount(): Int {
        return try {
            getBackCameraIds().size
        } catch (e: Exception) {
            1
        }
    }

    actual fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceModel = getDeviceModel(),
            osVersion = getOsVersion(),
            platform = getPlatform(),
            hasOis = hasOis(),
            hasUltrawide = hasUltrawide(),
            hasTelephoto = hasTelephoto(),
            sensorMegapixels = getSensorMegapixels(),
            lensCount = getLensCount()
        )
    }

    /**
     * Gets the primary back-facing camera ID (usually the main wide lens).
     */
    private fun getBackCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets all back-facing camera IDs (main, ultrawide, telephoto, etc.).
     * On API 28+, uses physical camera IDs from a logical multi-camera.
     */
    private fun getBackCameraIds(): List<String> {
        return try {
            val allBackCameras = cameraManager.cameraIdList.filter { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            }

            // On API 28+, try to get physical cameras from logical multi-camera
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val logicalCamera = allBackCameras.firstOrNull { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val capabilities = characteristics.get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )
                    capabilities?.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                    ) == true
                }

                if (logicalCamera != null) {
                    val characteristics = cameraManager.getCameraCharacteristics(logicalCamera)
                    val physicalIds = characteristics.physicalCameraIds
                    if (physicalIds.isNotEmpty()) {
                        return physicalIds.toList()
                    }
                }
            }

            allBackCameras
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets the focal length of the main (widest non-ultrawide) back camera.
     */
    private fun getMainCameraFocalLength(): Float {
        return try {
            val cameraId = getBackCameraId() ?: return 4.5f
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val focalLengths = characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            focalLengths?.firstOrNull() ?: 4.5f
        } catch (e: Exception) {
            4.5f // Reasonable default for a smartphone main camera
        }
    }
}
