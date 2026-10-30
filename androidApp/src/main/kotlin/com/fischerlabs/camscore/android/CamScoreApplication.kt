package com.fischerlabs.camscore.android

import android.app.Application
import com.fischerlabs.camscore.di.platformModule
import com.fischerlabs.camscore.di.sharedModule
import com.fischerlabs.camscore.platform.PlatformContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Android Application entry point.
 * Initializes Koin dependency injection and OpenCV native binaries.
 */
class CamScoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Provide PlatformContext for expect/actual dependencies
        val appPlatformModule = module {
            single<PlatformContext> { this@CamScoreApplication }
        }

        startKoin {
            androidLogger()
            androidContext(this@CamScoreApplication)
            modules(
                appPlatformModule,
                platformModule(),
                sharedModule
            )
        }
    }
}
