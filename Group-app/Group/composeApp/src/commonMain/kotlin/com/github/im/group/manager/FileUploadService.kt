package com.github.im.group.manager

import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.api.FileUploadResponse
import com.github.im.group.api.UploadFileRequest
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlin.math.log
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
            return uploadResponse
        }catch (e: Exception){
            Napier.e{"上传文件失败 $e"}
        }finally {
            //TODO  更新文件为 失败状态

        }

        val uploadResponse = FileUploadResponse(
            id = serverFileId,
            fileMeta = filesRepository.getFileMeta(serverFileId),
            fileStatus = MessagesStatus.FAILED.name
        )
        return uploadResponse
    }

}