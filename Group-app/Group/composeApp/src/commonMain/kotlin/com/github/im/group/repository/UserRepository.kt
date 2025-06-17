package com.github.im.group.repository

import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.PlatformType

class UserRepository {

    private var currentUser: UserInfo? = null
    private var currentAccountInfo : AccountInfo ? = null

    fun saveUser(user: UserInfo) {
        currentUser = user
        val accountInfo = AccountInfo(
            account = user.username,
            accountName = user.username,
            userId = user.userId,
            eMail = user.email,
            platformType = PlatformType.ANDROID,
        )
        saveAccountInfo(accountInfo)
    }

    private fun saveAccountInfo(accountInfo: AccountInfo){
        currentAccountInfo = accountInfo
    }
    fun getAccountInfo(): AccountInfo? = currentAccountInfo
    fun getUser(): UserInfo? = currentUser


    fun clearUser() {
        currentUser = null
        currentAccountInfo = null
    }
}