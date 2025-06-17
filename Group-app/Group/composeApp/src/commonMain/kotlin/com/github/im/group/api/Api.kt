package com.github.im.group.api

import ProxyApi
import com.github.im.group.model.UserInfo
import io.ktor.http.HttpMethod
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
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
     * 获取用户已激活的会话
     */
    suspend fun getConversation(conversationId:Long ): ConversationRes {
        return ProxyApi.request<Unit, ConversationRes>(
            hmethod = HttpMethod.Get,
            path = "/api/conversations/${conversationId}"
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
 * Chat Api
 */
object ChatApi {
    /**
     * 获取会话消息
     */
    suspend fun getMessages(conversationId: Long): PageResult<MessageDTO> {

        val requestBody = MessagePullRequest(
            conversationId = conversationId,
            fromAccountId = null,
            startTime = null,
            endTime = null,
            page = 0,
            size = 50,
            sort = null
        )
        return ProxyApi.request<MessagePullRequest, PageResult<MessageDTO>>(
            hmethod = HttpMethod.Post,
            path = "/api/messages/pull",
            body = requestBody
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
data class MessagePullRequest(
    val conversationId: Long? = null,
    val fromAccountId: Long? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 50,
    val sort: String? = null,
)



/**
 * 消息状态
 */
enum class MessageStatus {
    REJECT,
    OFFLINE,
    SENTFAIL,
    HISTORY,
    READ,
    UNREAD,
    SENT,
    UNSENT;

//    companion object {
//        fun fromCode(code: String): MessageStatus? {
//            return entries.find { it.toString() == code }
//        }
//    }
}

@Serializable
enum class ChatMessageType {
//   文本
    TEXT,
//   文件
    FILE,

}


@Serializable
/**
 * 分页
 */
data class PageResult<T>(
    val content: List<T>,
    val page: PageMeta
) {
    @Serializable
    data class PageMeta(
        val size: Int,
        val number: Int,
        val totalElements: Long,
        val totalPages: Int
    )
}
@Serializable
sealed interface MessagePayLoad

@Serializable
@SerialName("TEXT")
data class DefaultMessagePayLoad(
    val content: String
) : MessagePayLoad

@Serializable
@SerialName("FILE")
data class FileMeta(
    @SerialName("filename")
    val fileName: String,

    @SerialName("fileSize")
    val size: Long,

    val contentType: String,
    val hash: String
) : MessagePayLoad

@Serializable
data class FriendshipDTO(
    val id: Long? = null,
    val userInfo: UserInfo? = null,
    val friendUserInfo: UserInfo? = null
)

@Serializable
data class MessageDTO(
    val msgId: Long? = null,
    val conversationId: Long? = null,
    val content: String? = null,
    val fromAccountId: Long? = null,
    val sequenceId: Long? = null,
    val fromAccount: UserInfo? = null,
    val type: ChatMessageType ,
    val status: MessageStatus ,
    val timestamp: String, // ISO 格式时间
    val payload: MessagePayLoad? = null


){

}


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
    fun getName(currentUser: UserInfo?): String {
        return when (type) {
            ConversationType.GROUP -> groupName
            ConversationType.PRIVATE_CHAT -> {
                if (members.isEmpty()) return "无成员"

                // 返回非当前用户的名称
                val otherUser = members.firstOrNull() { it.userId != (currentUser?.userId ?: "") }
                otherUser?.username ?: ""

            }
        }
    }

//    companion object {
//        fun empty(): ConversationRes = ConversationRes()
//    }
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
