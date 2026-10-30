package com.fischerlabs.camscore.di

import com.fischerlabs.camscore.platform.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin DI module providing platform implementations.
 */
actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
    single { DeviceInfoProvider(get()) }
    single<CameraController> { createCameraController(get()) }
    single<SensorController> { createSensorController(get()) }
    single<OpenCVProcessor> { createOpenCVProcessor() }
    single<CertificateExporter> { createCertificateExporter(get()) }
}
