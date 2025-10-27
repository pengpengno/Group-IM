package com.github.im.group

import android.content.Context
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class AndroidCredentialStorage(private val context: Context) : CredentialStorage {

    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    override suspend fun saveUserInfo(userInfo: UserInfo) {
        withContext(Dispatchers.IO) {  // 切换到IO线程
            prefs.edit().apply {
                putLong("userId", userInfo.userId)
                putString("username", userInfo.username)
                putString("token", userInfo.token)
                putString("refreshToken", userInfo.refreshToken)
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
            token = prefs.getString("token", "") ?:"",
            refreshToken = prefs.getString("refreshToken", "") ?:"",
        )
    }

    override suspend fun autoLogin(status: Boolean): Boolean {
        withContext(Dispatchers.IO) {  // 切换到IO线程
            prefs.edit().apply {
                putBoolean("autoLogin", status)

                apply()
            }
        }
        return status
    }

    override  fun autoLoginState(): Boolean {
        return  prefs.getBoolean("autoLogin", false)
//        return withContext(Dispatchers.IO) {  // 切换到IO线程
//
//        }
    }

    override suspend fun clearUserInfo() {
        prefs.edit { clear() }
    }

}
