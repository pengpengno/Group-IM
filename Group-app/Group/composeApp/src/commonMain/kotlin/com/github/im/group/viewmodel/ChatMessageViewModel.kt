package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.FileStorageManager
import com.github.im.group.sdk.PickedFile
import com.github.im.group.sdk.SenderSdk
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * 聊天消息记录model
 */
data class ChatUiState(
//    val messages: List<MessageWrapper> = emptyList(),
    val messages: MutableList<MessageItem> = mutableListOf(),
    val conversation: ConversationRes = ConversationRes(),
    val loading: Boolean = false
)

/**
 * 文件下载状态
 */
data class FileDownloadState(
    val fileId: String,
    val isDownloading: Boolean = false,
    val isSuccess: Boolean = false,
    val fileContent: ByteArray? = null,
    val error: String? = null
)

class ChatMessageViewModel(
    val userRepository: UserRepository,
    val chatSessionManager: ChatSessionManager,
    val chatMessageRepository: ChatMessageRepository,
    val messageSyncRepository: MessageSyncRepository,
    val filesRepository: FilesRepository, // 添加文件仓库依赖
    val senderSdk: SenderSdk,
    val filePicker: FilePicker,  // 通过构造函数注入FilePicker
    val fileStorageManager: FileStorageManager // 文件存储管理器
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    private val messageIndex = mutableMapOf<String, Int>() // clientMsgId -> messages list index

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 多文件下载状态管理
    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())
    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()

    private val _loading = MutableStateFlow(false)




    /**
     * 接收到消息
     */
    fun onReceiveMessage(message: MessageItem){
        _uiState.update {
            val updatedMessages = it.messages.toMutableList()
            updatedMessages.add(message)
            it.copy(messages = updatedMessages)
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
    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                // 首先从本地数据库加载已有消息
                val localMessages = chatMessageRepository.getMessagesByConversation(conversationId)
                Napier.d("从本地加载到 ${localMessages.size} 条消息")
                
                // 更新UI显示本地消息
                _uiState.update {
                    val messageList = mutableListOf<MessageItem>()
                    localMessages.forEach { message ->
                        messageList.add(message)
                    }
                    it.copy(messages = messageList)
                }

                // 然后从服务器同步新消息（增量同步）
                val newMessageCount = messageSyncRepository.syncMessages(conversationId)
                Napier.d("同步到 $newMessageCount 条新消息")
                
                // 如果有新消息，重新从本地加载所有消息
                if (newMessageCount > 0) {
                    val updatedMessages = chatMessageRepository.getMessagesByConversation(conversationId)
                    Napier.d("更新后共有 ${updatedMessages.size} 条消息")
                    
                    _uiState.update {
                        val messageList = mutableListOf<MessageItem>()
                        updatedMessages.forEach { message ->
                            messageList.add(message)
                        }
                        it.copy(messages = messageList)
                    }
                }
            } catch (e: Exception) {
                Napier.e("加载消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 手动刷新消息（下拉刷新时调用）
     * @param conversationId 会话ID
     */
    fun refreshMessages(conversationId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                // 只执行同步操作，不重新加载本地数据
                val newMessageCount = messageSyncRepository.syncMessages(conversationId)
                Napier.d("刷新同步到 $newMessageCount 条新消息")
                
                // 如果有新消息，重新从本地加载所有消息
                if (newMessageCount > 0) {
                    val updatedMessages = chatMessageRepository.getMessagesByConversation(conversationId)
                    Napier.d("刷新后共有 ${updatedMessages.size} 条消息")
                    
                    _uiState.update {
                        val messageList = mutableListOf<MessageItem>()
                        updatedMessages.forEach { message ->
                            messageList.add(message)
                        }
                        it.copy(messages = messageList)
                    }
                }
            } catch (e: Exception) {
                Napier.e("刷新消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 加载更多历史消息（上拉加载更多）
     * @param conversationId 会话ID
     * @param beforeSequenceId 加载此序列号之前的消息
     */
    fun loadMoreMessages(conversationId: Long, beforeSequenceId: Long) {
        loadMessagesWithProgress(
            conversationId = conversationId,
            isFront = true,
            currentIndex = beforeSequenceId
        )
    }
    
    /**
     * 获取最新消息（下拉刷新获取新消息）
     * @param conversationId 会话ID
     * @param afterSequenceId 获取此序列号之后的消息
     */
    fun loadLatestMessages(conversationId: Long, afterSequenceId: Long) {
        loadMessagesWithProgress(
            conversationId = conversationId,
            isFront = false,
            currentIndex = afterSequenceId
        )
    }
    
    /**
     * 通用的消息加载方法，带有加载进度状态管理
     * @param conversationId 会话ID
     * @param isFront 是否是向前加载（历史消息）
     * @param currentIndex 当前索引
     */
    private fun loadMessagesWithProgress(
        conversationId: Long,
        isFront: Boolean,
        currentIndex: Long
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val messages = messageSyncRepository.getMessagesWithStrategy(conversationId, currentIndex, isFront)
                Napier.d("加载了: ${messages.size} 条消息")
                // 更新UI状态
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages
                    if (isFront) {
                        // 历史消息，添加到列表开头
                        updatedMessages.addAll(0, messages)
                    } else {
                        // 最新消息，添加到列表末尾
                        updatedMessages.addAll(messages)
                    }
                    currentState.copy(messages = updatedMessages)
                }
            } catch (e: Exception) {
                Napier.e("加载消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 接收/ 更新 服务端发送的新的新的消息
     */
    private fun addMessage(chatMessage: ChatMessage){
        senderSdk.sendMessage(chatMessage)
        chatMessageRepository.insertOrUpdateMessage(MessageWrapper(chatMessage))
        val message = MessageWrapper(chatMessage)
        _uiState.update {
            // 更新消息列表
            val updatedMessages = it.messages.toMutableList()
            updatedMessages.add(message)
            // clientMsgId  建立索引
            messageIndex[message.clientMsgId] = updatedMessages.size - 1
            it.copy(messages = updatedMessages)
        }
    }


    /**
     * 删除消息
     */
    fun removeMessage(clientMsgId: String) {
        val idx = messageIndex.remove(clientMsgId) ?: return
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            updatedMessages.removeAt(idx)
            // 重新建立索引
            updatedMessages.forEachIndexed { i, msg ->
                messageIndex[msg.clientMsgId] = i
            }
            state.copy(messages = updatedMessages)
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
                Napier.e("获取会话信息失败", e)
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
                    sendMessage(conversationId, response.id, messageType, response.fileMeta)
                } else {
                    // 如果无法读取文件，发送文件名作为消息
                    sendMessage(conversationId, "文件: ${file.name}", MessageType.FILE)
                }
            } catch (e: Exception) {
                // 处理上传失败的情况
                Napier.e("发送文件消息失败", e)
                // 即使上传失败，也发送文件名作为消息
                sendMessage(conversationId, "文件: ${file.name}", MessageType.FILE)
            }
        }
    }

     suspend fun getFileMessageMeta(fileId: String): FileMeta{
         return FileApi.getFileMeta(fileId)
    }

    /**
     * 获取文件元信息（本地优先）
     */
    fun getFileMessageMeta(messageItem: MessageItem): FileMeta? {
        if(messageItem.type.isFile()){
            messageItem.fileMeta?.let {
                return it
            }
            // 优先从本地数据库获取文件元数据
            filesRepository.getFileMeta(messageItem.content)?.let { 
                return it
            }
            // 如果本地没有，通过API获取并存储到本地
            runBlocking {
                try {
                    val fileMeta = getFileMessageMeta(messageItem.content)
                    // 将文件元数据存储到本地数据库
                    filesRepository.addFile(fileMeta)
                    return@runBlocking fileMeta
                } catch (e: Exception) {
                    Napier.e("获取文件元信息失败", e)
                    null
                }
            }
        }
        return null
    }
    
    /**
     * 检查文件是否存在（本地优先）
     * @param fileId 文件ID
     * @return 文件是否存在
     */
    fun isFileExists(fileId: String): Boolean {
        return fileStorageManager.isFileExists(fileId)
    }
    
    /**
     * 删除指定文件（本地和数据库记录）
     * @param fileId 文件ID
     * @return 删除是否成功
     */
    fun deleteFile(fileId: String): Boolean {
        return fileStorageManager.deleteFile(fileId)
    }
    
    /**
     * 清理过期文件
     */
    fun cleanupExpiredFiles() {
        fileStorageManager.cleanupExpiredFiles()
    }
    
    /**
     * 下载文件消息内容（实现本地优先策略）
     * @param fileId 文件ID
     */
    fun downloadFileMessage(fileId: String) {
        Napier.d("开始下载文件: $fileId")
        viewModelScope.launch {
            try {
                // 检查是否已经在下载该文件
                val currentState = _fileDownloadStates.value[fileId]
                if (currentState?.isDownloading == true) {
                    Napier.d("文件已在下载中: $fileId")
                    return@launch
                }
                
                // 更新下载状态为正在下载
                _fileDownloadStates.update { currentStates ->
                    currentStates + (fileId to FileDownloadState(
                        fileId = fileId,
                        isDownloading = true
                    ))
                }
                
                // 从文件存储管理器获取文件内容（实现本地优先策略）
                val fileContent = fileStorageManager.getFileContent(fileId)
                
                // 更新下载状态为成功
                _fileDownloadStates.update { currentStates ->
                    currentStates + (fileId to FileDownloadState(
                        fileId = fileId,
                        isDownloading = false,
                        isSuccess = true,
                        fileContent = fileContent
                    ))
                }
                
                Napier.d("文件下载成功: $fileId")
            } catch (e: Exception) {
                Napier.e("文件下载失败", e)
                // 更新下载状态为失败
                _fileDownloadStates.update { currentStates ->
                    currentStates + (fileId to FileDownloadState(
                        fileId = fileId,
                        isDownloading = false,
                        isSuccess = false,
                        error = e.message
                    ))
                }
            }
        }
    }
    
    /**
     * 清除指定文件的下载状态
     * @param fileId 文件ID
     */
    fun clearFileDownloadState(fileId: String) {
        _fileDownloadStates.update { currentStates ->
            currentStates - fileId
        }
    }
    
    /**
     * 获取本地文件路径
     * @param fileId 文件ID
     * @return 本地文件路径
     */
    fun getLocalFilePath(fileId: String): Path? {
        return fileStorageManager.getLocalFilePath(fileId)
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
                // 保存文件信息到本地数据库
                // 这里只是框架代码，实际实现将在后续添加
                sendMessage(conversationId, response.id, type, response.fileMeta)
            } catch (e: Exception) {
                // 处理上传失败的情况
                Napier.e("发送文件消息失败", e)
            }
        }
    }

    /**
     * 发送消息
     * @param conversationId 会话
     * @param message 消息
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun sendMessage(conversationId:Long, message:String, type: MessageType = MessageType.TEXT, fileMeta: FileMeta? = null){
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
                        clientMsgId =  Uuid.random().toString()
                    )
                    addMessage(chatMessage)
                }

            } catch (e: Exception) {
                Napier.e("发送消息失败", e)
            }
        }

    }

}