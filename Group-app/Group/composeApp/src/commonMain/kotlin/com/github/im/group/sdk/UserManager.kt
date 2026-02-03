package com.github.im.group.sdk

import kotlinx.coroutines.flow.StateFlow

/**
 * 用户信息管理接口
 */
interface UserManager {
    val currentUser: StateFlow<UserInfo?>
    val isLoggedIn: StateFlow<Boolean>
    
    /**
     * 登录用户
     */
    suspend fun login(username: String, password: String): LoginResult
    
    /**
     * 使用令牌登录
     */
    suspend fun loginWithToken(token: String): LoginResult
    
    /**
     * 注册新用户
     */
    suspend fun register(userInfo: RegistrationInfo): RegisterResult
    
    /**
     * 注销用户
     */
    suspend fun logout()

    /**
     * 获取当前用户信息
     */
    suspend fun getCurrentUser(): UserInfo?
    
//    /**
//     * 更改密码
//     */
//    suspend fun changePassword(oldPassword: String, newPassword: String): ChangePasswordResult
    
    /**
     * 获取用户配置
     */
    suspend fun getUserPreferences(): UserPreferences
    
    /**
     * 更新用户配置
     */
    suspend fun updateUserPreferences(preferences: UserPreferences)
}

/**
 * 用户信息数据类
 */
data class UserInfo(
    val userId: Long,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L,
    val isActive: Boolean = true
)

/**
 * 注册信息
 */
data class RegistrationInfo(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String? = null,
    val phoneNumber: String? = null
)

/**
 * 登录结果
 */
sealed class LoginResult {
    data class Success(val userInfo: UserInfo, val token: String) : LoginResult()
    data class Error(val message: String, val errorCode: String? = null) : LoginResult()
}

/**
 * 注册结果
 */
sealed class RegisterResult {
    data class Success(val userInfo: UserInfo) : RegisterResult()
    data class Error(val message: String, val errorCode: String? = null) : RegisterResult()
}

/**
 * 密码更改结果
 */
sealed class ChangePasswordResult {
    object Success : ChangePasswordResult()
    data class Error(val message: String) : ChangePasswordResult()
}


/**
 * 用户偏好设置
 */
data class UserPreferences(
    val theme: Theme = Theme.SYSTEM,
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val privacySettings: PrivacySettings = PrivacySettings(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)

/**
 * 主题枚举
 */
enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * 隐私设置
 */
data class PrivacySettings(
    val profileVisible: Boolean = true,
    val showLastSeen: Boolean = true,
    val allowContactFromStrangers: Boolean = true
)

/**
 * 通知设置
 */
data class NotificationSettings(
    val enablePushNotifications: Boolean = true,
    val enableSound: Boolean = true,
    val enableVibration: Boolean = true,
    val muteUntil: Long = 0L // 时间戳，表示静音到何时
)