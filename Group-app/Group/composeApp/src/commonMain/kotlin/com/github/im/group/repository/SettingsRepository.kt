package com.github.im.group.repository

import com.github.im.group.config.AppEnvironment
import com.github.im.group.config.ConfigManager
import com.github.im.group.manager.MessageNotificationPreferences
import com.github.im.group.manager.NotificationPreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class LocalNetworkSettings(
    val environment: AppEnvironment,
    val apiHost: String,
    val apiPort: Int,
    val tcpHost: String,
    val tcpPort: Int,
    val useTls: Boolean,
    val currentBaseUrl: String
)

data class LocalSettingsSnapshot(
    val notification: MessageNotificationPreferences,
    val network: LocalNetworkSettings
)

data class NetworkSettingsDraft(
    val environment: AppEnvironment = AppEnvironment.PROD,
    val apiHost: String = "",
    val apiPort: String = "",
    val tcpHost: String = "",
    val tcpPort: String = "",
    val useTls: Boolean = false
) {
    val isCustom: Boolean
        get() = environment == AppEnvironment.CUSTOM
}

class SettingsRepository(
    private val notificationPreferenceStore: NotificationPreferenceStore,
    private val configManager: ConfigManager
) {

    val localSettings: Flow<LocalSettingsSnapshot> = combine(
        notificationPreferenceStore.preferences,
        configManager.currentConfig,
        configManager.currentEnvironment
    ) { notification, config, environment ->
        LocalSettingsSnapshot(
            notification = notification,
            network = LocalNetworkSettings(
                environment = environment,
                apiHost = config.apiHost,
                apiPort = config.apiPort,
                tcpHost = config.tcpHost,
                tcpPort = config.tcpPort,
                useTls = config.useTls,
                currentBaseUrl = config.getBaseUrl()
            )
        )
    }

    suspend fun updateNotificationSettings(
        transform: (MessageNotificationPreferences) -> MessageNotificationPreferences
    ) {
        notificationPreferenceStore.updatePreferences(transform)
    }

    suspend fun saveNetworkSettings(draft: NetworkSettingsDraft) {
        if (draft.environment == AppEnvironment.CUSTOM) {
            configManager.updateCustomConfig(
                host = draft.apiHost,
                tcpHost = draft.tcpHost,
                port = draft.apiPort.toIntOrNull() ?: configManager.currentConfig.value.apiPort,
                tcpPort = draft.tcpPort.toIntOrNull() ?: configManager.currentConfig.value.tcpPort,
                useTls = draft.useTls
            )
        } else {
            configManager.setEnvironment(draft.environment)
        }
    }
}
