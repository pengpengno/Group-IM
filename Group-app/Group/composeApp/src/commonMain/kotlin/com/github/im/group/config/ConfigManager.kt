package com.github.im.group.config

import com.github.im.group.ProxyConfigStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 整个应用的配置管理器
 * 负责环境切换、配置持久化以及提供当前的 AppConfig
 */
class ConfigManager(
    private val storage: ProxyConfigStorage
) {
    private val _currentConfig = MutableStateFlow<AppConfig>(DevConfig())
    val currentConfig: StateFlow<AppConfig> = _currentConfig.asStateFlow()

    private val _currentEnvironment = MutableStateFlow(AppEnvironment.DEV)
    val currentEnvironment: StateFlow<AppEnvironment> = _currentEnvironment.asStateFlow()

    suspend fun initialize() {
        val savedState = storage.getProxyState()
        // 这里可以根据某种逻辑判断环境，目前简单处理：
        // 如果 enableProxy 为 true，则使用 CUSTOM 环境，否则默认 DEV
        if (savedState.enableProxy) {
            val config = CustomConfig(
                apiHost = savedState.host,
                apiPort = savedState.port,
                tcpPort = savedState.tcpPort
            )
            _currentConfig.value = config
            _currentEnvironment.value = AppEnvironment.CUSTOM
        } else {
            // 以后可以扩展存储选中的环境枚举，这里暂时默认 DEV
            setEnvironment(AppEnvironment.DEV)
        }
    }

    suspend fun setEnvironment(env: AppEnvironment) {
        val config = when (env) {
            AppEnvironment.DEV -> DevConfig()
            AppEnvironment.TEST -> TestConfig()
            AppEnvironment.PROD -> ProdConfig()
            AppEnvironment.CUSTOM -> {
                val state = storage.getProxyState()
                CustomConfig(state.host, state.port, state.tcpPort)
            }
        }
        _currentConfig.value = config
        _currentEnvironment.value = env
        
        // 更新存储的状态（如果是切换环境）
        if (env != AppEnvironment.CUSTOM) {
            storage.setProxyState(ProxySettingsState(
                host = config.apiHost,
                port = config.apiPort,
                tcpPort = config.tcpPort,
                enableProxy = false
            ))
        } else {
            val state = storage.getProxyState()
            storage.setProxyState(state.copy(enableProxy = true))
        }
    }

    suspend fun updateCustomConfig(host: String, port: Int, tcpPort: Int) {
        val config = CustomConfig(host, port, tcpPort)
        _currentConfig.value = config
        _currentEnvironment.value = AppEnvironment.CUSTOM
        storage.setProxyState(ProxySettingsState(host, port, tcpPort, true))
    }
}
