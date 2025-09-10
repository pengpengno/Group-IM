package com.github.im.group.db

import android.content.Context
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.UserStatus
import db.Message
import db.User

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "launch.db")
    }

    override fun createDatabase(): AppDatabase {
        return AppDatabase(createDriver(),
            messageAdapter = Message.Adapter(
                statusAdapter = EnumColumnAdapter<MessageStatus>()
            ),
            UserAdapter = User.Adapter(
                userStatusAdapter = EnumColumnAdapter<UserStatus>()
            ),
        )
    }
}