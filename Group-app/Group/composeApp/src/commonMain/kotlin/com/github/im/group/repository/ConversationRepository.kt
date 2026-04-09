package com.github.im.group.repository

import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.db.AppDatabase
import com.github.im.group.model.UserInfo
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class ConversationRepository(
    private val db: AppDatabase,
    private val userRepository: UserRepository
) {

    suspend fun getConversation(conversationId: Long): ConversationRes {
        Napier.d("Loading conversation: $conversationId")
        return try {
            val remoteConversation = ConversationApi.getConversation(conversationId)
            saveConversation(remoteConversation)
            remoteConversation
        } catch (e: Exception) {
            Napier.w("Remote conversation load failed, falling back to local cache: ${e.message}")
            getLocalConversation(conversationId) ?: throw e
        }
    }

    fun getLocalConversation(conversationId: Long): ConversationRes? {
        return try {
            val entity = db.conversationQueries.selectConversationById(conversationId).executeAsOneOrNull()
                ?: return null

            val members = db.conversationMemberQueries
                .selectUsersByConversation(conversationId)
                .executeAsList()
                .map { member ->
                    UserInfo(
                        userId = member.u_userId,
                        username = member.u_username,
                        email = member.u_email ?: "",
                        token = "",
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
                createdBy = members.firstOrNull { it.userId == entity.createdBy }
                    ?: UserInfo(entity.createdBy, "", "", "", "", null, null, null),
                createUserId = entity.createdBy,
                createAt = entity.createdAt.toString(),
                status = entity.status,
                conversationType = entity.conversationType,
                members = members
            )
        } catch (e: Exception) {
            Napier.e("Failed to load local conversation: $conversationId", e)
            null
        }
    }

    fun saveConversation(conversation: ConversationRes) {
        try {
            db.transaction {
                val createdAt = parseConversationTime(conversation.createAt)
                db.conversationQueries.insertConversation(
                    conversationId = conversation.conversationId,
                    groupName = conversation.groupName,
                    description = conversation.description,
                    createdBy = conversation.createUserId,
                    createdAt = createdAt,
                    status = conversation.status,
                    conversationType = conversation.conversationType
                )

                db.conversationMemberQueries.deleteByConversation(conversation.conversationId)

                conversation.members.forEach { userInfo ->
                    userRepository.addOrUpdateUser(userInfo)
                    db.conversationMemberQueries.insertMember(
                        conversationId = conversation.conversationId,
                        userId = userInfo.userId,
                        joinedAt = createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                        leftAt = null
                    )
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to save conversation locally: ${conversation.conversationId}", e)
            throw e
        }
    }

    fun getConversationsByUserId(userId: Long): List<ConversationRes> {
        return try {
            db.conversationMemberQueries
                .selectConversationsByUser(userId)
                .executeAsList()
                .mapNotNull { getLocalConversation(it.c_conversationId) }
        } catch (e: Exception) {
            Napier.e("Failed to load local conversations for user: $userId", e)
            emptyList()
        }
    }

    fun getLocalConversationByMembers(userId: Long, friendId: Long): ConversationRes? {
        return try {
            val conversationId = db.conversationMemberQueries
                .selectConversationByTwoUsers(userId, friendId)
                .executeAsList()
                .firstOrNull()
                ?: return null
            getLocalConversation(conversationId)
        } catch (e: Exception) {
            Napier.e("Failed to load private conversation by members: $userId/$friendId", e)
            null
        }
    }

    private fun parseConversationTime(value: String) =
        if (value.isNotBlank()) {
            runCatching { kotlinx.datetime.LocalDateTime.parse(value) }
                .getOrElse {
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
        } else {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
}
