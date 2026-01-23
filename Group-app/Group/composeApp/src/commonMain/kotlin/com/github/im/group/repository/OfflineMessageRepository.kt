package com.github.im.group.repository

import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import db.OfflineMessage
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 离线消息仓库，负责管理待发送的消息
 */
class OfflineMessageRepository(
    private val db: AppDatabase
) {
    
    /**
     * 保存离线消息到数据库
     */
    fun saveOfflineMessage(
        clientMsgId: String,
        conversationId: Long?,
        fromUserId: Long,
        toUserId: Long?,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        filePath: String? = null,
        fileSize: Long? = null,
        fileDuration: Int = 0
    ) {
        try {
            val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            db.offlineMessageQueries.insertOfflineMessage(
                client_msg_id = clientMsgId,
                conversation_id = conversationId,
                from_user_id = fromUserId,
                to_user_id = toUserId,
                content = content,
                message_type = messageType,
                file_path = filePath,
                file_size = fileSize,
                file_duration = fileDuration.toLong(),
                status = MessageStatus.SENDING,  // 修正：发送时状态应为SENDING
                created_at = currentTime,
                retry_count = 0,
                max_retry_count = 3
            )
            Napier.d("已保存离线消息: $clientMsgId")
        } catch (e: Exception) {
            Napier.e("保存离线消息失败: $clientMsgId", e)
        }
    }
    
    /**
     * 根据状态查询离线消息
     */
    fun getOfflineMessagesByStatus(status: MessageStatus): List<OfflineMessage> {
        return db.offlineMessageQueries.selectOfflineMessagesByStatus(status).executeAsList()
    }
    
    /**
     * 查询所有 未成功的离线消息
     * SENDING
     * FAILED
     */
    fun getPendingOfflineMessages(): List<OfflineMessage> {
        return db.offlineMessageQueries.selectSendingOfflineMessages().executeAsList()
    }
    
    /**
     * 根据clientMsgId查询离线消息
     */
    fun getOfflineMessageByClientMsgId(clientMsgId: String): OfflineMessage? {
        return db.offlineMessageQueries.selectOfflineMessageByClientMsgId(clientMsgId).executeAsOneOrNull()
    }
    
    /**
     * 更新离线消息状态
     */
    fun updateOfflineMessageStatus(clientMsgId: String, status: MessageStatus, incrementRetry: Boolean = false) {
        try {
            val message = getOfflineMessageByClientMsgId(clientMsgId)
            if (message != null) {
                val newRetryCount = if (incrementRetry) message.retry_count?.plus(1) else message.retry_count
                db.offlineMessageQueries.updateOfflineMessageStatus(
                    status = status,
                    retry_count = newRetryCount,
                    client_msg_id = clientMsgId
                )
                Napier.d("已更新离线消息状态: $clientMsgId -> $status, 重试次数: $newRetryCount")
            }
        } catch (e: Exception) {
            Napier.e("更新离线消息状态失败: $clientMsgId", e)
        }
    }
    
    /**
     * 更新离线消息的会话ID（当会话创建成功后）
     */
    fun updateOfflineMessageConversationId(clientMsgId: String, conversationId: Long) {
        try {
            db.offlineMessageQueries.updateOfflineMessageConversationId(
                conversation_id = conversationId,
                client_msg_id = clientMsgId
            )
            Napier.d("已更新离线消息会话ID: $clientMsgId -> $conversationId")
        } catch (e: Exception) {
            Napier.e("更新离线消息会话ID失败: $clientMsgId", e)
        }
    }
    
    /**
     * 删除离线消息
     */
    fun deleteOfflineMessage(clientMsgId: String) {
        try {
            db.offlineMessageQueries.deleteOfflineMessage(clientMsgId)
            Napier.d("已删除离线消息: $clientMsgId")
        } catch (e: Exception) {
            Napier.e("删除离线消息失败: $clientMsgId", e)
        }
    }
    
    /**
     * 删除已发送的离线消息
     */
    fun deleteSentOfflineMessages() {
        try {
            db.offlineMessageQueries.deleteSentOfflineMessages()
            Napier.d("已删除已发送的离线消息")
        } catch (e: Exception) {
            Napier.e("删除已发送的离线消息失败", e)
        }
    }
    
    /**
     * 清理失败次数过多的消息
     */
    fun cleanupFailedMessages() {
        try {
            db.offlineMessageQueries.cleanupFailedMessages()
            Napier.d("已清理失败次数过多的离线消息")
        } catch (e: Exception) {
            Napier.e("清理失败的离线消息失败", e)
        }
    }
    
    /**
     * 获取所有离线消息
     */
    fun getAllOfflineMessages(): List<OfflineMessage> {
        return db.offlineMessageQueries.selectAll().executeAsList()
    }
}