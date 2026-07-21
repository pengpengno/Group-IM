package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.config.AppEnvironment
import com.github.im.group.manager.MessageNotificationPreferences
import com.github.im.group.repository.LocalNetworkSettings
import com.github.im.group.repository.NetworkSettingsDraft
import com.github.im.group.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notification: MessageNotificationPreferences = MessageNotificationPreferences(),
    val network: LocalNetworkSettings = LocalNetworkSettings(
        environment = AppEnvironment.PROD,
        apiHost = "",
        apiPort = 443,
        tcpHost = "",
        tcpPort = 8088,
        useTls = true,
        currentBaseUrl = ""
    ),
    val networkDraft: NetworkSettingsDraft = NetworkSettingsDraft(),
    val isSavingNetwork: Boolean = false,
    val isNetworkDirty: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.localSettings.collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    notification = snapshot.notification,
                    network = snapshot.network,
                    networkDraft = if (_uiState.value.isNetworkDirty) {
                        _uiState.value.networkDraft
                    } else {
                        snapshot.network.toDraft()
                    }
                )
            }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationSettings {
                it.copy(enableNotifications = enabled)
            }
        }
    }

    fun updateNotificationSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationSettings {
                it.copy(enableSound = enabled)
            }
        }
    }

    fun updateNotificationPreview(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationSettings {
                it.copy(enablePreview = enabled)
            }
        }
    }

    fun setNetworkEnvironment(environment: AppEnvironment) {
        updateNetworkDraft { copy(environment = environment) }
    }

    fun setApiHost(value: String) {
        updateNetworkDraft { copy(apiHost = value) }
    }

    fun setApiPort(value: String) {
        updateNetworkDraft { copy(apiPort = value.filter(Char::isDigit)) }
    }

    fun setTcpHost(value: String) {
        updateNetworkDraft { copy(tcpHost = value) }
    }

    fun setTcpPort(value: String) {
        updateNetworkDraft { copy(tcpPort = value.filter(Char::isDigit)) }
    }

    fun setUseTls(enabled: Boolean) {
        updateNetworkDraft { copy(useTls = enabled) }
    }

    fun resetNetworkDraft() {
        _uiState.value = _uiState.value.copy(
            networkDraft = _uiState.value.network.toDraft(),
            isNetworkDirty = false
        )
    }

    fun saveNetworkSettings() {
        val draft = _uiState.value.networkDraft
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingNetwork = true)
            settingsRepository.saveNetworkSettings(draft)
            _uiState.value = _uiState.value.copy(
                isSavingNetwork = false,
                isNetworkDirty = false
            )
        }
    }

    private fun updateNetworkDraft(transform: NetworkSettingsDraft.() -> NetworkSettingsDraft) {
        val updatedDraft = _uiState.value.networkDraft.transform()
        _uiState.value = _uiState.value.copy(
            networkDraft = updatedDraft,
            isNetworkDirty = updatedDraft != _uiState.value.network.toDraft()
        )
    }

    private fun LocalNetworkSettings.toDraft(): NetworkSettingsDraft {
        return NetworkSettingsDraft(
            environment = environment,
            apiHost = apiHost,
            apiPort = apiPort.toString(),
            tcpHost = tcpHost,
            tcpPort = tcpPort.toString(),
            useTls = useTls
        )
    }
}
