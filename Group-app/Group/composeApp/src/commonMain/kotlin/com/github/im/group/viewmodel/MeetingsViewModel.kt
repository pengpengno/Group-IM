package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationType
import com.github.im.group.api.MeetingApi
import com.github.im.group.api.MeetingCreateRequest
import com.github.im.group.api.MeetingRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MeetingsState {
    object Idle : MeetingsState()
    object Loading : MeetingsState()
    data class Success(val meetings: List<MeetingRes>) : MeetingsState()
    data class Error(val message: String) : MeetingsState()
}

class MeetingsViewModel : ViewModel() {
    private val _state = MutableStateFlow<MeetingsState>(MeetingsState.Idle)
    val state: StateFlow<MeetingsState> = _state.asStateFlow()

    private val _groupConversations = MutableStateFlow<List<ConversationRes>>(emptyList())
    val groupConversations: StateFlow<List<ConversationRes>> = _groupConversations.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    fun fetchMeetings(userId: Long? = null) {
        viewModelScope.launch {
            _state.value = MeetingsState.Loading
            try {
                if (userId != null) {
                    _groupConversations.value = ConversationApi.getActiveConversationsByUserId(userId)
                        .filter { it.conversationType == ConversationType.GROUP }
                }
                val meetings = MeetingApi.listMyMeetings()
                _state.value = MeetingsState.Success(meetings)
            } catch (e: Exception) {
                _state.value = MeetingsState.Error(e.message ?: "获取会议列表失败")
            }
        }
    }

    fun createMeeting(
        conversationId: Long,
        title: String,
        participantIds: List<Long>,
        scheduledAt: String? = null,
        onCreated: (MeetingRes) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _creating.value = true
            try {
                val meeting = MeetingApi.createMeeting(
                    MeetingCreateRequest(
                        conversationId = conversationId,
                        title = title,
                        participantIds = participantIds,
                        scheduledAt = scheduledAt
                    )
                )
                onCreated(meeting)
            } catch (e: Exception) {
                onError(e.message ?: "创建会议失败")
            } finally {
                _creating.value = false
            }
        }
    }
}
