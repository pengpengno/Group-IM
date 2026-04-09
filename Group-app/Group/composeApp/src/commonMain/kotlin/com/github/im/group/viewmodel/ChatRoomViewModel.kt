package com.github.im.group.viewmodel

import ChatMessageBuilder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationType
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.api.UserApi
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.FileUploadService
import com.github.im.group.manager.MessageHandler
import com.github.im.group.manager.MessageRouter
import com.github.im.group.manager.getFile
import com.github.im.group.manager.getLocalFilePath
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.model.toUserInfo
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.OfflineMessageRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecordingResult
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.ChatRoomType
import db.OfflineMessage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SessionCreationState {
    object Idle : SessionCreationState()
    object Creating : SessionCreationState()
    object Pending : SessionCreationState()
    object Success : SessionCreationState()
    object Error : SessionCreationState()
}

data class ChatUiState(
    val messages: List<MessageItem> = emptyList(),
    val conversation: ConversationRes? = null,
    val chatRoom: ChatRoom? = null,
    val loading: Boolean = false,
    val messageIndex: Int = 0,
    val sessionCreationState: SessionCreationState = SessionCreationState.Idle,
    val error: String? = null,
    val friend: UserInfo? = null,
    val scrollToTop: Boolean = false,
) {
    fun hasCreateConversation(): Boolean = conversation != null

    fun getRoomName(): String {
        return conversation?.let {
            if (it.conversationType == ConversationType.PRIVATE_CHAT) {
                friend?.username ?: it.groupName
            } else {
                it.groupName
            }
        } ?: friend?.username.orEmpty()
    }
}

data class FileDownloadState(
    val fileId: String,
    val isDownloading: Boolean = false,
    val isSuccess: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
)

class ChatRoomViewModel(
    val userRepository: UserRepository,
    val chatSessionManager: MessageRouter,
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
) : ViewModel(), MessageHandler {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())
    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()

    private val messageStore = linkedMapOf<String, MessageItem>()
    private var activeConversationId: Long? = null

    private val _mediaMessages = MutableStateFlow<List<MessageItem>>(emptyList())
    val mediaMessages: StateFlow<List<MessageItem>> = _mediaMessages.asStateFlow()

    init {
        processOfflineMessages()
    }

    override fun onMessageReceived(message: MessageWrapper) {
        onReceiveMessage(message)
    }

    override fun onCleared() {
        super.onCleared()
        uiState.value.conversation?.conversationId?.let(::unregister)
    }

    fun getFile(fileId: String): File? = fileStorageManager.getFile(fileId)

    fun initChatRoom(room: ChatRoom) {
        viewModelScope.launch {
            _uiState.update { it.copy(chatRoom = room, error = null) }
            when (room.type) {
                ChatRoomType.CONVERSATION -> {
                    prepareConversation(room.roomId)
                    loadMessages(room.roomId)
                    loadConversationInfo(room.roomId)
                    register(room.roomId)
                }

                ChatRoomType.CREATE_PRIVATE -> {
                    val currentUser = userRepository.getLocalUserInfo() ?: return@launch
                    val existingConversation = conversationRepository
                        .getLocalConversationByMembers(currentUser.userId, room.roomId)

                    if (existingConversation != null) {
                        prepareConversation(existingConversation.conversationId)
                        _uiState.update { it.copy(conversation = existingConversation) }
                        loadMessages(existingConversation.conversationId)
                        loadConversationInfo(existingConversation.conversationId)
                        register(existingConversation.conversationId)
                    } else {
                        loadFriendInfo(room.roomId)
                    }
                }
            }
        }
    }

    fun sendText(content: String) {
        if (content.isBlank()) return
        performSend(content = content.trim())
    }

    fun sendVoice(voice: VoiceRecordingResult) {
        performSend(content = voice.file.name, pickedFile = voice.file, duration = voice.durationMillis)
    }

    fun sendFile(file: File, duration: Long = 0) {
        performSend(content = file.name, pickedFile = file, duration = duration)
    }

    private fun performSend(
        content: String,
        pickedFile: File? = null,
        duration: Long = 0
    ) {
        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            val currentConversationId = uiState.value.conversation?.conversationId
            val friendId = uiState.value.friend?.userId

            val targetConversationId = if (currentConversationId == null && friendId != null) {
                getOrCreatePrivateChat(currentUser.userId, friendId).conversationId
            } else {
                currentConversationId ?: return@launch
            }

            prepareConversation(targetConversationId)

            val outbound = when {
                pickedFile != null -> chatMessageBuilder.fileMessage(targetConversationId, pickedFile.name, pickedFile.size, duration)
                else -> chatMessageBuilder.textMessage(targetConversationId, content)
            }

            val messageItem = MessageWrapper(message = outbound)
            updateOrInsertMessage(messageItem, scrollToLatest = true)

            offlineMessageRepository.saveOfflineMessage(
                clientMsgId = messageItem.clientMsgId,
                conversationId = targetConversationId,
                fromUserId = currentUser.userId,
                toUserId = friendId,
                content = messageItem.content,
                messageType = messageItem.type,
                filePath = pickedFile?.path,
                fileSize = pickedFile?.size,
                fileDuration = duration.toInt()
            )

            sendPreparedMessage(
                currentUser = currentUser,
                messageItem = messageItem,
                pickedFile = pickedFile,
                duration = duration,
                toUserId = friendId,
                persistOffline = false,
                scrollToLatest = true
            )
        }
    }

    private suspend fun sendPreparedMessage(
        currentUser: UserInfo,
        messageItem: MessageWrapper,
        pickedFile: File?,
        duration: Long,
        toUserId: Long?,
        persistOffline: Boolean,
        scrollToLatest: Boolean = false
    ) {
        val clientMsgId = messageItem.clientMsgId

        if (persistOffline) {
            offlineMessageRepository.saveOfflineMessage(
                clientMsgId = clientMsgId,
                conversationId = messageItem.conversationId,
                fromUserId = currentUser.userId,
                toUserId = toUserId,
                content = messageItem.content,
                messageType = messageItem.type,
                filePath = pickedFile?.path,
                fileSize = pickedFile?.size,
                fileDuration = duration.toInt()
            )
        }

        offlineMessageRepository.updateOfflineMessageConversationId(clientMsgId, messageItem.conversationId)
        offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.SENDING)

        try {
            if (pickedFile != null && filesRepository.getFile(messageItem.content) == null) {
                filesRepository.addPendingFileRecord(
                    fileId = messageItem.content,
                    fileName = pickedFile.name,
                    duration = duration,
                    filePath = pickedFile.path
                )
            }

            senderSdk.sendMessage(messageItem.message ?: buildResendChatMessage(messageItem, currentUser))

            if (pickedFile != null) {
                uploadAttachment(messageItem, pickedFile, duration, scrollToLatest)
            }
        } catch (e: Exception) {
            markMessageAsFailed(clientMsgId, messageItem.content)
            offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.FAILED, incrementRetry = true)
            Napier.e("send message failed: $clientMsgId", e)
        }
    }

    private suspend fun uploadAttachment(
        messageItem: MessageWrapper,
        file: File,
        duration: Long,
        scrollToLatest: Boolean
    ) {
        try {
            val data = filePicker.readFileBytes(file)
            val response = fileUploadService.uploadFileData(
                serverFileId = messageItem.content,
                data = data,
                fileName = file.name,
                duration = duration
            )

            response.fileMeta?.let { filesRepository.addOrUpdateFile(it) }
            chatMessageRepository.getMessageByClientMsgId(messageItem.clientMsgId)?.let {
                updateOrInsertMessage(it, scrollToLatest)
            }
            clearOfflineMessageIfReady(messageItem.clientMsgId)
        } catch (e: Exception) {
            markMessageAsFailed(messageItem.clientMsgId, messageItem.content)
            offlineMessageRepository.updateOfflineMessageStatus(messageItem.clientMsgId, MessageStatus.FAILED, incrementRetry = true)
            Napier.e("upload attachment failed: ${messageItem.clientMsgId}", e)
        }
    }

    private fun buildResendChatMessage(
        messageItem: MessageItem,
        currentUser: UserInfo
    ): com.github.im.common.connect.model.proto.ChatMessage {
        return com.github.im.common.connect.model.proto.ChatMessage(
            content = messageItem.content,
            conversationId = messageItem.conversationId,
            fromUser = currentUser.toUserInfo(),
            type = com.github.im.common.connect.model.proto.MessageType.valueOf(messageItem.type.name),
            messagesStatus = com.github.im.common.connect.model.proto.MessagesStatus.SENDING,
            clientTimeStamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            clientMsgId = messageItem.clientMsgId
        )
    }

    private fun buildOfflineRetryMessage(
        offlineMessage: OfflineMessage,
        currentUser: UserInfo
    ): MessageWrapper {
        val message = com.github.im.common.connect.model.proto.ChatMessage(
            content = offlineMessage.content,
            conversationId = offlineMessage.conversation_id ?: 0L,
            fromUser = currentUser.toUserInfo(),
            type = com.github.im.common.connect.model.proto.MessageType.valueOf(offlineMessage.message_type.name),
            messagesStatus = com.github.im.common.connect.model.proto.MessagesStatus.SENDING,
            clientTimeStamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            clientMsgId = offlineMessage.client_msg_id
        )
        return MessageWrapper(message = message)
    }

    private fun buildOfflineRetryFile(offlineMessage: OfflineMessage): File? {
        val path = offlineMessage.file_path ?: return null
        val fileName = path.substringAfterLast('/').substringAfterLast('\\').ifBlank { offlineMessage.content }
        return File(
            name = fileName,
            path = path,
            mimeType = null,
            size = offlineMessage.file_size ?: 0L,
            data = FileData.Path(path)
        )
    }

    private fun prepareConversation(conversationId: Long) {
        if (activeConversationId != conversationId) {
            activeConversationId = conversationId
            messageStore.clear()
            _mediaMessages.value = emptyList()
            _uiState.update { it.copy(messages = emptyList(), messageIndex = -1, scrollToTop = false) }
        }
    }

    fun loadLocalMessages(conversationId: Long, limit: Long = 30) {
        prepareConversation(conversationId)
        applyMessages(chatMessageRepository.getMessagesByConversation(conversationId, limit))
    }

    fun loadMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            prepareConversation(conversationId)
            applyMessages(chatMessageRepository.getMessagesByConversation(conversationId, limit))

            _uiState.update { it.copy(loading = true) }
            try {
                val newMessageCount = withContext(Dispatchers.Default) {
                    messageSyncRepository.syncMessages(conversationId)
                }
                if (newMessageCount > 0) {
                    applyMessages(chatMessageRepository.getMessagesByConversation(conversationId, limit))
                }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun syncRemoteMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val newMessageCount = withContext(Dispatchers.Default) {
                    messageSyncRepository.syncMessages(conversationId)
                }
                if (newMessageCount > 0) {
                    prepareConversation(conversationId)
                    applyMessages(chatMessageRepository.getMessagesByConversation(conversationId, limit))
                }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun loadFriendInfo(friendUserId: Long) {
        try {
            _uiState.update { it.copy(friend = getUserById(friendUserId)) }
        } catch (_: Exception) {
            _uiState.update { it.copy(error = "获取好友信息失败") }
        }
    }

    private suspend fun loadConversationInfo(conversationId: Long) {
        val currentUser = userRepository.getLocalUserInfo() ?: return
        conversationRepository.getLocalConversation(conversationId)?.let { local ->
            _uiState.update { it.copy(conversation = local) }
            handlePrivateChatInfo(local, currentUser)
        }

        try {
            val remote = withContext(Dispatchers.Default) {
                conversationRepository.getConversation(conversationId)
            }
            _uiState.update { it.copy(conversation = remote) }
            handlePrivateChatInfo(remote, currentUser)
        } catch (e: Exception) {
            Napier.w("load conversation info failed: ${e.message}")
        }
    }

    private fun handlePrivateChatInfo(conversation: ConversationRes, currentInfo: UserInfo) {
        if (conversation.conversationType == ConversationType.PRIVATE_CHAT) {
            val friend = conversation.members.firstOrNull { it.userId != currentInfo.userId }
            _uiState.update { it.copy(friend = friend) }
        }
    }

    fun handleMessageAck(clientMsgId: String) {
        viewModelScope.launch {
            val updated = markMessageAsSent(clientMsgId)
            if (updated != null) {
                clearOfflineMessageIfReady(clientMsgId)
            }
        }
    }

    fun onReceiveMessage(message: MessageItem) {
        if (message.conversationId > 0) {
            prepareConversation(message.conversationId)
        }
        updateOrInsertMessage(message)
    }

    fun register(conversationId: Long) {
        chatSessionManager.registerHandler(conversationId, this)
    }

    fun unregister(conversationId: Long) {
        chatSessionManager.unregisterHandler(conversationId)
    }

    private fun updateOrInsertMessage(message: MessageItem, scrollToLatest: Boolean = false) {
        chatMessageRepository.insertOrUpdateMessage(message)
        if (message.seqId != 0L && message.clientMsgId.isNotBlank()) {
            messageStore.remove("C:${message.clientMsgId}")
        }
        messageStore[message.uniqueKey()] = message
        emitUiMessages(scrollToLatest)
    }

    private fun MessageItem.uniqueKey(): String = when {
        seqId != 0L -> "S:$seqId"
        clientMsgId.isNotBlank() -> "C:$clientMsgId"
        else -> "T:${kotlin.random.Random.nextLong()}"
    }

    private fun applyMessages(messages: List<MessageItem>) {
        messages.forEach { messageStore[it.uniqueKey()] = it }
        emitUiMessages()
    }

    private fun emitUiMessages(scrollToLatest: Boolean = false) {
        val sorted = messageStore.values
            .sortedWith(
                compareByDescending<MessageItem> { if (it.seqId != 0L) it.seqId else Long.MIN_VALUE }
                    .thenByDescending { it.clientTime ?: it.time }
            )

        _mediaMessages.value = sorted.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }
        _uiState.update {
            it.copy(
                messages = sorted,
                messageIndex = if (sorted.isNotEmpty()) 0 else -1,
                scrollToTop = scrollToLatest
            )
        }
    }

    private fun markMessageAsFailed(clientMsgId: String, fileId: String? = null) {
        val current = chatMessageRepository.getMessageByClientMsgId(clientMsgId)
        if (current is MessageWrapper) {
            updateOrInsertMessage(current.withStatus(MessageStatus.FAILED))
        }
        if (!fileId.isNullOrBlank()) {
            filesRepository.updateFileStatus(fileId, FileStatus.FAILED)
        }
    }

    private fun markMessageAsSent(clientMsgId: String): MessageItem? {
        val current = chatMessageRepository.getMessageByClientMsgId(clientMsgId)
        return if (current is MessageWrapper) {
            val updated = current.withStatus(MessageStatus.SENT)
            updateOrInsertMessage(updated)
            updated
        } else {
            current
        }
    }

    private fun clearOfflineMessageIfReady(clientMsgId: String) {
        val message = chatMessageRepository.getMessageByClientMsgId(clientMsgId) ?: return
        val delivered = message.status == MessageStatus.SENT ||
            message.status == MessageStatus.READ ||
            message.status == MessageStatus.RECEIVED

        if (!delivered) return

        val uploadCompleted = !message.type.isFile() ||
            filesRepository.getFileMeta(message.content)?.fileStatus == FileStatus.NORMAL

        if (uploadCompleted) {
            offlineMessageRepository.deleteOfflineMessage(clientMsgId)
        } else {
            offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.SENT)
        }
    }

    fun refreshMessages() {
        uiState.value.conversation?.conversationId?.let(::loadMessages)
    }

    fun loadLatestMessages(conversationId: Long, afterSequenceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                prepareConversation(conversationId)
                applyMessages(messageSyncRepository.getMessagesWithStrategy(conversationId, afterSequenceId, false))
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    suspend fun getUserById(userId: Long): UserInfo? {
        userRepository.getUserById(userId)?.let { return it }
        return try {
            val remoteUser = UserApi.getUserBasicInfo(userId)
            userRepository.addOrUpdateUser(remoteUser)
            remoteUser
        } catch (_: Exception) {
            null
        }
    }

    fun downloadFileMessage(fileId: String) {
        viewModelScope.launch {
            _fileDownloadStates.update { it + (fileId to FileDownloadState(fileId, isDownloading = true)) }
            try {
                val path = fileStorageManager.getFileContentPathWithProgress(fileId) { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    _fileDownloadStates.update { current ->
                        val item = current[fileId] ?: FileDownloadState(fileId)
                        current + (fileId to item.copy(progress = progress))
                    }
                }
                _fileDownloadStates.update { current ->
                    val item = current[fileId] ?: FileDownloadState(fileId)
                    current + (fileId to item.copy(isDownloading = false, isSuccess = path != null))
                }
            } catch (e: Exception) {
                _fileDownloadStates.update { current ->
                    val item = current[fileId] ?: FileDownloadState(fileId)
                    current + (fileId to item.copy(isDownloading = false, error = e.message))
                }
            }
        }
    }

    fun getLocalFilePath(fileId: String): String? = fileStorageManager.getLocalFilePath(fileId)

    suspend fun getFileMessageMetaAsync(messageItem: MessageItem): FileMeta? {
        if (!messageItem.type.isFile()) return null
        messageItem.fileMeta?.let { return it }
        filesRepository.getFileMeta(messageItem.content)?.let { return it }
        return try {
            val remote = FileApi.getFileMeta(messageItem.content)
            filesRepository.addOrUpdateFile(remote)
            remote
        } catch (_: Exception) {
            null
        }
    }

    fun clearSessionCreationError() {
        _uiState.update { it.copy(sessionCreationState = SessionCreationState.Idle, error = null) }
    }

    fun processOfflineMessages() {
        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            val pendingMessages = offlineMessageRepository.getPendingOfflineMessages()
            pendingMessages.forEach { offline ->
                val conversationId = offline.conversation_id ?: return@forEach
                val retryCount = offline.retry_count ?: 0L
                val maxRetryCount = offline.max_retry_count ?: 3L
                if (retryCount >= maxRetryCount) {
                    offlineMessageRepository.updateOfflineMessageStatus(offline.client_msg_id, MessageStatus.FAILED)
                    return@forEach
                }

                prepareConversation(conversationId)
                val retryMessage = buildOfflineRetryMessage(offline, currentUser)
                val retryFile = if (offline.message_type.isFile()) buildOfflineRetryFile(offline) else null
                if (!messageStore.containsKey("C:${offline.client_msg_id}")) {
                    updateOrInsertMessage(retryMessage)
                }

                sendPreparedMessage(
                    currentUser = currentUser,
                    messageItem = retryMessage,
                    pickedFile = retryFile,
                    duration = offline.file_duration ?: 0L,
                    toUserId = offline.to_user_id,
                    persistOffline = false
                )
            }
        }
    }

    suspend fun getOrCreatePrivateChat(userId: Long, friendId: Long): ConversationRes {
        conversationRepository.getLocalConversationByMembers(userId, friendId)?.let { local ->
            _uiState.update { it.copy(conversation = local) }
            return local
        }

        val remote = withContext(Dispatchers.Default) {
            ConversationApi.createOrGetConversation(friendId)
        }
        conversationRepository.saveConversation(remote)
        _uiState.update { it.copy(conversation = remote) }
        return remote
    }

    fun updateMessageIndex(index: Int) {
        _uiState.update { it.copy(messageIndex = index) }
    }

    fun resetScrollToTopFlag() {
        _uiState.update { it.copy(scrollToTop = false) }
    }

    fun withdrawMessage(message: MessageItem) {
        viewModelScope.launch {
            try {
                ChatApi.withdrawMessage(message.id)
                if (message is MessageWrapper) {
                    updateOrInsertMessage(message.withStatus(MessageStatus.REVOKE))
                }
            } catch (e: Exception) {
                Napier.e("withdraw message failed", e)
            }
        }
    }

    fun markConversationAsRead(conversationId: Long, currentUserId: Long) {
        viewModelScope.launch {
            try {
                val lastSeq = _uiState.value.messages.firstOrNull()?.seqId ?: 0L
                if (lastSeq <= 0L) return@launch

                chatMessageRepository.markConversationMessagesAsRead(conversationId, currentUserId)
                val updatedMessages = _uiState.value.messages.map { message ->
                    val shouldMarkRead = message.userInfo.userId != currentUserId && message.status == MessageStatus.SENT
                    if (shouldMarkRead && message is MessageWrapper) {
                        val updated = message.withStatus(MessageStatus.READ)
                        messageStore.remove(message.uniqueKey())
                        messageStore[updated.uniqueKey()] = updated
                        updated
                    } else {
                        message
                    }
                }
                _uiState.update { it.copy(messages = updatedMessages) }

                ChatApi.markConversationAsRead(conversationId = conversationId, sequenceId = lastSeq)
            } catch (e: Exception) {
                Napier.e("mark conversation as read failed", e)
            }
        }
    }
}
