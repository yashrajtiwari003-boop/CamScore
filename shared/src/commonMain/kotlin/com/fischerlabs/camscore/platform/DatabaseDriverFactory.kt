package com.fischerlabs.camscore.platform

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory(context: PlatformContext) {
    fun createDriver(): SqlDriver
}
