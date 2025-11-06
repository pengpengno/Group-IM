package com.github.im.group

import com.github.im.group.model.UserInfo

//  身份凭证
interface CredentialStorage {

    /**
     * 保存用户信息
     */
    suspend fun saveUserInfo(userInfo: UserInfo)

    /**
     * 读取用户信息
     */
    suspend fun getUserInfo(): UserInfo?


    /**
     * 清理登录信息
     * 推出登录路的时候调用
     */
    suspend fun clearUserInfo()

}
