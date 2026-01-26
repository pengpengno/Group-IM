package com.github.im.group.manager

import androidx.core.net.toUri
import com.github.im.group.androidContext
import com.github.im.group.sdk.File
import io.github.aakira.napier.Napier
import okio.Path.Companion.toPath

/**
 * 检查文件是否在本地存在（，Android 平台特定实现，支持 Content URI）
 * @param fileId 文件ID或文件路径
 * @return 文件是否在本地存在
 */
actual fun FileStorageManager.isFileExists(fileId: String): Boolean {
    try {

        val localFilePath = getLocalFilePath(fileId)
        Napier.d("检查文件是否存在: $fileId, path $localFilePath")
        // 如果 localFilePath 是 Content URI (以 content:// 开头 安卓平台 本地上传文件时 会是带有content的)
        if (localFilePath != null && localFilePath.startsWith("content://")) {
            Napier.d("检查 Content URI 文件是否存在: $fileId, uri: $localFilePath")
            val context = androidContext
            val uri = localFilePath.toUri() // 使用 localFilePath 而不是 fileId

            // 尝试打开输入流来检查 URI 是否有效
            return try {
                context.contentResolver.openInputStream(uri)?.use {
                    // 如果能成功打开输入流，则认为文件存在
                    true
                } != null
            } catch (e: Exception) {
                Napier.e("检查 Content URI 文件是否存在时发生错误: $fileId", e)
                false
            }
        }

        // 对于非 Content URI，使用通用实现
        // 检查本地是否存在
        if (localFilePath != null && localFilePath.isNotEmpty() && fileSystem.exists(localFilePath.toPath())) {
            Napier.d("文件已存在: $fileId, path $localFilePath")
            return true
        } else return false

    } catch (e: Exception) {
        Napier.e("检查文件是否存在时发生错误: $fileId", e)
        return false
    }
}

/**
 * 获取本地文件路径（Android 平台特定实现，支持 Content URI(conteng:// )
 * 如果文件在本地不存在 那么 就返回 null ( 注：不会返回 http 的链接)
 * @param fileId 文件ID
 * @return 本地文件路径，如果不存在则返回null
 */
actual fun FileStorageManager.getLocalFilePath(fileId: String): String? {
    try {


        val fileRecord = filesRepository.getFile(fileId)
        val fileMeta = filesRepository.getFileMeta(fileId)
        if (fileRecord != null) {
            return fileMeta?.toFile(fileRecord.storagePath)?.let {
                // 返回文件的完整路径
                val filePath = it.path
                // 确保路径不为空
                if ( filePath.isNotEmpty()) {
                    return@let filePath
                }
                return null
            }
        }

    } catch (e: Exception) {
        Napier.e("获取本地文件路径失败: $fileId", e)
        return null
    }
    return null
}

/**
 * 获取本地文件对象（Android 平台特定实现）
 * @param fileId 文件ID
 * @return 本地文件对象，如果不存在则返回null
 */
actual fun FileStorageManager.getFile(fileId: String): File? {
    try {

        val fileRecord = filesRepository.getFile(fileId)
        val fileMeta = filesRepository.getFileMeta(fileId)
        if (fileRecord != null && fileMeta != null) {
            val file = fileMeta.toFile(fileRecord.storagePath)
            // 确保文件路径不为空
            if (file.path.isNotEmpty()) {
                return file
            }
        }
        return null
    } catch (e: Exception) {
        Napier.e("获取本地文件对象失败: $fileId", e)
        return null
    }
}