package com.github.im.group.listener

import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.WebRTCManager

/**
 * WebRTC登录状态监听器
 * 处理与WebRTC相关的登录/登出操作
 */
class WebRTCLoginListener(
    private val webRTCManager: WebRTCManager
) : LoginStateListener {
    // 这里可以注入WebRTC管理器

    override fun onStateChanged() {

    }

    override fun onLogin(userInfo: UserInfo) {
        // 用户登录时执行的操作
        println("WebRTCLoginListener: 用户 ${userInfo.username} 已登录")
        
        // 1. 初始化WebRTC
        // webRTCManager.initialize()
        
        // 2. 连接到信令服务器
        // webRTCManager.connectToSignalingServer(serverUrl, userInfo.userId.toString())
        
        // 3. 其他WebRTC相关的登录操作
        // ...
    }
    
    override fun onLogout() {
        // 用户登出时执行的操作
        println("WebRTCLoginListener: 用户已登出")
        
        // 1. 断开与信令服务器的连接
        // webRTCManager.disconnectFromSignalingServer()

    }

}