package com.github.im.group.repository

import com.github.im.group.api.FileMeta
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.FileStatus
import db.FileResource
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FilesRepository(
    private val db: AppDatabase
) {

    fun addPendingFileRecord(
        fileId: String,
        fileName: String,
        duration: Long = 0,
        filePath: String,
        contentType: String = "",
        size: Long = 0
    ) {
        // 先保存一份本地附件占位信息。
        // 即使远端 fileMeta 还没准备好，聊天页也能先渲染“上传中”的文件气泡。
        db.filesQueries.insertFile(
            originalName = fileName,
            contentType = contentType,
            size = size,
            serverId = fileId,
            storagePath = filePath,
            hash = "",
            uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            status = FileStatus.UPLOADING,
            thumbnail = null,
            duration = duration
        )
    }

    fun addOrUpdateFile(metaFileMeta: FileMeta) {
        val existingFile = db.filesQueries.selectFileByServerId(metaFileMeta.fileId).executeAsOneOrNull()
        if (existingFile == null) {
            db.filesQueries.insertFile(
                originalName = metaFileMeta.fileName,
                contentType = metaFileMeta.contentType,
                size = metaFileMeta.size,
                serverId = metaFileMeta.fileId,
                storagePath = "",
                hash = metaFileMeta.hash,
                uploadTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                status = FileStatus.NORMAL,
                thumbnail = metaFileMeta.thumbnail,
                duration = metaFileMeta.duration.toLong()
            )
            return
        }

        // 服务端返回正式元数据时保留已有 storagePath，避免覆盖本地已下载/待上传路径。
        db.filesQueries.updateFileByServerId(
            originalName = metaFileMeta.fileName,
            contentType = metaFileMeta.contentType,
            size = metaFileMeta.size,
            serverId = metaFileMeta.fileId,
            storagePath = existingFile.storagePath,
            hash = metaFileMeta.hash,
            uploadTime = existingFile.uploadTime,
            status = metaFileMeta.fileStatus,
            thumbnail = metaFileMeta.thumbnail,
            duration = metaFileMeta.duration.toLong()
        )
    }

    fun addFiles(fileMetas: List<FileMeta>) {
        if (fileMetas.isEmpty()) return

        db.transaction {
            fileMetas.forEach(::addOrUpdateFile)
        }
    }

    fun getFile(fileId: String): FileResource? {
        return db.filesQueries.selectFileByServerId(fileId).executeAsOneOrNull()
    }

    fun updateStoragePath(fileId: String, storagePath: String) {
        val file = getFile(fileId) ?: return
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

    /**
     * 本地文件消息会先用临时 fileId 渲染，拿到服务端真实 fileId 后再平滑迁移。
     */
    fun replaceFileId(oldFileId: String, newFileId: String) {
        if (oldFileId == newFileId) return
        db.filesQueries.updateFileServerId(
            serverId = newFileId,
            serverId_ = oldFileId
        )
    }

    fun updateFileStatus(fileId: String, status: FileStatus) {
        val file = getFile(fileId) ?: return
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

    fun updateMediaResourceInfo(fileId: String, thumbnail: String? = null, duration: Long? = null) {
        val file = getFile(fileId) ?: return
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

    fun getExpiredFiles(thresholdTime: LocalDateTime): List<FileResource> {
        return db.filesQueries.selectFilesBeforeAccessTime(thresholdTime).executeAsList()
    }

    fun isFileStoredLocally(fileId: String): Boolean {
        val file = getFile(fileId)
        return file != null && file.status == FileStatus.NORMAL && file.storagePath.isNotEmpty()
    }

    fun getFileMeta(fileId: String): FileMeta? {
        val file = getFile(fileId) ?: return null
        // 消息 content 只存 fileId，真正渲染气泡时再从这里组装展示模型。
        return FileMeta(
            fileId = file.serverId ?: "",
            fileName = file.originalName,
            size = file.size,
            contentType = file.contentType,
            hash = file.hash,
            duration = if (file.duration > 0) file.duration.toInt() else 0,
            thumbnail = file.thumbnail,
            fileStatus = file.status
        )
    }
}
