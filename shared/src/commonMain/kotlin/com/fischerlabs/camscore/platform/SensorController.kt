package com.fischerlabs.camscore.platform

/**
 * Gyroscope reading at a point in time.
 * @param x Angular velocity around X axis in rad/s
 * @param y Angular velocity around Y axis in rad/s
 * @param z Angular velocity around Z axis in rad/s
 * @param timestampNanos Monotonic timestamp in nanoseconds
 */
data class GyroReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampNanos: Long
)

/**
 * Accelerometer reading at a point in time.
 * @param x Acceleration along X axis in m/s²
 * @param y Acceleration along Y axis in m/s²
 * @param z Acceleration along Z axis in m/s²
 * @param timestampNanos Monotonic timestamp in nanoseconds
 */
data class AccelReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampNanos: Long
)

/**
 * Interface for high-frequency inertial sensor data collection.
 * Used for OIS benchmarking (Phase 2) to correlate physical motion
 * with optical flow measurements.
 */
interface SensorController {
    /** Start logging gyroscope data at ~100Hz. */
    fun startGyroscopeLogging()

    /** Stop logging gyroscope data. */
    fun stopGyroscopeLogging()

    /** Start logging accelerometer data at ~100Hz. */
    fun startAccelerometerLogging()

    /** Stop logging accelerometer data. */
    fun stopAccelerometerLogging()

    /** Get the most recent gyroscope reading. */
    fun getLatestGyroReading(): GyroReading

    /** Get the most recent accelerometer reading. */
    fun getLatestAccelReading(): AccelReading

    /**
     * Get gyroscope readings from the last [durationMs] milliseconds.
     * @param durationMs Duration in milliseconds to look back
     */
    fun getGyroHistory(durationMs: Long): List<GyroReading>

    /**
     * Get the current angular velocity magnitude: √(x² + y² + z²)
     * Used as the denominator in the OIS compensation ratio.
     */
    fun getAngularVelocityMagnitude(): Double

    /** Clear all stored sensor reading history. */
    fun clearHistory()
}

expect fun createSensorController(context: PlatformContext): SensorController
