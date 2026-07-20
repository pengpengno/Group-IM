package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.AiBotApi
import com.github.im.group.api.AiBotReplyDto
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationType
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.api.MessageDTO
import com.github.im.group.api.UserApi
import com.github.im.group.bot.AI_ASSISTANT_CONVERSATION_ID
import com.github.im.group.bot.aiAssistantUser
import com.github.im.group.bot.buildAiAssistantConversation
import com.github.im.group.bot.isAiAssistantConversationId
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.ConversationListCoordinator
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.MessageFacade
import com.github.im.group.manager.MessageHandler
import com.github.im.group.manager.MessageRouter
import com.github.im.group.manager.MessageStore
import com.github.im.group.manager.getFile
import com.github.im.group.manager.getLocalFilePath
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.ChatScrollPositionRecord
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.VoiceRecordingResult
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.ChatRoomType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class SessionCreationState {
    data object Idle : SessionCreationState()
    data object Creating : SessionCreationState()
    data object Error : SessionCreationState()
}

data class ChatUiState(
    val room: ChatRoom? = null,
    val conversation: ConversationRes? = null,
    val friend: UserInfo? = null,
    val messages: List<MessageItem> = emptyList(),
    val isBotSession: Boolean = false,
    val isBotResponding: Boolean = false,
    val isInitializing: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val sessionCreationState: SessionCreationState = SessionCreationState.Idle,
    val error: String? = null,
    val scrollToLatestEvent: Long = 0L,
    val savedScrollPosition: ChatScrollPositionRecord? = null
) {
    val canSendMessages: Boolean
        get() = conversation != null && sessionCreationState != SessionCreationState.Creating

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
    val messageStore: MessageStore,
    val messageFacade: MessageFacade,
    val userRepository: UserRepository,
    val chatSessionManager: MessageRouter,
    val conversationRepository: ConversationRepository,
    val fileStorageManager: FileStorageManager,
    val filePicker: FilePicker,
    val filesRepository: FilesRepository,
    val conversationListCoordinator: ConversationListCoordinator
) : ViewModel(), MessageHandler {

    companion object {
        const val BOT_CARD_PREFIX = "[[BOT_CARD]]"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())
    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()

    private var activeConversationId: Long? = null
    private var activeRoomRequestId: Long = 0L
    private var refreshJob: Job? = null
    private var loadHistoryJob: Job? = null
    private var lastHistoryBoundarySeqId: Long? = null

    init {
        viewModelScope.launch {
            userRepository.getLocalUserInfo()?.let { user ->
                messageFacade.startSync(user)
            }
        }

        viewModelScope.launch {
            messageStore.messages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    override fun onMessageReceived(message: MessageWrapper) {
        messageStore.saveOrUpdate(message)
        conversationRepository.markConversationActive(message.conversationId)
        conversationListCoordinator.notifyConversationChanged(
            conversationId = message.conversationId,
            moveToTop = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        uiState.value.conversation?.conversationId?.let(::unregister)
    }

    fun getFile(fileId: String): File? = fileStorageManager.getFile(fileId)

    fun openRoom(room: ChatRoom) {
        val requestId = ++activeRoomRequestId
        refreshJob?.cancel()
        loadHistoryJob?.cancel()
        lastHistoryBoundarySeqId = null

        val previousConversationId = activeConversationId
        if (previousConversationId != null && previousConversationId != room.roomId) {
            unregister(previousConversationId)
        }

        activeConversationId = null
        messageStore.clear()
        _uiState.value = ChatUiState(room = room, isInitializing = true)

        viewModelScope.launch {
            when (room.type) {
                ChatRoomType.CONVERSATION -> {
                    if (isAiAssistantConversationId(room.roomId)) {
                        bindBotConversation(requestId)
                    } else {
                        bindExistingConversation(room.roomId, requestId)
                    }
                }

                ChatRoomType.CREATE_PRIVATE -> preparePrivateConversation(room.roomId, requestId)
                ChatRoomType.BOT -> bindBotConversation(requestId)
            }
        }
    }

    fun sendText(content: String) {
        if (content.isBlank()) return
        if (uiState.value.isBotSession) {
            sendBotText(content.trim())
            return
        }
        val normalized = content.trim()
        performSend(content = normalized)
        if (shouldTriggerGroupBot(normalized)) {
            sendGroupBotReply(normalized)
        }
    }

    fun sendVoice(voice: VoiceRecordingResult) {
        performSend(content = voice.file.name, pickedFile = voice.file, duration = voice.durationMillis)
    }

    fun sendFile(file: File, duration: Long = 0L) {
        performSend(content = file.name, pickedFile = file, duration = duration)
    }

    private fun performSend(
        content: String? = null,
        pickedFile: File? = null,
        duration: Long = 0L
    ) {
        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            val currentConversation = uiState.value.conversation ?: run {
                _uiState.update { it.copy(error = "私聊会话仍在准备中，请稍后再试。") }
                return@launch
            }
            val friendId = uiState.value.friend?.userId
            val targetConversationId = currentConversation.conversationId
            register(targetConversationId)
            conversationRepository.markConversationActive(targetConversationId)

            if (pickedFile != null) {
                messageFacade.sendFile(targetConversationId, pickedFile, duration, currentUser, friendId)
            } else if (content != null) {
                messageFacade.sendText(targetConversationId, content, currentUser, friendId)
            }

            conversationListCoordinator.notifyConversationChanged(
                conversationId = targetConversationId,
                moveToTop = true
            )
            triggerScrollToLatest()
        }
    }

    private fun refreshConversation(
        conversationId: Long,
        showLoading: Boolean,
        requestId: Long = activeRoomRequestId
    ) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (showLoading && isRequestActive(requestId)) {
                _uiState.update { it.copy(isRefreshing = true) }
            }
            try {
                val hasNewMessages = withContext(Dispatchers.Default) {
                    messageStore.syncRemote(conversationId)
                }
                if (hasNewMessages && isConversationActive(conversationId, requestId)) {
                    withContext(Dispatchers.Default) {
                        messageStore.loadLocal(conversationId, 30)
                    }
                }
            } catch (e: Exception) {
                Napier.e("sync messages failed", e)
                if (isConversationActive(conversationId, requestId)) {
                    _uiState.update { it.copy(error = e.message ?: "刷新消息失败") }
                }
            } finally {
                if (showLoading && isConversationActive(conversationId, requestId)) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    private suspend fun loadConversationInfo(
        conversationId: Long,
        requestId: Long = activeRoomRequestId
    ) {
        val currentUser = userRepository.getLocalUserInfo() ?: return
        conversationRepository.getLocalConversation(conversationId)?.let { local ->
            if (!isConversationActive(conversationId, requestId)) return
            updateConversationState(local, _uiState.value.savedScrollPosition, currentUser)
        }

        try {
            val remote = withContext(Dispatchers.Default) {
                conversationRepository.getConversation(conversationId)
            }
            if (!isConversationActive(conversationId, requestId)) return
            updateConversationState(remote, _uiState.value.savedScrollPosition, currentUser)
        } catch (e: Exception) {
            Napier.w("load conversation info failed: ${e.message}")
        }
    }

    private suspend fun updateConversationState(
        conversation: ConversationRes,
        savedPosition: ChatScrollPositionRecord?,
        currentUser: UserInfo?
    ) {
        val friend = if (conversation.conversationType == ConversationType.PRIVATE_CHAT && currentUser != null) {
            conversation.members.firstOrNull { it.userId != currentUser.userId }
        } else {
            _uiState.value.friend
        }

        _uiState.update {
            it.copy(
                conversation = conversation,
                friend = friend,
                savedScrollPosition = savedPosition,
                sessionCreationState = SessionCreationState.Idle,
                error = null
            )
        }
    }

    fun handleMessageAck(clientMsgId: String) {
        messageFacade.handleAck(clientMsgId)
    }

    fun retryMessage(message: MessageItem) {
        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            val friendId = uiState.value.friend?.userId
            messageFacade.retryMessage(
                messageItem = message,
                currentUser = currentUser,
                toUserId = friendId
            )
        }
    }

    fun register(conversationId: Long) {
        chatSessionManager.registerHandler(conversationId, this)
    }

    fun unregister(conversationId: Long) {
        chatSessionManager.unregisterHandler(conversationId)
    }

    fun refreshMessages() {
        if (uiState.value.isBotSession) return
        activeConversationId?.let { refreshConversation(it, showLoading = true) }
    }

    fun loadOlderMessages(beforeSequenceId: Long) {
        if (uiState.value.isBotSession) return
        val conversationId = activeConversationId ?: return
        if (beforeSequenceId <= 0L || lastHistoryBoundarySeqId == beforeSequenceId) {
            return
        }

        loadHistoryJob?.cancel()
        loadHistoryJob = viewModelScope.launch {
            lastHistoryBoundarySeqId = beforeSequenceId
            _uiState.update { it.copy(isLoadingHistory = true) }
            try {
                withContext(Dispatchers.Default) {
                    messageStore.loadHistoryBefore(conversationId, beforeSequenceId)
                }
            } catch (e: Exception) {
                lastHistoryBoundarySeqId = null
                Napier.e("load older messages failed", e)
            } finally {
                _uiState.update { it.copy(isLoadingHistory = false) }
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

    fun getCachedFileMeta(fileId: String): FileMeta? = filesRepository.getFileMeta(fileId)

    suspend fun getFileMessageMetaAsync(messageItem: MessageItem): FileMeta? {
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retryPreparePrivateChat() {
        uiState.value.room?.takeIf { it.type == ChatRoomType.CREATE_PRIVATE }?.let(::openRoom)
    }

    fun triggerScrollToLatest() {
        _uiState.update { it.copy(scrollToLatestEvent = it.scrollToLatestEvent + 1) }
    }

    fun saveReadingPosition(
        conversationId: Long,
        anchorMessage: MessageItem?,
        scrollOffset: Int
    ) {
        conversationRepository.saveChatScrollPosition(
            conversationId = conversationId,
            anchorMsgId = anchorMessage?.id?.takeIf { it > 0L },
            anchorSeqId = anchorMessage?.seqId ?: 0L,
            anchorClientMsgId = anchorMessage?.clientMsgId?.takeIf { it.isNotBlank() },
            scrollOffset = scrollOffset.coerceAtLeast(0)
        )
    }

    fun withdrawMessage(message: MessageItem) {
        if (isAiAssistantConversationId(message.conversationId)) return
        viewModelScope.launch {
            try {
                ChatApi.withdrawMessage(message.id)
                if (message is MessageWrapper) {
                    val updated = message.withStatus(MessageStatus.REVOKE)
                    messageStore.saveOrUpdate(updated)
                }
            } catch (e: Exception) {
                Napier.e("withdraw message failed", e)
            }
        }
    }

    fun markConversationAsRead(conversationId: Long, currentUserId: Long) {
        viewModelScope.launch {
            try {
                if (uiState.value.isBotSession) {
                    messageStore.markConversationRead(conversationId, currentUserId)
                    conversationListCoordinator.notifyConversationChanged(conversationId)
                    return@launch
                }
                val lastSeq = _uiState.value.messages.firstOrNull()?.seqId ?: 0L
                if (lastSeq <= 0L) return@launch

                messageStore.markConversationRead(conversationId, currentUserId)
                ChatApi.markConversationAsRead(conversationId = conversationId, sequenceId = lastSeq)
                conversationListCoordinator.notifyConversationChanged(conversationId)
            } catch (e: Exception) {
                Napier.e("mark conversation as read failed", e)
            }
        }
    }

    private suspend fun bindConversation(
        conversationId: Long,
        requestId: Long,
        initialConversation: ConversationRes? = null
    ) {
        if (!isRequestActive(requestId)) return

        activeConversationId = conversationId
        register(conversationId)
        conversationRepository.markConversationActive(conversationId)
        val savedPosition = conversationRepository.getChatScrollPosition(conversationId)
        val currentUser = userRepository.getLocalUserInfo()

        initialConversation?.let { conversation ->
            updateConversationState(conversation, savedPosition, currentUser)
        } ?: _uiState.update {
            it.copy(
                conversation = null,
                savedScrollPosition = savedPosition
            )
        }

        withContext(Dispatchers.Default) {
            messageStore.loadLocal(conversationId, 30)
        }

        _uiState.update {
            it.copy(
                isInitializing = false,
                sessionCreationState = SessionCreationState.Idle
            )
        }

        viewModelScope.launch {
            loadConversationInfo(conversationId, requestId)
        }
        refreshConversation(conversationId, showLoading = false, requestId = requestId)
        conversationListCoordinator.notifyConversationChanged(
            conversationId = conversationId,
            moveToTop = true
        )
    }

    private suspend fun preparePrivateConversation(friendId: Long, requestId: Long) {
        val currentUser = userRepository.getLocalUserInfo() ?: return
        try {
            _uiState.update {
                it.copy(
                    friend = getUserById(friendId),
                    sessionCreationState = SessionCreationState.Creating,
                    isInitializing = true,
                    error = null
                )
            }
            val conversation = conversationRepository.getLocalConversationByMembers(currentUser.userId, friendId)
                ?: withContext(Dispatchers.Default) {
                    ConversationApi.createOrGetConversation(friendId)
                }.also { remote ->
                    conversationRepository.saveConversation(remote)
                }

            bindConversation(conversation.conversationId, requestId, conversation)
        } catch (e: Exception) {
            Napier.e("prepare private conversation failed", e)
            if (!isRequestActive(requestId)) return
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    sessionCreationState = SessionCreationState.Error,
                    error = "创建私聊会话失败：${e.message ?: "未知错误"}"
                )
            }
        }
    }

    private suspend fun bindExistingConversation(conversationId: Long, requestId: Long) {
        val localConversation = conversationRepository.getLocalConversation(conversationId)
        bindConversation(conversationId, requestId, localConversation)
    }

    private suspend fun bindBotConversation(requestId: Long) {
        if (!isRequestActive(requestId)) return
        val currentUser = userRepository.getLocalUserInfo() ?: return
        val assistantConversation = buildAiAssistantConversation(currentUser)
        conversationRepository.saveConversation(assistantConversation)
        conversationRepository.markConversationActive(AI_ASSISTANT_CONVERSATION_ID)
        activeConversationId = AI_ASSISTANT_CONVERSATION_ID
        val savedPosition = conversationRepository.getChatScrollPosition(AI_ASSISTANT_CONVERSATION_ID)
        withContext(Dispatchers.Default) {
            messageStore.loadLocal(AI_ASSISTANT_CONVERSATION_ID, 50)
        }
        _uiState.update {
            it.copy(
                conversation = assistantConversation,
                friend = aiAssistantUser(),
                isBotSession = true,
                isBotResponding = false,
                isInitializing = false,
                sessionCreationState = SessionCreationState.Idle,
                savedScrollPosition = savedPosition,
                error = null
            )
        }
        conversationListCoordinator.notifyConversationChanged(
            conversationId = AI_ASSISTANT_CONVERSATION_ID,
            moveToTop = false
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun sendBotText(content: String) {
        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            conversationRepository.saveConversation(buildAiAssistantConversation(currentUser))
            conversationRepository.markConversationActive(AI_ASSISTANT_CONVERSATION_ID)

            val userMessage = createLocalTextMessage(
                conversationId = AI_ASSISTANT_CONVERSATION_ID,
                sender = currentUser,
                content = content
            )
            messageStore.saveOrUpdate(userMessage)
            _uiState.update { it.copy(isBotResponding = true, error = null) }
            conversationListCoordinator.notifyConversationChanged(
                conversationId = AI_ASSISTANT_CONVERSATION_ID,
                moveToTop = true
            )
            triggerScrollToLatest()

            runCatching {
                AiBotApi.sendMessage(
                    content = content,
                    conversationId = AI_ASSISTANT_CONVERSATION_ID,
                    fromAccountId = currentUser.userId,
                    clientMsgId = userMessage.clientMsgId
                )
            }.onSuccess { reply ->
                val botMessage = createLocalTextMessage(
                    conversationId = AI_ASSISTANT_CONVERSATION_ID,
                    sender = aiAssistantUser(),
                    content = renderBotReplyContent(reply)
                )
                messageStore.saveOrUpdate(botMessage)
                conversationListCoordinator.notifyConversationChanged(
                    conversationId = AI_ASSISTANT_CONVERSATION_ID,
                    moveToTop = true
                )
                triggerScrollToLatest()
            }.onFailure { error ->
                Napier.e("ai assistant reply failed", error)
                val fallbackMessage = createLocalTextMessage(
                    conversationId = AI_ASSISTANT_CONVERSATION_ID,
                    sender = aiAssistantUser(),
                    content = "AI 助手暂时不可用：${error.message ?: "请稍后重试"}"
                )
                messageStore.saveOrUpdate(fallbackMessage)
                _uiState.update { it.copy(error = error.message ?: "AI 助手暂时不可用") }
                conversationListCoordinator.notifyConversationChanged(
                    conversationId = AI_ASSISTANT_CONVERSATION_ID,
                    moveToTop = true
                )
                triggerScrollToLatest()
            }

            _uiState.update { it.copy(isBotResponding = false) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun createLocalTextMessage(
        conversationId: Long,
        sender: UserInfo,
        content: String
    ): MessageWrapper {
        return MessageWrapper(
            messageDto = MessageDTO(
                conversationId = conversationId,
                content = content,
                fromAccountId = sender.userId,
                clientMsgId = Uuid.random().toString(),
                fromAccount = sender,
                type = MessageType.TEXT,
                status = MessageStatus.SENT,
                timestamp = Clock.System.now().toString()
            )
        )
    }

    private fun renderBotReplyContent(reply: AiBotReplyDto): String {
        if (reply.messageType.equals("card", ignoreCase = true)) {
            return renderCardReply(reply.metadata, reply.content)
        }
        return reply.content.ifBlank { "我暂时没有组织出合适的回复。" }
    }

    private fun renderCardReply(metadata: JsonElement?, fallback: String): String {
        val json = metadata as? JsonObject ?: return fallback.ifBlank { "机器人返回了一张卡片消息。" }
        return BOT_CARD_PREFIX + json.toString()
    }

    private fun shouldTriggerGroupBot(content: String): Boolean {
        val conversation = uiState.value.conversation ?: return false
        if (conversation.conversationType != ConversationType.GROUP) return false
        return extractMentionPrompt(content).isNotBlank()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun sendGroupBotReply(content: String) {
        val prompt = extractMentionPrompt(content)
        if (prompt.isBlank()) return

        viewModelScope.launch {
            val currentUser = userRepository.getLocalUserInfo() ?: return@launch
            val conversationId = uiState.value.conversation?.conversationId ?: return@launch

            _uiState.update { it.copy(isBotResponding = true) }

            runCatching {
                AiBotApi.sendMessage(
                    content = prompt,
                    conversationId = conversationId,
                    fromAccountId = currentUser.userId,
                    clientMsgId = "group-bot-${Uuid.random()}"
                )
            }.onSuccess { reply ->
                val botMessage = createLocalTextMessage(
                    conversationId = conversationId,
                    sender = aiAssistantUser(),
                    content = renderBotReplyContent(reply)
                )
                messageStore.saveOrUpdate(botMessage)
                conversationListCoordinator.notifyConversationChanged(
                    conversationId = conversationId,
                    moveToTop = true
                )
                triggerScrollToLatest()
            }.onFailure { error ->
                Napier.e("group ai mention reply failed", error)
                val fallbackMessage = createLocalTextMessage(
                    conversationId = conversationId,
                    sender = aiAssistantUser(),
                    content = "AI 助手暂时无法响应这次 @ 提问：${error.message ?: "请稍后重试"}"
                )
                messageStore.saveOrUpdate(fallbackMessage)
                triggerScrollToLatest()
            }

            _uiState.update { it.copy(isBotResponding = false) }
        }
    }

    private fun extractMentionPrompt(content: String): String {
        return content.trim()
            .replaceFirst("^@机器人\\s*", "")
            .replaceFirst("^@AI助手\\s*", "")
            .replaceFirst("^@AI\\s+助手\\s*", "")
            .trim()
    }

    private fun isConversationActive(conversationId: Long, requestId: Long): Boolean {
        return activeConversationId == conversationId && isRequestActive(requestId)
    }

    private fun isRequestActive(requestId: Long): Boolean = activeRoomRequestId == requestId
}
