package com.github.im.group.bot

import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationStatus
import com.github.im.group.api.ConversationType
import com.github.im.group.model.UserInfo

const val AI_ASSISTANT_CONVERSATION_ID: Long = -20_260_720L
const val AI_ASSISTANT_USER_ID: Long = -20_260_721L
const val AI_ASSISTANT_NAME: String = "AI 助手"
const val AI_ASSISTANT_EMAIL: String = "ai-assistant@local.group"
const val AI_ASSISTANT_SUBTITLE: String = "支持问答、摘要、用户信息查询"

fun aiAssistantUser(): UserInfo = UserInfo(
    userId = AI_ASSISTANT_USER_ID,
    username = AI_ASSISTANT_NAME,
    email = AI_ASSISTANT_EMAIL
)

fun buildAiAssistantConversation(currentUser: UserInfo?): ConversationRes {
    val members = buildList {
        currentUser?.let(::add)
        add(aiAssistantUser())
    }
    return ConversationRes(
        conversationId = AI_ASSISTANT_CONVERSATION_ID,
        createdBy = aiAssistantUser(),
        createUserId = AI_ASSISTANT_USER_ID,
        createAt = "1970-01-01T00:00:00",
        groupName = AI_ASSISTANT_NAME,
        description = AI_ASSISTANT_SUBTITLE,
        members = members,
        status = ConversationStatus.ACTIVE,
        conversationType = ConversationType.PRIVATE_CHAT
    )
}

fun isAiAssistantConversationId(conversationId: Long): Boolean {
    return conversationId == AI_ASSISTANT_CONVERSATION_ID
}

fun ConversationRes.isAiAssistantConversation(): Boolean {
    return isAiAssistantConversationId(conversationId)
}
