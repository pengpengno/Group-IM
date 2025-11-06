package com.github.im.group

import android.content.Context
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import io.github.aakira.napier.Napier

class AndroidCredentialStorage(private val context: Context) : CredentialStorage {

    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    override suspend fun saveUserInfo(userInfo: UserInfo) {
        withContext(Dispatchers.IO) {  // 切换到IO线程
            Napier.d("Saving user info: $userInfo")
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
        if (!prefs.contains("userId")) {
            return null
        }
        return UserInfo(
            userId = prefs.getLong("userId", 0L),
            username = prefs.getString("username", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            token = prefs.getString("token", "") ?:"",
            refreshToken = prefs.getString("refreshToken", "") ?:"",
        )
    }


    override suspend fun clearUserInfo() {
        prefs.edit { clear() }
    }

}
