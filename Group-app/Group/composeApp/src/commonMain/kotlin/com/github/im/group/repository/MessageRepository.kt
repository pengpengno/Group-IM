package com.github.im.group.repository

import com.github.im.group.api.MessageDTO
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.ChatMessage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 聊天消息本地存储
 */
class ChatMessageRepository (
    private val db: AppDatabase
){


    /**
     * 根据 clientMsgId 查询消息
     */
    fun getMessageByClientMsgId(clientMsgId: String): MessageItem? {
        val entity = db.messageQueries.selectMessageByClientMsgId(clientMsgId).executeAsOneOrNull()
        return entity?.let {
            MessageWrapper(
                messageDto = entityToMessageDTO(it)
            )
        }
    }

    /**
     * 在本地数据库中插入单条消息
     */
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
                    sequence_id = messageItem.sequenceId
                )
        }
    }
    
    /**
     * 批量插入来自DTO的消息（使用事务优化）
     * 通过减少重复查询和优化事务处理来提高性能
     * 注意：SQLDelight没有原生的批量插入API，所以我们使用事务包装循环插入来模拟批量插入
     */
    fun insertMessages(messageDTOs: List<MessageDTO>) {
        if (messageDTOs.isEmpty()) return
        
        db.transaction {
            messageDTOs.forEach { messageDTO ->
                insertMessageInTransaction(messageDTO)
            }
        }
    }
    
    /**
     * 在事务中插入单条来自DTO的消息
     */
    private fun insertMessageInTransaction(messageDTO: MessageDTO) {
        val localDateTime = kotlinx.datetime.Instant.parse(messageDTO.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        db.messageQueries.insertMessage(
            conversation_id = messageDTO.conversationId ?: 0L,
            from_account_id = messageDTO.fromAccount?.userId ?: 0L,
            content = messageDTO.content ?: "",
            client_msg_id = messageDTO.clientMsgId ?: "",
            client_timestamp = localDateTime,
            type = messageDTO.type,
            status = messageDTO.status,
            sequence_id = messageDTO.sequenceId ?: 0L
        )
    }
    
    /**
     * 插入来自DTO的消息
     */
    fun insertMessage(messageDTO: MessageDTO) {
        db.transaction {
            insertMessageInTransaction(messageDTO)
        }
    }
    
    /**
     * 获取指定会话的最大序列号
     */
    fun getMaxSequenceId(conversationId: Long): Long {

        db.messageQueries
            .selectMaxSequenceIdByConversation(conversationId)
            .executeAsOneOrNull()
            .let {
                return it?.MAX ?: 0L
            }
    }
    
    /**
     * 获取指定会话的所有消息
     */
    fun getMessagesByConversation(conversationId: Long): List<MessageItem> {
        val entities = db.messageQueries.selectMessagesByConversation(conversationId).executeAsList()
        return entities.map { entity ->
            MessageWrapper(
                messageDto = entityToMessageDTO(entity)
            )
        }
    }
    
    /**
     * 获取指定会话的最新消息
     */
    fun getLatestMessage(conversationId: Long): MessageWrapper? {
        val entity = db.messageQueries.selectLatestMessageByConversation(conversationId).executeAsOneOrNull()
        return entity?.let {
            MessageWrapper(
                messageDto = entityToMessageDTO(it)
            )
        }
    }
    
    /**
     * 将数据库实体转换为MessageDTO
     */
    private fun entityToMessageDTO(entity: db.Message): MessageDTO {
        return MessageDTO(
            msgId = entity.msg_id,
            clientMsgId = entity.client_msg_id,
            content = entity.content,
            fromAccount = UserInfo(entity.from_account_id),
            type = entity.type,
            status = entity.status,
            timestamp = entity.client_timestamp.toString(),
            conversationId = entity.conversation_id,
            sequenceId = entity.sequence_id
        )
    }
}