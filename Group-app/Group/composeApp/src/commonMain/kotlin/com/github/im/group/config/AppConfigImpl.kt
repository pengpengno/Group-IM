package com.github.im.group.config

/**
 * 开发环境配置
 */
class DevConfig(
    override val apiHost: String = "192.168.0.101", // 内网调试IP
    override val apiPort: Int = 8080,
    override val tcpPort: Int = 8088
) : AppConfig {
    override val environment: AppEnvironment = AppEnvironment.DEV
}

/**
 * 测试环境配置
 */
class TestConfig(
    override val apiHost: String = "8.145.54.215",
    override val apiPort: Int = 8080,
    override val tcpPort: Int = 8088
) : AppConfig {
    override val environment: AppEnvironment = AppEnvironment.TEST
}

/**
 * 生产环境配置
 */
class ProdConfig(
    override val apiHost: String = "groumim.cn", // 示例域名
    override val apiPort: Int = 80,
    override val tcpPort: Int = 8088
) : AppConfig {
    override val environment: AppEnvironment = AppEnvironment.PROD
}

/**
 * 自定义/动态环境配置
 */
data class CustomConfig(
    override val apiHost: String,
    override val apiPort: Int,
    override val tcpPort: Int
) : AppConfig {
    override val environment: AppEnvironment = AppEnvironment.CUSTOM
}
