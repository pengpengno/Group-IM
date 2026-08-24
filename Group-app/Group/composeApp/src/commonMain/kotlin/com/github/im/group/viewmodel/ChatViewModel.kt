package com.github.im.group.viewmodel

import com.github.im.group.api.AiBotApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.GroupInfo
import com.github.im.group.api.UnauthorizedException
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.AppRuntimeState
import com.github.im.group.manager.ConversationListCoordinator
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.manager.NotificationPreferenceStore
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.ConversationUiPreference
import com.github.im.group.repository.UserRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

data class ConversationDisplayState(
    val conversation: ConversationRes,
    val lastMessage: String = "",
    val displayDateTime: String = "",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val pinRank: Long = 0L,
    val lastActiveAt: Long = 0L,
    val isMuted: Boolean = false,
    val muteUntil: Long = 0L
)

data class RealtimeConversationHint(
    val conversationId: Long,
    val title: String,
    val preview: String,
    val unreadCount: Int
)

data class ConversationListUiState(
    val conversations: List<ConversationDisplayState> = emptyList(),
    val totalUnreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val usedOfflineData: Boolean = false,
    val error: String? = null,
    val readAllInProgress: Boolean = false,
    val realtimeHint: RealtimeConversationHint? = null
)

class ChatViewModel(
    private val userRepository: UserRepository,
    private val loginStateManager: LoginStateManager,
    private val messageRepository: ChatMessageRepository,
    private val conversationRepository: ConversationRepository,
    private val conversationListCoordinator: ConversationListCoordinator,
    private val notificationPreferenceStore: NotificationPreferenceStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private var activeUserId: Long? = null

    init {
        viewModelScope.launch {
            conversationListCoordinator.events.collectLatest { event ->
                refreshConversationItem(event.conversationId, event.moveToTop)
            }
        }
    }

    fun loadConversations(userId: Long, forceRemote: Boolean = true) {
        activeUserId = userId
        viewModelScope.launch {
            ensureAssistantConversation()
            _uiState.update {
                it.copy(
                    isLoading = it.conversations.isEmpty(),
                    isSyncing = forceRemote,
                    error = null,
                    usedOfflineData = false
                )
            }

            val localConversations = loadLocalConversations(userId)
            updateConversationList(localConversations) {
                it.copy(
                    isLoading = false,
                    realtimeHint = it.realtimeHint
                )
            }

            if (!forceRemote) {
                _uiState.update { it.copy(isSyncing = false) }
                return@launch
            }

            try {
                val remoteConversations = loadRemoteConversations(userId)
                updateConversationList(remoteConversations) {
                    it.copy(
                        isSyncing = false,
                        usedOfflineData = false,
                        error = null
                    )
                }
            } catch (e: UnauthorizedException) {
                _uiState.update { it.copy(isSyncing = false, isLoading = false) }
                handleUnauthorizedException(e)
            } catch (e: Exception) {
                Napier.e("Failed to sync conversation list", e)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isLoading = false,
                        usedOfflineData = localConversations.isNotEmpty(),
                        error = e.message
                    )
                }
            }
        }
    }

    fun refreshCachedConversations(userId: Long) {
        activeUserId = userId
        viewModelScope.launch {
            ensureAssistantConversation()
            val localConversations = loadLocalConversations(userId)
            updateConversationList(localConversations) { it.copy(isLoading = false) }
        }
    }

    fun togglePinConversation(conversationId: Long) {
        val current = _uiState.value.conversations.firstOrNull {
            it.conversation.conversationId == conversationId
        } ?: return

        if (current.isPinned) {
            conversationRepository.unpinConversation(conversationId)
        } else {
            conversationRepository.pinConversation(conversationId)
        }

        viewModelScope.launch {
            refreshConversationItem(conversationId, moveToTop = false)
        }
    }

    fun toggleMuteConversation(conversationId: Long) {
        val current = _uiState.value.conversations.firstOrNull {
            it.conversation.conversationId == conversationId
        } ?: return

        val nextMuteUntil = if (current.isMuted) 0L else Long.MAX_VALUE
        conversationRepository.setConversationMuteUntil(conversationId, nextMuteUntil)
        AppRuntimeState.setConversationMutedUntil(conversationId, nextMuteUntil)

        viewModelScope.launch {
            refreshConversationItem(conversationId, moveToTop = false)
        }
    }

    fun muteConversationForEightHours(conversationId: Long) {
        setConversationMuteUntil(conversationId, Clock.System.now().toEpochMilliseconds() + 8L * 60L * 60L * 1000L)
    }

    fun muteConversationUntilEndOfDay(conversationId: Long) {
        val zone = TimeZone.currentSystemDefault()
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(zone)
        val millisUntilTomorrow =
            (23 - now.hour) * 60L * 60L * 1000L +
                (59 - now.minute) * 60L * 1000L +
                (59 - now.second) * 1000L +
                (999_999_999 - now.nanosecond) / 1_000_000L +
                1L
        setConversationMuteUntil(conversationId, nowInstant.toEpochMilliseconds() + millisUntilTomorrow)
    }

    fun muteConversationForever(conversationId: Long) {
        setConversationMuteUntil(conversationId, Long.MAX_VALUE)
    }

    fun unmuteConversation(conversationId: Long) {
        setConversationMuteUntil(conversationId, 0L)
    }

    fun clearRealtimeHint() {
        _uiState.update { it.copy(realtimeHint = null) }
    }

    fun clearRealtimeHint(conversationId: Long) {
        _uiState.update { state ->
            state.copy(
                realtimeHint = state.realtimeHint?.takeUnless { it.conversationId == conversationId }
            )
        }
    }

    fun markConversationRead(conversationId: Long) {
        val currentUserId = activeUserId ?: return
        viewModelScope.launch {
            val latestSeq = messageRepository.getLocalLatestMessage(conversationId)?.seqId ?: 0L
            if (latestSeq <= 0L) return@launch

            messageRepository.markConversationMessagesAsRead(conversationId, currentUserId)
            runCatching {
                ChatApi.markConversationAsRead(conversationId, latestSeq)
            }.onFailure { error: Throwable ->
                Napier.w("Failed to sync conversation read state: ${error.message}")
            }

            refreshConversationItem(conversationId, moveToTop = false)
            _uiState.update { state ->
                state.copy(
                    realtimeHint = state.realtimeHint?.takeUnless { it.conversationId == conversationId }
                )
            }
        }
    }

    fun markAllConversationsRead() {
        val currentUserId = activeUserId ?: return
        viewModelScope.launch {
            val unreadConversations = _uiState.value.conversations.filter { it.unreadCount > 0 }
            if (unreadConversations.isEmpty()) return@launch

            _uiState.update { it.copy(readAllInProgress = true) }
            unreadConversations.forEach { item ->
                val latestSeq = messageRepository.getLocalLatestMessage(item.conversation.conversationId)?.seqId ?: 0L
                if (latestSeq <= 0L) return@forEach

                messageRepository.markConversationMessagesAsRead(item.conversation.conversationId, currentUserId)
                runCatching {
                    ChatApi.markConversationAsRead(item.conversation.conversationId, latestSeq)
                }.onFailure { error: Throwable ->
                    Napier.w(
                        "Failed to sync all-read state for ${item.conversation.conversationId}: ${error.message}"
                    )
                }
            }

            updateConversationList(loadLocalConversations(currentUserId)) {
                it.copy(
                    readAllInProgress = false,
                    realtimeHint = null
                )
            }
            syncMutedConversationsToRuntime()
        }
    }

    private suspend fun loadLocalConversations(userId: Long): List<ConversationDisplayState> {
        return try {
            val localConversations = conversationRepository.getConversationsByUserId(userId)
            val preferences = conversationRepository.getConversationUiPreferences()
            includeAssistantConversation(
                localConversations.map { conversation ->
                    createConversationDisplayState(
                        conversation = conversation,
                        currentUserId = userId,
                        preference = preferences[conversation.conversationId]
                    )
                }
            )
        } catch (e: Exception) {
            Napier.e("Failed to load local conversations", e)
            emptyList()
        }
    }

    private suspend fun loadRemoteConversations(userId: Long): List<ConversationDisplayState> {
        val response = ConversationApi.getActiveConversations()
        response.forEach { conversationRepository.saveConversation(it) }

        val preferences = conversationRepository.getConversationUiPreferences()
        return includeAssistantConversation(
            response.map { conversation ->
                createConversationDisplayState(
                    conversation = conversation,
                    currentUserId = userId,
                    preference = preferences[conversation.conversationId]
                )
            }
        )
    }

    suspend fun createConversationDisplayState(
        conversation: ConversationRes,
        currentUserId: Long = 0L,
        preference: ConversationUiPreference? = null
    ): ConversationDisplayState {
        val latestMessage = messageRepository.getLocalLatestMessage(conversation.conversationId)
        val lastMessageText = latestMessage?.let(::getMessageDesc).orEmpty()
        val latestMessageTime = latestMessage?.clientTime ?: latestMessage?.time
        val displayDateTime = latestMessageTime?.let(::calculateDisplayDateTime).orEmpty()
        val localPreference = preference ?: conversationRepository.getConversationUiPreference(conversation.conversationId)
        val unreadCount = if (currentUserId > 0L) {
            messageRepository.getUnreadCount(conversation.conversationId, currentUserId)
        } else {
            0
        }
        val fallbackActiveAt = runCatching {
            (latestMessage?.clientTime ?: latestMessage?.time)
                ?.toInstant(TimeZone.currentSystemDefault())
                ?.toEpochMilliseconds()
        }.getOrNull() ?: 0L

        return ConversationDisplayState(
            conversation = conversation,
            lastMessage = lastMessageText,
            displayDateTime = displayDateTime,
            unreadCount = unreadCount,
            isPinned = localPreference?.isPinned == true,
            pinRank = localPreference?.pinRank ?: 0L,
            lastActiveAt = localPreference?.lastActiveAt?.takeIf { it > 0L } ?: fallbackActiveAt,
            isMuted = localPreference?.isMuted == true,
            muteUntil = localPreference?.muteUntil ?: 0L
        )
    }

    private fun getMessageDesc(message: MessageWrapper): String {
        return when (message.type) {
            MessageType.TEXT -> message.content
            MessageType.FILE -> "[文件]"
            MessageType.VOICE -> "[语音]"
            MessageType.VIDEO -> "[视频]"
            MessageType.IMAGE -> "[图片]"
            MessageType.MEETING -> "[会议]"
            MessageType.BOT_CARD -> "[机器人卡片]"
            MessageType.WORKBENCH -> "[工作台]"
        }
    }

    private fun handleUnauthorizedException(e: UnauthorizedException) {
        Napier.e("Token expired, logging out", e)
        userRepository.updateToLoggedOut()
        loginStateManager.setLoggedOut()
    }

    suspend fun createGroupChat(
        groupName: String,
        desc: String?,
        members: List<UserInfo>
    ): ConversationRes {
        val conversation = try {
            ConversationApi.createGroupConversation(
                GroupInfo(
                    groupName = groupName,
                    description = desc,
                    members = members
                )
            )
        } catch (e: UnauthorizedException) {
            handleUnauthorizedException(e)
            throw e
        }

        conversationRepository.saveConversation(conversation)
        conversationRepository.markConversationActive(conversation.conversationId)
        syncConversationIntoList(conversation)
        return conversation
    }

    suspend fun syncConversationToUI(conversationId: Long) {
        val conversation = runCatching {
            conversationRepository.getConversation(conversationId)
        }.getOrElse {
            Napier.e("Failed to sync conversation to UI", it)
            return
        }
        syncConversationIntoList(conversation)
    }

    private suspend fun syncConversationIntoList(conversation: ConversationRes) {
        val currentUserId = activeUserId ?: userRepository.getLocalUserInfo()?.userId ?: 0L
        val state = createConversationDisplayState(conversation, currentUserId)
        updateConversationList(
            _uiState.value.conversations.filterNot {
                it.conversation.conversationId == conversation.conversationId
            } + state
        )
    }

    private suspend fun refreshConversationItem(conversationId: Long, moveToTop: Boolean) {
        val currentUserId = activeUserId ?: userRepository.getLocalUserInfo()?.userId ?: 0L
        val conversation = conversationRepository.getLocalConversation(conversationId)
            ?: runCatching { conversationRepository.getConversation(conversationId) }.getOrNull()
            ?: return

        if (moveToTop) {
            conversationRepository.markConversationActive(conversationId)
        }

        val updated = createConversationDisplayState(conversation, currentUserId)
        val currentUser = userRepository.getLocalUserInfo()
        val notificationPreferences = notificationPreferenceStore.getSnapshot()
        val nextList = _uiState.value.conversations.filterNot {
            it.conversation.conversationId == conversationId
        } + updated

        updateConversationList(nextList) { current ->
            current.copy(
                realtimeHint = when {
                    moveToTop && updated.unreadCount > 0 &&
                        !updated.isMuted &&
                        notificationPreferences.enableNotifications &&
                        AppRuntimeState.shouldShowRealtimeConversationHint(updated.conversation.conversationId) -> RealtimeConversationHint(
                        conversationId = updated.conversation.conversationId,
                        title = updated.conversation.getName(currentUser),
                        preview = if (notificationPreferences.enablePreview) {
                            updated.lastMessage.ifBlank { "你有一条新消息" }
                        } else {
                            "你有一条新消息"
                        },
                        unreadCount = updated.unreadCount
                    )
                    current.realtimeHint?.conversationId == conversationId &&
                        (updated.unreadCount <= 0 || updated.isMuted) -> null
                    else -> current.realtimeHint
                }
            )
        }
    }

    private fun updateConversationList(
        conversations: List<ConversationDisplayState>,
        transform: (ConversationListUiState) -> ConversationListUiState = { it }
    ) {
        _uiState.update { current ->
            val sorted = sortConversations(conversations)
            syncMutedConversationsToRuntime(sorted)
            transform(
                current.copy(
                    conversations = sorted,
                    totalUnreadCount = sorted.sumOf { it.unreadCount }
                )
            )
        }
    }

    private fun syncMutedConversationsToRuntime(
        conversations: List<ConversationDisplayState> = _uiState.value.conversations
    ) {
        AppRuntimeState.replaceMutedConversationState(
            conversations
                .filter { it.isMuted }
                .associate { it.conversation.conversationId to it.muteUntil }
        )
    }

    private fun setConversationMuteUntil(conversationId: Long, muteUntil: Long) {
        conversationRepository.setConversationMuteUntil(conversationId, muteUntil)
        AppRuntimeState.setConversationMutedUntil(conversationId, muteUntil)
        if (muteUntil > 0L) {
            clearRealtimeHint(conversationId)
        }

        viewModelScope.launch {
            refreshConversationItem(conversationId, moveToTop = false)
        }
    }

    private fun sortConversations(items: List<ConversationDisplayState>): List<ConversationDisplayState> {
        return items.sortedWith(
            compareByDescending<ConversationDisplayState> { it.isPinned }
                .thenByDescending { if (it.isPinned) it.pinRank else it.lastActiveAt }
                .thenByDescending { it.unreadCount > 0 }
                .thenByDescending { it.lastActiveAt }
                .thenByDescending { it.conversation.conversationId }
        )
    }

    private fun calculateDisplayDateTime(createAt: kotlinx.datetime.LocalDateTime): String {
        return try {
            val now = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())

            when {
                createAt.date == now.date -> {
                    "${createAt.hour}:${createAt.minute.toString().padStart(2, '0')}"
                }

                createAt.date == now.date.minus(1, DateTimeUnit.DAY) -> {
                    "昨天 ${createAt.hour}:${createAt.minute.toString().padStart(2, '0')}"
                }

                (now.date.toEpochDays() - createAt.date.toEpochDays()) <= 7 -> {
                    when (createAt.dayOfWeek.ordinal) {
                        0 -> "周一"
                        1 -> "周二"
                        2 -> "周三"
                        3 -> "周四"
                        4 -> "周五"
                        5 -> "周六"
                        6 -> "周日"
                        else -> "近期"
                    }
                }

                now.year > createAt.year -> {
                    "${createAt.year}-${createAt.monthNumber.toString().padStart(2, '0')}-${createAt.dayOfMonth.toString().padStart(2, '0')}"
                }

                else -> {
                    "${createAt.monthNumber.toString().padStart(2, '0')}-${createAt.dayOfMonth.toString().padStart(2, '0')}"
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to format conversation time", e)
            createAt.toString()
        }
    }

    private suspend fun ensureAssistantConversation() {
        runCatching { AiBotApi.getConversation() }.onSuccess { conversationRepository.saveConversation(it) }
    }

    private suspend fun includeAssistantConversation(
        items: List<ConversationDisplayState>
    ): List<ConversationDisplayState> {
        return sortConversations(items)
    }
}
