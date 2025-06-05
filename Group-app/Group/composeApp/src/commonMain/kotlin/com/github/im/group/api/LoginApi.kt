package com.github.im.group.api

import ProxyApi
import com.github.im.group.model.UserInfo
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

object LoginApi {
    suspend fun login(username: String, password: String): UserInfo {
        val requestBody = LoginRequest(username, password)
        return ProxyApi.request(
            hmethod = HttpMethod.Post,
            path = "/api/user/login",
            body = requestBody
        )
    }
}

/**
 * 会话 API
 */
object ConversationApi{


    /**
     * 获取用户已激活的会话
     */
    suspend fun getActiveConversationsByUserId(userId:Long ): List<ConversationRes> {
        return ProxyApi.request(
            hmethod = HttpMethod.Get,
            path = "/api/conversation/${userId}"
        )
    }

}

@Serializable
data class ConversationRes(val conversationId: Long,
                           val groupName: String,
                           val description: String?="",
                           val members: List<UserInfo>,
                           val status: ConversationStatus,
                           val type: ConversationType
)
@Serializable
enum class ConversationStatus {
    ACTIVE,
    INACTIVE,
    DELETED

}
@Serializable
enum class ConversationType {
    GROUP,
    PRIVATE_CHAT
}
@Serializable
data class LoginRequest(val username: String,
                        val password: String,
                        val refreshToken: String?="",
)

@Serializable
data class LoginResponse(val token: String, val userId: Long)
