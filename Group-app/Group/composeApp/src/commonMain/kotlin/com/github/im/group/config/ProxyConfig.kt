
package com.github.im.group.config


data class ProxySettingsState(

    val host: String = "10.122.155.252",
    val tcpHost: String = "10.122.155.252",
    val port: Int = 8080,
    val tcpPort: Int = 8088,
    val useTls: Boolean = false,
    val enableProxy: Boolean = false
)

object ProxyConfig {
    // 默认使用开发配置
    private var _config: AppConfig = DevConfig()
    
    val config: AppConfig get() = _config

    var host: String 
        get() = _config.apiHost
        set(value) { /* 不建议直接设置，应通过 ConfigManager */ }

    var tcpHost: String
        get() = _config.tcpHost
        set(value) { /* 不建议直接设置，应通过 ConfigManager */ }

    var port: Int
        get() = _config.apiPort
        set(value) { /* 不建议直接设置，应通过 ConfigManager */ }

    var tcp_port: Int
        get() = _config.tcpPort
        set(value) { /* 不建议直接设置，应通过 ConfigManager */ }

    var useTls: Boolean
        get() = _config.useTls
        set(value) { /* 不建议直接设置，应通过 ConfigManager */ }

    var enableProxy: Boolean = false

    /**
     * 更新当前配置（供 ConfigManager 调用）
     */
    fun updateConfig(newConfig: AppConfig) {
        _config = newConfig
        enableProxy = newConfig.environment == AppEnvironment.CUSTOM
    }

    fun getBaseUrl(): String = _config.getBaseUrl()

    fun getWsBaseUrl(): String = _config.getWsBaseUrl()

    fun setProxy(host: String, port: Int) {
        // 这里的逻辑将来应迁移到 ConfigManager
        // 暂时为了兼容性保留，但标记为待办
        TODO("已在 ConfigManager 中实现，建议调用 ConfigManager.updateCustomConfig")
    }
}

//
///**
// * 代理设置
// */
//public data class ProxySettingsState(
//    val host: String = "192.168.1.6",
//    val port: Int = 8080,
//    val tcpPort: Int = 8088,
//    val enableProxy: Boolean = false
//) {
//    fun getBaseUrl(): String {
//        return if (enableProxy) {
//            "http://$host:$port"
//        } else {
//            "http://$host:8080"
//        }
//    }
//}