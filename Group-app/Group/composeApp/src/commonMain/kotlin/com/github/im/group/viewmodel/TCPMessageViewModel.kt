package com.github.im.group.viewmodel

import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.model.proto.BaseMessagePkg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class TCPMessageViewModel(
    private val sessionManager: ChatSessionManager
) {

    private val _messages = MutableStateFlow<BaseMessagePkg>(BaseMessagePkg())

    val uiState: StateFlow<BaseMessagePkg> = _messages.asStateFlow()

    val baseMessageFlow = _messages.asSharedFlow()


    fun updateMessage(message: BaseMessagePkg) {

        //  先判断 message 的具体类型
        when {
            message.accountInfo != null -> {
                println("Received account info: ${message.accountInfo}")
                _messages.value = message
            }
            message.message != null -> {
                println("Received chat message: ${message.message}")
                _messages.value = message
            }
            message.notification != null -> {
                println("Received notification: ${message.notification}")
                _messages.value = message
            }
            else -> {
                println("Unknown message received or payload is empty")
            }
        }

        _messages.value = message


    }

    fun onNewMessage(msg: BaseMessagePkg) {
//        _messages.value = msg
//        updateMessage(msg)
        sessionManager.routeMessage(msg)

    }

}