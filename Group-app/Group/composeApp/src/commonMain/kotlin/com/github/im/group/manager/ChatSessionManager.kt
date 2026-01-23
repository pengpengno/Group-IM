package com.github.im.group.manager


/**
 * 客户端 会话的管理
 */
import com.github.im.group.db.AppDatabase
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.viewmodel.ChatRoomViewModel
import io.github.aakira.napier.Napier

class ChatSessionManager  (
    private val db: AppDatabase,
    private val chatMessageRepository: ChatMessageRepository
){

    // 已初始化的 VM
    private val sessionMap = mutableMapOf<Long, ChatRoomViewModel>()

    // 缓存消息（ViewModel 未准备好前暂存）
    private val messageBuffer = mutableMapOf<Long, MutableList<ChatMessage>>()

    /**
     * 注册会话
     * 避免重复进入页面重复刷新
     */
    fun register(conversationId: Long, viewModel: ChatRoomViewModel) {

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
        Napier.i ( "Received message: $pkg")

        pkg.ack ?.let {

            // 回执 ACK 消息 更新 本地数据库 ，更新对应消息状态
            val conversationId = it.conversationId

            val vm = sessionMap[conversationId]
            if (vm != null) {
                it.clientMsgId.let { clientMsgId ->
//                    vm.receiveAckUpdateStatus(clientMsgId,it.ackTimestamp)
                }
//                vm.onReceiveMessage(it.clientMsgId)
            } else {
            }
            // 更新对应信息的 ui 状态
        }

        pkg.message ?.let {

            val targetId = it.conversationId
            val vm = sessionMap[targetId]
            if (vm != null) {
                Napier.i ( "route to conversation : $targetId")

                vm.onReceiveMessage(MessageWrapper(it))
            } else {
                // VM 不存在，先缓存在内存中，同时更新数据库
                Napier.i ( "route to conversation : $targetId")
                
                // 即使没有注册的ViewModel，也要将消息存储到数据库中
                chatMessageRepository.insertOrUpdateMessage(MessageWrapper(it))
                
                // 同时缓存到内存中，以便后续注册时使用
                messageBuffer.getOrPut(targetId) { mutableListOf() }.add(it)
            }
        }

    }
}
