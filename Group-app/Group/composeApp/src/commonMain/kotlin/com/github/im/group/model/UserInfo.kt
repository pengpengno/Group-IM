package com.github.im.group.model

import kotlinx.serialization.Serializable


@Serializable
data class UserInfo(
    val userId: Long,
    val username: String ,
    val email: String,
    val token: String = "",
    val refreshToken: String = "",
    val companyId: Long? = null,
    val phoneNumber: String? = null,
    val currentLoginCompanyId: Long? = null

)

/**
 * 转换为Protobuf格式
 */
fun UserInfo.toUserInfo(): com.github.im.group.model.proto.UserInfo {
    return com.github.im.group.model.proto.UserInfo(
        userId = userId,
        username = username,
        eMail = email,
        accessToken = refreshToken,
    )
}

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
