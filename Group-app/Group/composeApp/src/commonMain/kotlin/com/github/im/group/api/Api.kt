package com.github.im.group.api

import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.config.ProxyConfig
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
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path
import okio.SYSTEM


object LoginApi {
    /**
     * 鐧诲綍
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
     * 鑾峰彇鐢ㄦ埛鐨勫熀纭€淇℃伅
     * @param userId 鐢ㄦ埛id
     * @return 杩斿洖鐢ㄦ埛鐨勫熀纭€淇℃伅锛坕d锛?username ,email 杩欎簺锛?
     */
    suspend fun getUserBasicInfo(userId: Long): UserInfo {
        return ProxyApi.request<Unit, UserInfo>(
            hmethod = HttpMethod.Post,
            path = "/api/users/id/$userId",
        )
    }


}

/**
 * 浼氳瘽 API
 */
object ConversationApi{


    /**
     * 鑾峰彇鐢ㄦ埛宸叉縺娲荤殑浼氳瘽
     */
    suspend fun getActiveConversationsByUserId(userId:Long ): List<ConversationRes> {
        return ProxyApi.request<Unit, List<ConversationRes>>(
            hmethod = HttpMethod.Get,
            path = "/api/conversations/${userId}/active"
        )
    }
    /**
     * 鑾峰彇鐢ㄦ埛宸叉縺娲荤殑浼氳瘽
     */
    suspend fun getConversation(conversationId:Long ): ConversationRes {
        return ProxyApi.request<Unit, ConversationRes>(
            hmethod = HttpMethod.Get,
            path = "/api/conversations/${conversationId}"
        )
    }

    /**
     * 鑾峰彇鎴栬€呭垱寤虹鑱婁細璇?
     */
    suspend fun createOrGetConversation(friendId:Long): ConversationRes {
        return ProxyApi.request<Unit , ConversationRes>(
            hmethod = HttpMethod.Post,
            path = "/api/conversations/private-chat",
            requestParams = mapOf("friendId" to friendId.toString())
        )
    }

    /**
     * 鍒涘缓浼氳瘽
     */
    suspend fun createGroupConversation(groupInfo:GroupInfo ): ConversationRes {
        return ProxyApi.request< GroupInfo,ConversationRes>(
            hmethod = HttpMethod.Post,
            path = "/api/conversations/group",
            body = groupInfo
        )
    }

}

/**
 * 鏂囦欢澶勭悊 API
 */
object FileApi {
    /**
     * 棰勫垱寤烘枃浠惰褰曟帴鍙ｏ紝鑾峰彇鏂囦欢ID
     */
    suspend fun createFilePlaceholder(request: UploadFileRequest): FileUploadResponse {
        return ProxyApi.request<UploadFileRequest, FileUploadResponse>(
            hmethod = HttpMethod.Post,
            path = "/api/files/uploadId",
            body = request
        )
    }
    
    /**
     * 涓婁紶鏂囦欢锛堟柊鐨勪笂浼犳祦绋嬶級
     * @param file 鏂囦欢
     * @param fileName 鏂囦欢鍚?
     * @param duration 鏂囦欢鏃堕暱 閫傜敤浜庨煶瑙嗛鏂囦欢锛岄潪闊宠棰戞枃浠朵紶 0 鍗冲彲
     */
    suspend fun uploadFile(fileId : String ,file: ByteArray, fileName: String, duration: Long = 0): FileUploadResponse {
        
        return ProxyApi.uploadFile(fileId,file, fileName, duration, )
    }
    

    /**
     * 涓嬭浇鏂囦欢鍒版寚瀹氳矾寰勶紙娴佸紡涓嬭浇锛岄伩鍏峅OM锛?
     * @param fileId 鏂囦欢ID
     * @param outputPath 杈撳嚭鏂囦欢璺緞
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
            throw RuntimeException("涓嬭浇鏂囦欢澶辫触锛?{response.status}锛屽唴瀹? $errorText")
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
                    // 娴佸紡鍐欏叆
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val buffer = ByteArray(8 * 1024)
                    var totalBytesRead = 0L

                    FileSystem.SYSTEM.write(outputPath) {
                        while (!channel.isClosedForRead) {
                            val bytesRead = channel.readAvailable(buffer)
                            if (bytesRead <= 0) break
                            write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 璁＄畻骞舵洿鏂拌繘搴?
                            if (contentLength != null && contentLength > 0) {
                                val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                // 杩欓噷鍙互閫氳繃鍥炶皟鍑芥暟鎴栫姸鎬佹洿鏂版潵閫氱煡杩涘害
                            }
                        }
                    }
                } else {
                    // 灏忔枃浠剁洿鎺ュ啓鍏?
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
     * 涓嬭浇鏂囦欢鍒版寚瀹氳矾寰勶紙娴佸紡涓嬭浇锛岄伩鍏峅OM锛? 鏀寔杩涘害鍥炶皟
     * @param fileId 鏂囦欢ID
     * @param outputPath 杈撳嚭鏂囦欢璺緞
     * @param onProgress 杩涘害鍥炶皟鍑芥暟锛屽弬鏁颁负宸蹭笅杞藉瓧鑺傛暟鍜屾€诲瓧鑺傛暟
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
            throw RuntimeException("涓嬭浇鏂囦欢澶辫触锛?${response.status}锛屽唴瀹? $errorText")
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
                    // 娴佸紡鍐欏叆
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val buffer = ByteArray(8 * 1024)
                    var totalBytesRead = 0L

                    FileSystem.SYSTEM.write(outputPath) {
                        while (!channel.isClosedForRead) {
                            val bytesRead = channel.readAvailable(buffer)
                            if (bytesRead <= 0) break
                            write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 閫氱煡杩涘害
                            if (contentLength != null && contentLength > 0) {
                                onProgress(totalBytesRead, contentLength)
                            }
                        }
                    }
                } else {
                    // 灏忔枃浠剁洿鎺ュ啓鍏?
                    val bytes: ByteArray = response.bodyAsBytes()
                    FileSystem.SYSTEM.write(outputPath) {
                        write(bytes)
                    }
                    
                    // 灏忔枃浠剁洿鎺ュ畬鎴?
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
     * 鏍规嵁鏂囦欢Id鑾峰彇鏂囦欢鍏冩暟鎹?
     */
    suspend fun getFileMeta(fileId:String) : FileMeta{
        return ProxyApi.request< String,FileMeta>(
            hmethod = HttpMethod.Get,
            path = "/api/files/meta",
            requestParams = mapOf("fileId" to fileId)
        )
    }
    
    /**
     * 閫氳繃瀹㈡埛绔疘D鏌ヨ涓婁紶鐘舵€?
     * 鐢ㄤ簬鏂偣缁紶鍔熻兘
     * @param clientId 瀹㈡埛绔敓鎴愮殑UUID
     */
    suspend fun getUploadStatusByClientId(clientId: String): FileUploadResponse? {
        return try {
            ProxyApi.request<String, FileUploadResponse>(
                hmethod = HttpMethod.Get,
                path = "/api/files/upload-status",
                requestParams = mapOf("clientId" to clientId)
            )
        } catch (e: Exception) {
            // 濡傛灉鏈嶅姟绔病鏈夋壘鍒板搴旂殑涓婁紶鐘舵€侊紝杩斿洖null
            null
        }
    }
}

/**
 * Chat Api
 */
object ChatApi {

    /**
     * 鑾峰彇浼氳瘽娑堟伅
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

    /**
     * 鎾ゅ洖娑堟伅
     */
    suspend fun withdrawMessage(msgId: Long): Unit {
        return ProxyApi.request<Unit, Unit>(
            hmethod = HttpMethod.Post,
            path = "/api/messages/withdraw",
            requestParams = mapOf("msgId" to msgId.toString())
        )
    }

    /**
     * 鏍囪娑堟伅涓哄凡璇?     */
    suspend fun markAsRead(msgId: Long): Unit {
        return ProxyApi.request<Unit, Unit>(
            hmethod = HttpMethod.Post,
            path = "/api/messages/mark-as-read",
            requestParams = mapOf("msgId" to msgId.toString())
        )
    }

    /**
     * 鏍囪浼氳瘽鍒版寚瀹氬簭鍒椾负宸茶锛堟帹鑽愶紱鏀寔缇よ亰鎸夌敤鎴风淮搴﹀凡璇绘帹杩涳級
     */
    suspend fun markConversationAsRead(conversationId: Long, sequenceId: Long): Unit {
        return ProxyApi.request<Unit, Unit>(
            hmethod = HttpMethod.Post,
            path = "/api/messages/mark-as-read",
            requestParams = mapOf(
                "conversationId" to conversationId.toString(),
                "sequenceId" to sequenceId.toString()
            )
        )
    }
}

/**
 * Meeting API
 */
object MeetingApi {
    suspend fun createMeeting(request: MeetingCreateRequest): MeetingRes {
        return ProxyApi.request<MeetingCreateRequest, MeetingRes>(
            hmethod = HttpMethod.Post,
            path = "/api/meetings/create",
            body = request
        )
    }

    suspend fun joinMeeting(roomId: String): MeetingRes {
        return ProxyApi.request<MeetingJoinRequest, MeetingRes>(
            hmethod = HttpMethod.Post,
            path = "/api/meetings/join",
            body = MeetingJoinRequest(roomId)
        )
    }

    suspend fun leaveMeeting(roomId: String) {
        ProxyApi.request<MeetingLeaveRequest, Unit>(
            hmethod = HttpMethod.Post,
            path = "/api/meetings/leave",
            body = MeetingLeaveRequest(roomId)
        )
    }

    suspend fun endMeeting(roomId: String, recordMessage: Boolean = true) {
        ProxyApi.request<MeetingEndRequest, Unit>(
            hmethod = HttpMethod.Post,
            path = "/api/meetings/end",
            body = MeetingEndRequest(roomId, recordMessage)
        )
    }

    suspend fun getMeeting(roomId: String): MeetingRes {
        return ProxyApi.request<Unit, MeetingRes>(
            hmethod = HttpMethod.Get,
            path = "/api/meetings/room/$roomId"
        )
    }
}

/**
 * 濂藉弸 API
 */

object FriendShipApi {

    /**
     * 鏌ヨ鐢ㄦ埛鑱旂郴浜?
     */
    suspend fun getFriends(userId:Long) : List<FriendshipDTO>{

        return ProxyApi.request< Unit,List<FriendshipDTO>>(
            hmethod = HttpMethod.Post,
            path = "/api/friendships/list",
            requestParams = mapOf("userId" to userId.toString())
        )
    }

    /**
     * 娣诲姞鑱旂郴浜?
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
     * 鏌ヨ鐢ㄦ埛鐨勫ソ鍙嬭姹?
     * @param userId 鐢ㄦ埛ID
     * @return 濂藉弸璇锋眰鍒楄〃
     */
    suspend fun getSentFriendRequests(userId: Long): List<FriendshipDTO> {
        return ProxyApi.request<Unit, List<FriendshipDTO>>(
            hmethod = HttpMethod.Post,
            path = "/api/friendships/queryRequest",
            requestParams = mapOf("userId" to userId.toString())
        )
    }
    
    /**
     * 鍚屾濂藉弸璇锋眰
     * @param userId 鐢ㄦ埛ID
     * @param maxId 瀹㈡埛绔洰鍓嶆渶澶х殑鍏崇郴ID锛屽彧鑾峰彇姣旇繖涓狪D澶х殑鏁版嵁
     * @return 鏂扮殑濂藉弸璇锋眰鍒楄〃
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
    val fromSequenceId: Long? = null,  // 娣诲姞浠庢寚瀹歴equenceId寮€濮嬫媺鍙栫殑鍙傛暟
    val toSequenceId: Long = 0L,  // 娣诲姞浠庢寚瀹歴equenceId寮€濮嬫媺鍙栫殑鍙傛暟
)

@Serializable
data class MeetingCreateRequest(
    val conversationId: Long,
    val roomId: String? = null,
    val title: String? = null,
    val participantIds: List<Long> = emptyList(),
    val recordMessage: Boolean = true
)

@Serializable
data class MeetingJoinRequest(
    val roomId: String
)

@Serializable
data class MeetingLeaveRequest(
    val roomId: String
)

@Serializable
data class MeetingEndRequest(
    val roomId: String,
    val recordMessage: Boolean = true
)

@Serializable
data class MeetingParticipantRes(
    val userId: Long,
    val username: String? = null,
    val role: String? = null,
    val status: String? = null,
    val joinedAt: LocalDateTime? = null,
    val leftAt: LocalDateTime? = null
)

@Serializable
data class MeetingRes(
    val meetingId: Long,
    val conversationId: Long,
    val roomId: String,
    val title: String? = null,
    val hostId: Long? = null,
    val status: String? = null,
    val startedAt: LocalDateTime? = null,
    val endedAt: LocalDateTime? = null,
    val participants: List<MeetingParticipantRes> = emptyList()
)


@Serializable
/**
 * 鍒嗛〉
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
@SerialName("MEETING")
data class MeetingMessagePayLoad(
    val meetingId: Long? = null,
    val roomId: String? = null,
    val title: String? = null,
    val action: String? = null,
    val hostId: Long? = null,
    val participantIds: List<Long> = emptyList(),
    val participantCount: Int? = null
) : MessagePayLoad

@Serializable
data class UploadFileRequest(
    /**
     * 鏂囦欢澶у皬
     */
    val size: Long?,

    /**
     * 鏂囦欢鍚?
     */
    val fileName: String?,

    /**
     * 鏂囦欢鏃堕暱锛堝獟浣撴枃浠讹級
     */
    val duration: Long?,
)

@Serializable
data class FileUploadResponse(
    // 蹇呴』杩斿洖
    val id: String,
    // 鏂囦欢涓婁紶鍚?蹇呴』杩斿洖  涓嶅彲涓虹┖
    // 鍦ㄤ笂浼犲墠鑾峰彇鏂囦欢 id 鐨勬椂鍊?涓嶄細杩斿洖淇℃伅
    val fileMeta: FileMeta?,

    /**
     * 鏂囦欢鐨勭姸鎬?
     *
     */
    val fileStatus: String?,
)
@Serializable
@SerialName("FILE")
//Kotlinx Serialization 榛樿浼氫娇鐢?"type" 瀛楁 杩涜澶氭€?
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

    val duration  : Int =0,  // 濯掍綋璧勬簮鎵嶄細鍙堟椂闀?鍗曚綅 鏄?姣锛岄渶瑕佸睍绀虹殑鏃跺€?鎹㈢畻鎴愮 榛樿0 鍗冲彲

    val thumbnail :String? = null , // 缂╃暐鍥剧殑  fileId

    val fileStatus: FileStatus   // 鏂囦欢鐘舵€?


) : MessagePayLoad{
    /**
     * 鑾峰彇鏂囦欢URL
     * @return 鏂囦欢URL fileId
     */
    fun getFileUrl(): String? {
        // 鍙湁褰撴枃浠剁姸鎬佷负NORMAL鏃舵墠鍏佽鑾峰彇URL
        return if (fileStatus == FileStatus.NORMAL) {
            val baseUrl = if (ProxyConfig.enableProxy) {
                ProxyConfig.getBaseUrl()
            } else {
                ProxyConfig.getBaseUrl()

//                "http://${ProxyConfig.host}:${ProxyConfig.port}"
            }
            "$baseUrl/api/files/download/$fileId"
        } else {
            null // 鏂囦欢鐘舵€佷笉姝ｅ父鏃惰繑鍥瀗ull
        }
    }
}

@Serializable
data class FriendshipDTO(
    val id: Long? = null,  // 鍏崇郴ID
    val userInfo: UserInfo? = null, // 褰撳墠鐢ㄦ埛鐨勭敤鎴蜂俊鎭紝浠呰繑鍥炵敤鎴稩D鍜岀敤鎴峰悕
    val friendUserInfo: UserInfo? = null, // 濂藉弸鐨勭敤鎴蜂俊鎭?
    val status: FriendRequestStatus? = null,
    val conversationId: Long? = null  //涓庡ソ鍙嬪綋鍓嶅瓨鍦ㄧ殑浼氳瘽
)

/**
 * 濂藉弸璇锋眰
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
    val timestamp: String,  // 鏈嶅姟绔殑鏃堕棿鎴?

    val payload: MessagePayLoad? = null

){


}
public inline fun <reified T : MessagePayLoad> MessageDTO.extraAs(): T? {
    return payload as? T
}


@Serializable
data class GroupInfo(
    // 缇よ亰鍚嶇О
    val groupName: String? = null,
    // 缇よ亰鎻忚堪
    val description: String? = null,
    // 缇よ亰鐢ㄦ埛
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
                if (members.isEmpty()) return "鏃犳垚鍛?

                // 杩斿洖闈炲綋鍓嶇敤鎴风殑鍚嶇О
                val otherUser = members.firstOrNull() { it.userId != (currentUser?.userId ?: "") }
                otherUser?.username ?: ""

            }
        }
    }

    /**
     * 鐢ㄦ埛淇℃伅
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

/**
 * 公司/工作区 API
 */
object CompanyApi {
    /**
     * 获取当前用户所在的所有公司
     */
    suspend fun getMyCompanies(): List<com.github.im.group.model.CompanyDTO> {
        return ProxyApi.request<Unit, List<com.github.im.group.model.CompanyDTO>>(
            hmethod = HttpMethod.Get,
            path = "/api/company/my"
        )
    }

    /**
     * 切换当前登录公司
     * @param companyId 目标公司ID
     */
    suspend fun switchCompany(companyId: Long): com.github.im.group.model.UserInfo {
        return ProxyApi.request<Unit, com.github.im.group.model.UserInfo>(
            hmethod = HttpMethod.Post,
            path = "/api/company/switch/$companyId"
        )
    }
}

@Serializable
data class LoginResponse(
    val token: String, 
    val userId: Long,
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val currentLoginCompanyId: Long? = null,
    val companies: List<com.github.im.group.model.CompanyDTO> = emptyList()
)
