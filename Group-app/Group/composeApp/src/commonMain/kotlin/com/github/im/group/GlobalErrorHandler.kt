package com.github.im.group

import com.github.im.group.manager.LoginStateManager
import com.github.im.group.repository.UserRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * 全局异常处理器
 * 用于在整个APP生命周期内捕获未认证异常并触发相应的处理逻辑
 */
object GlobalErrorHandler {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _unauthorizedEvents = MutableSharedFlow<Unit>()
    val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents
    
    private var userRepository: UserRepository? = null
    private var loginStateManager: LoginStateManager? = null
    
    fun initialize(userRepository: UserRepository, loginStateManager: LoginStateManager) {
        this.userRepository = userRepository
        this.loginStateManager = loginStateManager
    }
    
    /**
     * 触发未认证异常处理流程
     * 会更新用户状态为登出，并清除本地凭证
     */
    fun handleUnauthorized() {
        val repo = userRepository
        val manager = loginStateManager
        
        if (repo != null && manager != null) {
            try {
                // 更新用户仓库状态为登出
                repo.updateToLoggedOut()
                // 通知登录状态管理器用户已登出
                manager.setLoggedOut()

                
                // 发送全局未认证事件
                scope.launch {
                    // 清除本地存储的无效凭据
                    GlobalCredentialProvider.storage.clearUserInfo()
                    // 更新全局凭证
                    GlobalCredentialProvider.currentToken = ""

                    _unauthorizedEvents.emit(Unit)
                }
                
                Napier.i("全局未认证异常处理完成")
            } catch (e: Exception) {
                Napier.e("处理未认证异常时出错", e)
            }
        } else {
            Napier.e("GlobalErrorHandler 未初始化")
        }
    }
}