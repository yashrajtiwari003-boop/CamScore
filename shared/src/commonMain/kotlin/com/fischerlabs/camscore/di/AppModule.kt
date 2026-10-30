package com.fischerlabs.camscore.di

import com.fischerlabs.camscore.platform.*
import com.fischerlabs.camscore.repository.CertificateRepository
import com.fischerlabs.camscore.repository.LeaderboardRepository
import com.fischerlabs.camscore.repository.TestRepository
import com.fischerlabs.camscore.viewmodel.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared Koin DI module for CamScore.
 * Platform-specific modules extend this with actual implementations.
 */
val sharedModule = module {
    // Repositories
    single { TestRepository(get()) }
    single { LeaderboardRepository() }
    single { CertificateRepository() }

    // ViewModels
    factory { HomeViewModel(get(), get()) }
    factory { DiagnosticViewModel(get(), get(), get(), get()) }
    factory { ResultViewModel(get(), get()) }
    factory { CertificateViewModel(get(), get()) }
    factory { LeaderboardViewModel(get()) }
}

/**
 * Expect platform-specific DI module providing actual implementations
 * of CameraController, SensorController, OpenCVProcessor, etc.
 */
expect fun platformModule(): Module
