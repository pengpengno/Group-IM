package com.github.im.group.manager

import com.github.im.group.api.FileApi
import com.github.im.group.api.FileUploadResponse
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FilePicker
import io.github.aakira.napier.Napier
import kotlin.uuid.ExperimentalUuidApi

/**
 * 文件上传服务
 * 负责处理文件上传相关的逻辑
 */
class FileUploadService(
    private val filePicker: FilePicker,
    private val userRepository: UserRepository,
    private val filesRepository: FilesRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val fileStorageManager: FileStorageManager
) {

    /**
     * 上传字节数组形式的文件
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun uploadFileData(
        serverFileId: String,
        data: ByteArray,
        fileName: String,
        duration: Long,
    ): FileUploadResponse {
        try{
            val uploadResponse = FileApi.uploadFile(serverFileId, data, fileName, duration)
            // 上传完毕后更新一下
            uploadResponse.fileMeta?.let {
                filesRepository.addOrUpdateFile(it)
            }
            Napier.i { "上传后的文件 $uploadResponse " }
            return uploadResponse
        }catch (e: Exception){
            Napier.e{"上传文件失败 $e"}
            //TODO  更新文件为 失败状态
            filesRepository.updateFileStatus(serverFileId, FileStatus.FAILED)

        }
        return FileUploadResponse(
            id = serverFileId,
            fileMeta = filesRepository.getFileMeta(serverFileId),
            fileStatus = FileStatus.FAILED.name,
        )
    }

}