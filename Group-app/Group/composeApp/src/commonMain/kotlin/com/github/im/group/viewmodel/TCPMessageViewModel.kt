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


//    fun updateMessage(message: BaseMessagePkg) {
//        //  先判断 message 的具体类型
//        when {
//            message.accountInfo != null -> {
//                Napier.d("Received account info: ${message.accountInfo}")
//                _messages.value = message
//            }
//            message.message != null -> {
//                Napier.d("Received chat message: ${message.message}")
//                _messages.value = message
//            }
//            message.notification != null -> {
//                Napier.d("Received notification: ${message.notification}")
//                _messages.value = message
//            }
//            message.ack != null -> {
//                Napier.d("Received ack: ${message.ack}")
//                _messages.value = message
//            }
//            else -> {
//                Napier.d("Unknown message received or payload is empty")
//            }
//        }
//
//        _messages.value = message
//    }

    fun onNewMessage(msg: BaseMessagePkg) {


        sessionManager.routeMessage(msg)
    }



}