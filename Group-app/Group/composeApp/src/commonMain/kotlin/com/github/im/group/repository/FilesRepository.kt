package com.github.im.group.repository

import com.github.im.group.api.FileMeta
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.FileStatus
import db.FileResource
import io.github.aakira.napier.Napier
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
     * 添加待上传的文件记录
     * 在文件上传前 获取到 uploaderId - fileId 后
     * 必要参数
     * fileName 文件名
     * fileId 服务端给的 uploaderId
     * duration 时长
     * status 文件状态 这个时候为  {@link FileStatus.UPLOADING}
     *
     * @param fileId 文件ID
     * @param fileName 文件名
     * @param duration 文件时长 媒体文件需要添加时长
     */
    fun addPendingFileRecord(fileId: String, fileName: String, duration: Long = 0,filePath: String) {
        db.filesQueries.insertFile(
            originalName = fileName,
            contentType = "",
            size = 0,  // 服务端计算后返回
            serverId = fileId,
            storagePath = filePath,
            hash = "", // 使用fileId作为hash
            uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            status = FileStatus.UPLOADING,
            thumbnail = null,
            duration = duration
        )
    }
    /**
     * 添加/更新文件记录到数据库
     * 如果文件已存在，则更新信息
     * @param metaFileMeta
     */
    fun addOrUpdateFile(metaFileMeta : FileMeta){
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
                status = FileStatus.NORMAL,
                thumbnail = metaFileMeta.thumbnail,
                duration = metaFileMeta.duration.toLong()
            )
        } else {

            Napier.d { "文件是否存在 ： $existingFile " }

            db.filesQueries.updateFileByServerId(
                originalName = metaFileMeta.fileName,
                contentType = metaFileMeta.contentType,
                size = metaFileMeta.size,
                serverId = metaFileMeta.fileId,
//                storagePath = "$STORE_PATH/${metaFileMeta.hash}",
                storagePath =existingFile.storagePath,
                hash = metaFileMeta.hash,
                uploadTime =existingFile.uploadTime,
                status = metaFileMeta.fileStatus,
                thumbnail = metaFileMeta.thumbnail,
                duration = metaFileMeta.duration.toLong()

            )
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
                addOrUpdateFile(fileMeta)
            }
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
            db.filesQueries.updateFileByServerId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = file.storagePath,
                hash = file.hash,
                uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                status = file.status,
                thumbnail = file.thumbnail,
                duration = file.duration,
                serverId = fileId
            )
        }
    }
    
    /**
     * 更新文件状态
     */
    fun updateFileStatus(fileId: String, status: FileStatus) {
        val file = getFile(fileId)
        if (file != null) {
            db.filesQueries.updateFileByServerId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = file.storagePath,
                hash = file.hash,
                uploadTime = file.uploadTime,
                status = status,
                thumbnail = file.thumbnail,
                duration = file.duration,
                serverId = fileId
            )
        }
    }
    
    /**
     * 更新文件的媒体资源信息（缩略图和时长）
     */
    fun updateMediaResourceInfo(fileId: String, thumbnail: String? = null, duration: Long? = null) {
        val file = getFile(fileId)
        if (file != null) {
            db.filesQueries.updateFileByServerId(
                originalName = file.originalName,
                contentType = file.contentType,
                size = file.size,
                storagePath = file.storagePath,
                hash = file.hash,
                uploadTime = file.uploadTime,
                status = file.status,
                thumbnail = thumbnail ?: file.thumbnail,
                duration = duration ?: file.duration,
                serverId = fileId
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
                hash = it.hash,
                duration = if (it.duration > 0) it.duration.toInt() else 0, // 如果时长为0则返回null
                thumbnail = it.thumbnail,
                fileStatus = it.status

            )
        }
    }
}