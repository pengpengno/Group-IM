package com.github.im.group.manager

import android.content.Context
import android.net.Uri
import com.github.im.group.androidContext
import com.github.im.group.sdk.File
import io.github.aakira.napier.Napier
import okio.Path
import okio.Path.Companion.toPath

/**
 * 检查文件是否存在（本地优先，Android 平台特定实现，支持 Content URI）
 * @param fileId 文件ID或文件路径
 * @return 文件是否存在
 */
actual fun FileStorageManager.isFileExists(fileId: String): Boolean {
    try {

        val localFilePath = getLocalFilePath(fileId)
        // 如果 fileId 是 Content URI (以 content:// 开头)
        if (fileId.startsWith("content://")) {
            val context = androidContext
            val uri = Uri.parse(fileId)

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
        if (localFilePath != null && fileSystem.exists(localFilePath.toPath())) {
            return true
        }else return false

    } catch (e: Exception) {
        Napier.e("检查文件是否存在时发生错误: $fileId", e)
        return false
    }
}

/**
 * 获取本地文件路径（Android 平台特定实现，支持 Content URI）
 * @param fileId 文件ID
 * @return 本地文件路径，如果不存在则返回null
 */
actual fun FileStorageManager.getLocalFilePath(fileId: String): String? {
    try {

         val file = getFile(fileId)


         return file?.let {
            val filePath = it.path
            return filePath
        }

    } catch (e: Exception) {
        Napier.e("获取本地文件路径失败: $fileId", e)
        return null
    }
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
        if (fileRecord != null) {
           return fileMeta?.toFile(fileRecord.storagePath)
        }
        return null
    } catch (e: Exception) {
        Napier.e("获取本地文件对象失败: $fileId", e)
        return null
    }
}