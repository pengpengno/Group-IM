package com.github.im.group.repository

import com.github.im.group.model.UserInfo

class UserRepository {
    private var currentUser: UserInfo? = null

    fun saveUser(user: UserInfo) {
        currentUser = user
    }

    fun getUser(): UserInfo? = currentUser


    fun clearUser() {
        currentUser = null
    }
}