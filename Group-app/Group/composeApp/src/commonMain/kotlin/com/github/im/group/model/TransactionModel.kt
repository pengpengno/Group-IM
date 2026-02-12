package com.github.im.group.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 本地事务实体，用于保证聊天操作的原子性
 */
@Serializable
data class Transaction(
    val id: String = generateTransactionId(),
    val type: TransactionType,
    val payload: String, // JSON序列化的操作数据
    val status: TransactionStatus = TransactionStatus.PENDING,
    val priority: TransactionPriority = TransactionPriority.NORMAL,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val errorMessage: String? = null
)

/**
 * 事务类型枚举
 */
enum class TransactionType {
    CREATE_CONVERSATION,    // 创建会话
    SEND_MESSAGE,          // 发送消息
    UPLOAD_FILE,           // 上传文件
    UPDATE_MESSAGE_STATUS, // 更新消息状态
    SYNC_CONVERSATION      // 同步会话
}

/**
 * 事务状态枚举
 */
enum class TransactionStatus {
    PENDING,      // 待处理
    PROCESSING,   // 处理中
    SUCCESS,      // 成功
    FAILED,       // 失败
    CANCELLED     // 已取消
}

/**
 * 事务优先级枚举
 */
enum class TransactionPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * 会话预创建结果
 */
sealed class SessionCreationResult {
    data class Success(val conversationId: Long) : SessionCreationResult()
    data class Error(val message: String, val exception: Exception? = null) : SessionCreationResult()
}

/**
 * 统一消息发送结果
 */
sealed class MessageSendResult {
    data class Success(val clientMsgId: String) : MessageSendResult()
    data class Error(val message: String, val exception: Exception? = null) : MessageSendResult()
}

/**
 * 生成唯一的事务ID
 */
private fun generateTransactionId(): String {
    return "txn_${Clock.System.now().toEpochMilliseconds()}_${(100000..999999).random()}"
}