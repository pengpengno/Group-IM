package com.github.im.group

import ProxySettingsState

//  代理设置的存储
expect class ProxyConfigStorage {


    suspend fun setProxyState(proxySettingsState: ProxySettingsState)


    suspend fun getProxyState(): ProxySettingsState


    /**
     * 清理登录信息
     * 推出登录路的时候调用
     */
    suspend fun clear()

}
