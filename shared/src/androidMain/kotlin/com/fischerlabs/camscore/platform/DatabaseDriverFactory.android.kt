package com.fischerlabs.camscore.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fischerlabs.camscore.database.CamScoreDatabase

actual class DatabaseDriverFactory actual constructor(private val context: PlatformContext) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = CamScoreDatabase.Schema,
            context = context,
            name = "camscore.db"
        )
    }
}
