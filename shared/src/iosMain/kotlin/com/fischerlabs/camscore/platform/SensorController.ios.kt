@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.fischerlabs.camscore.platform

import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.sqrt

/**
 * iOS SensorController implementation using CoreMotion CMMotionManager at ~100Hz.
 */
class IosSensorController(
    private val context: PlatformContext
) : SensorController {

    private val motionManager = CMMotionManager()
    private val gyroReadings = mutableListOf<GyroReading>()
    private val accelReadings = mutableListOf<AccelReading>()
    private var isGyroActive = false
    private var isAccelActive = false

    override fun startGyroscopeLogging() {
        if (!motionManager.isGyroAvailable() || isGyroActive) return
        motionManager.gyroUpdateInterval = 0.01 // 100 Hz

        motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            data?.let {
                val (gx, gy, gz) = it.rotationRate.useContents { Triple(x.toFloat(), y.toFloat(), z.toFloat()) }
                val reading = GyroReading(
                    x = gx,
                    y = gy,
                    z = gz,
                    timestampNanos = (it.timestamp * 1_000_000_000).toLong()
                )
                gyroReadings.add(reading)
                if (gyroReadings.size > 3000) {
                    gyroReadings.removeAt(0)
                }
            }
        }
        isGyroActive = true
    }

    override fun stopGyroscopeLogging() {
        if (isGyroActive) {
            motionManager.stopGyroUpdates()
            isGyroActive = false
        }
    }

    override fun startAccelerometerLogging() {
        if (!motionManager.isAccelerometerAvailable() || isAccelActive) return
        motionManager.accelerometerUpdateInterval = 0.01 // 100 Hz

        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
            data?.let {
                val (ax, ay, az) = it.acceleration.useContents {
                    Triple(x.toFloat() * 9.81f, y.toFloat() * 9.81f, z.toFloat() * 9.81f)
                }
                val reading = AccelReading(
                    x = ax,
                    y = ay,
                    z = az,
                    timestampNanos = (it.timestamp * 1_000_000_000).toLong()
                )
                accelReadings.add(reading)
                if (accelReadings.size > 3000) {
                    accelReadings.removeAt(0)
                }
            }
        }
        isAccelActive = true
    }

    override fun stopAccelerometerLogging() {
        if (isAccelActive) {
            motionManager.stopAccelerometerUpdates()
            isAccelActive = false
        }
    }

    override fun getLatestGyroReading(): GyroReading {
        return gyroReadings.lastOrNull() ?: GyroReading(0f, 0f, 0f, 0L)
    }

    override fun getLatestAccelReading(): AccelReading {
        return accelReadings.lastOrNull() ?: AccelReading(0f, 0f, 0f, 0L)
    }

    override fun getGyroHistory(durationMs: Long): List<GyroReading> {
        val nowNanos = getLatestGyroReading().timestampNanos
        val thresholdNanos = nowNanos - (durationMs * 1_000_000L)
        return gyroReadings.filter { it.timestampNanos >= thresholdNanos }
    }

    override fun getAngularVelocityMagnitude(): Double {
        val latest = getLatestGyroReading()
        return sqrt((latest.x * latest.x + latest.y * latest.y + latest.z * latest.z).toDouble())
    }

    override fun clearHistory() {
        gyroReadings.clear()
        accelReadings.clear()
    }
}

actual fun createSensorController(context: PlatformContext): SensorController {
    return IosSensorController(context)
}
