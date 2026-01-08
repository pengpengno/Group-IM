package com.github.im.group.manager

import io.github.aakira.napier.Napier
import okio.Path

/**
 * 检查文件是否存在（JVM 平台实现）
 * @param fileId 文件ID
 * @return 文件是否存在
 */
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
 * 获取本地文件路径（JVM 平台实现）
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

/**
 * 获取本地文件对象（JVM 平台实现）
 * @param fileId 文件ID
 * @return 本地文件对象，如果不存在则返回null
 */
actual fun FileStorageManager.getLocalFile(fileId: String): okio.Path? {
    try {
        val fileRecord = filesRepository.getFile(fileId)
        if (fileRecord != null) {
            if (fileRecord.storagePath.isNotEmpty()) {
                // 现在存储的是全路径，直接使用
                val fullPath = fileRecord.storagePath.toPath()
                if (fileSystem.exists(fullPath) && !fileSystem.metadata(fullPath).isDirectory) {
                    return fullPath
                }
            }
        }
        return null
    } catch (e: Exception) {
        Napier.e("获取本地文件对象失败: $fileId", e)
        return null
    }
}