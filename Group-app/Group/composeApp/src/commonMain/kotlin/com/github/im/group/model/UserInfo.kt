package com.github.im.group.model

import kotlinx.serialization.Serializable


@Serializable
data class UserInfo(
    val userId: Long = 0,
    val username: String = "",
    val email: String = "",
    val token: String = "",
    val refreshToken: String = "",
    val companyId: Long? = null,
    val phoneNumber: String? = null,
    val currentLoginCompanyId: Long? = null

)

fun defaultUserInfo(): UserInfo {
    return UserInfo(
        userId = 0,
        username = "",
        email = "",
        token = "",
        refreshToken = "",
        companyId = null,
        phoneNumber = null,
        currentLoginCompanyId = null
    )
}
