package com.github.im.group

import com.github.im.group.model.UserInfo

//  身份凭证
interface CredentialStorage {
//    suspend 关键字表示这是一个挂起函数，它只能在协程中或另一个挂起函数里调用。
//
//    挂起函数的作用是支持异步操作，不阻塞线程。
    suspend fun saveUserInfo(userInfo: UserInfo)

    suspend fun getUserInfo(): UserInfo?

    suspend fun clearUserInfo()
}
