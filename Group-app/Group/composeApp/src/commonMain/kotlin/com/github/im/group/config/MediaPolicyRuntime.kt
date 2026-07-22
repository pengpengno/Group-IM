package com.github.im.group.config

import com.github.im.group.api.SystemConfigApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaPolicy(
    val previewDefaultWidth: Int = 480,
    val previewMinWidth: Int = 160,
    val previewMaxWidth: Int = 1600,
    val previewDefaultQuality: Int = 75,
    val previewMinQuality: Int = 40,
    val previewMaxQuality: Int = 95,
    val thumbnailEnabled: Boolean = true,
    val thumbnailWidth: Int = 640,
    val thumbnailHeight: Int = 360,
    val thumbnailQuality: Int = 82,
    val uploadCompressionEnabled: Boolean = true,
    val uploadCompressMinSizeKb: Int = 350,
    val uploadMaxImageEdge: Int = 1600,
    val uploadJpegQuality: Int = 82
)

object MediaPolicyRuntime {
    private val _policy = MutableStateFlow(MediaPolicy())
    val policy: StateFlow<MediaPolicy> = _policy.asStateFlow()

    suspend fun refresh() {
        runCatching {
            SystemConfigApi.getMediaPolicy()
        }.onSuccess {
            _policy.value = it
        }.onFailure {
            Napier.w("Failed to refresh media policy. Falling back to cached/default values.", it)
        }
    }

    fun current(): MediaPolicy = _policy.value
}
