package com.fischerlabs.camscore.model

import kotlinx.serialization.Serializable

/**
 * Enumeration of supported target platforms.
 */
@Serializable
enum class Platform(val displayName: String) {
    ANDROID("Android"),
    IOS("iOS")
}

/**
 * Describes the hardware capabilities of the device under test.
 *
 * @property deviceModel Marketing name of the device (e.g. "Pixel 9 Pro").
 * @property osVersion OS version string (e.g. "Android 15" or "iOS 18.1").
 * @property platform Target platform enum.
 * @property hasOis Whether the primary rear camera module has optical image stabilisation.
 * @property hasUltrawide Whether an ultra-wide lens is present.
 * @property hasTelephoto Whether a telephoto lens is present.
 * @property sensorMegapixels Megapixel count of the primary sensor.
 * @property lensCount Total number of rear-facing camera lenses.
 */
@Serializable
data class DeviceInfo(
    val deviceModel: String,
    val osVersion: String,
    val platform: Platform,
    val hasOis: Boolean,
    val hasUltrawide: Boolean,
    val hasTelephoto: Boolean,
    val sensorMegapixels: Float,
    val lensCount: Int
) {
    /** True when the device has more than one rear lens. */
    val hasMultipleLenses: Boolean get() = lensCount > 1
}
