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
        val existingFile = db.filesQueries.selectFileByServerId(metaFileMeta.fileId).executeAsOneOrNull()
        
        if (existingFile == null) {
            // 如果文件不存在，则插入新记录
            db.filesQueries.insertFile(
                originalName = metaFileMeta.fileName,
                contentType = metaFileMeta.contentType,
                size = metaFileMeta.size,
                serverId = metaFileMeta.fileId,
//                storagePath = "$STORE_PATH/${metaFileMeta.hash}",
                storagePath = "",
                hash = metaFileMeta.hash,
                uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                clientId = metaFileMeta.hash,
                status = FileStatus.NORMAL,
                thumbnail = metaFileMeta.thumbnail,
                duration = metaFileMeta.duration.toLong()
            )
        } else {
            // 如果文件已存在，更新最后访问时间
            updateLastAccessTime(metaFileMeta.hash)
        }
    }
    
    /**
     * 批量添加文件记录到数据库（使用事务优化）
     * 通过减少重复查询和优化事务处理来提高性能
     * 注意：SQLDelight没有原生的批量插入API，所以我们使用事务包装循环插入来模拟批量插入
     * @param fileMetas 文件元数据列表
     */
    fun addFiles(fileMetas: List<FileMeta>) {
        if (fileMetas.isEmpty()) return
        
        db.transaction {
            fileMetas.forEach { fileMeta ->
                addFileInTransaction(fileMeta)
            }
        }
    }
    
    /**
     * 在事务中添加单个文件记录
     * @param fileMeta 文件元数据
     */
    private fun addFileInTransaction(fileMeta: FileMeta) {
        // 检查文件是否已存在
        val existingFile = db.filesQueries.selectFileByClientId(fileMeta.hash).executeAsOneOrNull()
        
        if (existingFile == null) {
            // 如果文件不存在，则插入新记录
            db.filesQueries.insertFile(
                originalName = fileMeta.fileName,
                contentType = fileMeta.contentType,
                serverId = fileMeta.fileId,
                size = fileMeta.size,
                storagePath = "$STORE_PATH/${fileMeta.hash}",
                hash = fileMeta.hash,
                uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                clientId = fileMeta.hash,
                status = FileStatus.NORMAL,
                thumbnail = fileMeta.thumbnail,
                duration = fileMeta.duration.toLong()
            )
        } else {
            // 如果文件已存在，更新最后访问时间
            updateLastAccessTimeInTransaction(fileMeta.hash)
        }
    }
    
    /**
     * 在事务中更新文件最后访问时间
     */
    private fun updateLastAccessTimeInTransaction(fileId: String) {
        val file = db.filesQueries.selectFileByClientId(fileId).executeAsOneOrNull()
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
                thumbnail = file.thumbnail,
                duration = file.duration,
                clientId_ = fileId
            )
        }
    }

    /**
     * 获取文件记录
     * @param fileId 文件ID 服务端的文件ID
     * @return 文件记录
     */
    fun getFile(fileId: String): FileResource?{
        return db.filesQueries.selectFileByServerId(fileId).executeAsOneOrNull()
    }
    
    /**
     * 更新文件的本地存储路径
     * @param fileId 文件ID 服务端的文件ID
     * @param storagePath 本地存储路径
     */
    fun updateStoragePath(fileId: String, storagePath: String) {
        val file = getFile(fileId)
        if (file != null) {
            db.filesQueries.updateFileByServerId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = storagePath,
                hash = file.hash,
                uploadTime = file.uploadTime,
                status = file.status,
                thumbnail = file.thumbnail,
                duration = file.duration,
                serverId = fileId
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
                thumbnail = file.thumbnail,
                duration = file.duration,
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
                thumbnail = file.thumbnail,
                duration = file.duration,
                clientId_ = fileId
            )
        }
    }
    
    /**
     * 更新文件的媒体资源信息（缩略图和时长）
     */
    fun updateMediaResourceInfo(fileId: String, thumbnail: String? = null, duration: Long? = null) {
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
                status = file.status,
                thumbnail = thumbnail ?: file.thumbnail,
                duration = duration ?: file.duration,
                clientId_ = fileId
            )
        }
    }
    
    /**
     * 获取过期文件（在指定时间之前未访问的文件）
     */
    fun getExpiredFiles(thresholdTime: LocalDateTime): List<FileResource> {
        return db.filesQueries.selectFilesBeforeAccessTime(thresholdTime).executeAsList()
    }
    
    /**
     * 检查文件是否已存储在本地
     */
    fun isFileStoredLocally(fileId: String): Boolean {
        val file = getFile(fileId)
        return file != null && file.status == FileStatus.NORMAL && file.storagePath.isNotEmpty()
    }
    
    /**
     * 根据文件ID获取文件元数据（本地优先）
     * @param fileId 文件ID
     * @return 文件元数据
     */
    fun getFileMeta(fileId: String): FileMeta? {
        val file = getFile(fileId)
        return file?.let {
            FileMeta(
                fileId = it.serverId?:"",
                fileName = it.originalName,
                size = it.size,
                contentType = it.contentType,
                hash = it.clientId,
                type = it.contentType,
                duration = if (it.duration > 0) it.duration.toInt() else 0, // 如果时长为0则返回null
                thumbnail = it.thumbnail
            )
        }
    }
}