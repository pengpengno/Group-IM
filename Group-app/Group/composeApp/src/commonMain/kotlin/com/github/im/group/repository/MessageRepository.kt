package com.github.im.group.repository

import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.proto.ChatMessage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * 聊天消息本地存储
 */
class ChatMessageRepository (
    private val db: AppDatabase
){


    @OptIn(ExperimentalTime::class)
    fun insertMessage(messageItem: ChatMessage){
        db.transaction {
            // 1. 毫秒 → Instant
            val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(messageItem.clientTimeStamp)

            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            db.messageQueries
                .insertMessage(
                    conversation_id = messageItem.conversationId,
                    from_account_id = messageItem.fromAccountInfo?.userId ?: 0L,
                    content = messageItem.content,
                    client_msg_id = messageItem.clientMsgId,
                    client_timestamp = localDateTime,
                    type = MessageType.valueOf(messageItem.type.name),
                    status =MessageStatus.valueOf(messageItem.messagesStatus.name),
                )
        }
    }
}