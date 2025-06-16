package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.MessageDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * 聊天消息记录model
 */

data class ChatUiState(
    val messages: List<MessageDTO> = emptyList(),
    val conversation: ConversationRes = ConversationRes(),
    val loading: Boolean = false
)


class ChatMessageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()


    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading

    fun loadMessages(uId: Long )  {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val response = ChatApi.getMessages(uId)
                _uiState.update {
                    it.copy(messages = response.content)
                }

            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }

    }


    /**
     * 查询指定 群聊
     */
    fun getConversation (uId: Long ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val response = ConversationApi.getConversation(uId)
                _uiState.update {
                    it.copy(conversation = response)
                }

            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }




}

