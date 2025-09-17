package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ChatApi
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.FileApi
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.sdk.SenderSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


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
    val chatMessageRepository: ChatMessageRepository,
    val senderSdk: SenderSdk ,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading



    /**
     * 接收到消息
     */
    fun onReceiveMessage(message: MessageItem){
        _uiState.update {
            it.copy(messages = it.messages + message)
        }
    }

    /**
     * 注册会话
     */
    fun register(conversationId: Long){
        chatSessionManager.register(conversationId,this)
    }

    fun unregister(conversationId: Long){
        chatSessionManager.unregister(conversationId)
    }


    /**
     * 分页加载数据
     */
    fun loadMessages(conversationId: Long , page: Int = 1 , size: Int = 50){

    }
    /**
     * 加载消息
     * @param conversationId 会话id
     * 默认加载最新的 50 条消息
     */
    fun loadMessages(conversationId: Long )  {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val response = ChatApi.getMessages(conversationId)

                val wrappedMessages = response.content.map {
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
     * 发送语音消息
     *
     * @param conversationId 会话ID
     * @param url 语音文件的URL地址
     */
    fun sendVoiceMessage(conversationId: Long,
                         data: ByteArray,
                         fileName:String,
                         duration:Long
                         ) {

        sendFileMessage(conversationId,data,fileName,duration,MessageType.VOICE)
    }
    /**
     * 发送文件消息
     * @param conversationId 会话ID
     * @param data 文件数据
     * @param fileName 文件名
     * @param type 消息类型
     */
     fun sendFileMessage(conversationId: Long ,data:ByteArray,fileName:String,duration: Long=0,type:MessageType = MessageType.FILE){
         viewModelScope.launch {
             val response  = FileApi.uploadFile(data,fileName,duration).let {
                 var fileMeta = it.fileMeta


//                 val message = ChatMessage(
//                     conversationId = conversationId,
//                     fromAccountInfo = userViewModel.getAccountInfo(),
//                     content = it.id,
//                     type = type,
//                 )
//                 senderSdk.sendMessage(message)
//
//                 _uiState.update {
//                     it.copy(messages = it.messages + MessageWrapper(message))
//                 }
                 sendMessage(conversationId,it.id,type)
             }
         }

    }
    /**
     * 发送消息
     * @param conversationId 会话
     * @param message 消息
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun sendMessage(conversationId:Long, message:String, type: MessageType = MessageType.TEXT){
        // 发送的 数据需要 再本地先保存
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if(message.isNotBlank()){
                    val chatMessage = ChatMessage(
                        content = message,
                        conversationId = conversationId,
                        fromAccountInfo = userViewModel.getAccountInfo(),
                        type = type,
                        messagesStatus = MessagesStatus.SENDING,
                        clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
                        clientMsgId =  Uuid.random().toString(),
                    )
                    senderSdk.sendMessage(chatMessage)

                    chatMessageRepository.insertMessage(chatMessage)
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

