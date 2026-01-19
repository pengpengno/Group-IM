package com.github.im.group.repository

import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationStatus
import com.github.im.group.db.AppDatabase
import com.github.im.group.model.UserInfo
import io.github.aakira.napier.Napier
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class ConversationRepository(
    private val db: AppDatabase,
    private val userRepository: UserRepository
) {
    
    /**
     * 根据会话ID获取会话信息（始终从远程获取最新信息并更新本地存储）
     * 包括会话基本信息和成员信息
     * @param conversationId 会话ID
     * @return 会话信息
     */
    suspend fun getConversation(conversationId: Long): ConversationRes {
        Napier.d("开始获取会话信息: conversationId=$conversationId")
        
        // 总是从远程获取最新信息
        Napier.d("从远程获取会话信息: conversationId=$conversationId")
        val remoteConversation = ConversationApi.getConversation(conversationId)
        
        Napier.d("从远程API获取到会话信息: conversationId=$conversationId, members count: ${remoteConversation.members.size}")
        
        // 保存到本地数据库（更新本地缓存）
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
                Napier.d("从本地数据库获取会话信息: conversationId=$conversationId")
                
                // 获取会话成员信息
                val membersResult = db.conversationMemberQueries.selectUsersByConversation(conversationId).executeAsList()
                Napier.d("获取到 ${membersResult.size} 个会话成员")
                
                val members = membersResult.map { member ->
                    UserInfo(
                        userId = member.u_userId,
                        username = member.u_username ?: "",
                        email = member.u_email ?: "",
                        token = "", // 从本地存储获取token不太合适
                        refreshToken = "",
                        phoneNumber = null,
                        companyId = null,
                        currentLoginCompanyId = null
                    )
                }

                ConversationRes(
                    conversationId = entity.conversationId,
                    groupName = entity.groupName ?: "",
                    description = entity.description,
                    // 使用成员中的创建者信息，如果没有则使用createUserId
                    createdBy = members.find { it.userId == entity.createdBy } ?: UserInfo(userId = entity.createdBy),
                    createUserId = entity.createdBy,
                    createAt = entity.createdAt.toString(),
                    status = ConversationStatus.valueOf(entity.status.name),
                    type = when (members.size) {
                        2 -> com.github.im.group.api.ConversationType.PRIVATE_CHAT // 私聊会话通常只有2个人
                        else -> com.github.im.group.api.ConversationType.GROUP // 群聊会话通常超过2人
                    },
                    lastMessage = "", // 本地数据库暂时没有存储最后消息
                    members = members // 添加成员信息
                )
            } else {
                Napier.d("本地数据库未找到会话信息: conversationId=$conversationId")
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
            Napier.d("开始保存会话信息到本地数据库: conversationId=${conversation.conversationId}, members count: ${conversation.members.size}")
            
            db.transaction {
                // 保存会话基本信息
                db.conversationQueries.insertConversation(
                    conversationId = conversation.conversationId,
                    groupName = conversation.groupName,
                    description = conversation.description,
                    createdBy = conversation.createUserId,
                    createdAt = if (conversation.createAt.isNotEmpty()) {
                        try {
                            kotlinx.datetime.LocalDateTime.parse(conversation.createAt)
                        } catch (e: Exception) {
                            Napier.w("解析会话创建时间失败: ${e.message}", e)
                            kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        }
                    } else {
                        kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    },
                    status = com.github.im.group.db.entities.ConversationStatus.valueOf(conversation.status.name)
                )

                // 删除现有的成员信息  人员变动 先删除本地 远程为准
                db.conversationMemberQueries.deleteByConversation(conversation.conversationId)
                Napier.d("已删除会话 $conversation.conversationId 的旧成员信息")

                // 保存成员信息
                conversation.members.forEachIndexed { index, userInfo ->
                    Napier.d("正在保存会话成员 $index: userId=${userInfo.userId}, username=${userInfo.username}")
                    
                    // 首先保存用户信息
                    userRepository.addOrUpdateUser(userInfo)

                    // 然后保存会话成员关系
                    db.conversationMemberQueries.insertMember(
                        conversationId = conversation.conversationId,
                        userId = userInfo.userId,
                        joinedAt = if (conversation.createAt.isNotEmpty()) {
                            try {
                                kotlinx.datetime.LocalDateTime.parse(conversation.createAt).toInstant(kotlinx.datetime.TimeZone.currentSystemDefault()).toEpochMilliseconds()
                            } catch (e: Exception) {
                                Napier.w("解析会话创建时间失败，使用当前时间: ${e.message}", e)
                                kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            }
                        } else {
                            kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        }, // 使用会话创建时间或当前时间作为加入时间
                        leftAt = null // 初始时未离开
                    )
                }
            }
            
            Napier.d("成功保存会话信息到本地数据库: conversationId=${conversation.conversationId}")
        } catch (e: Exception) {
            Napier.e("保存会话信息到本地数据库失败", e)
            throw e
        }
    }
}