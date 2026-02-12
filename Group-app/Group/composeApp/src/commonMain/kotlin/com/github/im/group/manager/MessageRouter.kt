package com.github.im.group.manager

import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.BaseMessagePkg

/**
 * 消息路由接口
 * 负责将接收到的消息路由到对应的处理器
 */
interface MessageRouter {
    /**
     * 路由消息到对应的处理器
     * @param pkg 基础消息包
     */
    fun routeMessage(pkg: BaseMessagePkg)
    
    /**
     * 注册消息处理器
     * @param conversationId 会话ID
     * @param handler 消息处理器
     */
    fun registerHandler(conversationId: Long, handler: MessageHandler)
    
    /**
     * 注销消息处理器
     * @param conversationId 会话ID
     */
    fun unregisterHandler(conversationId: Long)
}

/**
 * 消息处理器接口
 */
interface MessageHandler {
    /**
     * 处理接收到的消息
     * @param message 消息包装器
     */
    fun onMessageReceived(message: MessageWrapper)
}