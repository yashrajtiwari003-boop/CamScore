@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.fischerlabs.camscore.platform

import com.fischerlabs.camscore.model.DeviceInfo
import com.fischerlabs.camscore.model.Platform
import platform.AVFoundation.*
import platform.UIKit.UIDevice

/**
 * iOS DeviceInfoProvider querying UIDevice and AVCaptureDevice for camera hardware specs.
 */
actual class DeviceInfoProvider actual constructor(private val context: PlatformContext) {

    actual fun getDeviceModel(): String {
        return UIDevice.currentDevice.model
    }

    actual fun getOsVersion(): String {
        return "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
    }

    actual fun getPlatform(): Platform {
        return Platform.IOS
    }

    actual fun hasOis(): Boolean {
        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack
        )
        val defaultDevice = discoverySession.devices.firstOrNull() as? AVCaptureDevice ?: return true
        val activeFormat = defaultDevice.activeFormat
        return activeFormat.isVideoStabilizationModeSupported(AVCaptureVideoStabilizationModeCinematic)
    }

    actual fun hasUltrawide(): Boolean {
        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInUltraWideCamera),
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack
        )
        return discoverySession.devices.isNotEmpty()
    }

    actual fun hasTelephoto(): Boolean {
        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(
                AVCaptureDeviceTypeBuiltInTelephotoCamera
            ),
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack
        )
        return discoverySession.devices.isNotEmpty()
    }

    actual fun getSensorMegapixels(): Float {
        return 48.0f // Standard modern iPhone sensor
    }

    actual fun getLensCount(): Int {
        var count = 1
        if (hasUltrawide()) count++
        if (hasTelephoto()) count++
        return count
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
}
