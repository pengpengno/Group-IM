package com.github.im.group.ui

import kotlinx.serialization.Serializable


@Serializable
object Login

@Serializable
object Chat

@Serializable
object Home

@Serializable
data class ChatRoom(
    val conversationId: Long,
)


@Serializable
object Contacts

@Serializable
object Profile

@Serializable
object Settings

class Routing {
}