package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.GroupInfo
import com.github.im.group.api.UnauthorizedException
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.ConversationListCoordinator
import com.github.im.group.manager.LoginStateManager
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
    val lastActiveAt: Long = 0L
)

data class ConversationListUiState(
    val conversations: List<ConversationDisplayState> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val usedOfflineData: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val userRepository: UserRepository,
    private val loginStateManager: LoginStateManager,
    private val messageRepository: ChatMessageRepository,
    private val conversationRepository: ConversationRepository,
    private val conversationListCoordinator: ConversationListCoordinator
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

    /**
     * 会话列表统一入口：
     * 1. 先展示本地缓存，保证页面秒开
     * 2. 再按需同步远端，补齐最新会话状态
     */
    fun loadConversations(userId: Long, forceRemote: Boolean = true) {
        activeUserId = userId
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.conversations.isEmpty(),
                    isSyncing = forceRemote,
                    error = null,
                    usedOfflineData = false
                )
            }

            val localConversations = loadLocalConversations(userId)
            _uiState.update {
                it.copy(
                    conversations = localConversations,
                    isLoading = false
                )
            }

            if (!forceRemote) {
                _uiState.update { it.copy(isSyncing = false) }
                return@launch
            }

            try {
                val remoteConversations = loadRemoteConversations(userId)
                _uiState.update {
                    it.copy(
                        conversations = remoteConversations,
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
            val localConversations = loadLocalConversations(userId)
            _uiState.update {
                it.copy(
                    conversations = localConversations,
                    isLoading = false
                )
            }
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

    private suspend fun loadLocalConversations(userId: Long): List<ConversationDisplayState> {
        return try {
            val localConversations = conversationRepository.getConversationsByUserId(userId)
            val preferences = conversationRepository.getConversationUiPreferences()
            sortConversations(
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
        val response = ConversationApi.getActiveConversationsByUserId(userId)
        response.forEach { conversationRepository.saveConversation(it) }

        val preferences = conversationRepository.getConversationUiPreferences()
        return sortConversations(
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
        // 列表项展示数据统一在这里收口，避免 UI 自己拼摘要/时间/未读数
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
            lastActiveAt = localPreference?.lastActiveAt?.takeIf { it > 0L } ?: fallbackActiveAt
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
        // 群聊创建、消息收发、已读变化后，统一走这个入口回写列表
        val currentUserId = activeUserId ?: userRepository.getLocalUserInfo()?.userId ?: 0L
        val state = createConversationDisplayState(conversation, currentUserId)
        _uiState.update { current ->
            current.copy(
                conversations = sortConversations(
                    current.conversations.filterNot {
                        it.conversation.conversationId == conversation.conversationId
                    } + state
                )
            )
        }
    }

    private suspend fun refreshConversationItem(conversationId: Long, moveToTop: Boolean) {
        // 收到消息或会话状态变化时，只刷新受影响的那一项，避免整页重算
        val currentUserId = activeUserId ?: userRepository.getLocalUserInfo()?.userId ?: 0L
        val conversation = conversationRepository.getLocalConversation(conversationId)
            ?: runCatching { conversationRepository.getConversation(conversationId) }.getOrNull()
            ?: return

        if (moveToTop) {
            conversationRepository.markConversationActive(conversationId)
        }

        val updated = createConversationDisplayState(conversation, currentUserId)
        _uiState.update { current ->
            current.copy(
                conversations = sortConversations(
                    current.conversations.filterNot {
                        it.conversation.conversationId == conversationId
                    } + updated
                )
            )
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
}
