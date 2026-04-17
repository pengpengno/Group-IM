package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.MeetingApi
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

    fun fetchMeetings() {
        viewModelScope.launch {
            _state.value = MeetingsState.Loading
            try {
                val meetings = MeetingApi.listMyMeetings()
                _state.value = MeetingsState.Success(meetings)
            } catch (e: Exception) {
                _state.value = MeetingsState.Error(e.message ?: "获取会议列表失败")
            }
        }
    }
}
