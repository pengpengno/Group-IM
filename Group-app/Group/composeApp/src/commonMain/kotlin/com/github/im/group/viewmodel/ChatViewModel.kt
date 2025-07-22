package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.config.SocketClient
import com.github.im.group.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ChatRepository(private val api: ConversationApi, private val socket: SocketClient) {
    suspend fun loadConversations(userId: Long): List<ConversationRes> = api.getActiveConversationsByUserId(userId)
}



class ChatViewModel (
    val tcpClient: SocketClient,
    val userRepository: UserRepository

): ViewModel() {

    private val _conversations = MutableStateFlow(listOf(ConversationRes()))

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




    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ConversationApi.getActiveConversationsByUserId(conversationId)
                _conversations.value = response
            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _loading.value = false
            }
        }
    }



}
