package com.github.im.group

import com.github.im.group.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow

object DefaultCredentialStorage : CredentialStorage {

    private var currentUserInfo: UserInfo? = null

    private var autoLogin : Boolean = false


    override suspend fun saveUserInfo(userInfo: UserInfo) {
        currentUserInfo = userInfo
    }

    override suspend fun getUserInfo(): UserInfo? {
        return currentUserInfo
    }


    override suspend fun autoLogin(status: Boolean): Boolean {
        autoLogin = status
        return  autoLogin
    }

    override  fun autoLoginState(): Boolean {
        return autoLogin;
    }



    override suspend fun clearUserInfo() {
        currentUserInfo = null
    }
}