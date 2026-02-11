package com.github.im.group.service

import com.github.im.group.api.ConversationApi
import com.github.im.group.model.SessionCreationResult
import com.github.im.group.repository.ConversationRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 会话预创建服务
 * 负责在用户导航到聊天室之前预先创建或获取会话
 */
interface SessionPreCreationService {
    /**
     * 确保与指定用户的会话存在
     * @param currentUserId 当前用户ID
     * @param friendId 好友用户ID
     * @return 会话创建结果，包含conversationId或错误信息
     */
    suspend fun ensureSessionExists(currentUserId: Long, friendId: Long): SessionCreationResult
}

/**
 * 会话预创建服务实现
 */
class SessionPreCreationServiceImpl(
    private val conversationRepository: ConversationRepository,
) : SessionPreCreationService {

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    override suspend fun ensureSessionExists(currentUserId: Long, friendId: Long): SessionCreationResult = 
        withContext(dispatcher) {
        return@withContext try {
            Napier.d("开始预创建会话: currentUserId=$currentUserId, friendId=$friendId")
            
            // 1. 检查本地是否已存在会话
            val existingConversation = conversationRepository.getLocalConversationByMembers(currentUserId, friendId)
            
            if (existingConversation != null) {
                Napier.d("本地已存在会话: conversationId=${existingConversation.conversationId}")
                // 本地已存在会话，直接返回
                return@withContext SessionCreationResult.Success(existingConversation.conversationId)
            }
            
            Napier.d("本地不存在会话，从远程创建")
            // 2. 本地不存在，从远程创建新的私聊会话
            val newConversation = ConversationApi.createOrGetConversation(friendId)
            
            // 3. 保存到本地数据库
            conversationRepository.saveConversation(newConversation)
            
            Napier.d("成功创建会话: conversationId=${newConversation.conversationId}")
            SessionCreationResult.Success(newConversation.conversationId)
            
        } catch (e: Exception) {
            Napier.e("预创建会话失败", e)
            SessionCreationResult.Error(
                message = "Failed to create session: ${e.message}",
                exception = e
            )
        }
    }
}