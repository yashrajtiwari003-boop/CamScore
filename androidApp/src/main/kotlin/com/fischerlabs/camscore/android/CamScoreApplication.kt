package com.fischerlabs.camscore.android

import android.app.Application
import android.util.Log
import com.fischerlabs.camscore.di.platformModule
import com.fischerlabs.camscore.di.sharedModule
import com.fischerlabs.camscore.platform.PlatformContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

/**
 * Android Application entry point.
 * Initializes Koin dependency injection and platform context.
 */
class CamScoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Provide PlatformContext for expect/actual dependencies
            val appPlatformModule = module {
                single<PlatformContext> { this@CamScoreApplication }
            }

            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@CamScoreApplication)
                modules(
                    appPlatformModule,
                    platformModule(),
                    sharedModule
                )
            }
        } catch (e: Throwable) {
            Log.e("CamScore", "Startup initialization error", e)
        }
    }
}
