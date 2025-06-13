package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.config.SocketClient
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ChatRepository(private val api: ConversationApi, private val socket: SocketClient) {
    suspend fun loadConversations(userId: Long): List<ConversationRes> = api.getActiveConversationsByUserId(userId)
}



class ChatViewModel (
    val tcpClient: SocketClient
): ViewModel() {

    private val _conversations = MutableStateFlow(listOf(ConversationRes.empty()))

    val uiState:  StateFlow<List<ConversationRes>> = _conversations.asStateFlow()



    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun getConversation (uId: Long ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ConversationApi.getConversationInfo(uId)
                _conversations.value = response
            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _loading.value = false
            }
        }
    }
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


    fun sendMessage(conversationId:Long,message:String  ){

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chatMessage = ChatMessage(
                    content = message,
                    conversationId = conversationId,
                    // ...省略字段
                )
                val baseMessage = BaseMessagePkg(message = chatMessage)
                tcpClient.send(BaseMessagePkg.ADAPTER.encode(baseMessage))
            } catch (e: Exception) {
                println("发送失败: $e")
            }
        }
//        withContext(Dispatchers.IO) {  // 切换到IO协程
//            val chatMessage = ChatMessage(
//                content = message,
//                conversationId = conversationId,
//                conversationName = "",
//                fromAccountInfo = null,
//                toAccountInfo = null,
//                clientTimeStamp = 0,
//                serverTimeStamp = 0,
//                type = MessageType.TEXT,
//                messagesStatus = ChatMessage.MessagesStatus.UNSENT,
//                sequenceId = 0
//            )
//            val baseMessage  = BaseMessagePkg(message = chatMessage)
//
//            tcpClient.send(BaseMessagePkg.ADAPTER.encode(baseMessage))
//        }
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
