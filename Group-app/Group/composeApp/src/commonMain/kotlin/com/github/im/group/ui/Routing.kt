package com.github.im.group.ui

import kotlinx.serialization.Serializable


@Serializable
object Login


@Serializable
object Home

@Serializable
object ProxySetting



@Serializable
open class ChatRoom(

){

    /**
     * 创建私聊的聊天室
     */
    @Serializable
    data class CreatePrivate(val friendUserId: Long) : ChatRoom()
    /**
     * 已经存在的会话
     */
    @Serializable
    data class Conversation(val conversationId: Long): ChatRoom()

}
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