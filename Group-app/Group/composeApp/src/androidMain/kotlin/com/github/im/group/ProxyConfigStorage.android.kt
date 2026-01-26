package com.github.im.group

import ProxySettingsState
import android.content.Context
import androidx.core.content.edit
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ProxyConfigStorage(private val context: Context)  {

    private val prefs = context.getSharedPreferences("proxyConfig", Context.MODE_PRIVATE)

    actual suspend fun setProxyState(proxySettingsState: ProxySettingsState) {
        withContext(Dispatchers.IO) {  // 切换到IO线程
            Napier.d("Saving user info: $proxySettingsState")
            prefs.edit().apply {
                putString("host", proxySettingsState.host)
                putInt("port", proxySettingsState.port)
                putInt("tcpPort", proxySettingsState.tcpPort)
                putBoolean("enable", proxySettingsState.enableProxy)
                apply()
            }
        }

    }

    actual suspend fun getProxyState(): ProxySettingsState {

        // 先判断本地是否存在相关属性 ， 不存在就返回默认的即可
        if (!prefs.contains("host")) {
            return ProxySettingsState()
        }

        return ProxySettingsState(
            host = prefs.getString("host", "") ?: "",
            port = prefs.getInt("port", 0),
            tcpPort = prefs.getInt("tcpPort", 0),
            enableProxy = prefs.getBoolean("enable", false),
        )
    }

    actual suspend fun clear() {
        prefs.edit { clear() }

    }
}