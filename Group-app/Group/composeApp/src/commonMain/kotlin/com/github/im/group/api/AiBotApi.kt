package com.github.im.group.api

import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AutomationApprovalDecisionRequest(val approved: Boolean)

@Serializable
data class AutomationApprovalResponse(
    val approvalId: String,
    val executionId: String,
    val status: String,
    val actionable: Boolean
)

@Serializable data class CreateReplyRuleRequest(val conversationId: Long, val contains: String = "", val replyText: String)
@Serializable data class AutomationRuleDto(val ruleId: String, val conversationId: String, val enabled: Boolean, val configuration: String)
@Serializable data class AutomationExecutionDto(val executionId: String, val summary: String, val status: String, val resultSummary: String? = null)
@Serializable data class RuleEnabledRequest(val enabled: Boolean)
@Serializable data class ConversationBotConfigDto(val conversationId: String, val enabled: Boolean, val promptTemplate: String)
@Serializable data class ConversationMemberRoleDto(val userId: String, val role: String)

@Serializable
data class AiBotReplyDto(
    val content: String = "",
    val messageType: String = "text",
    val metadata: JsonElement? = null
)

object AiBotApi {
    suspend fun getConversation(): ConversationRes = ProxyApi.request<Unit, ConversationRes>(
        hmethod = HttpMethod.Post,
        path = "/api/ai-bot/conversation"
    )
    suspend fun sendMessage(
        content: String,
        conversationId: Long,
        fromAccountId: Long,
        clientMsgId: String
    ): AiBotReplyDto {
        return ProxyApi.request<MessageDTO, AiBotReplyDto>(
            hmethod = HttpMethod.Post,
            path = "/api/ai-bot/message",
            body = MessageDTO(
                conversationId = conversationId,
                content = content,
                fromAccountId = fromAccountId,
                clientMsgId = clientMsgId,
                type = MessageType.TEXT,
                status = MessageStatus.SENT,
                timestamp = kotlinx.datetime.Clock.System.now().toString()
            )
        )
    }

    suspend fun decideApproval(approvalId: String, approved: Boolean): AutomationApprovalResponse {
        return ProxyApi.request<AutomationApprovalDecisionRequest, AutomationApprovalResponse>(
            hmethod = HttpMethod.Post,
            path = "/api/automation/approvals/$approvalId/decision",
            body = AutomationApprovalDecisionRequest(approved)
        )
    }

    suspend fun listRules(): List<AutomationRuleDto> = ProxyApi.request<Unit, List<AutomationRuleDto>>(hmethod = HttpMethod.Get, path = "/api/automation/rules")
    suspend fun createReplyRule(conversationId: Long, contains: String, replyText: String): AutomationRuleDto = ProxyApi.request(
        hmethod = HttpMethod.Post, path = "/api/automation/rules", body = CreateReplyRuleRequest(conversationId, contains, replyText))
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): AutomationRuleDto = ProxyApi.request(
        hmethod = HttpMethod.Post, path = "/api/automation/rules/$ruleId/enable", body = RuleEnabledRequest(enabled))
    suspend fun listExecutions(): List<AutomationExecutionDto> = ProxyApi.request<Unit, List<AutomationExecutionDto>>(hmethod = HttpMethod.Get, path = "/api/automation/executions")
    suspend fun getGroupBotConfig(conversationId: Long): ConversationBotConfigDto = ProxyApi.request<Unit, ConversationBotConfigDto>(hmethod = HttpMethod.Get, path = "/api/bots/conversations/$conversationId/config")
    suspend fun listGroupRoles(conversationId: Long): List<ConversationMemberRoleDto> = ProxyApi.request<Unit, List<ConversationMemberRoleDto>>(hmethod = HttpMethod.Get, path = "/api/groups/$conversationId/members/roles")
}
