package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationType
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.api.UserApi
import com.github.im.group.db.entities.MessageStatus
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
    val messageStore: MessageStore,
    val messageFacade: MessageFacade,
    val userRepository: UserRepository,
    val chatSessionManager: MessageRouter,
    val conversationRepository: ConversationRepository,
    val fileStorageManager: FileStorageManager,
    val filePicker: FilePicker,
    val filesRepository: FilesRepository
) : ViewModel(), MessageHandler {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())
    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()

    private var activeConversationId: Long? = null

    init {
        viewModelScope.launch {
            userRepository.getLocalUserInfo()?.let { user ->
                messageFacade.startSync(user)
            }
        }
        
        viewModelScope.launch {
            messageStore.messages.collect { msgs ->
                _uiState.update { 
                    it.copy(
                        messages = msgs,
                        messageIndex = if (msgs.isNotEmpty()) 0 else -1
                    )
                }
            }
        }
    }

    override fun onMessageReceived(message: MessageWrapper) {
        if (message.conversationId > 0 && activeConversationId != message.conversationId) {
            // Not for current room visually immediately if background, but let's store it
        }
        messageStore.saveOrUpdate(message)
    }

    override fun onCleared() {
        super.onCleared()
        uiState.value.conversation?.conversationId?.let { unregister(it) }
    }

    fun getFile(fileId: String): File? = fileStorageManager.getFile(fileId)

    fun initChatRoom(room: ChatRoom) {
        viewModelScope.launch {
            _uiState.update { it.copy(chatRoom = room, error = null) }
            when (room.type) {
                ChatRoomType.CONVERSATION -> {
                    activeConversationId = room.roomId
                    loadLocalMessages(room.roomId)
                    loadConversationInfo(room.roomId)
                    register(room.roomId)
                    syncRemoteMessages(room.roomId)
                }

                ChatRoomType.CREATE_PRIVATE -> {
                    val currentUser = userRepository.getLocalUserInfo() ?: return@launch
                    val existingConversation = conversationRepository
                        .getLocalConversationByMembers(currentUser.userId, room.roomId)

                    if (existingConversation != null) {
                        activeConversationId = existingConversation.conversationId
                        _uiState.update { it.copy(conversation = existingConversation) }
                        loadLocalMessages(existingConversation.conversationId)
                        loadConversationInfo(existingConversation.conversationId)
                        register(existingConversation.conversationId)
                        syncRemoteMessages(existingConversation.conversationId)
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
        content: String? = null,
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

            if (currentConversationId == null) {
                activeConversationId = targetConversationId
                messageStore.clear()
            }

            if (pickedFile != null) {
                messageFacade.sendFile(targetConversationId, pickedFile, duration, currentUser, friendId)
            } else if (content != null) {
                messageFacade.sendText(targetConversationId, content, currentUser, friendId)
            }
            
            // Trigger scroll to bottom for the newly sent message
            _uiState.update { it.copy(scrollToTop = true) }
        }
    }

    fun loadLocalMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            messageStore.loadLocal(conversationId, limit)
        }
    }

    fun loadMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            messageStore.loadLocal(conversationId, limit)
            syncRemoteMessages(conversationId, limit)
        }
    }

    fun syncRemoteMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val hasNew = withContext(Dispatchers.Default) {
                    messageStore.syncRemote(conversationId)
                }
                if (hasNew) {
                    messageStore.loadLocal(conversationId, limit)
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
        messageFacade.handleAck(clientMsgId)
    }

    fun register(conversationId: Long) {
        chatSessionManager.registerHandler(conversationId, this)
    }

    fun unregister(conversationId: Long) {
        chatSessionManager.unregisterHandler(conversationId)
    }

    fun refreshMessages() {
        uiState.value.conversation?.conversationId?.let { syncRemoteMessages(it) }
    }

    fun loadLatestMessages(conversationId: Long, afterSequenceId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                withContext(Dispatchers.Default) {
                    messageStore.loadLatest(conversationId, afterSequenceId)
                }
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

    private suspend fun getOrCreatePrivateChat(userId: Long, friendId: Long): ConversationRes {
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
                val lastSeq = _uiState.value.messages.firstOrNull()?.seqId ?: 0L
                if (lastSeq <= 0L) return@launch

                messageStore.markConversationRead(conversationId, currentUserId)

                ChatApi.markConversationAsRead(conversationId = conversationId, sequenceId = lastSeq)
            } catch (e: Exception) {
                Napier.e("mark conversation as read failed", e)
            }
        }
    }
}
