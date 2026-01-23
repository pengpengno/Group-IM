package com.github.im.group.repository

import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.UserStatus
import com.github.im.group.model.UserInfo
import com.github.im.group.viewmodel.LoginState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


/**
 * 当前登录用户
 */
data class CurrentUserInfoContainer(
    val user: UserInfo,
)

class UserRepository (
    private val db:AppDatabase
){

    private val _userState = MutableStateFlow<LoginState>(LoginState.Idle)
    val userState = _userState.asStateFlow()

    /**
     * 获取当前本地的用户
     */

    suspend fun getLocalUserInfo(): UserInfo? {
        return GlobalCredentialProvider.storage.getUserInfo()
    }

//
//    /**
//     * 获取当前用户信息（如果已登录）
//     */
//    fun getCurrentUserInfo(): UserInfo? {
//        return when (val state = userState.value) {
//            is LoginState.Authenticated -> state.userInfo
//            else -> null
//        }
//    }
////
//    /**
//     * 安全执行需要登录用户的操作，只有在用户已登录时才执行
//     */
//    inline fun <T> withLoggedInUser(action: (UserInfo) -> T): T? {
//        val user = getCurrentUserInfo()
//        return if (user != null) {
//            action(user)
//        } else {
//            null
//        }
//    }

//
//    /**
//     * 异步获取当前用户信息，包括从本地存储检索
//     */
//    suspend fun getCurrentUserWithLocalFallback(): UserInfo? {
//        // 首先检查当前状态
//        val currentStateUser = getCurrentUserInfo()
//        if (currentStateUser != null) {
//            return currentStateUser
//        }
//
//        // 如果状态中没有，则从本地存储获取
//        return GlobalCredentialProvider.storage.getUserInfo()
//    }

    /**
     * 更新用户状态为检查中
     */
    fun updateToChecking() {
        _userState.value = LoginState.Checking
    }
    
    /**
     * 更新用户状态为认证中
     */
    fun updateToAuthenticating() {
        _userState.value = LoginState.Authenticating
    }
    
    /**
     * 更新用户状态为已认证
     */
    fun updateToAuthenticated(userInfo: UserInfo) {
        _userState.value = LoginState.Authenticated(userInfo)
    }
    
    /**
     * 更新用户状态为认证失败
     */
    fun updateToAuthenticationFailed(error: String, isNetworkError: Boolean = false) {
        _userState.value = LoginState.AuthenticationFailed(error, isNetworkError)
    }
    
    /**
     * 更新用户状态为登出中
     */
    fun updateToLoggingOut() {
        _userState.value = LoginState.LoggingOut
    }
    
    /**
     * 更新用户状态为登出（回到空闲状态）
     */
    fun updateToLoggedOut() {
        _userState.value = LoginState.Idle
    }

    /**
     * 保存当前用户至 数据库 ， 存在则更新信息
     * 同时 将当前用户 绑定 APP
     */
    fun saveCurrentUser(user: UserInfo) {
        updateToAuthenticated(user)
        addOrUpdateUser(user)
    }

    /**
     * 插入或更新用户信息
     * 用户不存在时才会插入， 存在则更新
     */
    fun addOrUpdateUsers(users: List<UserInfo>) {
        db.transaction {
            users.forEach { user ->
                addOrUpdateUser(user)
            }
        }
    }

    /**
     * 插入一条用户信息
     * 用户不存在时才会插入， 存在则忽略
     */
    fun addOrUpdateUser(user: UserInfo) {
        db.transaction {
            // 先检查用户是否存在
            val existingUser = db.userQueries.selectById(user.userId).executeAsOneOrNull()
            if (existingUser == null) {
                // 用户不存在则插入
                db.userQueries.insertUser(
                    userId = user.userId,
                    username = user.username,
                    email = user.email,
                    phoneNumber = "",
                    avatarUrl = null,
                    bio = null,
                    userStatus = UserStatus.ACTIVE
                )
            } else {
                db.userQueries.updateUser(
                    username = user.username,
                    email = user.email,
                    phoneNumber = "",
                    avatarUrl = null,
                    bio = null,
                    userStatus = UserStatus.ACTIVE,
                    userId = existingUser.userId
                )
            }
        }
    }
    
    /**
     * 根据用户ID获取用户信息
     * 
     * 此方法从本地数据库查询用户信息，如果本地数据库中存在对应的用户记录，
     * 则返回用户信息对象；否则返回null。
     * 
     * @param userId 需要查询的用户ID
     * @return 如果找到对应的用户记录则返回UserInfo对象，否则返回null
     * 
     * 注意：此方法只返回本地缓存的用户信息，不包含认证令牌信息（token和refreshToken）
     * 这些敏感信息应当通过安全的认证流程获取和管理
     */
    fun getUserById(userId: Long): UserInfo? {
        val userEntity = db.userQueries.selectById(userId).executeAsOneOrNull()
        return userEntity?.let {
            UserInfo(
                userId = it.userId,
                username = it.username,
                email = it.email ?: "",
                token = "",
                refreshToken = ""
            )
        }
    }
}