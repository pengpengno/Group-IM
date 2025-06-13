package com.github.im.group.ui

import com.github.im.group.model.UserInfo
import kotlinx.serialization.Serializable


@Serializable
object Login

@Serializable
object Chat

@Serializable
data class Home(
    val userInfo: UserInfo,
)



@Serializable
object Settings

class Routing {
}