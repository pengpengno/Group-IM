package com.github.im.group.ui

import kotlinx.serialization.Serializable

@Serializable
data class ChatRoom(
    val roomId: Long,
    val type: ChatRoomType
) {

    fun conversation() = ChatRoom(roomId, ChatRoomType.CONVERSATION)


    fun createPrivate() = ChatRoom(roomId, ChatRoomType.CREATE_PRIVATE)

    fun bot() = ChatRoom(roomId, ChatRoomType.BOT)

//
//    /**
//     * 创建私聊的聊天室
//     */
//    @Serializable
//    data class CreatePrivate(val friendUserId: Long) : ChatRoom()
//
//    /**
//     * 已经存在的会话
//     */
//    @Serializable
//    data class Conversation(val conversationId: Long) : ChatRoom()
}
fun conversation(roomId: Long):ChatRoom{
    return ChatRoom(roomId, ChatRoomType.CONVERSATION)
}
fun createPrivate(friendUserId: Long):ChatRoom{
    return ChatRoom(friendUserId, ChatRoomType.CREATE_PRIVATE)
}

fun botConversation(roomId: Long): ChatRoom {
    return ChatRoom(roomId, ChatRoomType.BOT)
}


@Serializable
enum class ChatRoomType {

    CONVERSATION,

    CREATE_PRIVATE,

    BOT
}


@Serializable
object Login

@Serializable
object Home

@Serializable
object ProxySetting

@Serializable
object Contacts

@Serializable
object Meetings

@Serializable
object Profile

@Serializable
object Settings

@Serializable
object Search

@Serializable
object AddFriend

@Serializable
data class VideoCall(
    val userId: Long,
)

class Routing {
}
