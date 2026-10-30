package com.fischerlabs.camscore.viewmodel

import com.fischerlabs.camscore.model.TestResult
import com.fischerlabs.camscore.repository.LeaderboardRepository
import com.fischerlabs.camscore.repository.TestRepository
import com.fischerlabs.camscore.repository.HealthTrendPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Results screen.
 * Manages test result display, leaderboard rank lookup, and score animation.
 */
class ResultViewModel(
    private val testRepository: TestRepository,
    private val leaderboardRepository: LeaderboardRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _testResult = MutableStateFlow<TestResult?>(null)
    val testResult: StateFlow<TestResult?> = _testResult.asStateFlow()

    private val _monthlyRank = MutableStateFlow<Int?>(null)
    val monthlyRank: StateFlow<Int?> = _monthlyRank.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _historicalResults = MutableStateFlow<List<TestResult>>(emptyList())
    val historicalResults: StateFlow<List<TestResult>> = _historicalResults.asStateFlow()

    /**
     * Load a test result by ID and look up its monthly rank.
     */
    fun loadResult(testId: String) {
        scope.launch {
            _isLoading.value = true
            try {
                val result = testRepository.getResultById(testId)
                _testResult.value = result

                // Look up monthly rank
                result?.let {
                    val rank = leaderboardRepository.getDeviceRank(
                        deviceModel = it.deviceInfo.deviceModel,
                        monthKey = it.monthKey
                    )
                    _monthlyRank.value = rank
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set a result directly (e.g., from the diagnostic screen).
     */
    fun setResult(result: TestResult) {
        _testResult.value = result
        scope.launch {
            // Submit to leaderboard and look up rank
            leaderboardRepository.submitScore(result)
            val rank = leaderboardRepository.getDeviceRank(
                deviceModel = result.deviceInfo.deviceModel,
                monthKey = result.monthKey
            )
            _monthlyRank.value = rank
        }
    }

    /**
     * Refresh the monthly rank from the leaderboard.
     */
    fun refreshRank() {
        val result = _testResult.value ?: return
        scope.launch {
            val rank = leaderboardRepository.getDeviceRank(
                deviceModel = result.deviceInfo.deviceModel,
                monthKey = result.monthKey
            )
            _monthlyRank.value = rank
        }
    }

    /**
     * Get all historical test results for display.
     */
    fun getHistoricalResults() {
        scope.launch {
            _historicalResults.value = testRepository.getAllResults()
        }
    }
}
