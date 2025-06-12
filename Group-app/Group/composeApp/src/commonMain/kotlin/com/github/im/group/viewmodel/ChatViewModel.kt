package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ChatViewModel : ViewModel() {

    private val _conversations = MutableStateFlow(listOf(ConversationRes.empty()))

    val uiState:  StateFlow<List<ConversationRes>> = _conversations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun getConversations(uId: Long ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ConversationApi.getActiveConversationsByUserId(uId)
                _conversations.value = response
            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _loading.value = false
            }
        }

    }



    }
