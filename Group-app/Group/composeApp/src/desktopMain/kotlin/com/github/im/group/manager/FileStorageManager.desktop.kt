package com.github.im.group.manager

import com.github.im.group.sdk.File
import io.github.aakira.napier.Napier
import okio.Path

actual fun FileStorageManager.isFileExists(fileId: String): Boolean {
    try {
        // 对于非 Content URI，使用通用实现
        // 检查本地是否存在
        val localFilePath = getLocalFilePath(fileId)
        if (localFilePath != null && fileSystem.exists(localFilePath.toPath())) {
            return true
        }

        // 检查数据库中是否存在记录
        return filesRepository.getFile(fileId) != null
    } catch (e: Exception) {
        Napier.e("检查文件是否存在时发生错误: $fileId", e)
        return false
    }
}

/**
 * 获取本地文件路径（Desktop 平台实现）
 * @param fileId 文件ID
 * @return 本地文件路径，如果不存在则返回null
 */
actual fun FileStorageManager.getLocalFilePath(fileId: String): String? {
    try {
        val fileRecord = filesRepository.getFile(fileId)
        if (fileRecord != null) {
            if (fileRecord.storagePath.isNotEmpty()) {
                // 现在存储的是全路径，直接使用
                val fullPath = fileRecord.storagePath.toPath()
                if (fileSystem.exists(fullPath) && !fileSystem.metadata(fullPath).isDirectory) {
                    return fullPath.toString()
                }
            }
        }
        return null
    } catch (e: Exception) {
        Napier.e("获取本地文件路径失败: $fileId", e)
        return null
    }
}

actual fun FileStorageManager.getFile(fileId: String): File? {
    TODO("Not yet implemented")
}