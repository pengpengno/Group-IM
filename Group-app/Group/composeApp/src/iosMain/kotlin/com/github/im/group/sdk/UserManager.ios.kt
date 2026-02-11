package com.github.im.group.sdk

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS平台用户管理器实现
 */
class IosUserManager(private val dataStorage: DataStorage) : UserManager {
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    override val currentUser: StateFlow<UserInfo?> = _currentUser

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    companion object {
        private const val CURRENT_USER_KEY = "current_user"
        private const val AUTH_TOKEN_KEY = "auth_token"
        private const val IS_LOGGED_IN_KEY = "is_logged_in"
    }

    init {
        // 从存储中恢复登录状态
        restoreLoginState()
    }

    private fun restoreLoginState() {
        val isLoggedInValue = dataStorage.getBoolean(IS_LOGGED_IN_KEY, false)
        _isLoggedIn.value = isLoggedInValue
        
        if (isLoggedInValue) {
            // 尝试恢复用户信息
            val userJson = dataStorage.getString(CURRENT_USER_KEY, "")
            if (userJson.isNotEmpty()) {
                // 实际的JSON反序列化将在后续开发中完成
                // 这里暂时设置为null
                _currentUser.value = null
            }
        }
    }

    override suspend fun login(username: String, password: String): LoginResult {
        Napier.d("iOS: Logging in user: $username")
        
        // 实际登录逻辑将在后续开发中完成
        // 这里返回模拟成功结果
        val mockUser = UserInfo(
            userId = 12345L,
            username = username,
            email = "$username@example.com",
            fullName = "User $username"
        )
        
        // 模拟登录成功
        _currentUser.value = mockUser
        _isLoggedIn.value = true
        
        // 保存登录状态到持久存储
        dataStorage.putBoolean(IS_LOGGED_IN_KEY, true)
        dataStorage.putString(CURRENT_USER_KEY, "") // 实际应该保存JSON序列化的用户信息
        
        return LoginResult.Success(mockUser, "mock_token_for_ios")
    }

    override suspend fun loginWithToken(token: String): LoginResult {
        Napier.d("iOS: Logging in with token")
        
        // 实际验证令牌的逻辑将在后续开发中完成
        // 这里假设令牌有效
        val mockUser = UserInfo(
            userId = 12345L,
            username = "mock_user",
            email = "mock@example.com",
            fullName = "Mock User"
        )
        
        _currentUser.value = mockUser
        _isLoggedIn.value = true
        
        // 保存登录状态
        dataStorage.putBoolean(IS_LOGGED_IN_KEY, true)
        
        return LoginResult.Success(mockUser, token)
    }

    override suspend fun register(userInfo: RegistrationInfo): RegisterResult {
        Napier.d("iOS: Registering user: ${userInfo.username}")
        
        // 实际注册逻辑将在后续开发中完成
        val mockUser = UserInfo(
            userId = 67890L,
            username = userInfo.username,
            email = userInfo.email,
            fullName = userInfo.fullName ?: userInfo.username
        )
        
        return RegisterResult.Success(mockUser)
    }

    override suspend fun logout() {
        Napier.d("iOS: Logging out user")
        
        _currentUser.value = null
        _isLoggedIn.value = false
        
        // 清除持久存储中的登录信息
        dataStorage.remove(IS_LOGGED_IN_KEY)
        dataStorage.remove(CURRENT_USER_KEY)
        dataStorage.remove(AUTH_TOKEN_KEY)
    }

    override suspend fun updateUserInfo(userInfo: UserInfo): UpdateResult {
        Napier.d("iOS: Updating user info for: ${userInfo.username}")
        
        // 实际更新逻辑将在后续开发中完成
        _currentUser.value = userInfo
        
        // 更新存储中的用户信息
        dataStorage.putString(CURRENT_USER_KEY, "") // 实际应该保存JSON序列化的用户信息
        
        return UpdateResult.Success
    }

    override suspend fun getCurrentUser(): UserInfo? {
        return _currentUser.value
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): ChangePasswordResult {
        Napier.d("iOS: Changing password for user")
        
        // 实际更改密码逻辑将在后续开发中完成
        return ChangePasswordResult.Success
    }

    override suspend fun getUserPreferences(): UserPreferences {
        Napier.d("iOS: Getting user preferences")
        
        // 从存储中获取用户偏好设置
        val themeOrdinal = dataStorage.getInt("theme", Theme.SYSTEM.ordinal)
        val language = dataStorage.getString("language", "en")
        val notificationsEnabled = dataStorage.getBoolean("notifications_enabled", true)
        
        val theme = Theme.values().getOrElse(themeOrdinal) { Theme.SYSTEM }
        
        return UserPreferences(
            theme = theme,
            language = language,
            notificationsEnabled = notificationsEnabled
        )
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences) {
        Napier.d("iOS: Updating user preferences")
        
        // 保存用户偏好设置到存储
        dataStorage.putInt("theme", preferences.theme.ordinal)
        dataStorage.putString("language", preferences.language)
        dataStorage.putBoolean("notifications_enabled", preferences.notificationsEnabled)
    }
}