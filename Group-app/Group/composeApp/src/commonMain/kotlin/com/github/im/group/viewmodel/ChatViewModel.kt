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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class ChatViewModel(
    private val userRepository: UserRepository,
    private val loginStateManager: LoginStateManager,
    private val messageRepository: ChatMessageRepository,
    private val conversationRepository: ConversationRepository,
    private val conversationListCoordinator: ConversationListCoordinator
) : ViewModel() {

    private val _conversations = MutableStateFlow(emptyList<ConversationDisplayState>())
    val conversationState: StateFlow<List<ConversationDisplayState>> = _conversations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            conversationListCoordinator.events.collectLatest { event ->
                refreshConversationItem(event.conversationId, event.moveToTop)
            }
        }
    }

    fun refreshUnreadCounts(currentUserId: Long) {
        _conversations.update { currentList ->
            sortConversations(
                currentList.map { item ->
                    val unreadCount = messageRepository.getUnreadCount(
                        item.conversation.conversationId,
                        currentUserId
                    )
                    if (unreadCount == item.unreadCount) {
                        item
                    } else {
                        item.copy(unreadCount = unreadCount)
                    }
                }
            )
        }
    }

    fun refreshConversationList(currentUserId: Long, includeRemote: Boolean = false) {
        viewModelScope.launch {
            loadLocalConversations(currentUserId)
            if (includeRemote) {
                loadRemoteConversations(currentUserId)
            }
        }
    }

    fun togglePinConversation(conversationId: Long) {
        val current = _conversations.value.firstOrNull {
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

    fun getConversations(userId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                loadLocalConversations(userId)
                loadRemoteConversations(userId)
            } catch (e: UnauthorizedException) {
                handleUnauthorizedException(e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Napier.d("Loading conversation list cancelled")
            } catch (e: Exception) {
                Napier.e("Failed to load conversation list", e)
                handleLoadFailure(userId)
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadLocalConversations(userId: Long) {
        try {
            val localConversations = conversationRepository.getConversationsByUserId(userId)
            val preferences = conversationRepository.getConversationUiPreferences()
            val displayStates = localConversations.map { conversation ->
                createConversationDisplayState(
                    conversation = conversation,
                    currentUserId = userId,
                    preference = preferences[conversation.conversationId]
                )
            }
            _conversations.value = sortConversations(displayStates)
        } catch (e: Exception) {
            Napier.e("Failed to load local conversations", e)
        }
    }

    private suspend fun loadRemoteConversations(userId: Long) {
        try {
            val response = ConversationApi.getActiveConversationsByUserId(userId)
            response.forEach { conversationRepository.saveConversation(it) }

            val preferences = conversationRepository.getConversationUiPreferences()
            val displayStates = response.map { conversation ->
                createConversationDisplayState(
                    conversation = conversation,
                    currentUserId = userId,
                    preference = preferences[conversation.conversationId]
                )
            }
            _conversations.value = sortConversations(displayStates)
        } catch (e: Exception) {
            Napier.e("Failed to load remote conversations", e)
        }
    }

    suspend fun createConversationDisplayState(
        conversation: ConversationRes,
        currentUserId: Long = 0L,
        preference: ConversationUiPreference? = null
    ): ConversationDisplayState {
        val latestMessage = messageRepository.getLocalLatestMessage(conversation.conversationId)
        val lastMessageText = latestMessage?.let(::getMessageDesc).orEmpty()
        val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) }.orEmpty()
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
            MessageType.FILE -> "File"
            MessageType.VOICE -> "Voice"
            MessageType.VIDEO -> "Video"
            MessageType.IMAGE -> "Image"
            MessageType.MEETING -> "Meeting"
        }
    }

    private fun handleUnauthorizedException(e: UnauthorizedException) {
        Napier.e("Token expired, logging out", e)
        userRepository.updateToLoggedOut()
        loginStateManager.setLoggedOut()
    }

    private suspend fun handleLoadFailure(userId: Long) {
        if (_conversations.value.isNotEmpty()) {
            return
        }

        try {
            val localConversations = conversationRepository.getConversationsByUserId(userId)
            val preferences = conversationRepository.getConversationUiPreferences()
            _conversations.value = sortConversations(
                localConversations.map { conversation ->
                    createConversationDisplayState(
                        conversation = conversation,
                        currentUserId = userId,
                        preference = preferences[conversation.conversationId]
                    )
                }
            )
        } catch (e: Exception) {
            Napier.e("Failed to recover local conversations", e)
            _conversations.value = emptyList()
        }
    }

    fun getOrCreatePrivateChat(userId: Long, friendId: Long) {
        viewModelScope.launch {
            val localConversation = conversationRepository.getLocalConversationByMembers(userId, friendId)
            val conversation = if (localConversation != null) {
                localConversation
            } else {
                try {
                    ConversationApi.createOrGetConversation(friendId)
                } catch (e: UnauthorizedException) {
                    handleUnauthorizedException(e)
                    throw e
                }
            }

            conversationRepository.saveConversation(conversation)
            conversationRepository.markConversationActive(conversation.conversationId)
            val state = createConversationDisplayState(conversation, userId)
            _conversations.update { current ->
                sortConversations(current.filterNot {
                    it.conversation.conversationId == conversation.conversationId
                } + state)
            }
        }
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
        val currentUserId = userRepository.getLocalUserInfo()?.userId ?: 0L
        val state = createConversationDisplayState(conversation, currentUserId)
        _conversations.update { current ->
            sortConversations(current.filterNot {
                it.conversation.conversationId == conversation.conversationId
            } + state)
        }
        return conversation
    }

    suspend fun syncConversationToUI(conversationId: Long) {
        try {
            val conversation = conversationRepository.getConversation(conversationId)
            val currentUserId = userRepository.getLocalUserInfo()?.userId ?: 0L
            val state = createConversationDisplayState(conversation, currentUserId)
            _conversations.update { current ->
                sortConversations(current.filterNot {
                    it.conversation.conversationId == conversationId
                } + state)
            }
        } catch (e: Exception) {
            Napier.e("Failed to sync conversation to UI", e)
        }
    }

    private suspend fun refreshConversationItem(conversationId: Long, moveToTop: Boolean) {
        val currentUserId = userRepository.getLocalUserInfo()?.userId ?: 0L
        val conversation = withContext(Dispatchers.Default) {
            conversationRepository.getLocalConversation(conversationId)
                ?: runCatching { conversationRepository.getConversation(conversationId) }.getOrNull()
        } ?: return

        if (moveToTop) {
            conversationRepository.markConversationActive(conversationId)
        }

        val updated = createConversationDisplayState(conversation, currentUserId)
        _conversations.update { current ->
            sortConversations(current.filterNot {
                it.conversation.conversationId == conversationId
            } + updated)
        }
    }

    private fun sortConversations(items: List<ConversationDisplayState>): List<ConversationDisplayState> {
        return items.sortedWith(
            compareByDescending<ConversationDisplayState> { it.isPinned }
                .thenByDescending { if (it.isPinned) it.pinRank else it.lastActiveAt }
                .thenByDescending { it.unreadCount > 0 }
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
                    "Yesterday ${createAt.hour}:${createAt.minute.toString().padStart(2, '0')}"
                }

                (now.date.toEpochDays() - createAt.date.toEpochDays()) <= 7 -> {
                    when (createAt.dayOfWeek.ordinal) {
                        0 -> "Mon"
                        1 -> "Tue"
                        2 -> "Wed"
                        3 -> "Thu"
                        4 -> "Fri"
                        5 -> "Sat"
                        6 -> "Sun"
                        else -> "Recent"
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
