package com.github.im.group.api

import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AiBotReplyDto(
    val content: String = "",
    val messageType: String = "text",
    val metadata: JsonElement? = null
)

object AiBotApi {
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
}
