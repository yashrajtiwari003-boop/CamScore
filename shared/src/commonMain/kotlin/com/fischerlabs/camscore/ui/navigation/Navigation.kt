package com.fischerlabs.camscore.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.fischerlabs.camscore.model.Certificate
import com.fischerlabs.camscore.model.LeaderboardEntry
import com.fischerlabs.camscore.model.PreCheckStatus
import com.fischerlabs.camscore.model.TestResult
import com.fischerlabs.camscore.ui.components.BottomNavBar
import com.fischerlabs.camscore.ui.screens.*
import com.fischerlabs.camscore.viewmodel.DiagnosticViewModel
import com.fischerlabs.camscore.viewmodel.LeaderboardViewModel.LeaderboardFilter

// ── Route Definitions ─────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Diagnostic : Screen("diagnostic")
    data class Result(val testId: String) : Screen("result/$testId")
    data class CertificateView(val testId: String) : Screen("certificate/$testId")
    data object Leaderboard : Screen("leaderboard")
    data object History : Screen("history")
    data object Onboarding : Screen("onboarding")
}

// ── Bottom Navigation Items ───────────────────────────────────────────────────

enum class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: Screen
) {
    HOME(
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        screen = Screen.Home
    ),
    HISTORY(
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        screen = Screen.History
    ),
    LEADERBOARD(
        label = "Ranking",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        screen = Screen.Leaderboard
    );
}

// ── Navigation Host ───────────────────────────────────────────────────────────

@Composable
fun CamScoreNavHost(
    showOnboarding: Boolean = false,
    preCheckStatus: PreCheckStatus = PreCheckStatus(lightingOk = true, textureOk = true, isReady = true, hasOis = true),
    diagnosticState: DiagnosticViewModel.DiagnosticUiState = DiagnosticViewModel.DiagnosticUiState(),
    testResult: TestResult? = null,
    certificate: Certificate? = null,
    leaderboardEntries: List<LeaderboardEntry> = emptyList(),
    historyResults: List<TestResult> = emptyList(),
    currentMonth: String = "2026-10",
    leaderboardFilter: LeaderboardFilter = LeaderboardFilter.ALL,
    monthlyRank: Int? = null,
    onStartDiagnostic: () -> Unit = {},
    onCancelDiagnostic: () -> Unit = {},
    onRefreshLeaderboard: () -> Unit = {},
    onFilterLeaderboard: (LeaderboardFilter) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onExportCertificatePng: () -> Unit = {},
    onExportCertificatePdf: () -> Unit = {},
    onShareCertificate: () -> Unit = {},
    onShareResult: () -> Unit = {},
    isExportingCertificate: Boolean = false,
    isLoadingLeaderboard: Boolean = false
) {
    var currentScreen by remember {
        mutableStateOf<Screen>(if (showOnboarding) Screen.Onboarding else Screen.Home)
    }
    var selectedNavItem by remember { mutableStateOf(BottomNavItem.HOME) }
    var activeTestResult by remember(testResult) { mutableStateOf(testResult) }
    var activeCertificate by remember(certificate) { mutableStateOf(certificate) }

    val showBottomNav = currentScreen is Screen.Home ||
            currentScreen is Screen.History ||
            currentScreen is Screen.Leaderboard

    if (currentScreen is Screen.Onboarding) {
        OnboardingScreen(
            onComplete = { currentScreen = Screen.Home }
        )
        return
    }

    if (currentScreen is Screen.Diagnostic) {
        DiagnosticScreen(
            uiState = diagnosticState,
            onCancel = {
                onCancelDiagnostic()
                currentScreen = Screen.Home
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    selectedItem = selectedNavItem,
                    onItemSelected = { item ->
                        selectedNavItem = item
                        currentScreen = item.screen
                    },
                    hasNewResults = historyResults.isNotEmpty()
                )
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    modifier = modifier,
                    preCheckStatus = preCheckStatus,
                    onStartTest = {
                        onStartDiagnostic()
                        currentScreen = Screen.Diagnostic
                    },
                    onNavigateToLeaderboard = {
                        selectedNavItem = BottomNavItem.LEADERBOARD
                        currentScreen = Screen.Leaderboard
                    }
                )
            }
            is Screen.History -> {
                HistoryScreen(
                    modifier = modifier,
                    results = historyResults,
                    onResultClick = { testId ->
                        val clickedResult = historyResults.firstOrNull { it.testId == testId }
                        if (clickedResult != null) {
                            activeTestResult = clickedResult
                            currentScreen = Screen.Result(testId)
                        }
                    },
                    onClearHistory = onClearHistory
                )
            }
            is Screen.Leaderboard -> {
                LeaderboardScreen(
                    modifier = modifier,
                    entries = leaderboardEntries,
                    currentMonth = currentMonth,
                    selectedFilter = leaderboardFilter,
                    onFilterChange = onFilterLeaderboard,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onRefresh = onRefreshLeaderboard,
                    isLoading = isLoadingLeaderboard
                )
            }
            is Screen.Result -> {
                val res = activeTestResult ?: testResult
                if (res != null) {
                    ResultScreen(
                        modifier = modifier,
                        result = res,
                        monthlyRank = monthlyRank,
                        onViewCertificate = {
                            currentScreen = Screen.CertificateView(res.testId)
                        },
                        onShareResults = onShareResult,
                        onTestAgain = {
                            selectedNavItem = BottomNavItem.HOME
                            currentScreen = Screen.Home
                        }
                    )
                } else {
                    HomeScreen(
                        modifier = modifier,
                        preCheckStatus = preCheckStatus,
                        onStartTest = {
                            onStartDiagnostic()
                            currentScreen = Screen.Diagnostic
                        },
                        onNavigateToLeaderboard = {
                            selectedNavItem = BottomNavItem.LEADERBOARD
                            currentScreen = Screen.Leaderboard
                        }
                    )
                }
            }
            is Screen.CertificateView -> {
                val cert = activeCertificate ?: certificate
                if (cert != null) {
                    CertificateScreen(
                        modifier = modifier,
                        certificate = cert,
                        onExportPng = onExportCertificatePng,
                        onExportPdf = onExportCertificatePdf,
                        onShare = onShareCertificate,
                        isExporting = isExportingCertificate
                    )
                } else {
                    HomeScreen(
                        modifier = modifier,
                        preCheckStatus = preCheckStatus,
                        onStartTest = {
                            onStartDiagnostic()
                            currentScreen = Screen.Diagnostic
                        },
                        onNavigateToLeaderboard = {
                            selectedNavItem = BottomNavItem.LEADERBOARD
                            currentScreen = Screen.Leaderboard
                        }
                    )
                }
            }
            is Screen.Diagnostic -> { /* Handled above */ }
            is Screen.Onboarding -> { /* Handled above */ }
        }
    }
}
