package com.github.im.group.ui

import kotlinx.serialization.Serializable


@Serializable
object Login

@Serializable
object Chat

@Serializable
object Home

@Serializable
object ProxySetting

@Serializable
data class ChatRoom(
    val conversationId: Long,
)
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