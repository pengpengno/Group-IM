package com.github.im.group.api

import ProxyApi
import ProxyConfig
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.db.entities.FriendRequestStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.ApiResponse
import com.github.im.group.model.DepartmentInfo
import com.github.im.group.model.OrganizationStructure
import com.github.im.group.model.UserInfo
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
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
 * user info api
 *  1. find user by email or account
 *  2. register
 *  3. reset-pwd
 *  4. logout
 */
object UserApi{


    /**
     * find user info by uAccountName  or email
     */
    suspend fun findUser(queryString:String): PageResult<UserInfo> {
        return ProxyApi.request<String, PageResult<UserInfo>>(
            hmethod = HttpMethod.Post,
            path = "/api/users/query",
            requestParams = mapOf("query" to queryString)
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
     * 获取或者创建私聊会话
     */
    suspend fun createOrGetConversation(userId:Long ,friendId:Long): ConversationRes {
        return ProxyApi.request<Unit , ConversationRes>(
            hmethod = HttpMethod.Post,
            path = "/api/conversations/private-chat",
            requestParams = mapOf("userId" to userId.toString(),"" +
                    "friendId" to friendId.toString())
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
 * 文件处理 API
 */
object FileApi {
    /**
     * 上传文件
     * 不
     * @param file 文件
     * @param fileName 文件名
     * @param duration 文件时长 多余 音视频文件，非音视频文件 传 0 即可
     */
    suspend fun uploadFile(file: ByteArray,fileName:String,duration:Long): FileUploadResponse {
        return ProxyApi.uploadFile(file, fileName,duration)
    }

    /**
     * 下载文件
     * @param fileId 文件ID
     */
    suspend fun downloadFile(fileId: String): ByteArray {
        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        }

        val response = ProxyApi.client.prepareGet("$baseUrl/api/files/download/$fileId") {
            headers {
                val token = GlobalCredentialProvider.currentToken
                if (token.isNotEmpty()) {
                    header("Proxy-Authorization", "Basic $token")
                    header("Authorization", "Bearer $token")
                }
            }
        }.execute()
        
        return when (response.status.value) {
            in 200..299 -> {
                response.bodyAsBytes()
            }
            else -> {
                val errorText = response.bodyAsText()
                throw RuntimeException("下载文件失败：${response.status}，内容: $errorText")
            }
        }
    }

    /**
     * 根据文件Id获取文件元数据
     */
    suspend fun getFileMeta(fileId:String) : FileMeta{
        return ProxyApi.request< String,FileMeta>(
            hmethod = HttpMethod.Get,
            path = "/api/files/meta",
            requestParams = mapOf("fileId" to fileId)
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
    suspend fun getMessages(conversationId: Long, fromSequenceId: Long = 0, toSequenceId: Long = 0): PageResult<MessageDTO> {


        val requestBody = MessagePullRequest(
            conversationId = conversationId,
            fromAccountId = null,
            startTime = null,
            endTime = null,
            page = 0,
            size = 50,
            sort = null,
            fromSequenceId = fromSequenceId,
            toSequenceId = toSequenceId
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
     * 查询用户联系人
     */
    suspend fun getFriends(userId:Long) : List<FriendshipDTO>{

        return ProxyApi.request< Unit,List<FriendshipDTO>>(
            hmethod = HttpMethod.Post,
            path = "/api/friendships/list",
            requestParams = mapOf("userId" to userId.toString())
        )
    }

    /**
     * 添加联系人
     */
    suspend fun addFriend(userId:Long,friendId:Long ,applyRemark :String) : FriendshipDTO{
        return ProxyApi.request< FriendRequest,FriendshipDTO>(
            hmethod = HttpMethod.Post,
            path = "/api/friendships/request",
            body =FriendRequest(userId,friendId,applyRemark)
//            requestParams = mapOf("userId" to userId.toString(),"friendId" to friendId.toString())
        )
    }
    
    /**
     * 查询用户的好友请求
     * @param userId 用户ID
     * @return 好友请求列表
     */
    suspend fun getSentFriendRequests(userId: Long): List<FriendshipDTO> {
        return ProxyApi.request<Unit, List<FriendshipDTO>>(
            hmethod = HttpMethod.Post,
            path = "/api/friendships/queryRequest",
            requestParams = mapOf("userId" to userId.toString())
        )
    }
    
    /**
     * 同步好友请求
     * @param userId 用户ID
     * @param maxId 客户端目前最大的关系ID，只获取比这个ID大的数据
     * @return 新的好友请求列表
     */
    suspend fun syncFriendRequests(userId: Long, maxId: Long): List<FriendshipDTO> {
        return ProxyApi.request<Unit, List<FriendshipDTO>>(
            hmethod = HttpMethod.Post,
            path = "/api/friendships/sync",
            requestParams = mapOf(
                "userId" to userId.toString(),
                "maxId" to maxId.toString()
            )
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
    val fromSequenceId: Long? = null,  // 添加从指定sequenceId开始拉取的参数
    val toSequenceId: Long = 0L,  // 添加从指定sequenceId开始拉取的参数
)


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
//sealed interface MessagePayLoad
sealed interface MessagePayLoad
@Serializable
data class FileMetaPayload(
    val fileMeta: FileMeta
) : MessagePayLoad
@Serializable
@SerialName("TEXT")
data class DefaultMessagePayLoad(
    val content: String
) : MessagePayLoad

@Serializable
data class FileUploadResponse(
    val id: String    ,

    val fileMeta: FileMeta ,
)
@Serializable
@SerialName("FILE")
//Kotlinx Serialization 默认会使用 "type" 字段 进行多态
data class FileMeta(
    @SerialName("filename")
    val fileName: String,

    @SerialName("fileSize")
    val size: Long,

    val contentType: String,

    val hash: String,

    val type: String,
    val duration  :Int ?

) : MessagePayLoad

@Serializable
data class FriendshipDTO(
    val id: Long? = null,  // 关系ID
    val userInfo: UserInfo? = null, // 当前用户的用户信息，仅返回用户ID和用户名 /TODO 这个可以省略
    val friendUserInfo: UserInfo? = null, // 好友的用户信息
    val status: FriendRequestStatus? = null,
    val conversationId: Long? = null  //与好友当前存在的会话
)

/**
 * 好友请求
 */
@Serializable
data class FriendRequest  (
     val userId: Long ,
     val friendId: Long,
     val applyRemark: String,
)




@Serializable
data class MessageDTO(
    val msgId: Long? = null,
    val conversationId: Long? = null,
    val content: String? = null,
    val fromAccountId: Long? = null,
    val clientMsgId: String? = null,
    val sequenceId: Long? = null,
    val fromAccount: UserInfo? = null,
    val type: MessageType,
    val status: MessageStatus,
    val timestamp: String,  // 服务端的时间戳

    val payload: MessagePayLoad? = null

){


}
public inline fun <reified T : MessagePayLoad> MessageDTO.extraAs(): T? {
    return payload as? T
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
    val createdBy : UserInfo = UserInfo(),
    val createUserId : Long = -1 ,
    val createAt : String = "",
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

    /**
     * 用户信息
     */
    fun getOtherUser(currentUser: UserInfo?): UserInfo? {
        return when (type) {
            ConversationType.GROUP -> null // 群聊暂时不支持视频通话
            ConversationType.PRIVATE_CHAT -> {
                if (members.isEmpty()) return null
                members.firstOrNull { it.userId != currentUser?.userId }
            }
        }
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
data class LoginResponse(
    val token: String, 
    val userId: Long,
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val currentLoginCompanyId: Long? = null
)

/**
 * 组织架构 API
 */
object OrganizationApi {
    
    /**
     * 获取当前的登录用户公司的组织架构
     */
    suspend fun getOrganizationStructure(): ApiResponse<DepartmentInfo> {
        return ProxyApi.request<Unit, ApiResponse<DepartmentInfo>>(
            hmethod = HttpMethod.Get,
            path = "/api/company/structure"
        )
    }
    
    /**
     * 获取当前用户的组织架构
     */
    suspend fun getCurrentUserOrganizationStructure(): ApiResponse<DepartmentInfo> {
        // 使用全局凭证中的公司ID，如果不存在则尝试从当前用户信息中获取
//        val companyId = GlobalCredentialProvider.companyId
//            ?: GlobalCredentialProvider.storage.getUserInfo()?.currentLoginCompanyId
//            ?: 1L
//        return getOrganizationStructure(companyId)
        return getOrganizationStructure()
    }
}
