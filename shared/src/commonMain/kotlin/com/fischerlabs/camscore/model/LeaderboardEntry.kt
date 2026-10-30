package com.fischerlabs.camscore.model

import kotlinx.serialization.Serializable

/**
 * A single row in the monthly global leaderboard.
 *
 * @property rank 1-based position in the leaderboard for the given month.
 * @property deviceModel Marketing name of the device.
 * @property platform Platform the test was run on.
 * @property camScore Uncapped CamScore benchmark value.
 * @property monthKey YYYY-MM leaderboard period.
 * @property isOisDevice Whether the device has OIS hardware.
 * @property testCount Total number of tests submitted by this device model in the month.
 */
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val deviceModel: String,
    val platform: Platform,
    val camScore: Int,
    val monthKey: String,
    val isOisDevice: Boolean,
    val testCount: Int
)
