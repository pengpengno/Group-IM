package com.github.im.group.manager

import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.viewmodel.ChatMessageViewModel

class ChatSessionManager {

    // 已初始化的 VM
    private val sessionMap = mutableMapOf<Long, ChatMessageViewModel>()

    // 缓存消息（ViewModel 未准备好前暂存）
    private val messageBuffer = mutableMapOf<Long, MutableList<ChatMessage>>()

    fun register(conversationId: Long, viewModel: ChatMessageViewModel) {
        sessionMap[conversationId] = viewModel

        // 若缓存中有消息，立即投递并清除
        messageBuffer.remove(conversationId)?.forEach { message ->
//            val msg = message.message ?: return
            viewModel.onReceiveMessage(MessageWrapper(message))
        }
    }

    fun unregister(conversationId: Long) {
        sessionMap.remove(conversationId)
    }

    fun routeMessage(pkg: BaseMessagePkg) {
        val msg = pkg.message ?: return
        val targetId = msg.conversationId
        val vm = sessionMap[targetId]
        if (vm != null) {
            vm.onReceiveMessage(MessageWrapper(msg))
        } else {
            // VM 不存在，先缓存在内存中
            messageBuffer.getOrPut(targetId) { mutableListOf() }.add(msg)
        }
    }
}
