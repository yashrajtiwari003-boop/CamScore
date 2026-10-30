package com.fischerlabs.camscore.platform

import com.fischerlabs.camscore.model.DeviceInfo
import com.fischerlabs.camscore.model.Platform

expect class DeviceInfoProvider(context: PlatformContext) {
    fun getDeviceModel(): String
    fun getOsVersion(): String
    fun getPlatform(): Platform
    fun hasOis(): Boolean
    fun hasUltrawide(): Boolean
    fun hasTelephoto(): Boolean
    fun getSensorMegapixels(): Float
    fun getLensCount(): Int
    fun getDeviceInfo(): DeviceInfo
}
