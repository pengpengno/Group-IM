package com.github.im.group

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.DatabaseDriverFactory
import com.github.im.group.db.localDateTimeAdapter
import com.github.im.group.db.entities.ConversationStatus
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.db.entities.FriendRequestStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.db.entities.UserStatus
import db.Conversation
import db.FileResource
import db.Friendship
import db.Message
import db.User
import app.cash.sqldelight.EnumColumnAdapter

class DesktopDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // 设置数据库版本号，如果添加了新表需要增加版本号
        AppDatabase.Schema.migrate(driver, 1, 2)
        return driver
    }

    override fun createDatabase(): AppDatabase {
        val driver = createDriver()
        AppDatabase.Schema.create(driver)
        return AppDatabase(driver,
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
            FriendshipAdapter = Friendship.Adapter(
                statusAdapter = EnumColumnAdapter<FriendRequestStatus>(),
                created_atAdapter = localDateTimeAdapter,
                updated_atAdapter = localDateTimeAdapter
            )
        )
    }
}