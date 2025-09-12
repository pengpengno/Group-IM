package com.github.im.group.manager

import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.viewmodel.ChatMessageViewModel


/**
 * 客户端 会话的管理
 */
class ChatSessionManager {

    // 已初始化的 VM
    private val sessionMap = mutableMapOf<Long, ChatMessageViewModel>()

    // 缓存消息（ViewModel 未准备好前暂存）
    private val messageBuffer = mutableMapOf<Long, MutableList<ChatMessage>>()

    /**
     * 注册会话
     * 避免重复进入页面重复刷新
     */
    fun register(conversationId: Long, viewModel: ChatMessageViewModel) {
        sessionMap[conversationId] = viewModel
        // 若缓存中有消息，立即投递并清除
        messageBuffer.remove(conversationId)?.forEach { message ->
            viewModel.onReceiveMessage(MessageWrapper(message))
        }
    }

    fun unregister(conversationId: Long) {
        sessionMap.remove(conversationId)
    }

    /**
     * 路由消息
     * 将 消息根据会话ID路由到对应的 VM 中
     */

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
