package com.github.im.group.listener

import com.github.im.group.manager.LoginStateListener
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.WebRTCManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class WebRTCLoginListener(
    private val webRTCManager: WebRTCManager
) : LoginStateListener {

    override fun onStateChanged() = Unit

    override fun onLogin(userInfo: UserInfo) {
        Napier.d("WebRTCLoginListener: user ${userInfo.username} logged in")
        CoroutineScope(Dispatchers.IO).launch {
            webRTCManager.initialize()
            webRTCManager.connectToSignalingServer("", userInfo.userId.toString())
        }
    }

    override fun onLogout() {
        Napier.d("WebRTCLoginListener: user logged out")
        webRTCManager.release()
    }
}
