package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.sdk.SenderSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * 聊天消息记录model
 */

data class ChatUiState(
    val messages: List<MessageItem> = emptyList(),
    val conversation: ConversationRes = ConversationRes(),
    val loading: Boolean = false
)


class ChatMessageViewModel(
    val userViewModel: UserViewModel,
    val chatSessionManager: ChatSessionManager,
    val senderSdk: SenderSdk ,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading

    fun onReceiveMessage(message: MessageItem){
        _uiState.update {
            it.copy(messages = it.messages + message)
        }
    }

    fun register(conversationId: Long){
        chatSessionManager.register(conversationId,this)
    }

    fun unregister(conversationId: Long){
        chatSessionManager.unregister(conversationId)
    }

    fun loadMessages(uId: Long )  {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val response = ChatApi.getMessages(uId)

                val wrappedMessages = response.content.map {
                    MessageWrapper(messageDto = it)
                }.sortedByDescending { it.seqId }

                val wrappedMessages2 = response.content.map {
                    MessageWrapper(messageDto = it)
                }.sortedBy { it.seqId }
                println("receive $wrappedMessages")

                _uiState.update {
                    it.copy(messages = wrappedMessages)
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
     * 接收新的消息
     */
    fun addMessage(message: MessageItem){
        _uiState.update {
            it.copy(messages = it.messages + message)
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


    /**
     * 发送消息
     */
    fun sendMessage(conversationId:Long,message:String ){

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if(message.isNotBlank()){
                    val chatMessage = ChatMessage(
                        content = message,
                        conversationId = conversationId,
                        fromAccountInfo = userViewModel.getAccountInfo(),
                        type = MessageType.TEXT
                    )
                    senderSdk.sendMessage(chatMessage)
                    _uiState.update {
                        it.copy(messages = it.messages + MessageWrapper(chatMessage))
                    }
                }

            } catch (e: Exception) {
                println("发送失败: $e")
            }
        }

    }

}

