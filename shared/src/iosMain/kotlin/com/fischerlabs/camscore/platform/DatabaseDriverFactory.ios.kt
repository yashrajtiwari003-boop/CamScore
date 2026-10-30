package com.fischerlabs.camscore.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.fischerlabs.camscore.database.CamScoreDatabase

/**
 * iOS DatabaseDriverFactory using NativeSqliteDriver.
 */
actual class DatabaseDriverFactory actual constructor(private val context: PlatformContext) {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(CamScoreDatabase.Schema, "camscore.db")
    }
}
