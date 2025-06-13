package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.MessageDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * 聊天消息记录model
 */
class ChatMessageViewModel : ViewModel() {

    private val _messages = MutableStateFlow(listOf(MessageDTO.EMPTY))

    val uiState:  StateFlow<List<MessageDTO>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadMessages(uId: Long )  {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ChatApi.getMessages(uId)
                _messages.value = response.content
            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _loading.value = false
            }
        }

    }




    }
