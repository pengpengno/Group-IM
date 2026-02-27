package com.github.im.group.ui.video

import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 资源监控器
 * 用于跟踪和管理视频通话中各种资源的分配和释放
 */
class ResourceMonitor {
    private val mutex = Mutex()
    private val allocatedResources = mutableSetOf<String>()
    private val releasedResources = mutableSetOf<String>()
    private var isMonitoring = false
    
    /**
     * 开始监控资源
     */
    suspend fun startMonitoring() {
        mutex.withLock {
            isMonitoring = true
            allocatedResources.clear()
            releasedResources.clear()
            Napier.d("资源监控已启动")
        }
    }
    
    /**
     * 停止监控资源
     */
    suspend fun stopMonitoring() {
        mutex.withLock {
            isMonitoring = false
            Napier.d("资源监控已停止")
            logResourceSummary()
        }
    }
    
    /**
     * 标记资源已分配
     */
    suspend fun markAllocated(resourceId: String, resourceType: ResourceType) {
        mutex.withLock {
            if (!isMonitoring) return
            
            val fullResourceId = "${resourceType.prefix}_$resourceId"
            allocatedResources.add(fullResourceId)
            Napier.d("资源已分配: $fullResourceId")
        }
    }
    
    /**
     * 标记资源已释放
     */
    suspend fun markReleased(resourceId: String, resourceType: ResourceType) {
        mutex.withLock {
            if (!isMonitoring) return
            
            val fullResourceId = "${resourceType.prefix}_$resourceId"
            if (fullResourceId in allocatedResources) {
                releasedResources.add(fullResourceId)
                Napier.d("资源已释放: $fullResourceId")
            } else {
                Napier.w("尝试释放未分配的资源: $fullResourceId")
            }
        }
    }
    
    /**
     * 检查资源是否已释放
     */
    suspend fun isReleased(resourceId: String, resourceType: ResourceType): Boolean {
        return mutex.withLock {
            val fullResourceId = "${resourceType.prefix}_$resourceId"
            fullResourceId in releasedResources
        }
    }
    
    /**
     * 获取未释放的资源列表
     */
    suspend fun getUnreleasedResources(): List<String> {
        return mutex.withLock {
            allocatedResources.filter { it !in releasedResources }
        }
    }
    
    /**
     * 强制释放所有资源
     */
    suspend fun forceReleaseAll() {
        mutex.withLock {
            val unreleased = getUnreleasedResources()
            if (unreleased.isNotEmpty()) {
                Napier.w("强制释放以下未释放的资源: $unreleased")
                releasedResources.addAll(unreleased)
            }
        }
    }
    
    /**
     * 重置监控器
     */
    suspend fun reset() {
        mutex.withLock {
            allocatedResources.clear()
            releasedResources.clear()
            isMonitoring = false
            Napier.d("资源监控器已重置")
        }
    }
    
    /**
     * 记录资源摘要
     */
    private suspend fun logResourceSummary() {
        mutex.withLock {
            val totalAllocated = allocatedResources.size
            val totalReleased = releasedResources.size
            val unreleased = getUnreleasedResources()
            
            Napier.i("""
                资源监控摘要:
                - 总分配资源: $totalAllocated
                - 总释放资源: $totalReleased
                - 未释放资源: ${unreleased.size}
                - 未释放资源列表: $unreleased
            """.trimIndent())
        }
    }
}

/**
 * 资源类型枚举
 */
enum class ResourceType(val prefix: String) {
    LOCAL_MEDIA_STREAM("local_stream"),
    REMOTE_VIDEO_TRACK("remote_video"),
    REMOTE_AUDIO_TRACK("remote_audio"),
    WEBRTC_CONNECTION("webrtc_conn"),
    MEDIA_PLAYER("media_player"),
    CAMERA("camera"),
    MICROPHONE("microphone"),
    SPEAKER("speaker")
}

/**
 * 资源管理扩展函数
 */
suspend inline fun <T> ResourceMonitor.useResource(
    resourceId: String,
    resourceType: ResourceType,
    block: () -> T
): T {
    markAllocated(resourceId, resourceType)
    try {
        return block()
    } finally {
        markReleased(resourceId, resourceType)
    }
}

/**
 * 资源安全释放扩展函数
 */
suspend fun ResourceMonitor.safeRelease(
    resourceId: String,
    resourceType: ResourceType,
    releaseAction: () -> Unit
) {
    try {
        if (!isReleased(resourceId, resourceType)) {
            releaseAction()
            markReleased(resourceId, resourceType)
        }
    } catch (e: Exception) {
        Napier.e("释放资源时发生错误: ${resourceType.prefix}_$resourceId", e)
    }
}