package com.github.im.group.repository

import com.github.im.group.api.ChatApi
import com.github.im.group.api.MessageDTO
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import db.Message
import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ChatMessageRepository(
    private val db: AppDatabase,
    private val userRepository: UserRepository
) {

    fun getMessageByClientMsgId(clientMsgId: String): MessageItem? {
        val entity = db.messageQueries.selectMessageByClientMsgId(clientMsgId).executeAsOneOrNull() ?: return null
        return MessageWrapper(messageDto = entityToMessageDTO(entity))
    }

    fun insertOrUpdateMessage(messageItem: MessageItem) {
        saveUserInfo(messageItem.userInfo)

        val existing = findExistingMessage(messageItem)
        if (existing != null) {
            mergeIntoExistingMessage(existing, messageItem)
            return
        }

        insertNewMessage(messageItem)
    }

    fun insertMessages(messageDTOs: List<MessageDTO>) {
        if (messageDTOs.isEmpty()) return
        db.transaction {
            messageDTOs.forEach { insertOrUpdateMessage(MessageWrapper(messageDto = it)) }
        }
    }

    fun getMaxSequenceId(conversationId: Long): Long {
        return db.messageQueries
            .selectMaxSequenceIdByConversation(conversationId)
            .executeAsOneOrNull()
            ?.MAX ?: 0L
    }

    fun getMessagesByConversation(conversationId: Long, limit: Long = 30): List<MessageItem> {
        val rows = db.messageQueries
            .selectMessagesWithUserInfoByConversation(conversationId, limit)
            .executeAsList()

        return rows.map { row ->
            rowToMessageWrapper(
                msgId = row.msg_id,
                conversationId = row.conversation_id,
                clientMsgId = row.client_msg_id,
                fromAccountId = row.from_account_id,
                username = row.username,
                email = row.email,
                status = row.status,
                content = row.content,
                type = row.type,
                serverTimestamp = row.server_timestamp,
                clientTimestamp = row.client_timestamp,
                sequenceId = row.sequence_id
            )
        }
    }

    fun getLatestMessages(conversationId: Long, limit: Long = 30): List<MessageItem> {
        return getMessagesByConversation(conversationId, limit)
    }

    suspend fun getLatestMessage(conversationId: Long): MessageWrapper? {
        try {
            val localMaxSequenceId = getMaxSequenceId(conversationId)
            val remoteMessages = ChatApi.getMessages(conversationId, localMaxSequenceId).content

            if (remoteMessages.isNotEmpty()) {
                val users = remoteMessages
                    .mapNotNull { it.fromAccount }
                    .filter { it.username.isNotBlank() }
                if (users.isNotEmpty()) {
                    userRepository.addOrUpdateUsers(users)
                }
                insertMessages(remoteMessages)
            }
        } catch (e: Exception) {
            Napier.e("sync latest message failed, fallback to local conversationId=$conversationId", e)
        }

        return getLocalLatestMessage(conversationId)
    }

    fun getLocalLatestMessage(conversationId: Long): MessageWrapper? {
        return try {
            val row = db.messageQueries
                .selectLatestMessageWithUserInfoByConversation(conversationId)
                .executeAsOneOrNull() ?: return null

            rowToMessageWrapper(
                msgId = row.msg_id,
                conversationId = row.conversation_id,
                clientMsgId = row.client_msg_id,
                fromAccountId = row.from_account_id,
                username = row.username,
                email = row.email,
                status = row.status,
                content = row.content,
                type = row.type,
                serverTimestamp = row.server_timestamp,
                clientTimestamp = row.client_timestamp,
                sequenceId = row.sequence_id
            )
        } catch (e: Exception) {
            Napier.e("get local latest message failed, conversationId=$conversationId", e)
            null
        }
    }

    fun getMessagesBeforeSequence(
        conversationId: Long,
        beforeSequenceId: Long,
        limit: Long = 20
    ): List<MessageItem> {
        val rows = db.messageQueries
            .selectMessagesWithUserInfoBeforeSequence(conversationId, beforeSequenceId, limit)
            .executeAsList()

        return rows.map { row ->
            rowToMessageWrapper(
                msgId = row.msg_id,
                conversationId = row.conversation_id,
                clientMsgId = row.client_msg_id,
                fromAccountId = row.from_account_id,
                username = row.username,
                email = row.email,
                status = row.status,
                content = row.content,
                type = row.type,
                serverTimestamp = row.server_timestamp,
                clientTimestamp = row.client_timestamp,
                sequenceId = row.sequence_id
            )
        }
    }

    fun getMessagesAfterSequence(
        conversationId: Long,
        afterSequenceId: Long,
        limit: Long = 20
    ): List<MessageItem> {
        val rows = db.messageQueries
            .selectMessagesWithUserInfoAfterSequence(conversationId, afterSequenceId, limit)
            .executeAsList()

        return rows.map { row ->
            rowToMessageWrapper(
                msgId = row.msg_id,
                conversationId = row.conversation_id,
                clientMsgId = row.client_msg_id,
                fromAccountId = row.from_account_id,
                username = row.username,
                email = row.email,
                status = row.status,
                content = row.content,
                type = row.type,
                serverTimestamp = row.server_timestamp,
                clientTimestamp = row.client_timestamp,
                sequenceId = row.sequence_id
            )
        }
    }

    fun getUnreadCount(conversationId: Long, currentUserId: Long): Int {
        return try {
            db.messageQueries.selectUnreadCountByConversation(conversationId, currentUserId)
                .executeAsOne()
                .toInt()
        } catch (e: Exception) {
            Napier.e("get unread count failed, conversationId=$conversationId", e)
            0
        }
    }

    fun markConversationMessagesAsRead(conversationId: Long, currentUserId: Long) {
        try {
            db.messageQueries.markConversationMessagesAsRead(conversationId, currentUserId)
        } catch (e: Exception) {
            Napier.e("mark conversation messages as read failed, conversationId=$conversationId", e)
        }
    }

    private fun findExistingMessage(messageItem: MessageItem): Message? {
        val byClientMsgId = messageItem.clientMsgId.takeIf { it.isNotBlank() }
            ?.let { db.messageQueries.selectMessageByClientMsgId(it).executeAsOneOrNull() }
        if (byClientMsgId != null) return byClientMsgId

        if (messageItem.id > 0L) {
            val byMsgId = db.messageQueries.selectMessageByMsgId(messageItem.id).executeAsOneOrNull()
            if (byMsgId != null) return byMsgId
        }

        return null
    }

    private fun mergeIntoExistingMessage(existing: Message, messageItem: MessageItem) {
        val mergedStatus = resolveMergedStatus(existing.status, messageItem.status)
        val mergedServerTime = messageItem.time.takeUnless { it == EPOCH }
            ?: existing.server_timestamp
            ?: existing.client_timestamp
            ?: EPOCH
        val mergedSequence = if (messageItem.seqId > 0L) messageItem.seqId else existing.sequence_id ?: 0L
        val mergedMsgId = messageItem.id.takeIf { it > 0L } ?: existing.msg_id

        when {
            existing.client_msg_id != null -> db.messageQueries.updateMessageByClientMsgId(
                status = mergedStatus,
                server_timestamp = mergedServerTime,
                sequence_id = mergedSequence,
                msg_id = mergedMsgId,
                client_msg_id = existing.client_msg_id
            )

            existing.msg_id != null -> db.messageQueries.updateMessageByMsgId(
                status = mergedStatus,
                server_timestamp = mergedServerTime,
                sequence_id = mergedSequence,
                msg_id = existing.msg_id
            )

            else -> Unit
        }
    }

    private fun insertNewMessage(messageItem: MessageItem) {
        db.transaction {
            if (messageItem.id > 0L) {
                db.messageQueries.insertMessageWithMsgId(
                    conversation_id = messageItem.conversationId,
                    msg_id = messageItem.id,
                    from_account_id = messageItem.userInfo.userId,
                    content = messageItem.content,
                    client_msg_id = messageItem.clientMsgId.ifBlank { null },
                    type = messageItem.type,
                    status = messageItem.status,
                    server_timestamp = messageItem.time.takeUnless { it == EPOCH } ?: messageItem.clientTime ?: EPOCH,
                    sequence_id = messageItem.seqId
                )
            } else {
                db.messageQueries.insertMessage(
                    conversation_id = messageItem.conversationId,
                    from_account_id = messageItem.userInfo.userId,
                    content = messageItem.content,
                    client_msg_id = messageItem.clientMsgId.ifBlank { null },
                    type = messageItem.type,
                    status = messageItem.status,
                    client_timestamp = messageItem.clientTime ?: messageItem.time.takeUnless { it == EPOCH } ?: currentLocalTime(),
                    sequence_id = messageItem.seqId
                )
            }
        }
    }

    private fun saveUserInfo(userInfo: UserInfo) {
        if (userInfo.userId <= 0L) return
        db.transaction {
            val existingUser = db.userQueries.selectById(userInfo.userId).executeAsOneOrNull()
            if (existingUser == null) {
                db.userQueries.insertUser(
                    userId = userInfo.userId,
                    username = userInfo.username,
                    email = userInfo.email,
                    phoneNumber = userInfo.phoneNumber ?: "",
                    avatarUrl = null,
                    bio = null,
                    userStatus = com.github.im.group.db.entities.UserStatus.ACTIVE
                )
            }
        }
    }

    private fun entityToMessageDTO(entity: Message): MessageDTO {
        return MessageDTO(
            msgId = entity.msg_id,
            clientMsgId = entity.client_msg_id,
            content = entity.content,
            fromAccount = userRepository.getUserById(entity.from_account_id),
            type = entity.type,
            status = entity.status,
            timestamp = effectiveTimestamp(entity.server_timestamp, entity.client_timestamp).toString(),
            conversationId = entity.conversation_id,
            sequenceId = entity.sequence_id
        )
    }

    private fun rowToMessageWrapper(
        msgId: Long?,
        conversationId: Long,
        clientMsgId: String?,
        fromAccountId: Long,
        username: String?,
        email: String?,
        status: MessageStatus,
        content: String,
        type: com.github.im.group.db.entities.MessageType,
        serverTimestamp: LocalDateTime?,
        clientTimestamp: LocalDateTime?,
        sequenceId: Long?
    ): MessageWrapper {
        return MessageWrapper(
            messageDto = MessageDTO(
                msgId = msgId,
                conversationId = conversationId,
                clientMsgId = clientMsgId,
                fromAccountId = fromAccountId,
                status = status,
                content = content,
                type = type,
                timestamp = effectiveTimestamp(serverTimestamp, clientTimestamp).toString(),
                sequenceId = sequenceId,
                fromAccount = UserInfo(
                    userId = fromAccountId,
                    username = username.orEmpty(),
                    email = email.orEmpty()
                )
            )
        )
    }

    private fun effectiveTimestamp(
        serverTimestamp: LocalDateTime?,
        clientTimestamp: LocalDateTime?
    ): LocalDateTime {
        return serverTimestamp ?: clientTimestamp ?: EPOCH
    }

    private fun currentLocalTime(): LocalDateTime {
        return Instant.fromEpochMilliseconds(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private fun resolveMergedStatus(
        existingStatus: MessageStatus,
        incomingStatus: MessageStatus
    ): MessageStatus {
        return when {
            incomingStatus == MessageStatus.READ || existingStatus == MessageStatus.READ -> MessageStatus.READ
            incomingStatus == MessageStatus.RECEIVED || existingStatus == MessageStatus.RECEIVED -> MessageStatus.RECEIVED
            incomingStatus == MessageStatus.SENT || existingStatus == MessageStatus.SENT -> MessageStatus.SENT
            incomingStatus == MessageStatus.FAILED -> MessageStatus.FAILED
            else -> incomingStatus
        }
    }

    companion object {
        private val EPOCH: LocalDateTime = Instant.fromEpochMilliseconds(0)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
