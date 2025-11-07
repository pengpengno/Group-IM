package com.github.im.group.sdk

import com.github.im.group.api.FileApi
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.repository.FilesRepository
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * 文件存储管理器
 * 实现本地优先策略，管理文件的存储、检索和清理
 */
class FileStorageManager(
    private val filesRepository: FilesRepository,
    private val fileSystem: FileSystem,
    private val baseDirectory: Path
) {
    /**
     * 获取文件内容（实现本地优先策略）
     * @param fileId 文件ID
     * @return 文件字节数组
     */
    suspend fun getFileContent(fileId: String): ByteArray {
        Napier.d("获取文件内容: $fileId")
        
        try {
            // 1. 检查本地是否存在该文件
            val localFilePath = getLocalFilePath(fileId)
            if (localFilePath != null && fileSystem.exists(localFilePath)) {
                Napier.d("从本地获取文件: $fileId")
                // 本地存在，直接返回本地文件内容
                return fileSystem.read(localFilePath) {
                    readByteArray()
                }
            }
            
            // 2. 本地不存在，从服务器下载
            Napier.d("从服务器下载文件: $fileId")
            val fileContent = FileApi.downloadFile(fileId)
            
            // 3. 保存到本地
            saveFileLocally(fileId, fileContent)
            
            return fileContent
        } catch (e: Exception) {
            Napier.e("获取文件内容失败: $fileId", e)
            throw e
        }
    }
    
    /**
     * 获取本地文件路径
     * @param fileId 文件ID
     * @return 本地文件路径，如果不存在则返回null
     */
    fun getLocalFilePath(fileId: String): Path? {
        try {
            val fileRecord = filesRepository.getFile(fileId)
            if (fileRecord != null) {
                val fullPath = baseDirectory / fileRecord.storagePath
                if (fileSystem.exists(fullPath)) {
                    return fullPath
                }
            }
            return null
        } catch (e: Exception) {
            Napier.e("获取本地文件路径失败: $fileId", e)
            return null
        }
    }
    
    /**
     * 将文件保存到本地
     * @param fileId 文件ID
     * @param fileContent 文件内容
     */
    private fun saveFileLocally(fileId: String, fileContent: ByteArray) {
        try {
            val fileRecord = filesRepository.getFile(fileId)
            if (fileRecord != null) {
                val filePath = baseDirectory / fileRecord.storagePath
                
                // 确保目录存在
                filePath.parent?.let { parent ->
                    if (!fileSystem.exists(parent)) {
                        fileSystem.createDirectories(parent)
                    }
                }
                
                // 写入文件
                fileSystem.write(filePath) {
                    write(fileContent)
                }
                
                // 更新数据库中的存储路径
                filesRepository.updateStoragePath(fileId, fileRecord.storagePath)
                // 更新最后访问时间
                filesRepository.updateLastAccessTime(fileId)
                Napier.d("文件已保存到本地: $filePath")
            }
        } catch (e: Exception) {
            Napier.e("保存文件到本地失败", e)
        }
    }
    
    /**
     * 定期检查并清理过期文件
     * 根据时序图要求实现文件清理机制
     */
    fun cleanupExpiredFiles() {
        Napier.d("开始清理过期文件")
        try {
            // 获取很久未访问的文件记录（这里假设超过30天未访问的文件为过期文件）
            val thresholdTime = Clock.System.now()
                .minus(kotlin.time.Duration.parse("30d"))
                .toLocalDateTime(TimeZone.UTC)
            
            // 查询很久未访问的文件记录
            val expiredFiles = filesRepository.getExpiredFiles(thresholdTime)
            Napier.d("找到 ${expiredFiles.size} 个过期文件")
            
            expiredFiles.forEach { file ->
                val filePath = baseDirectory / file.storagePath
                if (fileSystem.exists(filePath)) {
                    fileSystem.delete(filePath)
                    Napier.d("已删除本地过期文件: ${file.clientId}")
                }
                // 更新文件状态为已清理
                filesRepository.updateFileStatus(file.clientId, FileStatus.DELETED)
                Napier.d("已更新文件状态为已清理: ${file.clientId}")
            }
            
            Napier.d("过期文件清理完成")
        } catch (e: Exception) {
            Napier.e("清理过期文件失败", e)
        }
    }
    
    /**
     * 检查文件是否存在（本地优先）
     * @param fileId 文件ID
     * @return 文件是否存在
     */
    fun isFileExists(fileId: String): Boolean {
        try {
            // 检查本地是否存在
            val localFilePath = getLocalFilePath(fileId)
            if (localFilePath != null && fileSystem.exists(localFilePath)) {
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
     * 删除指定文件（本地文件和数据库记录）
     * @param fileId 文件ID
     * @return 删除是否成功
     */
    fun deleteFile(fileId: String): Boolean {
        return try {
            val fileRecord = filesRepository.getFile(fileId)
            if (fileRecord != null) {
                val filePath = baseDirectory / fileRecord.storagePath
                // 删除本地文件
                if (fileSystem.exists(filePath)) {
                    fileSystem.delete(filePath)
                }
                // 更新数据库状态
                filesRepository.updateFileStatus(fileId, FileStatus.DELETED)
                Napier.d("文件已删除: $fileId")
                return true
            } else {
                Napier.w("尝试删除不存在的文件: $fileId")
                return false
            }
        } catch (e: Exception) {
            Napier.e("删除文件失败: $fileId", e)
            false
        }
    }
}
