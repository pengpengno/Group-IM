package com.github.im.group.db

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
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
import db.OfflineMessage
import db.OrganizationCache
import db.User

class IOSDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "launch.db")
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
            FileResourceAdapter = FileResource.Adapter(
                statusAdapter = EnumColumnAdapter<FileStatus>(),
                uploadTimeAdapter = localDateTimeAdapter
            ),
            FriendshipAdapter = Friendship.Adapter(
                statusAdapter = EnumColumnAdapter<FriendRequestStatus>(),
                created_atAdapter = localDateTimeAdapter,
                updated_atAdapter = localDateTimeAdapter
            ),
            OfflineMessageAdapter = OfflineMessage.Adapter(
                statusAdapter = EnumColumnAdapter<MessageStatus>(),
                created_atAdapter = localDateTimeAdapter,
                message_typeAdapter = EnumColumnAdapter<MessageType>()
            ),
            OrganizationCacheAdapter = OrganizationCache.Adapter(
                updated_atAdapter = localDateTimeAdapter
            ),
        )
    }
}