package com.github.im.group

import com.github.im.group.config.ProxySettingsState

actual class ProxyConfigStorage {
    actual suspend fun setProxyState(proxySettingsState: ProxySettingsState) {
    }

    actual suspend fun getProxyState(): ProxySettingsState {
        TODO("Not yet implemented")
    }

    actual suspend fun clear() {
    }
}