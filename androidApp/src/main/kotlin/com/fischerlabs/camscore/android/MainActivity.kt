package com.fischerlabs.camscore.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import com.fischerlabs.camscore.CamScoreApp
import com.fischerlabs.camscore.viewmodel.*
import org.koin.android.ext.android.inject

/**
 * Main Activity for CamScore on Android.
 * Requests hardware camera permissions and hosts the Compose Multiplatform UI.
 */
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by inject()
    private val diagnosticViewModel: DiagnosticViewModel by inject()
    private val resultViewModel: ResultViewModel by inject()
    private val certificateViewModel: CertificateViewModel by inject()
    private val leaderboardViewModel: LeaderboardViewModel by inject()

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            homeViewModel.startPreCheck()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check & request camera permission
        checkAndRequestPermissions()

        setContent {
            CamScoreApp(
                homeViewModel = homeViewModel,
                diagnosticViewModel = diagnosticViewModel,
                resultViewModel = resultViewModel,
                certificateViewModel = certificateViewModel,
                leaderboardViewModel = leaderboardViewModel,
                darkThemeDefault = isSystemInDarkTheme()
            )
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            homeViewModel.startPreCheck()
        }
    }
}
