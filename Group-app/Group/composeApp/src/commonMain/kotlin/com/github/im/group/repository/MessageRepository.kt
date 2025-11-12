package com.github.im.group.repository

import com.github.im.group.api.MessageDTO
import com.github.im.group.db.AppDatabase
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import db.Message
import kotlinx.datetime.Instant
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
    fun insertOrUpdateMessage(messageItem: MessageItem){
        // 检查是否已经存在相同 clientMsgId 的消息

        val existingMessage = messageItem.id.let {
            if (it <= 0L){
                // 如果id 不存在说明是 本地生成的消息  还没在服务端ACK 的消息/  推送的消息
                db.messageQueries.selectMessageByClientMsgId(messageItem.clientMsgId).executeAsOneOrNull()
            }else{
                // 如果id 存在说明是 服务端推送的消息
                db.messageQueries.selectMessageByMsgId(it).executeAsOneOrNull()
            }
        }
        
        if (existingMessage != null) {
            // 如果消息已存在，则更新它
            db.messageQueries.updateMessageByClientMsgId(
                status = messageItem.status,
                server_timestamp = messageItem.time,
                sequence_id = messageItem.seqId,
                client_msg_id = messageItem.clientMsgId,
                msg_id = messageItem.id
            )
        } else {
            // 如果消息不存在，则插入新记录
            db.transaction {
                val clientTime = messageItem.clientTime?.let {
                    Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
                }
                db.messageQueries
                    .insertMessage(
                        conversation_id = messageItem.conversationId,
                        from_account_id = messageItem.userInfo?.userId ?: 0L,
                        content = messageItem.content,
                        client_msg_id = messageItem.clientMsgId,
                        client_timestamp = clientTime,
                        type = messageItem.type,
                        status =messageItem.status,
                        sequence_id = messageItem.seqId
                    )
            }
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
                insertOrUpdateMessage(MessageWrapper(messageDto = messageDTO))
            }
        }
    }
    

    
    /**
     * 根据 msgId 更新消息
     */
    private fun updateMessageByMsgId(messageDTO: MessageDTO) {
        val localDateTime = kotlinx.datetime.Instant.parse(messageDTO.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

        messageDTO.msgId?.let { msgId ->
            // 如果消息已存在，则更新它
            db.messageQueries.updateMessageByMsgId(
                status = messageDTO.status,
                server_timestamp = localDateTime,
                sequence_id = messageDTO.sequenceId ?: 0L,
                msg_id = msgId
            )
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
     * 获取指定会话中指定序列号之前的消息
     * @param conversationId 会话ID
     * @param beforeSequenceId 在此序列号之前的消息
     * @param limit 限制返回的消息数量
     */
    fun getMessagesBeforeSequence(conversationId: Long, beforeSequenceId: Long, limit: Long = 20): List<MessageItem> {
        val entities = db.messageQueries.selectMessagesBeforeSequence(conversationId, beforeSequenceId,
            limit
        ).executeAsList()
        return entities.map { entity ->
            MessageWrapper(
                messageDto = entityToMessageDTO(entity)
            )
        }
    }
    
    /**
     * 获取指定会话中指定序列号之后的消息
     * @param conversationId 会话ID
     * @param afterSequenceId 在此序列号之后的消息
     */
    fun getMessagesAfterSequence(conversationId: Long, afterSequenceId: Long, limit: Long = 20): List<MessageItem> {
        val entities = db.messageQueries.selectMessagesAfterSequence(conversationId, afterSequenceId,limit).executeAsList()
        return entities.map { entity ->
            MessageWrapper(
                messageDto = entityToMessageDTO(entity)
            )
        }
    }


    /**
     * 将DTO转换为数据库实体
     */
    private fun dtoToExistMessage(dto: MessageDTO, message: Message ): Message {
        val localDateTime = kotlinx.datetime.Instant.parse(dto.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        return  message.copy(
            msg_id = dto.msgId,
            client_msg_id = dto.clientMsgId,
            conversation_id = dto.conversationId ?: 0L,
            from_account_id = dto.fromAccount?.userId ?: 0L,
            content = dto.content ?: "",
            type = dto.type,
            status = dto.status,
            server_timestamp = localDateTime,
            sequence_id = dto.sequenceId
        )
    }
    /**
     * 将数据库实体转换为MessageDTO
     */
    private fun entityToMessageDTO(entity: Message): MessageDTO {
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