package com.novelsim.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.novelsim.app.database.NovelSimulatorDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(NovelSimulatorDatabase.Schema, "novel_simulator.db")
    }
}
