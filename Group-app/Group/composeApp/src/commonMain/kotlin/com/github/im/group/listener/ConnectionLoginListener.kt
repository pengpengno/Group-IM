package com.github.im.group.listener

import com.github.im.group.manager.LoginStateListener
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.SenderSdk
import io.github.aakira.napier.Napier

/**
 * 连接管理登录状态监听器
 * 处理与TCP/WebSocket连接相关的登录/登出操作
 */
class ConnectionLoginListener(private val senderSdk: SenderSdk) : LoginStateListener {
    
    override fun onLogin(userInfo: UserInfo) {
        // 用户登录时执行的操作
        Napier.d("ConnectionLoginListener: 用户 ${userInfo.username} 已登录")
        
        senderSdk.loginConnect()
    }
    
    override fun onLogout() {
        // 用户登出时执行的操作
        Napier.d("ConnectionLoginListener: 用户已登出")
        
        // 1. 断开TCP连接
        // senderSdk.disconnect()
        
        // 2. 断开WebSocket连接
        // senderSdk.disconnectWebSocket()
        
        // 3. 停止自动重连
        senderSdk.stopAutoReconnect()
        
        // 4. 其他连接相关的登出操作
        // ...
    }

    override fun onStateChanged() {
        // 登录状态变化时执行的操作
        // 可以根据状态变化执行特定操作
    }
}