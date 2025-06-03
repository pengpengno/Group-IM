package com.github.im.group

import android.content.Context
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCredentialStorage(private val context: Context) : CredentialStorage {

    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    override suspend fun saveUserInfo(userInfo: UserInfo) {
        // 这里模拟写入SharedPreferences的异步操作
        withContext(Dispatchers.IO) {  // 切换到IO线程
            prefs.edit().apply {
                putLong("userId", userInfo.userId)
                putString("username", userInfo.username)
                putString("token", userInfo.token)
                apply()
            }
        }
    }

    override suspend fun getUserInfo(): UserInfo? {
        val userId = prefs.getLong("userId", -1)
        if (userId == -1L) return null

        return UserInfo(
            userId = userId,
            username = prefs.getString("username", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            token = prefs.getString("token", null),
            refreshToken = prefs.getString("refreshToken", null)
        )
    }

    override suspend fun clearUserInfo() {
        prefs.edit().clear().apply()
    }

}
