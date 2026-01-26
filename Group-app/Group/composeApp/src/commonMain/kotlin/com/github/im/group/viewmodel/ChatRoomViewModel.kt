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
import com.github.im.group.manager.getFile
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
 * 会话创建状态
 * 
 * 逻辑1: Idle - 闲置状态
 * 逻辑2: Creating - 创建中状态
 * 逻辑3: Pending - 待处理状态
 * 逻辑4: Success - 成功状态
 * 逻辑5: Error - 错误状态
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
 * 
 * 逻辑1: messages - 消息列表
 * 逻辑2: conversation - 会话信息
 * 逻辑3: chatRoom - 聊天室对象
 * 逻辑4: loading - 加载状态
 * 逻辑5: messageIndex - 消息索引
 * 逻辑6: sessionCreationState - 会话创建状态
 * 逻辑7: error - 错误信息
 * 逻辑8: friend - 好友信息
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
 * 
 * 逻辑1: fileId - 文件ID
 * 逻辑2: isDownloading - 是否正在下载
 * 逻辑3: isSuccess - 是否下载成功
 * 逻辑4: progress - 下载进度
 * 逻辑5: error - 错误信息
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
 * 
 * 核心逻辑：
 * 逻辑1: 消息加载 - 优先从本地获取，同时同步远程数据
 * 逻辑2: 消息发送 - 立即显示，异步发送，失败重试
 * 逻辑3: 消息状态管理 - 客户端ID与服务器ID映射
 * 逻辑4: 会话管理 - 创建、注册、注销
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
    
    /**
     * ViewModel被清除时的清理工作
     * 
     * 逻辑1: 注销当前会话，避免内存泄漏
     */
    override fun onCleared() {
        super.onCleared()
        uiState.value.conversation?.conversationId?.let { unregister(it) }
    }


    fun getFile(fileId: String): File?{
        return fileStorageManager.getFile(fileId)
    }

    /**
     * 初始化聊天室
     * 
     * 逻辑1: 更新UI状态，设置聊天室信息
     * 逻辑2: 根据聊天室类型执行不同初始化流程
     * 逻辑3: 加载消息和会话信息
     * 逻辑4: 注册会话监听器
     */
    fun initChatRoom(room: ChatRoom) {
        viewModelScope.launch {
            _uiState.update { it.copy(chatRoom = room, error = null) }
            when (room.type) {
                ChatRoomType.CONVERSATION -> {
                    val conversationId = room.roomId
                    loadMessages(conversationId)
                    loadConversationInfo(conversationId)
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
     * 
     * 逻辑1: 调用核心发送方法发送文本消息
     */
    fun sendText(content: String) {
        performSend(content)
    }

    /**
     * 统一发送入口：语音消息
     * 
     * 逻辑1: 调用核心发送方法发送语音消息
     */
    fun sendVoice(voice: VoiceRecordingResult) {
        performSend(
            content = voice.file.name,
//            type = MessageType.VOICE,
            pickedFile = voice.file ,
            duration = voice.durationMillis
        )
    }

    /**
     * 统一发送入口：文件消息 (包括图片、视频,音频  )
     *  如果是音视频类的 需要 添加时长
     * 逻辑1: 调用核心发送方法发送文件消息
     */
    fun sendFile(file: File,duration: Long = 0) {
//        val type = when {
//            file?.mimeType?.startsWith("image/") -> MessageType.IMAGE
//            file?.mimeType?.startsWith("video/") == true -> MessageType.VIDEO
//            else -> MessageType.FILE
//        }

        performSend(content = file.name, pickedFile = file,duration=duration)
//        performSend(content = file.name/, type = type, pickedFile = file)
    }

    /**
     * 核心发送逻辑 (Offline-First)
     * 
     * 逻辑1: 生成 ClientMsgId 并立即插入 UI，提供即时反馈
     * 逻辑2: 存入离线消息库，确保离线时也能重发
     * 逻辑3: 尝试发送消息，若 Session 未建立则启动建立流程
     * 逻辑4: 处理发送失败的情况，加入离线重试队列
     */
    private fun performSend(
        content: String,
//        type: MessageType,
        pickedFile: File? = null,
        duration: Long = 0
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
                    chatMessageBuilder.fileMessage(targetConversationId, pickedFile.name, pickedFile.size,duration)
                }
//                voiceResult != null -> {
//                    chatMessageBuilder.fileMessage(targetConversationId, voiceResult.file.name, voiceResult.file.size, voiceResult.durationMillis)
//                }
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
                filePath = pickedFile?.path ,
                fileSize = pickedFile?.size,
                fileDuration = duration.toInt()
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
                pickedFile?.let { file ->
                    // 获取文件数据并上传
                    val data = filePicker.readFileBytes(file)
                    val fileUploadResponse = fileUploadService.uploadFileData(
                        serverFileId = messageItem.content, // 使用消息内容字段作为文件ID
                        data = data,
                        fileName = file.name,
                        duration = duration
                    )

                    // 更新数据库文件记录信息
                    fileUploadResponse.fileMeta?.let { fileMeta ->
                        filesRepository.addOrUpdateFile(fileMeta)
                        
                        // 更新UI上的文件状态
                        // 从数据库获取最新消息并更新，确保包含最新的文件元数据
                        val updatedDbMessage = chatMessageRepository.getMessageByClientMsgId(clientMsgId)
                        updatedDbMessage?.let { msg ->
                            updateOrInsertMessage(msg)
                        }


                        
                        // 文件上传成功后，清理离线消息记录
                        offlineMessageRepository.deleteOfflineMessage(clientMsgId)
                    }

                }
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

    /**
     * 发送文本消息内部方法
     * 
     * 逻辑1: 构建文本消息
     * 逻辑2: 通过SDK发送消息
     */
    private suspend fun sendTextMessageInternal(conversationId: Long, content: String, clientMsgId: String) {
        val chatMessage = chatMessageBuilder.textMessage(conversationId, content)
        senderSdk.sendMessage(chatMessage)
    }

    /**
     * 发送语音消息内部方法
     * 
     * 逻辑1: 调用文件消息发送方法处理语音消息
     */
    private suspend fun sendVoiceMessageInternal(conversationId: Long, voice: VoiceRecordingResult, clientMsgId: String) {
        sendFileMessageInternal(conversationId, voice.file, MessageType.VOICE, clientMsgId, voice.durationMillis)
    }

    /**
     * 发送文件消息
     * 
     * 逻辑1: 读取文件字节数据
     * 逻辑2: 构建文件消息
     * 逻辑3: 添加待处理文件到存储管理器
     * 逻辑4: 发送消息信令
     * 逻辑5: 上传文件数据
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
    /**
     * 从本地加载消息
     * 
     * 逻辑1: 从数据库获取指定会话的消息
     * 逻辑2: 应用消息到UI
     */
    fun loadLocalMessages(conversationId: Long, limit: Long = 30) {
        val messages = chatMessageRepository.getMessagesByConversation(conversationId, limit)
        applyMessages(messages)
    }

    /**
     * 加载消息 - 优先从本地获取，同时同步远程数据
     * 
     * 逻辑1: 优先从本地数据库加载消息，立即显示给用户
     * 逻辑2: 同步远程消息以确保数据最新
     * 逻辑3: 在同步期间显示加载动画
     * 逻辑4: 完成后更新UI状态
     */
    fun loadMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            // 优先从本地加载数据
            val localMessages = chatMessageRepository.getMessagesByConversation(conversationId, limit)
            applyMessages(localMessages)
            
            // 总是从远程获取最新数据，以确保数据是最新的
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

    /**
     * 同步远程消息
     * 
     * 逻辑1: 设置加载状态
     * 逻辑2: 从远程同步消息
     * 逻辑3: 如果有新消息则更新本地数据
     * 逻辑4: 清除加载状态
     */
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

    /**
     * 加载好友信息
     * 
     * 逻辑1: 通过用户ID获取好友信息
     * 逻辑2: 更新UI状态中的好友信息
     * 逻辑3: 处理获取失败的情况
     */
    private suspend fun loadFriendInfo(friendUserId: Long) {
        try {
            val friend = getUserById(friendUserId)
            _uiState.update { it.copy(friend = friend) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "获取好友信息失败") }
        }
    }

    /**
     * 加载会话信息
     * 
     * 逻辑1: 从本地获取会话信息
     * 逻辑2: 更新UI状态
     * 逻辑3: 从远程获取会话信息
     * 逻辑4: 处理私聊会话信息
     */
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

    /**
     * 处理私聊会话信息
     * 
     * 逻辑1: 检查会话类型是否为私聊
     * 逻辑2: 获取另一个参与者（非当前用户）
     * 逻辑3: 更新UI状态中的好友信息
     */
    private fun handlePrivateChatInfo(conversation: ConversationRes, currentInfo: UserInfo) {
        if (conversation.type == ConversationType.PRIVATE_CHAT) {
            val friend = conversation.members.firstOrNull { it.userId != currentInfo.userId }
            _uiState.update { it.copy(friend = friend) }
        }
    }

    /**
     * 处理消息确认（ACK）
     * 
     * 逻辑1: 根据客户端消息ID获取消息
     * 逻辑2: 更新消息状态
     * 逻辑3: 从离线消息库中删除已确认的消息
     */
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

    /**
     * 接收到新消息时的处理
     * 
     * 逻辑1: 将新消息添加或更新到本地存储和UI
     */
    fun onReceiveMessage(message: MessageItem) {
        updateOrInsertMessage(message)
    }

    /**
     * 注册会话监听器
     * 
     * 逻辑1: 将当前ViewModel注册到会话管理器
     */
    fun register(conversationId: Long) {
        chatSessionManager.register(conversationId, this)
    }

    /**
     * 注销会话监听器
     * 
     * 逻辑1: 从会话管理器中注销当前会话
     */
    fun unregister(conversationId: Long) {
        chatSessionManager.unregister(conversationId)
    }

    /**
     * 更新或插入消息到本地存储和内存缓存
     * 
     * 逻辑1: 插入或更新消息到数据库
     * 逻辑2: 生成消息唯一键
     * 逻辑3: 从内存缓存中移除旧的客户端消息（如果有）
     * 逻辑4: 将消息添加到内存缓存
     * 逻辑5: 发出UI更新事件
     */
    private fun updateOrInsertMessage(message: MessageItem) {
        chatMessageRepository.insertOrUpdateMessage(message)
        val key = message.uniqueKey()
        if (message.seqId != 0L && message.clientMsgId.isNotBlank()) {
            messageStore.remove("C:${message.clientMsgId}")
        }
        messageStore[key] = message
        emitUiMessages()
    }

    /**
     * 生成消息唯一键
     * 
     * 逻辑1: 优先使用序列ID（服务器分配）
     * 逻辑2: 其次使用客户端消息ID
     * 逻辑3: 最后生成临时键（随机数）
     */
    private fun MessageItem.uniqueKey(): String = when {
        seqId != 0L -> "S:$seqId"
        clientMsgId.isNotBlank() -> "C:$clientMsgId"
        else -> "T:${kotlin.random.Random.nextInt()}"
    }

    /**
     * 应用消息列表到内存缓存
     * 
     * 逻辑1: 将消息列表中的每条消息添加到内存缓存
     * 逻辑2: 发出UI更新事件
     */
    private fun applyMessages(messages: List<MessageItem>) {
        messages.forEach { msg -> messageStore[msg.uniqueKey()] = msg }
        emitUiMessages()
    }

    /**
     * 发出UI消息更新事件
     * 
     * 逻辑1: 按序列ID降序排列消息（未确认消息排在最前面）
     * 逻辑2: 过滤出媒体消息（图片和视频）
     * 逻辑3: 更新UI状态中的消息列表
     */
    private fun emitUiMessages() {
        val sorted = messageStore.values.sortedByDescending { it.seqId.takeIf { it != 0L } ?: Long.MAX_VALUE }
        _mediaMessages.value = sorted.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }
        _uiState.update { it.copy(messages = sorted) }
    }

    /**
     * 刷新消息
     * 
     * 逻辑1: 获取当前会话ID
     * 逻辑2: 使用新的loadMessages方法刷新消息
     */
    fun refreshMessages() {
        uiState.value.conversation?.conversationId?.let { loadMessages(it) }
    }

    /**
     * 加载最新消息（通常是向上滚动时加载历史消息）
     * 
     * 逻辑1: 设置加载状态
     * 逻辑2: 从指定序列ID之后获取消息
     * 逻辑3: 应用新获取的消息
     * 逻辑4: 清除加载状态
     */
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

    /**
     * 根据用户ID获取用户信息
     * 
     * 逻辑1: 优先从本地仓库获取用户信息
     * 逻辑2: 如果本地不存在则从远程API获取
     * 逻辑3: 将远程获取的信息保存到本地仓库
     */
    suspend fun getUserById(userId: Long): UserInfo? {
        var user = userRepository.getUserById(userId)
        if (user != null) return user
        return try {
            user = UserApi.getUserBasicInfo(userId)
            userRepository.addOrUpdateUser(user)
            user
        } catch (e: Exception) { null }
    }

    /**
     * 下载文件消息
     * 
     * 逻辑1: 更新文件下载状态为正在下载
     * 逻辑2: 执行文件下载并跟踪进度
     * 逻辑3: 更新下载完成状态
     * 逻辑4: 处理下载异常
     */
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

    /**
     * 获取文件本地路径
     * 
     * 逻辑1: 通过文件ID从文件存储管理器获取本地路径
     */
    fun getLocalFilePath(fileId: String): String? = fileStorageManager.getLocalFilePath(fileId)
    
    /**
     * 异步获取文件消息元数据
     * 
     * 逻辑1: 检查消息是否为文件类型
     * 逻辑2: 优先从消息对象中获取已有元数据
     * 逻辑3: 从本地仓库获取元数据
     * 逻辑4: 从远程API获取元数据
     * 逻辑5: 将元数据保存到本地仓库
     */
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

    /**
     * 清除会话创建错误
     * 
     * 逻辑1: 重置会话创建状态为闲置
     * 逻辑2: 清除错误信息
     */
    fun clearSessionCreationError() {
        _uiState.update { it.copy(sessionCreationState = SessionCreationState.Idle, error = null) }
    }

    /**
     * 使用离线消息初始化
     * 
     * 逻辑1: 处理所有待处理的离线消息
     */
    private fun initializeWithOfflineMessages() {
        processOfflineMessages()
    }

    /**
     * 处理离线消息
     * 
     * 逻辑1: 获取待处理的离线消息
     * 逻辑2: 遍历每条消息并执行重发逻辑
     * 
     * 注意: 目前仅获取消息，具体重发逻辑待实现
     */
    fun processOfflineMessages() {
        viewModelScope.launch {
            val pending = offlineMessageRepository.getPendingOfflineMessages()
            pending.forEach { msg ->
                // 这里应实现具体的离线重试逻辑，调用 performSend 类似的工作流
            }
        }
    }

    /**
     * 获取或创建私聊会话
     * 
     * 逻辑1: 从本地仓库查找现有会话
     * 逻辑2: 如果找到现有会话则直接返回
     * 逻辑3: 如果未找到则通过API创建新会话
     * 逻辑4: 保存新会话到本地仓库
     * 逻辑5: 更新UI状态
     */
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
