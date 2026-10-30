package com.fischerlabs.camscore.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.sqrt

/**
 * Android SensorController implementation using SensorManager.
 *
 * Records gyroscope and accelerometer data at ~100Hz for OIS
 * benchmarking correlation with optical flow data.
 */
class AndroidSensorController(context: Context) : SensorController {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Thread-safe deques for sensor readings, bounded to prevent memory leaks
    private val gyroHistory = ConcurrentLinkedDeque<GyroReading>()
    private val accelHistory = ConcurrentLinkedDeque<AccelReading>()

    // Maximum number of readings to retain (~100Hz * 30 seconds = 3000)
    private val maxHistorySize = 3000

    private var gyroListenerActive = false
    private var accelListenerActive = false

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val reading = GyroReading(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestampNanos = event.timestamp
            )
            gyroHistory.addLast(reading)
            // Trim if exceeds max size
            while (gyroHistory.size > maxHistorySize) {
                gyroHistory.pollFirst()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No-op: accuracy changes don't affect our variance calculations
        }
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val reading = AccelReading(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestampNanos = event.timestamp
            )
            accelHistory.addLast(reading)
            while (accelHistory.size > maxHistorySize) {
                accelHistory.pollFirst()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No-op
        }
    }

    override fun startGyroscopeLogging() {
        if (gyroListenerActive || gyroscope == null) return

        // SENSOR_DELAY_FASTEST gives ~200-500Hz; we use a custom period of 10ms (100Hz)
        // SensorManager.registerListener with a desired period in microseconds
        val desiredPeriodUs = 10_000 // 10ms = 100Hz
        sensorManager.registerListener(
            gyroListener,
            gyroscope,
            desiredPeriodUs
        )
        gyroListenerActive = true
    }

    override fun stopGyroscopeLogging() {
        if (!gyroListenerActive) return
        sensorManager.unregisterListener(gyroListener)
        gyroListenerActive = false
    }

    override fun startAccelerometerLogging() {
        if (accelListenerActive || accelerometer == null) return

        val desiredPeriodUs = 10_000 // 10ms = 100Hz
        sensorManager.registerListener(
            accelListener,
            accelerometer,
            desiredPeriodUs
        )
        accelListenerActive = true
    }

    override fun stopAccelerometerLogging() {
        if (!accelListenerActive) return
        sensorManager.unregisterListener(accelListener)
        accelListenerActive = false
    }

    override fun getLatestGyroReading(): GyroReading {
        return gyroHistory.peekLast() ?: GyroReading(0f, 0f, 0f, 0L)
    }

    override fun getLatestAccelReading(): AccelReading {
        return accelHistory.peekLast() ?: AccelReading(0f, 0f, 0f, 0L)
    }

    override fun getGyroHistory(durationMs: Long): List<GyroReading> {
        if (gyroHistory.isEmpty()) return emptyList()

        val latestTimestamp = gyroHistory.peekLast()?.timestampNanos ?: return emptyList()
        val durationNanos = durationMs * 1_000_000L
        val cutoffTimestamp = latestTimestamp - durationNanos

        // Filter readings within the requested duration window
        return gyroHistory.filter { it.timestampNanos >= cutoffTimestamp }
    }

    override fun getAngularVelocityMagnitude(): Double {
        val latest = gyroHistory.peekLast() ?: return 0.0
        return sqrt(
            (latest.x.toDouble() * latest.x.toDouble()) +
                    (latest.y.toDouble() * latest.y.toDouble()) +
                    (latest.z.toDouble() * latest.z.toDouble())
        )
    }

    override fun clearHistory() {
        gyroHistory.clear()
        accelHistory.clear()
    }
}

actual fun createSensorController(context: PlatformContext): SensorController {
    return AndroidSensorController(context)
}
