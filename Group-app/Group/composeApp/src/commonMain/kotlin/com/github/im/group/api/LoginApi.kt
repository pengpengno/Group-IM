package com.github.im.group.api

import ProxyApi
import com.github.im.group.model.UserInfo
import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable


object LoginApi {
    suspend fun login(username: String, password: String,  refreshToken: String): UserInfo {
        val requestBody = LoginRequest(username, password,refreshToken)
        return ProxyApi.request(
            hmethod = HttpMethod.Post,
            path = "/api/users/login",
            body = requestBody,
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
        return ProxyApi.request<Unit, List<ConversationRes>>(
            hmethod = HttpMethod.Get,
            path = "/api/conversations/${userId}/active"
        )
    }

    /**
     * 创建会话
     */
    suspend fun createConversation(userId:Long ,friendId:Long): List<ConversationRes> {
        return ProxyApi.request<Unit , List<ConversationRes> >(
            hmethod = HttpMethod.Get,
            path = "/api/conversations/private-chat",
            requestParams = mapOf("userId" to userId.toString(),"friendId" to friendId.toString())
        )
    }

    /**
     * 创建会话
     */
    suspend fun createGroupConversation(groupInfo:GroupInfo ): ConversationRes {
        return ProxyApi.request< GroupInfo,ConversationRes>(
            hmethod = HttpMethod.Get,
            path = "/api/conversations/group",
            body = groupInfo
        )
    }

}

/**
 * 好友 API
 */

object FriendShipApi {

    /**
     * 查询用户连卸任
     */
    suspend fun getFriends(userId:Long) : List<FriendshipDTO>{

        return ProxyApi.request< Unit,List<FriendshipDTO>>(
            hmethod = HttpMethod.Get,
            path = "/api/friendships/list",
            requestParams = mapOf("userId" to userId.toString())
        )
    }
}
@Serializable
data class FriendshipDTO(
    val id: Long? = null,
    val userInfo: UserInfo? = null,
    val friendUserInfo: UserInfo? = null
)

@Serializable
data class GroupInfo(
    val groupName: String? = null,
    val description: String? = null,
    val members: List<UserInfo>? = listOf(),
)

@Serializable
data class ConversationRes(
    val conversationId: Long = -1,
    val groupName: String = "",
    val description: String? = "",
    val members: List<UserInfo> = emptyList(),
    val status: ConversationStatus = ConversationStatus.ACTIVE, // 或者默认值
    val type: ConversationType = ConversationType.PRIVATE_CHAT, // 或者默认值
    val lastMessage: String = "",
) {
    fun getName(): String {
        return when (type) {
            ConversationType.GROUP -> groupName
            ConversationType.PRIVATE_CHAT -> members.firstOrNull()?.username ?: "未知用户"
        }
    }

    companion object {
        fun empty(): ConversationRes = ConversationRes()
    }
}

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
data class LoginRequest(
    val loginAccount: String,
    val password: String,
    val refreshToken: String? = "",
)

@Serializable
data class LoginResponse(val token: String, val userId: Long)
