package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile
import com.github.im.group.sdk.SenderSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * 聊天消息记录model
 */
data class ChatUiState(
//    val messages: List<MessageWrapper> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val conversation: ConversationRes = ConversationRes(),
    val loading: Boolean = false
)


class ChatMessageViewModel(
    val userRepository: UserRepository,
//    val userViewModel: UserViewModel,
    val chatSessionManager: ChatSessionManager,
    val chatMessageRepository: ChatMessageRepository,
    val senderSdk: SenderSdk,
    val filePicker: FilePicker,  // 通过构造函数注入FilePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    private val messageIndex = mutableMapOf<String, Int>() // clientMsgId -> messages list index


    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _loading = MutableStateFlow(false)




    /**
     * 接收到消息
     */
    fun onReceiveMessage(message: MessageItem){
        _uiState.update {
            it.copy(messages = it.messages + message)
        }
    }


    /**
     * 更新消息
     * 从本地数据库中获取最新消息数据
     * 更新 状态  ui
     *
     */
    fun updateMessage(clientMsgId: String?) {
        if (clientMsgId == null) return

        val idx = messageIndex[clientMsgId] ?: return

        // 从数据库中查询最新版本的消息
        val latestMessage = chatMessageRepository.getMessageByClientMsgId(clientMsgId) ?: return

        // 更新 StateFlow
        _uiState.update { state ->
            val updatedList = state.messages.toMutableList()
            updatedList[idx] = latestMessage  // 替换为最新版本
            state.copy(messages = updatedList)
        }
    }

    /**
     * 注册会话
     */
    fun register(conversationId: Long){
        chatSessionManager.register(conversationId,this)
    }

    fun unregister(conversationId: Long){
        chatSessionManager.unregister(conversationId)
    }



    /**
     * 加载消息
     * @param conversationId 会话id
     * 默认加载最新的 50 条消息
     */
    fun loadMessages(conversationId: Long )  {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val response = ChatApi.getMessages(conversationId)

                val wrappedMessages = response.content.map {
                    MessageWrapper(messageDto = it)
                }.sortedBy { it.seqId }

                println("receive $wrappedMessages")

                _uiState.update {
                    it.copy(messages = wrappedMessages)
                }

            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }

    }

    /**
     * 接收新的消息
     */
    private fun addMessage(chatMessage: ChatMessage){
        senderSdk.sendMessage(chatMessage)

        chatMessageRepository.insertMessage(chatMessage)
        val message = MessageWrapper(chatMessage)
        _uiState.update {
            // 更新消息列表
            val updatedList = it.messages + message
            // clientMsgId  建立索引
            messageIndex[message.clientMsgId] = updatedList.lastIndex
            it.copy(messages = updatedList)
        }
    }


    /**
     * 删除消息
     */
    fun removeMessage(clientMsgId: String) {
        val idx = messageIndex.remove(clientMsgId) ?: return
        _uiState.update { state ->
            val updatedList = state.messages.toMutableList()
            updatedList.removeAt(idx)
            // 重新建立索引
            updatedList.forEachIndexed { i, msg ->
                messageIndex[msg.clientMsgId] = i
            }
            state.copy(messages = updatedList)
        }
    }


    /**
     * 查询指定 群聊
     */
    fun getConversation (uId: Long ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(loading = true)
            }

            try {
                val response = ConversationApi.getConversation(uId)
                _uiState.update {
                    it.copy(conversation = response)
                }

            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 发送语音消息
     *
     * @param conversationId 会话ID
     * @param url 语音文件的URL地址
     */
    fun sendVoiceMessage(conversationId: Long,
                         data: ByteArray,
                         fileName:String,
                         duration:Long
                         ) {

        sendFileMessage(conversationId,data,fileName,duration,MessageType.VOICE)
    }

    /**
     * 发送文件消息
     * @param conversationId 会话ID
     * @param file 选择的文件
     */
    fun sendFileMessage(conversationId: Long, file: PickedFile) {
        // TODO 发送 存在延迟
        //  优化项
        /**
         * 1. 发送带有 clientId 的消息体
         * 2. 服务端接受 返回 ACK 消息
         * 。。。 没想好
          */

        viewModelScope.launch(Dispatchers.IO) {
            try {

                fun isImageFile(mimeType: String? = null, filename: String? = null): Boolean {
                    mimeType?.let {
                        if (it.startsWith("image/")) return true
                    }
                    filename?.let {
                        val lower = it.lowercase()
                        return lower.endsWith(".jpg") ||
                                lower.endsWith(".jpeg") ||
                                lower.endsWith(".png") ||
                                lower.endsWith(".gif") ||
                                lower.endsWith(".bmp") ||
                                lower.endsWith(".webp") ||
                                lower.endsWith(".heic")
                    }
                    return false
                }

                fun isAudioFile(mimeType: String? = null, filename: String? = null): Boolean {
                    mimeType?.let {
                        if (it.startsWith("audio/")) return true
                    }
                    filename?.let {
                        val lower = it.lowercase()
                        return lower.endsWith(".mp3") ||
                                lower.endsWith(".wav") ||
                                lower.endsWith(".aac") ||
                                lower.endsWith(".flac") ||
                                lower.endsWith(".ogg") ||
                                lower.endsWith(".m4a")
                    }
                    return false
                }

                fun isVideoFile(mimeType: String? = null, filename: String? = null): Boolean {
                    mimeType?.let {
                        if (it.startsWith("video/")) return true
                    }
                    filename?.let {
                        val lower = it.lowercase()
                        return lower.endsWith(".mp4") ||
                                lower.endsWith(".mov") ||
                                lower.endsWith(".avi") ||
                                lower.endsWith(".mkv") ||
                                lower.endsWith(".flv") ||
                                lower.endsWith(".webm")
                    }
                    return false
                }

                val isImageMimeType = isImageFile(file.mimeType, file.name)
                val isAudioMimeType = isAudioFile(file.mimeType, file.name)
                val isVideoMimeType = isVideoFile(file.mimeType, file.name)
                // 封装下 返回 messageType

                val messageType = when {
                    isImageMimeType -> MessageType.IMAGE
                    isAudioMimeType -> MessageType.VOICE
                    isVideoMimeType -> MessageType.VIDEO
                    else -> {
                        MessageType.FILE
                    }
                }

                // 先读取文件字节数据
                val fileBytes = filePicker.readFileBytes(file)
                if (fileBytes != null) {
                    // 在后台线程中上传文件并发送消息
                    val response = withContext(Dispatchers.IO) {
                        FileApi.uploadFile(fileBytes, file.name, 0)
                    }
                    sendMessage(conversationId, response.id, messageType)
                } else {
                    // 如果无法读取文件，发送文件名作为消息
                    sendMessage(conversationId, "文件: ${file.name}", MessageType.FILE)
                }
            } catch (e: Exception) {
                // 处理上传失败的情况
                e.printStackTrace()
                // 即使上传失败，也发送文件名作为消息
                sendMessage(conversationId, "文件: ${file.name}", MessageType.FILE)
            }
        }
    }

     suspend fun getFileMessageMeta(fileId: String): FileMeta{
         return FileApi.getFileMeta(fileId)
    }

    /**
     * 获取文件元信息
     */
    fun getFileMessageMeta(messageItem: MessageItem): FileMeta? {
        if(messageItem.type.isFile()){
            messageItem.fileMeta?.let {
                return it
            }

            // 如果没有，通过API获取
            runBlocking {
                try {
                    getFileMessageMeta(messageItem.content)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
        return null
    }

    /**
     * 发送文件消息
     * @param conversationId 会话ID
     * @param data 文件数据
     * @param fileName 文件名
     * @param duration 时长（用于语音消息）
     * @param type 消息类型
     */
    fun sendFileMessage(conversationId: Long, data: ByteArray, fileName: String, duration: Long = 0, type: MessageType = MessageType.FILE) {
        viewModelScope.launch {
            try {
                val response = FileApi.uploadFile(data, fileName, duration)
                sendMessage(conversationId, response.id, type)
            } catch (e: Exception) {
                // 处理上传失败的情况
                e.printStackTrace()
            }
        }
    }

    /**
     * 发送消息
     * @param conversationId 会话
     * @param message 消息
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun sendMessage(conversationId:Long, message:String, type: MessageType = MessageType.TEXT){
        // 发送的 数据需要 再本地先保存
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val accountInfo = userRepository.withLoggedInUser { it.accountInfo }
                if(message.isNotBlank()){
                    val chatMessage = ChatMessage(
                        content = message,
                        conversationId = conversationId,
                        fromAccountInfo = accountInfo,
                        type = type,
                        messagesStatus = MessagesStatus.SENDING,
                        clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
                        clientMsgId =  Uuid.random().toString(),
                    )
                    addMessage(chatMessage)
                }

            } catch (e: Exception) {
                println("发送失败: $e")
            }
        }

    }

}