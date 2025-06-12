package com.github.im.group.model

import kotlinx.serialization.Serializable


@Serializable
data class UserInfo(
    val userId: Long = 0,
    val username: String = "",
    val email: String = "",
    val token: String = "",
    val refreshToken: String = ""
)
