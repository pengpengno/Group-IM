package com.github.im.group.repository

import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.FileApi
import com.github.im.group.api.MessageDTO
import com.github.im.group.api.extraAs
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.sdk.FileStorageManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * 消息同步仓库，负责处理本地和远程消息的同步
 */
class MessageSyncRepository(
    private val messageRepository: ChatMessageRepository,
    private val filesRepository: FilesRepository,
    private val fileStorageManager: FileStorageManager
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    /**
     * 同步指定会话的消息
     * @param conversationId 会话ID
     * @return 同步到的新消息数量
     */
    suspend fun syncMessages(conversationId: Long): Int {
        Napier.d("开始同步会话消息: conversationId=$conversationId")
        
        try {
            // 获取会话信息，包括索引信息
            val conversation = ConversationApi.getConversation(conversationId)
            Napier.d("获取会话信息: $conversation")
            
            // 获取本地最大序列号
            val localMaxSequenceId = messageRepository.getMaxSequenceId(conversationId)
            Napier.d("本地最大序列号: $localMaxSequenceId")
            
            // 从服务器获取消息，只获取比本地序列号大的消息
            val pageResult = ChatApi.getMessages(conversationId, localMaxSequenceId)
            val remoteMessages = pageResult.content
            Napier.d("从服务器获取到 ${remoteMessages.size} 条新消息")
            
            if (remoteMessages.isNotEmpty()) {
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
     * 获取指定会话的消息（本地优先）
     * @param conversationId 会话ID
     * @return 消息列表
     */
    suspend fun getMessages(conversationId: Long): List<MessageItem> {
        Napier.d("获取会话消息: conversationId=$conversationId")
        
        try {
            // 从本地获取所有消息
            val localMessages = messageRepository.getMessagesByConversation(conversationId)
            Napier.d("从本地获取到 ${localMessages.size} 条消息")
            
            // 对于文件类型消息，确保文件已下载
            localMessages.forEach { message ->
                if (message.type.isFile()) {
                    message.fileMeta?.let { fileMeta ->
                        ensureFileDownloaded(fileMeta.hash)
                    }
                }
            }
            
            return localMessages
        } catch (e: Exception) {
            Napier.e("获取消息时发生错误", e)
            return emptyList()
        }
    }
    
    /**
     * 确保文件已下载到本地
     * @param fileId 文件ID
     */
    private suspend fun ensureFileDownloaded(fileId: String) {
        try {
            // 检查文件是否已经存储在本地
            if (!filesRepository.isFileStoredLocally(fileId)) {
                Napier.d("文件 $fileId 尚未存储在本地，开始下载")
                try {
                    // 从服务器下载文件
                    val fileContent = FileApi.downloadFile(fileId)
                    
                    // 保存文件到本地
                    syncScope.launch {
                        fileStorageManager.getFileContent(fileId)
                    }
                    
                    Napier.d("文件 $fileId 下载完成")
                } catch (e: Exception) {
                    Napier.e("下载文件失败: $fileId", e)
                }
            } else {
                Napier.d("文件 $fileId 已存在于本地")
            }
        } catch (e: Exception) {
            Napier.e("检查文件状态时发生错误: $fileId", e)
        }
    }
}