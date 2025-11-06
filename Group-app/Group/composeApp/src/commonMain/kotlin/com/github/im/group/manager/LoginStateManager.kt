package com.github.im.group.manager

import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.repository.UserState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.StateFlow

/**
 * 登录状态监听器接口
 */
interface LoginStateListener {
    /**
     * 当用户登录时调用
     */
    fun onLogin(userInfo: UserInfo)
    
    /**
     * 当用户登出时调用
     */
    fun onLogout()
    
    /**
     * 当登录状态发生变化时调用
     */
    fun onStateChanged()
}

/**
 * 用户数据同步监听器
 * 当用户登录或登出时同步用户数据
 */
class UserDataSyncListener(private val userRepository: UserRepository) : LoginStateListener {
    override fun onLogin(userInfo: UserInfo) {
        // 用户登录时保存用户信息到数据库
        userRepository.saveCurrentUser(userInfo)
    }

    override fun onLogout() {
        // 用户登出时可以执行清理操作

    }

    override fun onStateChanged() {
        // 状态变化时可以执行一些通用操作
    }
}

/**
 * UI状态更新监听器
 * 当登录状态变化时更新UI状态
 */
class UIStateUpdateListener(private val userRepository: UserRepository) : LoginStateListener {
    val uiState: StateFlow<UserState> = userRepository.userState

    override fun onLogin(userInfo: UserInfo) {
        // UI状态由UserRepository管理
    }

    override fun onLogout() {
        // UI状态由UserRepository管理
    }

    override fun onStateChanged() {
        // UI状态由UserRepository管理
    }
}
//
///**
// * UI状态枚举
// */
//sealed class LoginUIState {
//    object Idle : LoginUIState()
//    object Loading : LoginUIState()
//    data class Success(val userInfo: UserInfo) : LoginUIState()
//    object LoggedOut : LoginUIState()
//    data class Error(val message: String) : LoginUIState()
//}

/**
 * 登录状态管理器
 * 使用观察者模式分发登录状态变化事件
 * 所有状态由UserRepository统一管理
 */
class LoginStateManager(
    val userRepository: UserRepository
) {
    private val listeners = mutableSetOf<LoginStateListener>()
    
    /**
     * 添加登录状态监听器
     */
    fun addListener(listener: LoginStateListener) {
        Napier.d  ("添加登录状态监听器: $listener")
        listeners.add(listener)
    }
    
    /**
     * 移除登录状态监听器
     */
    fun removeListener(listener: LoginStateListener) {
        listeners.remove(listener)
    }
    
    /**
     * 设置为登录中状态
     */
    fun setLoggingIn() {
        // 可以在这里添加登录中的逻辑
        notifyStateChanged()
    }
    
    /**
     * 设置为已登录状态
     */
    fun setLoggedIn(userInfo: UserInfo) {
        notifyLogin(userInfo)
        notifyStateChanged()
    }
    
    /**
     * 设置为登出中状态
     */
    fun setLoggingOut() {
        // 可以在这里添加登出中的逻辑
        notifyStateChanged()
    }
    
    /**
     * 设置为已登出状态
     */
    fun setLoggedOut() {
        notifyLogout()
        notifyStateChanged()
    }
    
    /**
     * 通知用户已登录
     */
    fun notifyLogin(userInfo: UserInfo) {
        listeners.forEach { it.onLogin(userInfo) }
    }
    
    /**
     * 通知用户已登出
     */
    private fun notifyLogout() {
        listeners.forEach { it.onLogout() }
    }
    
    /**
     * 通知状态变化
     */
    fun notifyStateChanged() {
        listeners.forEach { it.onStateChanged() }
    }
}