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
     * 如果远程获取失败 那么使用本地以及存储了 的信息
     * 包括会话基本信息和成员信息
     * @param conversationId 会话ID
     * @return 会话信息
     */
    suspend fun getConversation(conversationId: Long): ConversationRes {
        Napier.d("开始获取会话信息: conversationId=$conversationId")
        
        // 首先尝试从远程获取最新信息
        Napier.d("从远程获取会话信息: conversationId=$conversationId")
        try {
            val remoteConversation = ConversationApi.getConversation(conversationId)
            
            Napier.d("从远程API获取到会话信息: conversationId=$conversationId, members count: ${remoteConversation.members.size}")
            
            // 保存到本地数据库（更新本地缓存）
            saveConversation(remoteConversation)
            
            return remoteConversation
        } catch (e: Exception) {
            Napier.w("从远程获取会话信息失败: ${e.message}")
            
            // 远程获取失败，尝试从本地数据库获取
            Napier.d("尝试从本地数据库获取会话信息: conversationId=$conversationId")
            val localConversation = getLocalConversation(conversationId)
            
            if (localConversation != null) {
                Napier.d("从本地数据库获取到会话信息: conversationId=$conversationId, members count: ${localConversation.members.size}")
                return localConversation
            } else {
                // 本地也没有数据，抛出原始异常
                Napier.e("本地数据库也没有会话信息，抛出原始异常: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * 从本地数据库获取会话信息
     * @param conversationId 会话ID
     * @return 会话信息，如果不存在则返回null
     */
    fun getLocalConversation(conversationId: Long): ConversationRes? {
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
                        username = member.u_username,
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
                    createdBy = members.find { it.userId == entity.createdBy }!!,
                    createUserId = entity.createdBy,
                    createAt = entity.createdAt.toString(),
                    status = ConversationStatus.valueOf(entity.status.name),
                    type = when (members.size) {
                        2 -> com.github.im.group.api.ConversationType.PRIVATE_CHAT // 私聊会话通常只有2个人
                        else -> com.github.im.group.api.ConversationType.GROUP // 群聊会话通常超过2人
                    },
//                    lastMessage = "", // 本地数据库暂时没有存储最后消息
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
    fun saveConversation(conversation: ConversationRes) {
        try {
            Napier.d("开始保存会话信息到本地数据库: conversationId=${conversation.conversationId}, members count: ${conversation.members.size}")
            
            db.transaction {
                // 检查会话是否已存在
                val existingConversation = db.conversationQueries.selectConversationById(conversation.conversationId).executeAsOneOrNull()
                
                if (existingConversation != null) {
                    // 会话已存在，先删除再插入更新（因为没有直接的更新查询）
                    db.conversationQueries.deleteConversation(conversation.conversationId)
                    Napier.d("已删除旧会话信息: conversationId=${conversation.conversationId}")
                }
                
                // 插入会话信息（无论是新会话还是更新的会话）
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
                
                if (existingConversation != null) {
                    Napier.d("已更新会话信息: conversationId=${conversation.conversationId}")
                } else {
                    Napier.d("已插入新会话信息: conversationId=${conversation.conversationId}")
                }

                // 删除现有的成员信息，然后重新插入（因为成员可能有变动）

                //TODO  在大群聊模式 这种设计必须要改变， 大群聊的用户数据只保留 部分 其他  的通过  API 来实时 拉取即可
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
    
    /**
     * 根据成员ID获取会话列表（从本地数据库）
     * @param userId 用户ID
     * @return 会话列表
     */
    fun getConversationsByUserId(userId: Long): List<ConversationRes> {
        return try {
            Napier.d("从本地数据库获取用户 $userId 的会话列表")
            
            // 获取用户参与的所有会话信息
            val conversationMembers = db.conversationMemberQueries.selectConversationsByUser(userId).executeAsList()
            Napier.d("用户 $userId 参与了 ${conversationMembers.size} 个会话")
            
            // 获取每个会话的详细信息
            conversationMembers.mapNotNull { conversationMember ->
                getLocalConversation(conversationMember.c_conversationId)
            }.also {
                Napier.d("成功获取 ${it.size} 个会话的详细信息")
            }
        } catch (e: Exception) {
            Napier.e("从本地数据库获取用户会话列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 根据成员ID获取私聊会话（从本地数据库）
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @return 会话信息，如果不存在则返回null
     */
    fun getLocalConversationByMembers(userId: Long, friendId: Long): ConversationRes? {
        return try {
            // 使用单条SQL查询找出同时包含这两个用户的会话
            val conversationIds = db.conversationMemberQueries.selectConversationByTwoUsers(userId, friendId).executeAsList()
            
            // 如果找到了共同会话，则获取会话详情
            if (conversationIds.isNotEmpty()) {
                val conversationId = conversationIds.first()
                return getLocalConversation(conversationId)
            } else {
                null
            }
        } catch (e: Exception) {
            Napier.e("从本地数据库根据成员获取会话失败", e)
            null
        }
    }
}