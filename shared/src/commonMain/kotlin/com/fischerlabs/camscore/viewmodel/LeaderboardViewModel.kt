package com.fischerlabs.camscore.viewmodel

import com.fischerlabs.camscore.model.LeaderboardEntry
import com.fischerlabs.camscore.repository.LeaderboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the Monthly Global Leaderboard.
 * Manages leaderboard entries, month selection, and OIS/Non-OIS filter.
 */
class LeaderboardViewModel(
    private val leaderboardRepository: LeaderboardRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    enum class LeaderboardFilter(val displayName: String) {
        ALL("All Phones"),
        OIS_ONLY("OIS Phones"),
        NON_OIS_ONLY("Non-OIS / Budget")
    }

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()

    private val _currentMonth = MutableStateFlow(getCurrentMonthKey())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LeaderboardFilter.ALL)
    val selectedFilter: StateFlow<LeaderboardFilter> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLeaderboard(getCurrentMonthKey())
    }

    /**
     * Load leaderboard for a specific month.
     */
    fun loadLeaderboard(monthKey: String) {
        _currentMonth.value = monthKey
        scope.launch {
            _isLoading.value = true
            try {
                val filter = _selectedFilter.value
                val isOisOnly = when (filter) {
                    LeaderboardFilter.ALL -> null
                    LeaderboardFilter.OIS_ONLY -> true
                    LeaderboardFilter.NON_OIS_ONLY -> false
                }
                _entries.value = leaderboardRepository.getLeaderboardByFilter(
                    isOisOnly = isOisOnly,
                    monthKey = monthKey
                )
            } catch (_: Exception) {
                _entries.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Change the leaderboard filter (All, OIS-only, Non-OIS only).
     */
    fun changeFilter(filter: LeaderboardFilter) {
        _selectedFilter.value = filter
        loadLeaderboard(_currentMonth.value)
    }

    /**
     * Navigate to the previous month.
     */
    fun previousMonth() {
        val current = _currentMonth.value
        val parts = current.split("-")
        var year = parts[0].toInt()
        var month = parts[1].toInt() - 1
        if (month < 1) {
            month = 12
            year--
        }
        val newKey = "$year-${month.toString().padStart(2, '0')}"
        loadLeaderboard(newKey)
    }

    /**
     * Navigate to the next month.
     */
    fun nextMonth() {
        val current = _currentMonth.value
        val parts = current.split("-")
        var year = parts[0].toInt()
        var month = parts[1].toInt() + 1
        if (month > 12) {
            month = 1
            year++
        }
        val newKey = "$year-${month.toString().padStart(2, '0')}"
        loadLeaderboard(newKey)
    }

    /**
     * Refresh the current leaderboard data.
     */
    fun refreshRankings() {
        loadLeaderboard(_currentMonth.value)
    }

    /**
     * Get the display name for a month key (e.g., "2026-10" → "October 2026").
     */
    fun getMonthDisplayName(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0]
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return monthKey
        return if (monthIndex in 0..11) "${monthNames[monthIndex]} $year" else monthKey
    }

    private fun getCurrentMonthKey(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
    }
}
