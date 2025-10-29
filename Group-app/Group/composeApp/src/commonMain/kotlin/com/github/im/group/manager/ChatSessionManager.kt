package com.github.im.group.manager

import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.viewmodel.ChatMessageViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


/**
 * 客户端 会话的管理
 */
class ChatSessionManager  (
    private val db: AppDatabase
){

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
        pkg.ack ?.let {

            // 回执 ACK 消息 更新 本地数据库 ，更新对应消息状态
            val conversationId = it.conversationId
            // ACK 确认收到消息 表明发送成功， 更新数据库对应消息状态
            val ackLocalDateTime: LocalDateTime =
                Instant.fromEpochMilliseconds(it.ackTimestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            db.messageQueries
                .updateMessageByClientMsgId(MessageStatus.SENT,
                    ackLocalDateTime,
                    it.serverMsgId, it.serverMsgId,
                    client_msg_id = it.clientMsgId)
            val vm = sessionMap[conversationId]
            if (vm != null) {
                vm.updateMessage(it.clientMsgId)
            } else {

            }
            // 更新对应信息的 ui 状态
        }


        pkg.message ?.let {

            val targetId = it.conversationId
            val vm = sessionMap[targetId]
            if (vm != null) {
                vm.onReceiveMessage(MessageWrapper(it))
            } else {
                // VM 不存在，先缓存在内存中
                messageBuffer.getOrPut(targetId) { mutableListOf() }.add(it)
            }
        }

    }
}
