package com.github.im.group.manager

import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.MessageSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * V8 Architecture: MessageStore
 * 负责管理应用在内存中的 O(1) 消息索引缓存、处理与数据库和远程同步层的核心逻辑
 * 并对外暴露 StateFlow 供 UI 响应式订阅（替代手动触发刷新）
 */
class MessageStore(
    private val chatMessageRepository: ChatMessageRepository,
    private val messageSyncRepository: MessageSyncRepository
) {
    private val cache = linkedMapOf<String, MessageItem>()
    
    // 采用 StateFlow 暴露数据，完全适配 Compose 的响应式更新模型 (by remember / collectAsState)
    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    fun uniqueKeyOf(msg: MessageItem): String = when {
        msg.seqId != 0L -> "S:${msg.seqId}"
        msg.clientMsgId.isNotBlank() -> "C:${msg.clientMsgId}"
        else -> "T:${kotlin.random.Random.nextLong()}"
    }

    private fun emit() {
        _messages.value = getSortedMessages()
    }

    fun clear() {
        cache.clear()
        emit()
    }

    suspend fun loadLocal(conversationId: Long, limit: Long = 30) {
        cache.clear()
        val msgs = chatMessageRepository.getMessagesByConversation(conversationId, limit)
        msgs.forEach { cache[uniqueKeyOf(it)] = it }
        emit()
    }

    suspend fun syncRemote(conversationId: Long): Boolean {
        return messageSyncRepository.syncMessages(conversationId) > 0
    }

    suspend fun loadLatest(conversationId: Long, afterSeqId: Long) {
        val msgs = messageSyncRepository.getMessagesWithStrategy(conversationId, afterSeqId, false)
        msgs.forEach { cache[uniqueKeyOf(it)] = it }
        emit()
    }

    fun saveOrUpdate(message: MessageItem): MessageItem {
        chatMessageRepository.insertOrUpdateMessage(message)
        if (message.seqId != 0L && message.clientMsgId.isNotBlank()) {
            cache.remove("C:${message.clientMsgId}")
        }
        cache[uniqueKeyOf(message)] = message
        emit()
        return message
    }

    fun get(clientMsgId: String): MessageItem? {
        return cache["C:$clientMsgId"] ?: chatMessageRepository.getMessageByClientMsgId(clientMsgId)
    }

    private fun getSortedMessages(): List<MessageItem> {
        return cache.values.sortedWith(
            compareByDescending<MessageItem> { if (it.seqId != 0L) it.seqId else Long.MIN_VALUE }
                .thenByDescending { it.clientTime ?: it.time }
        )
    }

    fun markConversationRead(conversationId: Long, currentUserId: Long) {
        chatMessageRepository.markConversationMessagesAsRead(conversationId, currentUserId)
        var hasUpdates = false
        cache.values.toList().forEach { message ->
            val shouldMarkRead = message.userInfo.userId != currentUserId && message.status == MessageStatus.SENT
            if (shouldMarkRead && message is MessageWrapper) {
                val updated = message.withStatus(MessageStatus.READ)
                cache.remove(uniqueKeyOf(message))
                cache[uniqueKeyOf(updated)] = updated
                hasUpdates = true
            }
        }
        if (hasUpdates) emit()
    }
}
