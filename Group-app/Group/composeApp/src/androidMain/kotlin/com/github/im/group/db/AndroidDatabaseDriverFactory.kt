package com.github.im.group.db

import android.content.Context
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.github.im.group.db.entities.ConversationStatus
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.db.entities.UserStatus
import db.Conversation
import db.FileResource
import db.Message
import db.User

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "launch.db")
    }

    override fun createDatabase(): AppDatabase {
        return AppDatabase(createDriver(),
            messageAdapter = Message.Adapter(
                statusAdapter = EnumColumnAdapter<MessageStatus>(),
                client_timestampAdapter = localDateTimeAdapter,
                server_timestampAdapter = localDateTimeAdapter,
                typeAdapter = EnumColumnAdapter<MessageType>(),
            ),
            UserAdapter = User.Adapter(
                userStatusAdapter = EnumColumnAdapter<UserStatus>()

            ),
            ConversationAdapter = Conversation.Adapter(
                createdAtAdapter = localDateTimeAdapter,
                statusAdapter = EnumColumnAdapter<ConversationStatus>()
            ),
            FileResourceAdapter =  FileResource.Adapter(
                statusAdapter = EnumColumnAdapter<FileStatus>(),
                uploadTimeAdapter = localDateTimeAdapter
            ),
        )
    }
}