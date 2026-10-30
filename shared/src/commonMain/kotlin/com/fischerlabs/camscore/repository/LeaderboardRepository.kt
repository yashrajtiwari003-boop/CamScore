package com.fischerlabs.camscore.repository

import com.fischerlabs.camscore.model.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.Direction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository managing Firebase Firestore Monthly Global Ranking.
 * Stores anonymous device-model scores partitioned by YYYY-MM month keys.
 */
class LeaderboardRepository {

    private val firestore = Firebase.firestore
    private val leaderboardCollection = firestore.collection("monthly_rankings")

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: Flow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    /**
     * Submit a device score to the monthly global ranking.
     * Uses device model + month as a composite key, keeping only the highest score per device per month.
     */
    suspend fun submitScore(result: TestResult) = withContext(Dispatchers.Default) {
        val monthKey = result.monthKey
        val deviceModel = result.deviceInfo.deviceModel
        val docId = "${monthKey}_${deviceModel.replace(" ", "_")}"

        val monthCollection = leaderboardCollection.document(monthKey)
            .collection("scores")

        // Check if this device already has a higher score this month
        val existingDoc = try {
            monthCollection.document(docId).get()
        } catch (_: Exception) {
            null
        }

        val existingScore = existingDoc?.let {
            if (it.exists) it.get<Int>("camScore") else null
        }

        // Only update if new score is higher (or no existing score)
        if (existingScore == null || result.camScore > existingScore) {
            monthCollection.document(docId).set(
                mapOf(
                    "deviceModel" to deviceModel,
                    "platform" to result.deviceInfo.platform.name,
                    "camScore" to result.camScore,
                    "healthPercent" to result.overallHealthPercent,
                    "monthKey" to monthKey,
                    "isOisDevice" to result.deviceInfo.hasOis,
                    "testCount" to ((existingDoc?.let {
                        if (it.exists) it.get<Int>("testCount") else 0
                    } ?: 0) + 1),
                    "lastUpdated" to result.timestamp.toEpochMilliseconds()
                )
            )
        }
    }

    /**
     * Fetch the monthly leaderboard, ranked by CamScore descending.
     */
    suspend fun getMonthlyLeaderboard(
        monthKey: String,
        limit: Int = 100
    ): List<LeaderboardEntry> = withContext(Dispatchers.Default) {
        try {
            val snapshot = leaderboardCollection.document(monthKey)
                .collection("scores")
                .orderBy("camScore", Direction.DESCENDING)
                .limit(limit)
                .get()

            val entries = snapshot.documents.mapIndexed { index, doc ->
                LeaderboardEntry(
                    rank = index + 1,
                    deviceModel = doc.get("deviceModel"),
                    platform = try {
                        Platform.valueOf(doc.get<String>("platform"))
                    } catch (_: Exception) {
                        Platform.ANDROID
                    },
                    camScore = doc.get("camScore"),
                    monthKey = doc.get("monthKey"),
                    isOisDevice = doc.get("isOisDevice"),
                    testCount = doc.get("testCount")
                )
            }
            _leaderboard.value = entries
            entries
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get the rank of a specific device model in a given month.
     */
    suspend fun getDeviceRank(
        deviceModel: String,
        monthKey: String
    ): Int? = withContext(Dispatchers.Default) {
        try {
            val allScores = getMonthlyLeaderboard(monthKey, 1000)
            allScores.firstOrNull {
                it.deviceModel.equals(deviceModel, ignoreCase = true)
            }?.rank
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Get leaderboard filtered by OIS capability.
     */
    suspend fun getLeaderboardByFilter(
        isOisOnly: Boolean?,
        monthKey: String,
        limit: Int = 100
    ): List<LeaderboardEntry> = withContext(Dispatchers.Default) {
        val allEntries = getMonthlyLeaderboard(monthKey, 500)

        val filtered = when (isOisOnly) {
            true -> allEntries.filter { it.isOisDevice }
            false -> allEntries.filter { !it.isOisDevice }
            null -> allEntries
        }

        // Re-rank after filtering
        filtered.take(limit).mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }
}
