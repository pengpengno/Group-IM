package com.github.im.group.viewmodel

import ChatMessageBuilder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationType
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.api.UserApi
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.FileUploadService
import com.github.im.group.manager.getLocalFilePath
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.OfflineMessageRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecordingResult
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.ChatRoomType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * 会话创建状态枚举
 */
sealed class SessionCreationState {
    object Idle : SessionCreationState()
    object Creating : SessionCreationState()
    object Pending : SessionCreationState()
    object Success : SessionCreationState()
    object Error : SessionCreationState()
}

/**
 * 聊天室 UI 状态数据类
 */
data class ChatUiState(
    val messages: List<MessageItem> = emptyList(),
    val conversation: ConversationRes? = null,
    val chatRoom: ChatRoom? = null,
    val loading: Boolean = false,
    val messageIndex: Int = 0,
    val sessionCreationState: SessionCreationState = SessionCreationState.Idle,
    val error: String? = null,
    val friend: UserInfo? = null,
) {
    fun getRoomName(): String {
        return conversation?.let {
            if (it.type == ConversationType.PRIVATE_CHAT) {
                friend?.username ?: it.groupName
            } else {
                it.groupName
            }
        } ?: friend?.username ?: ""
    }
}

/**
 * 文件下载进度状态
 */
data class FileDownloadState(
    val fileId: String,
    val isDownloading: Boolean = false,
    val isSuccess: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

/**
 * ChatRoomViewModel - 聊天室核心业务逻辑管理器
 * 
 * 职责：
 * 1. 管理聊天消息的生命周期（加载、同步、发送、接收、ACK 确认）
 * 2. 处理离线消息与断网重连逻辑 (Offline-First 策略)
 * 3. 统一发送逻辑，屏蔽 UI 层对 Session 状态的判断
 */
class ChatRoomViewModel(
    val userRepository: UserRepository,
    val chatSessionManager: ChatSessionManager,
    val chatMessageRepository: ChatMessageRepository,
    val messageSyncRepository: MessageSyncRepository,
    val filesRepository: FilesRepository,
    val conversationRepository: ConversationRepository,
    val offlineMessageRepository: OfflineMessageRepository,
    val filePicker: FilePicker,
    val fileStorageManager: FileStorageManager,
    val senderSdk: SenderSdk,
    val chatMessageBuilder: ChatMessageBuilder,
    val fileUploadService: FileUploadService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())
    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()
    
    private val messageStore = mutableMapOf<String, MessageItem>()
    private val _mediaMessages = MutableStateFlow<List<MessageItem>>(emptyList())
    val mediaMessages: StateFlow<List<MessageItem>> = _mediaMessages.asStateFlow()
    
    init {
        initializeWithOfflineMessages()
    }
    
    override fun onCleared() {
        super.onCleared()
        uiState.value.conversation?.conversationId?.let { unregister(it) }
    }

    /**
     * 初始化聊天室
     */
    fun initChatRoom(room: ChatRoom) {
        viewModelScope.launch {
            _uiState.update { it.copy(chatRoom = room, error = null) }
            when (room.type) {
                ChatRoomType.CONVERSATION -> {
                    val conversationId = room.roomId
                    loadLocalMessages(conversationId)
                    loadConversationInfo(conversationId)
                    syncRemoteMessages(conversationId)
                    register(conversationId)
                }
                ChatRoomType.CREATE_PRIVATE -> {
                    loadFriendInfo(room.roomId)
                }
            }
        }
    }

    /**
     * 统一发送入口：文本消息
     */
    fun sendText(content: String) {
        performSend(content)
    }

    /**
     * 统一发送入口：语音消息
     */
    fun sendVoice(voice: VoiceRecordingResult) {
        performSend(
            content = voice.file.name,
//            type = MessageType.VOICE,
            voiceResult = voice
        )
    }

    /**
     * 统一发送入口：文件消息 (包括图片、视频)
     */
    fun sendFile(file: File) {
//        val type = when {
//            file?.mimeType?.startsWith("image/") -> MessageType.IMAGE
//            file?.mimeType?.startsWith("video/") == true -> MessageType.VIDEO
//            else -> MessageType.FILE
//        }
        performSend(content = file.name, pickedFile = file)
//        performSend(content = file.name/, type = type, pickedFile = file)
    }

    /**
     * 核心发送逻辑 (Offline-First)
     * 1. 生成 ClientMsgId 并立即插入 UI
     * 2. 存入离线消息库
     * 3. 尝试发送，若 Session 未建立则启动建立流程
     */
    private fun performSend(
        content: String,
//        type: MessageType,
        pickedFile: File? = null,
        voiceResult: VoiceRecordingResult? = null
    ) {
        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            val conversationId = uiState.value.conversation?.conversationId
            val friendId = uiState.value.friend?.userId


            // 如果是私聊且没会话，先创建
            val targetConversationId = if (conversationId == null && friendId != null) {
                getOrCreatePrivateChat(currentUser.userId, friendId).conversationId
            } else {
                //  如果 conversationId 不为空的话，说明已经有会话了
                conversationId!!
            }

            // 1. 构建并展示消息 Item (立即反馈)
            val time = Clock.System.now().toString()

            // 使用chatMessageBuilder 生成 消息
            // 三种类型 文本   、文件、 和 录音
            val chatMessage = when {
                pickedFile != null -> {
                    chatMessageBuilder.fileMessage(targetConversationId, pickedFile.name, pickedFile.size,)
                }
                voiceResult != null -> {
                    chatMessageBuilder.fileMessage(targetConversationId, voiceResult.file.name, voiceResult.file.size, voiceResult.durationMillis)
                }
                else -> {
                    chatMessageBuilder.textMessage(targetConversationId, content)
                }
            }
//

            val messageItem = MessageWrapper(chatMessage)
            updateOrInsertMessage(messageItem)
            val clientMsgId = messageItem.clientMsgId
            val type =  messageItem.type
            // 2. 持久化到离线库
            offlineMessageRepository.saveOfflineMessage(
                clientMsgId = messageItem.clientMsgId,
                conversationId = conversationId,
                fromUserId = currentUser.userId,
                toUserId = friendId,
                content = content,
                messageType = messageItem.type,
                filePath = pickedFile?.path ?: voiceResult?.file?.path,
                fileSize = pickedFile?.size ?: voiceResult?.bytes?.size?.toLong(),
                fileDuration = (voiceResult?.durationMillis ?: 0L).toInt() / 1000
            )

            // 3. 尝试发送
            try {


                // 更新离线库中的会话 ID
                offlineMessageRepository.updateOfflineMessageConversationId(clientMsgId, targetConversationId)


                /**
                 * 直接发送消息
                 */
                senderSdk.sendMessage(chatMessage)


                // 如果是文件类型的  / 录音类型的 ， 还要继续上传文件
//
//
//                // 分类型执行真实发送
//                when (type) {
//                    MessageType.TEXT -> sendTextMessageInternal(targetConversationId, content)
//                    MessageType.VOICE -> sendVoiceMessageInternal(targetConversationId, voiceResult!!, clientMsgId)
//                    MessageType.IMAGE, MessageType.VIDEO, MessageType.FILE ->
//                        sendFileMessageInternal(targetConversationId, pickedFile!!, type, clientMsgId)
//                    else -> {}
//                }
            } catch (e: Exception) {
                Napier.e("发送失败，已进入离线重试队列: $clientMsgId", e)
                // UI 状态依然是 SENDING，由离线重试机制处理
            }
        }
    }

    private suspend fun sendTextMessageInternal(conversationId: Long, content: String, clientMsgId: String) {
        val chatMessage = chatMessageBuilder.textMessage(conversationId, content)
        senderSdk.sendMessage(chatMessage)
    }

    private suspend fun sendVoiceMessageInternal(conversationId: Long, voice: VoiceRecordingResult, clientMsgId: String) {
        sendFileMessageInternal(conversationId, voice.file, MessageType.VOICE, clientMsgId, voice.durationMillis)
    }

    /***
     * 发送文件消息
     */
    private suspend fun sendFileMessageInternal(
        conversationId: Long,
        file: File,
        type: MessageType,
        clientMsgId: String,
        duration: Long = 0
    ) {
        withContext(Dispatchers.IO) {
            val data = filePicker.readFileBytes(file)
            val chatMessage = chatMessageBuilder.fileMessage(conversationId, file.name, file.size, duration)
            val fileId = chatMessage.content
            
            // 1. 占位文件
            fileStorageManager.addPendingFile(fileId, file.name, duration, file.path)
            
            // 2. 发送信令
            senderSdk.sendMessage(chatMessage)
            
            // 3. 上传二进制
            fileUploadService.uploadFileData(fileId, data, file.name, duration)
        }
    }

    /**
     * 加载消息系列
     */
    fun loadLocalMessages(conversationId: Long, limit: Long = 30) {
        val messages = chatMessageRepository.getMessagesByConversation(conversationId, limit)
        applyMessages(messages)
    }

    fun syncRemoteMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val newMessageCount = withContext(Dispatchers.IO) {
                    messageSyncRepository.syncMessages(conversationId)
                }
                if (newMessageCount > 0) {
                    val messages = chatMessageRepository.getMessagesByConversation(conversationId, limit)
                    applyMessages(messages)
                }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun loadFriendInfo(friendUserId: Long) {
        try {
            val friend = getUserById(friendUserId)
            _uiState.update { it.copy(friend = friend) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "获取好友信息失败") }
        }
    }

    private suspend fun loadConversationInfo(conversationId: Long) {
        val local = conversationRepository.getLocalConversation(conversationId)
        val currentInfo = userRepository.getLocalUserInfo()!!
        if (local != null) {
            _uiState.update { it.copy(conversation = local) }
            handlePrivateChatInfo(local, currentInfo)
        }
        try {
            val remote = withContext(Dispatchers.IO) { conversationRepository.getConversation(conversationId) }
            _uiState.update { it.copy(conversation = remote) }
            handlePrivateChatInfo(remote, currentInfo)
        } catch (e: Exception) {
            Napier.w("远程获取会话信息失败")
        }
    }

    private fun handlePrivateChatInfo(conversation: ConversationRes, currentInfo: UserInfo) {
        if (conversation.type == ConversationType.PRIVATE_CHAT) {
            val friend = conversation.members.firstOrNull { it.userId != currentInfo.userId }
            _uiState.update { it.copy(friend = friend) }
        }
    }

    fun handleMessageAck(clientMsgId: String) {
        viewModelScope.launch {
            val updatedMsg = chatMessageRepository.getMessageByClientMsgId(clientMsgId)
            if (updatedMsg != null) {
                updateOrInsertMessage(updatedMsg)
                // 发送成功，清理离线消息
                offlineMessageRepository.deleteOfflineMessage(clientMsgId)
            }
        }
    }

    fun onReceiveMessage(message: MessageItem) {
        updateOrInsertMessage(message)
    }

    fun register(conversationId: Long) {
        chatSessionManager.register(conversationId, this)
    }

    fun unregister(conversationId: Long) {
        chatSessionManager.unregister(conversationId)
    }

    private fun updateOrInsertMessage(message: MessageItem) {
        chatMessageRepository.insertOrUpdateMessage(message)
        val key = message.uniqueKey()
        if (message.seqId != 0L && message.clientMsgId.isNotBlank()) {
            messageStore.remove("C:${message.clientMsgId}")
        }
        messageStore[key] = message
        emitUiMessages()
    }

    private fun MessageItem.uniqueKey(): String = when {
        seqId != 0L -> "S:$seqId"
        clientMsgId.isNotBlank() -> "C:$clientMsgId"
        else -> "T:${kotlin.random.Random.nextInt()}"
    }

    private fun applyMessages(messages: List<MessageItem>) {
        messages.forEach { msg -> messageStore[msg.uniqueKey()] = msg }
        emitUiMessages()
    }

    private fun emitUiMessages() {
        val sorted = messageStore.values.sortedByDescending { it.seqId.takeIf { it != 0L } ?: Long.MAX_VALUE }
        _mediaMessages.value = sorted.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }
        _uiState.update { it.copy(messages = sorted) }
    }

    fun refreshMessages() {
        uiState.value.conversation?.conversationId?.let { syncRemoteMessages(it) }
    }

    fun loadLatestMessages(conversationId: Long, afterSequenceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val messages = messageSyncRepository.getMessagesWithStrategy(conversationId, afterSequenceId, false)
                applyMessages(messages)
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    suspend fun getUserById(userId: Long): UserInfo? {
        var user = userRepository.getUserById(userId)
        if (user != null) return user
        return try {
            user = UserApi.getUserBasicInfo(userId)
            userRepository.addOrUpdateUser(user)
            user
        } catch (e: Exception) { null }
    }

    fun downloadFileMessage(fileId: String) {
        viewModelScope.launch {
            _fileDownloadStates.update { it + (fileId to FileDownloadState(fileId, isDownloading = true)) }
            try {
                val path = fileStorageManager.getFileContentPathWithProgress(fileId) { down, total ->
                    val prog = if (total > 0) down.toFloat() / total.toFloat() else 0f
                    _fileDownloadStates.update { current ->
                        val item = current[fileId] ?: FileDownloadState(fileId)
                        current + (fileId to item.copy(progress = prog))
                    }
                }
                _fileDownloadStates.update { current ->
                    val item = current[fileId] ?: FileDownloadState(fileId)
                    current + (fileId to item.copy(isDownloading = false, isSuccess = path != null))
                }
            } catch (e: Exception) {
                _fileDownloadStates.update { it + (fileId to it[fileId]!!.copy(isDownloading = false, error = e.message)) }
            }
        }
    }

    fun getLocalFilePath(fileId: String): String? = fileStorageManager.getLocalFilePath(fileId)
    
    suspend fun getFileMessageMetaAsync(messageItem: MessageItem): FileMeta? {
        if (!messageItem.type.isFile()) return null
        messageItem.fileMeta?.let { return it }
        filesRepository.getFileMeta(messageItem.content)?.let { return it }
        return try {
            val meta = FileApi.getFileMeta(messageItem.content)
            filesRepository.addOrUpdateFile(meta)
            meta
        } catch (e: Exception) { null }
    }

    fun clearSessionCreationError() {
        _uiState.update { it.copy(sessionCreationState = SessionCreationState.Idle, error = null) }
    }

    private fun initializeWithOfflineMessages() {
        processOfflineMessages()
    }

    fun processOfflineMessages() {
        viewModelScope.launch {
            val pending = offlineMessageRepository.getPendingOfflineMessages()
            pending.forEach { msg ->
                // 这里应实现具体的离线重试逻辑，调用 performSend 类似的工作流
            }
        }
    }

    suspend fun getOrCreatePrivateChat(userId: Long, friendId: Long): ConversationRes {
        val local = conversationRepository.getLocalConversationByMembers(userId, friendId)
        if (local != null) {
            _uiState.update { it.copy(conversation = local) }
            return local
        }
        val remote = withContext(Dispatchers.IO) { ConversationApi.createOrGetConversation(friendId) }
        conversationRepository.saveConversation(remote)
        _uiState.update { it.copy(conversation = remote) }
        return remote
    }
}
