package com.github.im.group.model

import com.github.im.group.api.ChatMessageType
import com.github.im.group.api.MessageDTO
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import kotlinx.datetime.LocalDateTime

class Chat {
}

interface MessageItem {
    val id: Long
    val time: LocalDateTime

    val userInfo : UserInfo
    /**
     * 序列号
     */
    val seqId : Long

    // 可选：用于 UI 渲染的类型标识
    val type: ChatMessageType

    val content: String
}
data class MessageWrapper(
    val message: ChatMessage? = null,
    val messageDto: MessageDTO? = null
) : MessageItem {

    override val id: Long = message?.msgId ?: messageDto?.msgId ?: 0L

    override val time: LocalDateTime =
        LocalDateTime.parse( messageDto?.timestamp ?:"1970-01-01T00:00:00.000")

    override val seqId: Long = message?.sequenceId ?: messageDto?.sequenceId ?: 0L

    override val type: ChatMessageType get() = when{
        message?.type != null -> typeTransForm(message.type)
        messageDto?.type != null -> messageDto.type
//        messageDto?.content?.startsWith("http") == true -> ChatMessageType.FILE
        else -> ChatMessageType.TEXT
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

/**
 * 转化
 */
fun typeTransForm ( type: MessageType) : ChatMessageType{

    return when(type){
        MessageType.TEXT -> ChatMessageType.TEXT
        MessageType.FILE -> ChatMessageType.FILE
//        MessageType.VOICE -> ChatMessageType.VOICE
        MessageType.MARKDOWN -> TODO()
        MessageType.STREAM -> TODO()
        MessageType.VIDEO -> TODO()
        MessageType.IMAGE -> TODO()
    }
}
