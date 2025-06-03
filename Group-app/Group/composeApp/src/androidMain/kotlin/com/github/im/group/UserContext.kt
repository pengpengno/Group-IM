package com.github.im.group

import com.github.im.group.model.UserInfo

object UserContext {
    var userInfo: UserInfo? = null

    fun getUserInfo(): UserInfo? {
        return userInfo
    }

    fun setUserInfo(userInfo: UserInfo) {
        this.userInfo = userInfo
    }


}