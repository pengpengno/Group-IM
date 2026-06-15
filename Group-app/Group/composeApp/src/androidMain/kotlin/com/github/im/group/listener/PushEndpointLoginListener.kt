package com.github.im.group.listener

import com.github.im.group.manager.LoginStateListener
import com.github.im.group.model.UserInfo
import com.github.im.group.notification.AndroidPushEndpointRegistrar
import io.github.aakira.napier.Napier

class PushEndpointLoginListener(
    private val pushEndpointRegistrar: AndroidPushEndpointRegistrar
) : LoginStateListener {

    override fun onLogin(userInfo: UserInfo) {
        Napier.d("PushEndpointLoginListener: syncing cached FCM token after login for userId=${userInfo.userId}")
        pushEndpointRegistrar.syncStoredTokenIfAvailable()
    }

    override fun onLogout() {
        Napier.d("PushEndpointLoginListener: logout observed, keep endpoint disabled state unchanged for now")
    }

    override fun onStateChanged() = Unit
}
