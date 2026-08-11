package com.github.im.group.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateManifest(
    val versionCode: Int, val versionName: String, val apkUrl: String, val sha256: String,
    val sizeBytes: Long, val changelog: String = "", val forceUpdate: Boolean = false,
    val minSupportedVersionCode: Int = 0
)

enum class AppUpdatePhase { IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, FAILED, UNSUPPORTED }

data class AppUpdateUiState(
    val currentVersion: String = "", val update: AppUpdateManifest? = null,
    val phase: AppUpdatePhase = AppUpdatePhase.UNSUPPORTED, val downloadedBytes: Long = 0,
    val totalBytes: Long = 0, val autoDownloadOnWifi: Boolean = true, val message: String? = null
)

interface AppUpdateService {
    val state: StateFlow<AppUpdateUiState>
    fun checkForUpdate()
    fun downloadOrInstall()
    fun setAutoDownloadOnWifi(enabled: Boolean)
}

class NoOpAppUpdateService : AppUpdateService {
    override val state = MutableStateFlow(AppUpdateUiState())
    override fun checkForUpdate() = Unit
    override fun downloadOrInstall() = Unit
    override fun setAutoDownloadOnWifi(enabled: Boolean) = Unit
}
