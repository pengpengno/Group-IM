package com.github.im.group.manager

import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.repository.ChatMessageRepository
import io.github.aakira.napier.Napier

class ChatSessionManager(
    private val chatMessageRepository: ChatMessageRepository
) : MessageRouter {

    // 已初始化的消息处理器
    private val handlers = mutableMapOf<Long, MessageHandler>()

    // 缓存消息（处理器未准备好前暂存）
    private val messageBuffer = mutableMapOf<Long, MutableList<ChatMessage>>()

    /**
     * 注册消息处理器
     * 避免重复进入页面重复刷新
     */
    override fun registerHandler(conversationId: Long, handler: MessageHandler) {
        handlers[conversationId] = handler
        // 若缓存中有消息，立即投递并清除
        messageBuffer.remove(conversationId)?.forEach { message ->
            handler.onMessageReceived(MessageWrapper(message))
        }
    }

    override fun unregisterHandler(conversationId: Long) {
        handlers.remove(conversationId)
    }

    /**
     * 路由消息
     * 将消息根据会话ID路由到对应的处理器中
     */
    override fun routeMessage(pkg: BaseMessagePkg) {
        Napier.i("Routing message: $pkg")

        // 处理ACK消息
        pkg.ack?.let { ack ->
            val conversationId = ack.conversationId
            val handler = handlers[conversationId]
            if (handler != null) {
                // 处理ACK逻辑
                Napier.i("Processing ACK for conversation: $conversationId")
            }
        }

        // 处理普通消息
        pkg.message?.let { chatMessage ->
            val conversationId = chatMessage.conversationId
            val handler = handlers[conversationId]
            
            if (handler != null) {
                Napier.i("Route to conversation: $conversationId")
                handler.onMessageReceived(MessageWrapper(chatMessage))
            } else {
                // 处理器不存在，先缓存在内存中，同时更新数据库
                Napier.i("Buffer message for conversation: $conversationId")
                
                // 即使没有注册的处理器，也要将消息存储到数据库中
                chatMessageRepository.insertOrUpdateMessage(MessageWrapper(chatMessage))
                
                // 同时缓存到内存中，以便后续注册时使用
                messageBuffer.getOrPut(conversationId) { mutableListOf() }.add(chatMessage)
            }
        }
    }
}
