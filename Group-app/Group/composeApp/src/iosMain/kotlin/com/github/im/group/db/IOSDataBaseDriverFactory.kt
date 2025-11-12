package com.github.im.group.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import db.AppDatabase

class IOSDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        // 设置数据库版本号，如果添加了新表需要增加版本号
        return NativeSqliteDriver(AppDatabase.Schema, "launch.db", version = 2)
    }
}