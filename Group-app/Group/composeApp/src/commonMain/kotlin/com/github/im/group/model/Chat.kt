package com.github.im.group.model

import com.github.im.group.api.FileMeta
import com.github.im.group.api.MessageDTO
import com.github.im.group.api.MessagePayLoad
import com.github.im.group.api.extraAs
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessagesStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

class Chat {
}

interface MessageItem {
    val id: Long

    val conversationId:Long

    val clientMsgId : String

    val time: LocalDateTime

    val userInfo : UserInfo
    /**
     * 序列号
     */
    val seqId : Long

    // 可选：用于 UI 渲染的类型标识
    val type: MessageType

    val status : MessageStatus?

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



    override val status: MessageStatus?
        get() = when{
           
            message?.messagesStatus != null -> MessageStatus.valueOf(message.messagesStatus.name)
            messageDto?.status != null -> messageDto.status
            else -> null
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
