package com.github.im.group.sdk

import com.github.im.common.connect.model.proto.ChatMessage
import com.github.im.common.connect.model.proto.MessageType
import com.github.im.common.connect.model.proto.MessagesStatus
import com.github.im.common.connect.model.proto.UserInfo
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * V8 Architecture: ChatMessageBuilder
 *
 * 职责：纯函数式构建本地 ChatMessage Proto 对象，不依赖任何外部状态/网络/协程。
 * - 调用方（MessageFacade）负责传入已解析的 [UserInfo]，Builder 不再自行获取用户。
 * - 文件消息的构建（fileId 占位 → 换取 serverFileId）完全由 MessageFacade 负责，
 *   Builder 只提供原始的 proto 构造能力。
 */
interface ChatMessageBuilder {
    /**
     * 构建文本消息。
     * @param userInfo 已解析的 Proto UserInfo，由调用方提供，避免 Builder 内部产生网络/协程依赖。
     */
    fun textMessage(conversationId: Long, content: String, userInfo: UserInfo): ChatMessage

    /**
     * 使用指定的内容（文本或已解析的 fileId/占位ID）与消息类型构建一条消息。
     * 适用于文件、图片、语音、视频等所有非文本类型的 proto 构造。
     */
    fun buildMessage(conversationId: Long, content: String, messageType: MessageType, userInfo: UserInfo): ChatMessage
}

class ChatMessageBuilderImpl : ChatMessageBuilder {

    override fun textMessage(conversationId: Long, content: String, userInfo: UserInfo): ChatMessage {
        return buildMessage(conversationId, content, MessageType.TEXT, userInfo)
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun buildMessage(
        conversationId: Long,
        content: String,
        messageType: MessageType,
        userInfo: UserInfo
    ): ChatMessage {
        val clientMsgId = Uuid.random().toString()
        return ChatMessage(
            content = content,
            conversationId = conversationId,
            fromUser = userInfo,
            type = messageType,
            messagesStatus = MessagesStatus.SENDING,
            clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
            clientMsgId = clientMsgId
        )
    }
}