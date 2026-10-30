package com.fischerlabs.camscore.ui.model

/**
 * All shared data models/enums used across the CamScore UI.
 */

// ── Badge Types ───────────────────────────────────────────────────────────────

enum class BadgeType {
    SUCCESS, WARNING, ERROR, INFO, NEUTRAL
}

// ── Health Rating ─────────────────────────────────────────────────────────────

enum class HealthRating(val label: String) {
    MINT("PASS"),
    MINOR_WEAR("MINOR WEAR"),
    FAIL("FAIL")
}

// ── Diagnostic Phase ──────────────────────────────────────────────────────────

enum class DiagnosticPhase(val label: String) {
    IDLE("Idle"),
    PRE_CHECK("Pre-Check"),
    AUTOFOCUS("Autofocus Speed"),
    OIS_STABILITY("OIS Stabilization"),
    LENS_SWITCH("Lens Switch & Color"),
    COMPLETED("Completed"),
    ERROR("Error")
}

// ── Platform ──────────────────────────────────────────────────────────────────

enum class Platform(val label: String) {
    ANDROID("Android"),
    IOS("iOS")
}

// ── Leaderboard Filter ────────────────────────────────────────────────────────

enum class LeaderboardFilter(val label: String) {
    ALL("All Phones"),
    OIS_ONLY("OIS Phones"),
    NON_OIS_ONLY("Non-OIS / Budget")
}

// ── Pre-Check Status ──────────────────────────────────────────────────────────

data class PreCheckStatus(
    val lightingOk: Boolean = false,
    val textureOk: Boolean = false,
    val isReady: Boolean = false,
    val ambientLux: Float = 0f,
    val textureScore: Float = 0f,
    val hasOis: Boolean = true,
    val errorMessage: String? = null
)

// ── Device Info ───────────────────────────────────────────────────────────────

data class DeviceInfo(
    val deviceModel: String = "Unknown Device",
    val hasOis: Boolean = true
)

// ── Sub-test Results ──────────────────────────────────────────────────────────

data class AfResult(
    val healthPercent: Float = 0f,
    val lockTimeMs: Long = 0L,
    val uncappedScore: Int = 0
)

data class OisResult(
    val healthPercent: Float = 0f,
    val isNonOisDevice: Boolean = false,
    val uncappedScore: Int = 0
)

data class LensResult(
    val healthPercent: Float = 0f,
    val colorShiftDeltaE: Float = 0f,
    val switchLagMs: Long = 0L,
    val uncappedScore: Int = 0
)

// ── Test Result ───────────────────────────────────────────────────────────────

data class TestResult(
    val testId: String = "",
    val camScore: Int = 0,
    val overallHealthPercent: Float = 0f,
    val healthRating: HealthRating = HealthRating.MINT,
    val afResult: AfResult = AfResult(),
    val oisResult: OisResult = OisResult(),
    val lensResult: LensResult = LensResult(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val testDate: String = "",
    val testTime: String = ""
)

// ── Diagnostic UI State ───────────────────────────────────────────────────────

data class DiagnosticUiState(
    val currentPhase: DiagnosticPhase = DiagnosticPhase.IDLE,
    val elapsedSeconds: Float = 0f,
    val phaseProgress: Float = 0f,
    val hasOis: Boolean = true,
    val gyroData: List<Float> = emptyList(),
    val opticalFlowData: List<Float> = emptyList(),
    val isNonOisDevice: Boolean = false
)

// ── Certificate ───────────────────────────────────────────────────────────────

data class Certificate(
    val testId: String = "",
    val deviceModel: String = "",
    val platform: Platform = Platform.ANDROID,
    val testDate: String = "",
    val camScore: Int = 0,
    val healthPercent: Float = 0f,
    val healthRating: HealthRating = HealthRating.MINT,
    val afHealthPercent: Float = 0f,
    val oisHealthPercent: Float? = null,
    val lensHealthPercent: Float = 0f,
    val monthlyRank: Int? = null,
    val verificationHash: String = "",
    val verificationUrl: String = "",
    val isOisDevice: Boolean = true,
    val monthKey: String = ""
)

// ── Leaderboard Entry ─────────────────────────────────────────────────────────

data class LeaderboardEntry(
    val rank: Int = 0,
    val deviceModel: String = "",
    val platform: Platform = Platform.ANDROID,
    val camScore: Int = 0,
    val monthKey: String = "",
    val isOisDevice: Boolean = true,
    val testCount: Int = 0,
    val isCurrentUser: Boolean = false
)
