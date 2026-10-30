package com.fischerlabs.camscore

import androidx.compose.ui.window.ComposeUIViewController
import com.fischerlabs.camscore.di.platformModule
import com.fischerlabs.camscore.di.sharedModule
import com.fischerlabs.camscore.platform.PlatformContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

/**
 * Initializes Koin dependency injection for iOS native runtime.
 */
fun initKoin() {
    val iosContextModule = module {
        single { PlatformContext() }
    }
    startKoin {
        modules(
            iosContextModule,
            platformModule(),
            sharedModule
        )
    }
}

/**
 * Creates the root UIViewController wrapping the shared Compose Multiplatform application.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CamScoreApp()
}
