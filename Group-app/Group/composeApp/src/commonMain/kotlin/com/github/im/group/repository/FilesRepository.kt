package com.github.im.group.repository

import com.github.im.group.api.FileMeta
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.FileStatus
import db.FileResource
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 文件记录
 *
 */
class FilesRepository(
    private val db: AppDatabase
) {

    private val STORE_PATH : String = "files"


    /**
     * 添加文件记录到数据库
     * @param metaFileMeta
     */
    fun addFile(metaFileMeta : FileMeta){
        // 检查文件是否已存在
        val existingFile = db.filesQueries.selectFileByClientId(metaFileMeta.hash).executeAsOneOrNull()
        
        if (existingFile == null) {
            // 如果文件不存在，则插入新记录
            db.filesQueries.insertFile(
                originalName = metaFileMeta.fileName,
                contentType = metaFileMeta.contentType,
                size = metaFileMeta.size,
                storagePath = "$STORE_PATH/${metaFileMeta.hash}",
                hash = metaFileMeta.hash,
                uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                clientId = metaFileMeta.hash,
                status = FileStatus.NORMAL
            )
        } else {
            // 如果文件已存在，更新最后访问时间
            updateLastAccessTime(metaFileMeta.hash)
        }
    }

    /**
     * 获取文件记录
     */
    fun getFile(fileId: String): FileResource?{
        return db.filesQueries.selectFileByClientId(fileId).executeAsOneOrNull()
    }
    
    /**
     * 更新文件的本地存储路径
     */
    fun updateStoragePath(fileId: String, storagePath: String) {
        val file = getFile(fileId)
        if (file != null) {
            db.filesQueries.updateFileByClientId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = storagePath,
                hash = file.hash,
                uploadTime = file.uploadTime,
                clientId = file.clientId,
                status = file.status,
                clientId_ = fileId
            )
        }
    }
    
    /**
     * 更新文件最后访问时间
     */
    fun updateLastAccessTime(fileId: String) {
        val file = getFile(fileId)
        if (file != null) {
            db.filesQueries.updateFileByClientId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = file.storagePath,
                hash = file.hash,
                uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                clientId = file.clientId,
                status = file.status,
                clientId_ = fileId
            )
        }
    }
    
    /**
     * 更新文件状态
     */
    fun updateFileStatus(fileId: String, status: FileStatus) {
        val file = getFile(fileId)
        if (file != null) {
            db.filesQueries.updateFileByClientId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = file.storagePath,
                hash = file.hash,
                uploadTime = file.uploadTime,
                clientId = file.clientId,
                status = status,
                clientId_ = fileId
            )
        }
    }
    
    /**
     * 获取过期文件（在指定时间之前未访问的文件）
     */
    fun getExpiredFiles(thresholdTime: LocalDateTime): List<FileResource> {

        //TODO 待实现
        return emptyList()
//        return db.filesQueries.selectFilesBeforeAccessTime(thresholdTime).executeAsList()
    }
}