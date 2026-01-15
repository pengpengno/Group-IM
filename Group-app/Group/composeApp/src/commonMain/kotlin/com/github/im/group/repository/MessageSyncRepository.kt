package com.github.im.group.repository

import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.FileApi
import com.github.im.group.api.MessageDTO
import com.github.im.group.api.extraAs
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.manager.FileStorageManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

/**
 * 消息同步仓库，负责处理本地和远程消息的同步
 */
class MessageSyncRepository(
    private val messageRepository: ChatMessageRepository,
    private val filesRepository: FilesRepository,
    private val fileStorageManager: FileStorageManager,
    private val userRepository: UserRepository
) {

    /**
     * 同步指定会话的新消息
     * 1. 获取本地会话 最大索引 SequenceId
     * 2. 从服务器获取消息，只获取比本地序列号大的消息
     * 3. 插入到本地数据库
     *    3.1 同时保存文件的元数据信息
     * 4. 返回新增的数据量
     * @param conversationId 会话ID
     * @return 同步到的新消息数量
     */
    suspend fun syncMessages(conversationId: Long): Int {
        Napier.d("开始同步会话消息: conversationId=$conversationId")
        
        try {
            // 获取会话信息，包括索引信息
//            val conversation = ConversationApi.getConversation(conversationId)
//            Napier.d("获取会话信息: $conversation")
            
            // 获取本地最大序列号
            val localMaxSequenceId = messageRepository.getMaxSequenceId(conversationId)
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
                messageRepository.insertMessages(remoteMessages)
                
                // 批量保存文件元数据
                val fileMetas = remoteMessages
                    .filter { it.type.isFile() }
                    .mapNotNull { it.extraAs<com.github.im.group.api.FileMeta>() }
                
                if (fileMetas.isNotEmpty()) {
                    filesRepository.addFiles(fileMetas)
                }
                
                Napier.d("同步完成，新增 ${remoteMessages.size} 条消息")
                return remoteMessages.size
            }
            
            Napier.d("同步完成，无新消息")
            return 0
        } catch (e: Exception) {
            Napier.e("同步消息时发生错误", e)
            return 0
        }
    }
    
    /**
     * 通用的消息获取方法，实现本地优先策略
     * @param conversationId 会话ID
     * @param currentIndex 当前索引
     * @param isForward 是否向前获取（历史消息）
     * @return 消息列表
     */
    suspend fun getMessagesWithStrategy(
        conversationId: Long,
        currentIndex: Long,
        isForward: Boolean
    ): List<MessageItem> {
        Napier.d("获取消息: conversationId=$conversationId, currentIndex=$currentIndex, isForward=$isForward")
        
        try {
            val messageItem =  isForward.let {
                if (isForward) {
                    messageRepository.getMessagesBeforeSequence(conversationId, currentIndex)
                } else {
                    //  查询当前索引 之后的数据
                    messageRepository.getMessagesAfterSequence(conversationId, currentIndex)
                }
            }.let {items->
                items.ifEmpty {
                    // 从服务器获取消息
                    val remoteMessages = if (isForward) {
                        ChatApi.getMessages(conversationId, toSequenceId = currentIndex).content
                    } else {
                        ChatApi.getMessages(conversationId, fromSequenceId = currentIndex).content
                    }

                    Napier.d("从服务器获取到 ${remoteMessages.size} 条消息")

                    if (remoteMessages.isNotEmpty()) {
                        // 保存到本地数据库
                        Napier.d ("保存到本地数据库 ${remoteMessages.size}" )
                        messageRepository.insertMessages(remoteMessages)

                        // 保存文件元数据
                        val fileMetas = remoteMessages
                            .filter { it.type.isFile() }
                            .mapNotNull { it.extraAs<com.github.im.group.api.FileMeta>() }

                        if (fileMetas.isNotEmpty()) {
                            filesRepository.addFiles(fileMetas)
                        }

                        Napier.d("从服务器获取并保存了 ${remoteMessages.size} 条消息")
                        return@ifEmpty remoteMessages
                    } else {
                        return@ifEmpty emptyList()
                    }
                }
                return@let items
            }

            return messageItem
        } catch (e: Exception) {
            Napier.e("获取消息时发生错误", e)
            return emptyList()
        }
    }


}