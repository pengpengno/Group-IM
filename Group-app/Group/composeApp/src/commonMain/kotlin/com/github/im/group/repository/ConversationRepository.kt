package com.github.im.group.repository

import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationStatus
import com.github.im.group.db.AppDatabase
import io.github.aakira.napier.Napier
import kotlinx.datetime.toLocalDateTime

class ConversationRepository(
    private val db: AppDatabase
) {
    
    /**
     * 根据会话ID获取会话信息（本地优先策略）
     * @param conversationId 会话ID
     * @return 会话信息
     */
    suspend fun getConversation(conversationId: Long): ConversationRes {
        // 首先尝试从本地数据库获取
        val localConversation = getLocalConversation(conversationId)
        if (localConversation != null) {
            Napier.d("从本地数据库获取到会话信息: conversationId=$conversationId")
            return localConversation
        }
        
        // 如果本地没有，则从远程获取
        Napier.d("本地数据库未找到会话信息，从远程获取: conversationId=$conversationId")
        val remoteConversation = ConversationApi.getConversation(conversationId)
        
        // 保存到本地数据库
        saveConversation(remoteConversation)
        
        return remoteConversation
    }
    
    /**
     * 从本地数据库获取会话信息
     * @param conversationId 会话ID
     * @return 会话信息，如果不存在则返回null
     */
    private fun getLocalConversation(conversationId: Long): ConversationRes? {
        return try {
            val entity = db.conversationQueries.selectConversationById(conversationId).executeAsOneOrNull()
            if (entity != null) {
                ConversationRes(
                    conversationId = entity.conversationId,
                    groupName = entity.groupName ?: "",
                    description = entity.description,
//                    createdBy = ,
                    createUserId = entity.createdBy,
                    createAt = entity.createdAt.toString(),
                    status = ConversationStatus.valueOf(entity.status.name)

                )
            } else {
                null
            }
        } catch (e: Exception) {
            Napier.e("从本地数据库获取会话信息失败", e)
            null
        }
    }
    
    /**
     * 保存会话信息到本地数据库
     * @param conversation 会话信息
     */
    private fun saveConversation(conversation: ConversationRes) {
        try {
            db.conversationQueries.insertConversation(
                conversationId = conversation.conversationId,
                groupName = conversation.groupName,
                description = conversation.description,
//                createdBy = conversation.createdBy,
                createdBy = conversation.createUserId,
                createdAt = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                status = com.github.im.group.db.entities.ConversationStatus.valueOf(conversation.status.name)
            )
        } catch (e: Exception) {
            Napier.e("保存会话信息到本地数据库失败", e)
        }
    }
}