package com.github.im.group.config

/**
 * 应用环境枚举
 */
enum class AppEnvironment(val displayName: String) {
    DEV("开发环境"),
    TEST("测试环境"),
    PROD("生产环境"),
    CUSTOM("自定义")
}

/**
 * 应用配置接口
 */
interface AppConfig {
    val environment: AppEnvironment
    val apiHost: String
    val apiPort: Int
    val tcpPort: Int
    
    fun getBaseUrl(): String = "http://$apiHost:$apiPort"
}
