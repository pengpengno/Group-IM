package com.github.im.group.api

import ProxyApi
import ProxyConfig
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.db.entities.FriendRequestStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.ApiResponse
import com.github.im.group.model.DepartmentInfo
import com.github.im.group.model.UserInfo
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path


object LoginApi {
    /**
     * 登录
     */
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


    /**
     * 获取用户的基础信息
     * @param userId 用户id
     * @return 返回用户的基础信息（id， username ,email 这些）
     */
    suspend fun getUserBasicInfo(userId: Long): UserInfo {
        return ProxyApi.request<Unit, UserInfo>(
            hmethod = HttpMethod.Post,
            path = "/api/users/id/$userId",
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
    suspend fun createOrGetConversation(friendId:Long): ConversationRes {
        return ProxyApi.request<Unit , ConversationRes>(
            hmethod = HttpMethod.Post,
            path = "/api/conversations/private-chat",
            requestParams = mapOf("friendId" to friendId.toString())
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
     * 预创建文件记录接口，获取文件ID
     */
    suspend fun createFilePlaceholder(request: UploadFileRequest): FileUploadResponse {
        return ProxyApi.request<UploadFileRequest, FileUploadResponse>(
            hmethod = HttpMethod.Post,
            path = "/api/files/uploadId",
            body = request
        )
    }
    
    /**
     * 上传文件（新的上传流程）
     * @param file 文件
     * @param fileName 文件名
     * @param duration 文件时长 适用于音视频文件，非音视频文件传 0 即可
     */
    suspend fun uploadFile(fileId : String ,file: ByteArray, fileName: String, duration: Long = 0): FileUploadResponse {
        
        return ProxyApi.uploadFile(fileId,file, fileName, duration, )
    }
    

    /**
     * 下载文件到指定路径（流式下载，避免OOM）
     * @param fileId 文件ID
     * @param outputPath 输出文件路径
     */
    suspend fun downloadFileToPath(fileId: String, outputPath: Path) {
        val baseUrl = ProxyConfig.getBaseUrl()

        val response: HttpResponse = ProxyApi.client.request("$baseUrl/api/files/download/$fileId") {
            method = HttpMethod.Get
            headers {
                val token = GlobalCredentialProvider.currentToken
                if (token.isNotEmpty()) {
                    append("Proxy-Authorization", "Basic $token")
                    append("Authorization", "Bearer $token")
                }
            }
        }

        val statusValue: Int = response.status.value
        if (statusValue !in 200..299) {
            val errorText: String = response.bodyAsText()
            throw RuntimeException("下载文件失败：${response.status}，内容: $errorText")
        }

        val contentLength: Long? = response.contentLength()
        val contentType: String = response.headers["Content-Type"] ?: "application/octet-stream"

        val sizeLimit: Long = when {
            contentType.startsWith("audio/") -> 50L * 1024 * 1024
            contentType.startsWith("image/") -> 10L * 1024 * 1024
            contentType.startsWith("video/") -> 100L * 1024 * 1024
            else -> 50L * 1024 * 1024
        }

        val useStream: Boolean = contentLength == null || contentLength > sizeLimit

        withContext(Dispatchers.IO) {
            try {
                if (useStream) {
                    // 流式写入
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val buffer = ByteArray(8 * 1024)
                    var totalBytesRead = 0L

                    FileSystem.SYSTEM.write(outputPath) {
                        while (!channel.isClosedForRead) {
                            val bytesRead = channel.readAvailable(buffer)
                            if (bytesRead <= 0) break
                            write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 计算并更新进度
                            if (contentLength != null && contentLength > 0) {
                                val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                // 这里可以通过回调函数或状态更新来通知进度
                            }
                        }
                    }
                } else {
                    // 小文件直接写入
                    val bytes: ByteArray = response.bodyAsBytes()
                    FileSystem.SYSTEM.write(outputPath) {
                        write(bytes)
                    }
                }
            } catch (e: Exception) {
                if (FileSystem.SYSTEM.exists(outputPath)) {
                    FileSystem.SYSTEM.delete(outputPath)
                }
                throw e
            }
        }
    }

    /**
     * 下载文件到指定路径（流式下载，避免OOM）- 支持进度回调
     * @param fileId 文件ID
     * @param outputPath 输出文件路径
     * @param onProgress 进度回调函数，参数为已下载字节数和总字节数
     */
    suspend fun downloadFileToPathWithProgress(fileId: String, outputPath: Path, onProgress: (Long, Long) -> Unit) {
        val baseUrl = ProxyConfig.getBaseUrl()

        val response: HttpResponse = ProxyApi.client.request("$baseUrl/api/files/download/$fileId") {
            method = HttpMethod.Get
            headers {
                val token = GlobalCredentialProvider.currentToken
                if (token.isNotEmpty()) {
                    append("Proxy-Authorization", "Basic $token")
                    append("Authorization", "Bearer $token")
                }
            }
        }

        val statusValue: Int = response.status.value
        if (statusValue !in 200..299) {
            val errorText: String = response.bodyAsText()
            throw RuntimeException("下载文件失败：'${response.status}，内容: $errorText")
        }

        val contentLength: Long? = response.contentLength()
        val contentType: String = response.headers["Content-Type"] ?: "application/octet-stream"

        val sizeLimit: Long = when {
            contentType.startsWith("audio/") -> 50L * 1024 * 1024
            contentType.startsWith("image/") -> 10L * 1024 * 1024
            contentType.startsWith("video/") -> 100L * 1024 * 1024
            else -> 50L * 1024 * 1024
        }

        val useStream: Boolean = contentLength == null || contentLength > sizeLimit

        withContext(Dispatchers.IO) {
            try {
                if (useStream) {
                    // 流式写入
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val buffer = ByteArray(8 * 1024)
                    var totalBytesRead = 0L

                    FileSystem.SYSTEM.write(outputPath) {
                        while (!channel.isClosedForRead) {
                            val bytesRead = channel.readAvailable(buffer)
                            if (bytesRead <= 0) break
                            write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 通知进度
                            if (contentLength != null && contentLength > 0) {
                                onProgress(totalBytesRead, contentLength)
                            }
                        }
                    }
                } else {
                    // 小文件直接写入
                    val bytes: ByteArray = response.bodyAsBytes()
                    FileSystem.SYSTEM.write(outputPath) {
                        write(bytes)
                    }
                    
                    // 小文件直接完成
                    if (contentLength != null) {
                        onProgress(contentLength, contentLength)
                    }
                }
            } catch (e: Exception) {
                if (FileSystem.SYSTEM.exists(outputPath)) {
                    FileSystem.SYSTEM.delete(outputPath)
                }
                throw e
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
    
    /**
     * 通过客户端ID查询上传状态
     * 用于断点续传功能
     * @param clientId 客户端生成的UUID
     */
    suspend fun getUploadStatusByClientId(clientId: String): FileUploadResponse? {
        return try {
            ProxyApi.request<String, FileUploadResponse>(
                hmethod = HttpMethod.Get,
                path = "/api/files/upload-status",
                requestParams = mapOf("clientId" to clientId)
            )
        } catch (e: Exception) {
            // 如果服务端没有找到对应的上传状态，返回null
            null
        }
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
data class UploadFileRequest(
    /**
     * 文件大小
     */
    val size: Long?,

    /**
     * 文件名
     */
    val fileName: String?,

    /**
     * 文件时长（媒体文件）
     */
    val duration: Long?,
)

@Serializable
data class FileUploadResponse(
    // 必须返回
    val id: String,
    // 文件上传后 必须返回  不可为空
    // 在上传前获取文件 id 的时候 不会返回信息
    val fileMeta: FileMeta?,

    /**
     * 文件的状态
     *
     */
    val fileStatus: String?,
)
@Serializable
@SerialName("FILE")
//Kotlinx Serialization 默认会使用 "type" 字段 进行多态
data class FileMeta(

    @SerialName("fileId")
    val fileId : String,

    @SerialName("filename")
    val fileName: String,

    @SerialName("fileSize")
    val size: Long,

    val contentType: String,

    val hash: String = "",

    @SerialName("type")
    val type: String = "FILE",

    val duration  : Int =0,  // 媒体资源才会又时长 单位 是 毫秒，需要展示的时候 换算成秒 默认0 即可

    val thumbnail :String? = null , // 缩略图的  fileId

    val fileStatus: FileStatus   // 文件状态


) : MessagePayLoad{
    /**
     * 获取文件URL
     * @return 文件URL fileId
     */
    fun getFileUrl(): String? {
        // 只有当文件状态为NORMAL时才允许获取URL
        return if (fileStatus == FileStatus.NORMAL) {
            val baseUrl = if (ProxyConfig.enableProxy) {
                ProxyConfig.getBaseUrl()
            } else {
                ProxyConfig.getBaseUrl()

//                "http://${ProxyConfig.host}:${ProxyConfig.port}"
            }
            "$baseUrl/api/files/download/$fileId"
        } else {
            null // 文件状态不正常时返回null
        }
    }
}

@Serializable
data class FriendshipDTO(
    val id: Long? = null,  // 关系ID
    val userInfo: UserInfo? = null, // 当前用户的用户信息，仅返回用户ID和用户名
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
    // 群聊名称
    val groupName: String? = null,
    // 群聊描述
    val description: String? = null,
    // 群聊用户
    val members: List<UserInfo>? = listOf(),
){
    fun isGroup():Boolean{
        return members != null && members.isNotEmpty() && members.size > 2
    }
}

@Serializable
data class ConversationRes(
    val conversationId: Long ,
    val createdBy : UserInfo? = null ,
    val createUserId : Long = 0,
    val createAt : String = "" ,
    val groupName: String = "",
    val description: String? = "",
    val members: List<UserInfo> = emptyList(),
    val status: ConversationStatus = ConversationStatus.ACTIVE,
    val type: ConversationType = ConversationType.PRIVATE_CHAT,
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
            ConversationType.GROUP -> null
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
        return getOrganizationStructure()
    }
}
