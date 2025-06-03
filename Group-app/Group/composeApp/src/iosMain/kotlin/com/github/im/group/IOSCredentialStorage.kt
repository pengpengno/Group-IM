package com.github.im.group

import com.github.im.group.model.UserInfo

class IOSCredentialStorage(private val settings: Settings) : CredentialStorage {
    override suspend fun saveUserInfo(userInfo: UserInfo) {
        settings.putLong("userId", userInfo.userId)
        settings.putString("username", userInfo.username)
        settings.putString("email", userInfo.email)
        settings.putString("token", userInfo.token)
        settings.putString("refreshToken", userInfo.refreshToken)
    }

    override suspend fun getUserInfo(): UserInfo? {
        val userId = settings.getLong("userId", -1L)
        if (userId == -1L) return null

        return UserInfo(
            userId = userId,
            username = settings.getString("username", "") ?: "",
            email = settings.getString("email", "") ?: "",
            token = settings.getString("token", null),
            refreshToken = settings.getString("refreshToken", null)
        )
    }

    override suspend fun clearUserInfo() {
        settings.clear()
    }
}
