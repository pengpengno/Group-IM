package com.github.im.group

import com.github.im.group.model.UserInfo

object DefaultCredentialStorage : CredentialStorage {

    private var currentUserInfo: UserInfo? = null


    override suspend fun saveUserInfo(userInfo: UserInfo) {
        currentUserInfo = userInfo
    }

    override suspend fun getUserInfo(): UserInfo? {
        return currentUserInfo
    }

    override suspend fun clearUserInfo() {
        currentUserInfo = null
    }
}