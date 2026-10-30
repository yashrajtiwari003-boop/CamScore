package com.fischerlabs.camscore.repository

import com.fischerlabs.camscore.database.CamScoreDatabase
import com.fischerlabs.camscore.model.*
import com.fischerlabs.camscore.platform.DatabaseDriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * Repository managing local test history via SQLDelight database.
 * Provides offline storage for test results and health trend tracking.
 */
class TestRepository(driverFactory: DatabaseDriverFactory) {

    private val database = CamScoreDatabase(driverFactory.createDriver())
    private val queries = database.camScoreQueries

    private val _results = MutableStateFlow<List<TestResult>>(emptyList())
    val results: Flow<List<TestResult>> = _results.asStateFlow()

    suspend fun saveTestResult(result: TestResult) = withContext(Dispatchers.Default) {
        queries.insertTestResult(
            test_id = result.testId,
            device_model = result.deviceInfo.deviceModel,
            os_version = result.deviceInfo.osVersion,
            platform = result.deviceInfo.platform.name,
            has_ois = if (result.deviceInfo.hasOis) 1L else 0L,
            has_ultrawide = if (result.deviceInfo.hasUltrawide) 1L else 0L,
            has_telephoto = if (result.deviceInfo.hasTelephoto) 1L else 0L,
            sensor_megapixels = result.deviceInfo.sensorMegapixels.toDouble(),
            lens_count = result.deviceInfo.lensCount.toLong(),
            timestamp_epoch_ms = result.timestamp.toEpochMilliseconds(),
            af_lock_time_ms = result.afResult.lockTimeMs,
            af_peak_sharpness = result.afResult.peakSharpness,
            af_sharpness_variance = result.afResult.sharpnessVariance,
            af_health_percent = result.afResult.healthPercent.toDouble(),
            af_uncapped_score = result.afResult.uncappedScore,
            ois_dampening_ratio = result.oisResult.dampeningRatio,
            ois_pixel_shake = result.oisResult.pixelShake,
            ois_gyro_shake = result.oisResult.gyroShake,
            ois_health_percent = result.oisResult.healthPercent.toDouble(),
            ois_uncapped_score = result.oisResult.uncappedScore,
            ois_is_non_ois_device = if (result.oisResult.isNonOisDevice) 1L else 0L,
            lens_color_shift_delta_e = result.lensResult.colorShiftDeltaE,
            lens_switch_lag_ms = result.lensResult.switchLagMs,
            lens_health_percent = result.lensResult.healthPercent.toDouble(),
            lens_uncapped_score = result.lensResult.uncappedScore,
            overall_health_percent = result.overallHealthPercent.toDouble(),
            cam_score = result.camScore.toLong(),
            health_rating = result.healthRating.name,
            month_key = result.monthKey,
            verification_hash = result.verificationHash
        )
        refreshResults()
    }

    suspend fun getAllResults(): List<TestResult> = withContext(Dispatchers.Default) {
        queries.getAllResults().executeAsList().map { it.toTestResult() }
    }

    suspend fun getResultById(testId: String): TestResult? = withContext(Dispatchers.Default) {
        queries.getResultById(testId).executeAsOneOrNull()?.toTestResult()
    }

    suspend fun getResultsByDevice(deviceModel: String): List<TestResult> = withContext(Dispatchers.Default) {
        queries.getResultsByDevice(deviceModel).executeAsList().map { it.toTestResult() }
    }

    suspend fun getHealthTrend(): List<HealthTrendPoint> = withContext(Dispatchers.Default) {
        queries.getHealthTrend().executeAsList().map { row ->
            HealthTrendPoint(
                testId = row.test_id,
                timestampMs = row.timestamp_epoch_ms,
                healthPercent = row.overall_health_percent.toFloat(),
                camScore = row.cam_score.toInt()
            )
        }
    }

    suspend fun deleteResult(testId: String) = withContext(Dispatchers.Default) {
        queries.deleteResult(testId)
        refreshResults()
    }

    suspend fun clearHistory() = withContext(Dispatchers.Default) {
        queries.clearAllHistory()
        refreshResults()
    }

    suspend fun getResultCount(): Long = withContext(Dispatchers.Default) {
        queries.countResults().executeAsOne()
    }

    private suspend fun refreshResults() {
        _results.value = getAllResults()
    }

    private fun com.fischerlabs.camscore.database.Test_results.toTestResult(): TestResult {
        val platform = try {
            Platform.valueOf(this.platform)
        } catch (_: Exception) {
            Platform.ANDROID
        }

        val deviceInfo = DeviceInfo(
            deviceModel = this.device_model,
            osVersion = this.os_version,
            platform = platform,
            hasOis = this.has_ois != 0L,
            hasUltrawide = this.has_ultrawide != 0L,
            hasTelephoto = this.has_telephoto != 0L,
            sensorMegapixels = this.sensor_megapixels.toFloat(),
            lensCount = this.lens_count.toInt()
        )

        return TestResult(
            testId = this.test_id,
            deviceInfo = deviceInfo,
            timestamp = Instant.fromEpochMilliseconds(this.timestamp_epoch_ms),
            afResult = AutofocusResult(
                lockTimeMs = this.af_lock_time_ms,
                peakSharpness = this.af_peak_sharpness,
                sharpnessVariance = this.af_sharpness_variance,
                healthPercent = this.af_health_percent.toFloat(),
                uncappedScore = this.af_uncapped_score
            ),
            oisResult = OisResult(
                dampeningRatio = this.ois_dampening_ratio,
                pixelShake = this.ois_pixel_shake,
                gyroShake = this.ois_gyro_shake,
                healthPercent = this.ois_health_percent.toFloat(),
                uncappedScore = this.ois_uncapped_score,
                isNonOisDevice = this.ois_is_non_ois_device != 0L
            ),
            lensResult = LensSwitchResult(
                colorShiftDeltaE = this.lens_color_shift_delta_e,
                switchLagMs = this.lens_switch_lag_ms,
                healthPercent = this.lens_health_percent.toFloat(),
                uncappedScore = this.lens_uncapped_score
            ),
            overallHealthPercent = this.overall_health_percent.toFloat(),
            camScore = this.cam_score.toInt(),
            healthRating = try {
                HealthRating.valueOf(this.health_rating)
            } catch (_: Exception) {
                HealthRating.fromPercent(this.overall_health_percent.toFloat())
            },
            monthKey = this.month_key,
            verificationHash = this.verification_hash
        )
    }
}

/**
 * A single data point for the health-over-time trend chart.
 */
data class HealthTrendPoint(
    val testId: String,
    val timestampMs: Long,
    val healthPercent: Float,
    val camScore: Int
)
