import com.github.im.group.api.FileApi
import com.github.im.group.api.UploadFileRequest
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.FileTypeDetector
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.repository.UserRepository
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime
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
            messageType = messageType
        )

        return chatMessage
    }

    /**
     * 构建发送的文本消息
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun buildSendChatMessage( conversationId: Long, messageContent: String, messageType: MessageType) : ChatMessage{


        // 创建一个本地消息记录
        val clientMsgId = Uuid.random().toString()
        val accountInfo = userRepository.withLoggedInUser { it.accountInfo }

        // 首先发送一个带有本地状态的消息，状态为发送中
       return  ChatMessage(
                content = messageContent, // 使用服务端返回的ID作为内容
                conversationId = conversationId,
                fromAccountInfo = accountInfo,
                type = messageType,
                messagesStatus = MessagesStatus.SENDING,
                clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
                clientMsgId = clientMsgId
        )
    }
}