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

data class ConversationUiPreference(
    val conversationId: Long,
    val isPinned: Boolean = false,
    val pinRank: Long = 0L,
    val lastActiveAt: Long = 0L
)

data class ChatScrollPositionRecord(
    val conversationId: Long,
    val anchorMsgId: Long? = null,
    val anchorSeqId: Long = 0L,
    val anchorClientMsgId: String? = null,
    val scrollOffset: Int = 0,
    val updatedAt: Long = 0L
)

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

                db.conversationUiPreferenceQueries.insertConversationUiPreferenceIfMissing(conversation.conversationId)
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

    fun getConversationUiPreferences(): Map<Long, ConversationUiPreference> {
        return try {
            db.conversationUiPreferenceQueries
                .selectAllConversationUiPreferences()
                .executeAsList()
                .associate { row ->
                    row.conversation_id to ConversationUiPreference(
                        conversationId = row.conversation_id,
                        isPinned = row.is_pinned != 0L,
                        pinRank = row.pin_rank,
                        lastActiveAt = row.last_active_at
                    )
                }
        } catch (e: Exception) {
            Napier.e("Failed to load conversation ui preferences", e)
            emptyMap()
        }
    }

    fun getConversationUiPreference(conversationId: Long): ConversationUiPreference? {
        return try {
            db.conversationUiPreferenceQueries.insertConversationUiPreferenceIfMissing(conversationId)
            db.conversationUiPreferenceQueries
                .selectConversationUiPreferenceByConversation(conversationId)
                .executeAsOneOrNull()
                ?.let { row ->
                    ConversationUiPreference(
                        conversationId = row.conversation_id,
                        isPinned = row.is_pinned != 0L,
                        pinRank = row.pin_rank,
                        lastActiveAt = row.last_active_at
                    )
                }
        } catch (e: Exception) {
            Napier.e("Failed to load conversation ui preference: $conversationId", e)
            null
        }
    }

    fun markConversationActive(conversationId: Long, lastActiveAt: Long = Clock.System.now().toEpochMilliseconds()) {
        try {
            db.conversationUiPreferenceQueries.insertConversationUiPreferenceIfMissing(conversationId)
            db.conversationUiPreferenceQueries.updateConversationLastActiveAt(lastActiveAt, conversationId)
        } catch (e: Exception) {
            Napier.e("Failed to mark conversation active: $conversationId", e)
        }
    }

    fun pinConversation(conversationId: Long) {
        try {
            db.conversationUiPreferenceQueries.insertConversationUiPreferenceIfMissing(conversationId)
            val nextRank = (db.conversationUiPreferenceQueries.selectMaxPinnedConversationRank().executeAsOneOrNull()?.MAX
                ?: 0L) + 1L
            db.conversationUiPreferenceQueries.updateConversationPinState(
                is_pinned = 1L,
                pin_rank = nextRank,
                conversation_id = conversationId
            )
        } catch (e: Exception) {
            Napier.e("Failed to pin conversation: $conversationId", e)
        }
    }

    fun unpinConversation(conversationId: Long) {
        try {
            db.conversationUiPreferenceQueries.insertConversationUiPreferenceIfMissing(conversationId)
            db.conversationUiPreferenceQueries.updateConversationPinState(
                is_pinned = 0L,
                pin_rank = 0L,
                conversation_id = conversationId
            )
        } catch (e: Exception) {
            Napier.e("Failed to unpin conversation: $conversationId", e)
        }
    }

    fun saveChatScrollPosition(
        conversationId: Long,
        anchorMsgId: Long?,
        anchorSeqId: Long,
        anchorClientMsgId: String?,
        scrollOffset: Int
    ) {
        try {
            db.chatScrollPositionQueries.upsertChatScrollPosition(
                conversation_id = conversationId,
                anchor_msg_id = anchorMsgId,
                anchor_seq_id = anchorSeqId.takeIf { it > 0L },
                anchor_client_msg_id = anchorClientMsgId,
                scroll_offset = scrollOffset.toLong(),
                updated_at = Clock.System.now().toEpochMilliseconds()
            )
        } catch (e: Exception) {
            Napier.e("Failed to save chat scroll position: $conversationId", e)
        }
    }

    fun getChatScrollPosition(conversationId: Long): ChatScrollPositionRecord? {
        return try {
            db.chatScrollPositionQueries
                .selectChatScrollPositionByConversation(conversationId)
                .executeAsOneOrNull()
                ?.let { row ->
                    ChatScrollPositionRecord(
                        conversationId = row.conversation_id,
                        anchorMsgId = row.anchor_msg_id,
                        anchorSeqId = row.anchor_seq_id ?: 0L,
                        anchorClientMsgId = row.anchor_client_msg_id,
                        scrollOffset = row.scroll_offset.toInt(),
                        updatedAt = row.updated_at
                    )
                }
        } catch (e: Exception) {
            Napier.e("Failed to load chat scroll position: $conversationId", e)
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
