package com.github.im.group.model

import com.github.im.group.api.FileMeta
import com.github.im.group.api.MessageDTO
import com.github.im.group.api.extraAs
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.db.entities.UserStatus
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.ChatMessage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class Chat {
}

interface MessageItem {
    /**
     * 消息ID 服务端生产的id
     */
    val id: Long

    /**
     * 会话ID
     */
    val conversationId:Long

    /**
     * 客户端发送时生成的 MsgId
     */
    val clientMsgId : String

    /**
     * 客户端发送消息的时间
     * TODO 目前只有本地发送出去的数据 会存
     */
    val clientTime: LocalDateTime?

    /**
     * 服务端的消息时间
     */
    val time: LocalDateTime

    val userInfo : UserInfo
    /**
     *  服务端生成的 序列号
     */
    val seqId : Long

    /**
     * 消息类型
     */
    val type: MessageType

    /**
     * 消息状态
     */
    val status : MessageStatus

    val content: String

    /**
     * 如果是文件类型的消息，返回文件元数据
     * 可能存在是文件类型但是没有文件元数据 ，如TCP 发送消息的时候 只会 携带其fileId ，需要通过API 再次查找
     * 不是 则返回 null
     */
    val fileMeta : FileMeta?

}



data class MessageWrapper(
    val message: ChatMessage? = null,
    val messageDto: MessageDTO? = null
) : MessageItem {


    override val clientTime: LocalDateTime?
        get() = when {
            message?.clientTimeStamp != null -> {
                val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(message.clientTimeStamp)
                instant.toLocalDateTime(TimeZone.currentSystemDefault())
            }
            else -> null
        }
    override val status: MessageStatus
        get() = when{
            message?.messagesStatus != null -> MessageStatus.valueOf(message.messagesStatus.name)
            messageDto?.status != null -> messageDto.status
            else -> MessageStatus.SENDING
        }

    override val fileMeta: FileMeta?
        get() = when {

            messageDto?.type?.isFile() == true
                 -> {
                messageDto.extraAs<FileMeta>()
            }
            else -> null
        }
    override val conversationId: Long = message?.conversationId ?: messageDto?.conversationId ?: 0L

    override val id: Long = message?.msgId ?: messageDto?.msgId ?: 0L

    override val clientMsgId: String = message?.clientMsgId ?: messageDto?.clientMsgId ?: ""

    override val time: LocalDateTime =
        LocalDateTime.parse( messageDto?.timestamp ?:"1970-01-01T00:00:00.000")

    override val seqId: Long = message?.sequenceId ?: messageDto?.sequenceId ?: 0L

    override val type: com.github.im.group.db.entities.MessageType
        get() = when{
        message?.type != null -> com.github.im.group.db.entities.MessageType.valueOf(message.type.name)
        messageDto?.type != null -> messageDto.type
//        messageDto?.content?.startsWith("http") == true -> MessageType.FILE
        else -> MessageType.TEXT
    }

    override val content: String
        get() = when{
            message?.type != null -> message.content
            messageDto?.content != null -> messageDto.content
            else -> ""
        }

    override val userInfo: UserInfo
        get() = when{
            message?.fromAccountInfo != null -> accountInfoTransForm(message.fromAccountInfo)
            messageDto?.fromAccount != null -> messageDto.fromAccount
            else -> UserInfo()
        }
}

fun accountInfoTransForm ( accountInfo: AccountInfo) : UserInfo{
    return UserInfo(
        accountInfo.userId,
        accountInfo.account,
        accountInfo.eMail,
    )
}