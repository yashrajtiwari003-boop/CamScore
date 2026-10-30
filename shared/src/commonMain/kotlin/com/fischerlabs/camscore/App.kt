package com.fischerlabs.camscore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fischerlabs.camscore.model.*
import com.fischerlabs.camscore.ui.components.CamScoreTopBar
import com.fischerlabs.camscore.ui.navigation.CamScoreNavHost
import com.fischerlabs.camscore.ui.theme.CamScoreTheme
import com.fischerlabs.camscore.viewmodel.*
import kotlinx.coroutines.delay

/**
 * Root Composable entry point for CamScore by Fischer Labs (Android & iOS).
 * Houses the dual-theme adaptive brand system, navigation host, and diagnostic state coordinator.
 */
@Composable
fun CamScoreApp(
    homeViewModel: HomeViewModel? = null,
    diagnosticViewModel: DiagnosticViewModel? = null,
    resultViewModel: ResultViewModel? = null,
    certificateViewModel: CertificateViewModel? = null,
    leaderboardViewModel: LeaderboardViewModel? = null,
    darkThemeDefault: Boolean = isSystemInDarkTheme()
) {
    var isDarkTheme by remember { mutableStateOf(darkThemeDefault) }

    // State bindings
    val preCheckStatus = homeViewModel?.preCheckStatus?.collectAsState()?.value
        ?: PreCheckStatus(
            lightingOk = true,
            textureOk = true,
            isReady = true,
            ambientLux = 350f,
            textureScore = 0.85f,
            hasOis = true,
            errorMessage = null
        )

    val diagnosticUiState = diagnosticViewModel?.uiState?.collectAsState()?.value
        ?: DiagnosticViewModel.DiagnosticUiState(
            currentPhase = DiagnosticPhase.IDLE,
            elapsedSeconds = 0f,
            phaseProgress = 0f,
            hasOis = true,
            isRunning = false
        )

    val currentResult = resultViewModel?.testResult?.collectAsState()?.value
    val monthlyRank = resultViewModel?.monthlyRank?.collectAsState()?.value
    val historyResults = resultViewModel?.historicalResults?.collectAsState()?.value ?: emptyList()

    val certificate = certificateViewModel?.certificate?.collectAsState()?.value
    val isExporting = certificateViewModel?.isExporting?.collectAsState()?.value ?: false

    val leaderboardEntries = leaderboardViewModel?.entries?.collectAsState()?.value ?: emptyList()
    val currentMonth = leaderboardViewModel?.currentMonth?.collectAsState()?.value ?: "2026-10"
    val selectedFilter = leaderboardViewModel?.selectedFilter?.collectAsState()?.value
        ?: LeaderboardViewModel.LeaderboardFilter.ALL
    val isLoadingLeaderboard = leaderboardViewModel?.isLoading?.collectAsState()?.value ?: false

    // Initialize pre-check
    LaunchedEffect(Unit) {
        homeViewModel?.startPreCheck()
        leaderboardViewModel?.refreshRankings()
        resultViewModel?.getHistoricalResults()
    }

    CamScoreTheme(darkTheme = isDarkTheme) {
        Scaffold(
            topBar = {
                CamScoreTopBar(
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    isDarkMode = isDarkTheme
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CamScoreNavHost(
                    preCheckStatus = preCheckStatus,
                    diagnosticState = diagnosticUiState,
                    testResult = currentResult,
                    certificate = certificate,
                    leaderboardEntries = leaderboardEntries,
                    historyResults = historyResults,
                    currentMonth = currentMonth,
                    leaderboardFilter = selectedFilter,
                    monthlyRank = monthlyRank,
                    onStartDiagnostic = {
                        val info = homeViewModel?.deviceInfo?.value ?: DeviceInfo(
                            deviceModel = "Pixel 8 Pro",
                            osVersion = "Android 14",
                            platform = Platform.ANDROID,
                            hasOis = true,
                            hasUltrawide = true,
                            hasTelephoto = true,
                            sensorMegapixels = 50.0f,
                            lensCount = 3
                        )
                        diagnosticViewModel?.startDiagnostic(info)
                    },
                    onCancelDiagnostic = {
                        diagnosticViewModel?.cancelDiagnostic()
                    },
                    onRefreshLeaderboard = {
                        leaderboardViewModel?.refreshRankings()
                    },
                    onFilterLeaderboard = { filter ->
                        leaderboardViewModel?.changeFilter(filter)
                    },
                    onPreviousMonth = {
                        leaderboardViewModel?.previousMonth()
                    },
                    onNextMonth = {
                        leaderboardViewModel?.nextMonth()
                    },
                    onExportCertificatePng = {
                        certificateViewModel?.exportAsPng()
                    },
                    onExportCertificatePdf = {
                        certificateViewModel?.exportAsPdf()
                    },
                    onShareCertificate = {
                        certificateViewModel?.shareCertificate()
                    },
                    onShareResult = {
                        certificateViewModel?.shareCertificate()
                    },
                    isExportingCertificate = isExporting,
                    isLoadingLeaderboard = isLoadingLeaderboard
                )
            }
        }
    }
}
