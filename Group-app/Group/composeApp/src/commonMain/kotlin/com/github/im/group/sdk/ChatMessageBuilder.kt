import com.github.im.group.api.FileApi
import com.github.im.group.api.UploadFileRequest
import com.github.im.group.manager.FileTypeDetector
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.model.toUserInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.viewmodel.LoginState
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// 消息发送器接口，统一处理所有类型消息的发送
interface ChatMessageBuilder {

    /**
     * 构建文本消息
     */
    suspend fun textMessage(conversationId: Long, content: String)  : ChatMessage

    suspend fun fileMessage(conversationId: Long,fileName: String,fileSize:Long, duration: Long = 0,) : ChatMessage
}

// 实现消息发送器
class ChatMessageBuilderImpl(
    private val userRepository: UserRepository,
) : ChatMessageBuilder {
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun textMessage(conversationId: Long, content: String) : ChatMessage {
        return buildSendChatMessage(conversationId, content, MessageType.TEXT)
    }



    @OptIn(ExperimentalUuidApi::class)
    override suspend fun fileMessage(conversationId: Long,fileName: String,fileSize:Long, duration: Long,)
    : ChatMessage {
        // 预创建文件记录，获取文件ID

        val messageType = FileTypeDetector.getMessageType(filename = fileName)
        val uploadRequest = UploadFileRequest(
            size =fileSize,
            fileName = fileName,
            duration = duration
        )
        val response = FileApi.createFilePlaceholder(uploadRequest)
        val serverFileId = response.id

        val chatMessage = buildSendChatMessage(
            messageContent = serverFileId,
            conversationId = conversationId,
            messageType = messageType,
        )

        return chatMessage
    }

    /**
     * 构建发送的文本消息
     * 
     * 修复离线状态下的类型转换问题：
     * 1. 安全检查用户登录状态
     * 2. 在离线状态下使用本地缓存的用户信息
     * 3. 添加适当的错误处理和日志记录
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun buildSendChatMessage( conversationId: Long, messageContent: String, messageType: MessageType) : ChatMessage{
        // 创建一个本地消息记录
        val clientMsgId = Uuid.random().toString()
        
        // 安全获取用户信息，避免类型转换异常
        val accountInfo = getUserInfoSafely()
        
        if (accountInfo == null) {
            Napier.e("无法获取用户信息，使用默认值构建消息")
            // 在极端情况下使用默认值
            return ChatMessage(
                content = messageContent,
                conversationId = conversationId,
                fromUser = com.github.im.group.model.proto.UserInfo(
                    userId = 0L,
                    username = "Unknown",
                    eMail = ""
                ),
                type = messageType,
                messagesStatus = MessagesStatus.SENDING,
                clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
                clientMsgId = clientMsgId
            )
        }

        // 首先发送一个带有本地状态的消息，状态为发送中
       return  ChatMessage(
                content = messageContent, // 使用服务端返回的ID作为内容
                conversationId = conversationId,
                fromUser = accountInfo,
                type = messageType,
                messagesStatus = MessagesStatus.SENDING,
                clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
                clientMsgId = clientMsgId
        )
    }
    
    /**
     * 安全获取用户信息
     * 
     * 处理各种登录状态：
     * - Authenticated: 直接返回用户信息
     * - Authenticating: 尝试从本地存储获取
     * - 其他状态: 返回null并记录日志
     */
    private suspend fun getUserInfoSafely(): com.github.im.group.model.proto.UserInfo? {
        return try {
            val loginState = userRepository.userState.value
            
            when (loginState) {
                is LoginState.Authenticated -> {
                    // 正常认证状态，直接使用
                    Napier.d("用户已认证，使用当前用户信息")
                    return loginState.userInfo.toUserInfo()
                }
                is LoginState.Authenticating -> {
                    // 认证中状态，尝试从本地存储获取用户信息
                    Napier.w("用户正在认证中，尝试从本地获取用户信息")
                    val localUserInfo = userRepository.getLocalUserInfo()
                    if (localUserInfo != null) {
                        Napier.d("从本地存储获取到用户信息")
                        return localUserInfo.toUserInfo()
                    } else {
                        Napier.e("认证中且本地无用户信息")
                        return null
                    }
                }
                is LoginState.AuthenticationFailed -> {
                    Napier.e("用户认证失败: ${loginState.error}")
                    // 尝试从本地存储获取，可能是网络问题导致的认证失败
                    val localUserInfo = userRepository.getLocalUserInfo()
                    if (localUserInfo != null) {
                        Napier.d("认证失败但本地有用户信息，使用本地数据")
                        return localUserInfo.toUserInfo()
                    }
                    return null
                }
                is LoginState.Idle -> {
                    Napier.e("用户未登录")
                    // 尝试从本地存储获取
                    val localUserInfo = userRepository.getLocalUserInfo()
                    if (localUserInfo != null) {
                        Napier.d("未登录状态但本地有用户信息")
                        return localUserInfo.toUserInfo()
                    }
                    return null
                }
                is LoginState.Checking -> {
                    Napier.w("正在检查用户凭据，尝试从本地获取")
                    val localUserInfo = userRepository.getLocalUserInfo()
                    if (localUserInfo != null) {
                        return localUserInfo.toUserInfo()
                    }
                    return null
                }
                is LoginState.LoggingOut -> {
                    Napier.w("用户正在登出，尝试从本地获取最后的用户信息")
                    val localUserInfo = userRepository.getLocalUserInfo()
                    if (localUserInfo != null) {
                        return localUserInfo.toUserInfo()
                    }
                    return null
                }
            }
        } catch (e: Exception) {
            Napier.e("获取用户信息时发生异常", e)
            // 最后的降级方案：尝试从本地存储获取
            try {
                val localUserInfo = userRepository.getLocalUserInfo()
                if (localUserInfo != null) {
                    Napier.d("异常情况下从本地获取到用户信息")
                    return localUserInfo.toUserInfo()
                }
            } catch (localException: Exception) {
                Napier.e("从本地获取用户信息也失败", localException)
            }
            return null
        }
    }
}