package com.github.im.group.repository

import com.github.im.group.api.ChatApi
import com.github.im.group.api.MessageDTO
import com.github.im.group.api.extraAs
import com.github.im.group.db.AppDatabase
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import db.Message
import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 聊天消息本地存储
 */
class ChatMessageRepository (
    private val db: AppDatabase,
    private val userRepository: UserRepository
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
     *
     * 根据 clientMsgId 来查询
     */
    fun insertOrUpdateMessage(messageItem: MessageItem){

        // 检查是否已经存在相同 clientMsgId 的消息
        val existingMessage = messageItem.id.let {
            if (it <= 0L){
                // 如果id 不存在说明是 本地生成的消息  还没在服务端ACK 的消息/  推送的消息
                db.messageQueries.selectMessageByClientMsgId(messageItem.clientMsgId).executeAsOneOrNull()
            }else{
                // 如果id 存在说明是 服务端推送的消息 先在本地根据 clientID 查不到则说明不是本地更新的返回消息 再 通过 msg_id 查询
                db.messageQueries.selectMessageByClientMsgId(messageItem.clientMsgId).executeAsOneOrNull()
                    .let { message ->
                        if (message == null){
                            return@let db.messageQueries.selectMessageByMsgId(it).executeAsOneOrNull()
                        }
                        return@let  message
                    }

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
                messageItem.id.let {
                    if (it <= 0L){
                        // 如果id 不存在说明是 本地生成的消息  还没在服务端ACK 的消息/  推送的消息
                        db.messageQueries.insertMessage(
                            conversation_id = messageItem.conversationId,
                            from_account_id = messageItem.userInfo.userId,
                            content = messageItem.content,
                            client_msg_id = messageItem.clientMsgId,
                            client_timestamp = clientTime,
                            type = messageItem.type,
                            status =messageItem.status,
                            sequence_id = messageItem.seqId
                        )
                    }else{
                        // 如果id 存在说明是 服务端推送的消息
                        db.messageQueries.insertMessageWithMsgId(
                            conversation_id = messageItem.conversationId,
                            from_account_id = messageItem.userInfo.userId,
                            content = messageItem.content,
                            client_msg_id = messageItem.clientMsgId,
                            server_timestamp = messageItem.time,
                            msg_id = messageItem.id,
                            type = messageItem.type,
                            status =messageItem.status,
                            sequence_id = messageItem.seqId
                        )
                    }
                }
            }
            
            // 同时保存用户信息到用户表
            saveUserInfo(messageItem.userInfo)
        }
    }
    
    /**
     * 保存用户信息到本地数据库
     */
    private fun saveUserInfo(userInfo: UserInfo) {
        db.transaction {
            // 先检查用户是否存在
            val existingUser = db.userQueries.selectById(userInfo.userId).executeAsOneOrNull()
            if (existingUser == null) {
                // 用户不存在则插入
                db.userQueries.insertUser(
                    userId = userInfo.userId,
                    username = userInfo.username,
                    email = userInfo.email,
                    phoneNumber = "",
                    avatarUrl = null,
                    bio = null,
                    userStatus = com.github.im.group.db.entities.UserStatus.ACTIVE
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
     * 获取指定会话的 所有消息
     * 根据服务端的创建时间排序 DESC
     * @param conversationId 会话ID
     * @param limit 限制返回的消息数量 默认30
     */
    fun getMessagesByConversation(conversationId: Long, limit: Long = 30): List<MessageItem> {
        // 获取指定会话的所有消息  ORDER  BY  server_time_stamp DESC
        val entities = db.messageQueries.selectMessagesWithUserInfoByConversation(conversationId, limit).executeAsList()
        return entities.map { entity ->
            MessageWrapper(
                messageDto = MessageDTO(
                    msgId = entity.msg_id,
                    conversationId = entity.conversation_id,
                    clientMsgId = entity.client_msg_id,
                    fromAccountId = entity.from_account_id,
                    status = entity.status,
                    content = entity.content,
                    type =  entity.type,
                    timestamp = entity.server_timestamp.toString(),
                    sequenceId = entity.sequence_id,
                    fromAccount = UserInfo(
                        userId = entity.from_account_id,
                        username = entity.username?:"",
                        email = entity.email?:""
                    ),
                )
            )
        }
    }
    
    /**
     * 获取指定会话的最新消息
     * 优先
     */
    suspend fun getLatestMessage(conversationId: Long): MessageWrapper? {

        // 获取本地最新Index

        val localMaxSequenceId = getMaxSequenceId(conversationId)
        Napier.d("本地最大序列号: $localMaxSequenceId")

        // 从服务器获取消息，只获取比本地序列号大的消息
        val pageResult = ChatApi.getMessages(conversationId, localMaxSequenceId)
        val remoteMessages = pageResult.content
        Napier.d("从服务器获取到 ${remoteMessages.size} 条新消息")

        if (remoteMessages.isNotEmpty()) {

            // 先保存用户数据
            val userInfos = remoteMessages.filter {
                it.fromAccount != null && it.fromAccount.username.isNotBlank()
            }.mapNotNull { it ->
                it.fromAccount?.takeIf { it.username.isNotBlank() }
            }
            if (userInfos.isNotEmpty()) {
                userRepository.addOrUpdateUsers(userInfos)
            }
            // 批量保存新消息到本地（使用事务）
            insertMessages(remoteMessages)

            // 批量保存文件元数据
            val fileMetas = remoteMessages
                .filter { it.type.isFile() }
                .mapNotNull { it.extraAs<com.github.im.group.api.FileMeta>() }

            Napier.d("同步完成，新增 ${remoteMessages.size} 条消息")
        }
        // 开始查询 展示 最新消息
        val entity = db.messageQueries.selectLatestMessageWithUserInfoByConversation(conversationId).executeAsOneOrNull()
        return entity?.let {
            MessageWrapper(
                messageDto = MessageDTO(
                    msgId = entity.msg_id,  // 可能为空
                    conversationId = entity.conversation_id,
                    status = entity.status,
                    content = entity.content,
                    type =  entity.type,
                    timestamp = entity.server_timestamp.toString(),  // 可能为空
                    fromAccount = UserInfo(
                        userId = entity.from_account_id,
                        username = entity.username?:"",
                        email = entity.email?:""
                    ),

                )
            )
        }
    }

//    fun updateMsgStatusAndTimeStampByClientMsgId(clientMsgId: String, status: MessageStatus, timestamp: LocalDateTime){
//        db.messageQueries.updateMessageByClientMsgId(status, timestamp, clientMsgId)
//
//    }
    
    /**
     * 获取指定会话中指定序列号之前的消息
     * @param conversationId 会话ID
     * @param beforeSequenceId 在此序列号之前的消息
     * @param limit 限制返回的消息数量
     */
    fun getMessagesBeforeSequence(conversationId: Long, beforeSequenceId: Long, limit: Long = 20): List<MessageItem> {
        val entities = db.messageQueries.selectMessagesWithUserInfoBeforeSequence(conversationId, beforeSequenceId,
            limit
        ).executeAsList()
        return entities.map { entity ->
            MessageWrapper(
                messageDto = MessageDTO(
                    msgId = entity.msg_id,
                    conversationId = entity.conversation_id,
                    status = entity.status,
                    content = entity.content,
                    type =  entity.type,
                    timestamp = entity.server_timestamp.toString(),
                    fromAccount = UserInfo(
                        userId = entity.from_account_id,
                        username = entity.username?:"",
                        email = entity.email?:""
                    ),

                    )
            )
        }
    }
    
    /**
     * 获取指定会话中指定序列号之后的消息
     * @param conversationId 会话ID
     * @param afterSequenceId 在此序列号之后的消息
     */
    fun getMessagesAfterSequence(conversationId: Long, afterSequenceId: Long, limit: Long = 20): List<MessageItem> {
        val entities = db.messageQueries.selectMessagesWithUserInfoAfterSequence(conversationId, afterSequenceId, limit).executeAsList()
        return entities.map { entity ->
            MessageWrapper(
                messageDto = MessageDTO(
                    msgId = entity.msg_id,
                    conversationId = entity.conversation_id,
                    status = entity.status,
                    content = entity.content,
                    type =  entity.type,
                    timestamp = entity.server_timestamp.toString(),
                    fromAccount = UserInfo(
                        userId = entity.from_account_id,
                        username = entity.username?:"",
                        email = entity.email?:""
                    ),

                    )
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
            timestamp = entity.server_timestamp.toString(),
            conversationId = entity.conversation_id,
            sequenceId = entity.sequence_id
        )
    }
}