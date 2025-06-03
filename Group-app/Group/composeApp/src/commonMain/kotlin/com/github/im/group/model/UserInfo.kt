package com.github.im.group.model

import kotlinx.serialization.Serializable


@Serializable
data class UserInfo(
    val userId: Long,
    val username: String,
    val email: String,
    val token: String? = null,
    val refreshToken: String? = null
)
