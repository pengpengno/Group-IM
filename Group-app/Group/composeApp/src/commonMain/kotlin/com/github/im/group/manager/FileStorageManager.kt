package com.github.im.group.manager

import com.github.im.group.repository.FilesRepository
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.*
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.model.proto.MessageType
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import okio.Path.Companion.toPath
import okio.Path
import java.io.FileNotFoundException

/***
 * 判断文件是否存在
 * Android 端 存在 content 有android 管理其封装过的路径 需要特殊处理
 */
expect fun FileStorageManager.isFileExists(fileId: String): Boolean

expect fun FileStorageManager.getLocalFilePath(fileId: String): String?

expect fun FileStorageManager.getFile(fileId: String): File?


/**
 * 读取文件内容，支持大文件（如视频文件）
 * 使用流式处理以有效处理大文件，避免内存溢出
 * @param file 要读取的文件对象
 * @return 文件的字节数组内容
 */
fun readFile(file: File): ByteArray {
    return when (file.data) {
        is FileData.Path -> {
            val path = file.data.path
            val fileSystem = FileSystem.SYSTEM
            
            // 检查文件是否存在
            if (!fileSystem.exists(path.toPath())) {
                throw FileNotFoundException("文件不存在: $path")
            }
            
            // 使用Okio读取文件内容
            fileSystem.read(path.toPath()) {
                readByteArray()
            }
        }
        is FileData.Bytes -> {
            // 如果文件数据已经以字节数组形式存在，直接返回
            file.data.data
        }
      
        FileData.None -> {
            throw IllegalStateException("文件数据为空")
        }
    }
}

/**
 * 将FileMeta对象转换为File对象
 * @param path 本地文件路径
 * @return File对象
 */
fun FileMeta.toFile(path: String): File {
    return File(
        name = this.fileName,
        path = path,
        mimeType = this.contentType,
        size = this.size,
        data = FileData.Path(path)
    )
}

class FileStorageManager(
    val filesRepository: FilesRepository,
    val fileSystem: FileSystem,
    private val baseDirectory: Path
) {

    /**
     * 获取文件内容（实现本地优先策略）
     * @param fileId 文件ID
     * @return 文件字节数组
     */
    suspend fun getFileContent(fileId: String): ByteArray {
        Napier.d("获取文件内容: $fileId")
        val file = filesRepository.getFile(fileId)
        /**
         * a) 如果文件记录为空那么 从远程下载即可 包括记录 以及文件
         * b) 如果文件不为空那么 检查本地是否存在记录 ,存在则返回本地文件内容
         *    b1) 如果记录存在但是文件在 相应的位置不存在 指定文件 , 那么重新下载 , 并且更新文件记录
         */

        try {
            // 情况 a) 文件记录为空，从远程下载
            if (file == null) {
                Napier.d("文件记录不存在，从服务器下载文件: $fileId")
                val fileContent = FileApi.downloadFile(fileId)

                // 保存到本地
                saveFileLocally(fileId, fileContent)

                return fileContent
            }

            // 情况 b) 文件记录存在，检查本地文件是否存在
            val localFilePathStr = getLocalFilePath(fileId)
            if (localFilePathStr != null) {
                val localFilePath = localFilePathStr.toPath()
                if (fileSystem.exists(localFilePath)) {
                    Napier.d("从本地获取文件: $fileId")
                    // 本地存在，直接返回本地文件内容
                    return fileSystem.read(localFilePath) {
                        readByteArray()
                    }
                }
            }

            // 情况 b1) 记录存在但本地文件不存在，重新下载
            Napier.d("本地文件不存在，从服务器重新下载: $fileId")
            val fileContent = FileApi.downloadFile(fileId)

            // 保存到本地
            saveFileLocally(fileId, fileContent)

            return fileContent
        } catch (e: Exception) {
            Napier.e("获取文件内容失败: $fileId", e)
            throw e
        }
    }


    /**
     * 添加 处于上传中的文件
     * 发送文件的逻辑 :
     * 1. 创建文件记录，并保存到数据库中 生成clientId(UUID)
     * 2. 发送文件 ( 以 clientId 作为标识)
     * 3. 等待文件上传完毕(包括 断点续传等)
     * 4. 更新文件记录状态为成功
     *
     */
    fun addPendingFile(fileId: String, fileName: String, duration: Long,filePath: String) {
        try {
            // 使用FilesRepository添加待上传文件记录
            filesRepository.addPendingFileRecord(fileId, fileName, duration,filePath)
        } catch (e: Exception) {
            Napier.e("添加待上传文件记录失败: $fileId", e)
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
                // 使用公共方法生成相对文件路径
                val relativeFilePath = generateFilePathWithFileName(fileRecord.contentType, fileRecord.originalName, fileRecord.originalName)
                
                // 确保目录存在 - 处理 parent 可能为 null 的情况
                val parentPath = relativeFilePath.parent
                if (parentPath != null) {
                    val directoryPath = baseDirectory / parentPath
                    if (!fileSystem.exists(directoryPath)) {
                        fileSystem.createDirectories(directoryPath)
                    }
                } else {
                    // 如果没有父路径，说明是根目录，通常不需要创建
                    if (!fileSystem.exists(baseDirectory)) {
                        fileSystem.createDirectories(baseDirectory)
                    }
                }

                // 写入文件
                val fullPath = baseDirectory / relativeFilePath
                fileSystem.write(fullPath) {
                    write(fileContent)
                }

                // 更新数据库中的存储路径 - 存储相对路径，便于跨设备使用
                filesRepository.updateStoragePath(fileId, relativeFilePath.toString())
                // 更新最后访问时间
                filesRepository.updateLastAccessTime(fileId)
                Napier.d("文件已保存到本地: $fullPath")
            }
        } catch (e: Exception) {
            Napier.e("保存文件到本地失败", e)
        }
    }

    /**
     * 根据文件类型获取分类路径
     * @param contentType MIME类型
     * @param fileName 文件名
     * @return 对应的分类目录名
     */
    private fun getFileCategoryPath(contentType: String, fileName: String): Path {
        return when {
            FileTypeDetector.isImageFile(contentType, fileName) -> "images".toPath()
            FileTypeDetector.isAudioFile(contentType, fileName) -> "audio".toPath()
            FileTypeDetector.isVideoFile(contentType, fileName) -> "videos".toPath()
            else -> "files".toPath()
        }
    }

    /**
     * 生成文件路径 - 公共方法，遵循项目规范
     * 文件存储路径应按年-月创建日期目录，文件命名必须使用原始文件名（originalName）
     * @param contentType MIME类型
     * @param originalFileName 原始文件名
     * @param fileName 文件名
     * @return 完整的文件路径
     */
    fun generateFilePathWithFileName(contentType: String, originalFileName: String, fileName: String): Path {
        val fileCategoryPath = getFileCategoryPath(contentType, fileName)
        val yearMonthPath = generateFilePath()
        val directoryPath = fileCategoryPath / yearMonthPath
        
        // 使用原始文件名
        return directoryPath / originalFileName
    }

    /**
     * 定期检查并清理过期文件
     * 根据时序图要求实现文件清理机制
     */
    fun cleanupExpiredFiles() {
        Napier.d("开始清理过期文件")
//        try {
//            // 获取很久未访问的文件记录（这里假设超过30天未访问的文件为过期文件）
//            val thresholdTime = Clock.System.now()
//                .minus(Duration.parse("30d"))
//                .toLocalDateTime(TimeZone.Companion.UTC)
//
//            // 查询很久未访问的文件记录
//            val expiredFiles = filesRepository.getExpiredFiles(thresholdTime)
//            Napier.d("找到 ${expiredFiles.size} 个过期文件")
//
//            expiredFiles.forEach { file ->
//                // 从数据库存储路径重建完整路径
//                val filePath = baseDirectory / file.storagePath
//                if (fileSystem.exists(filePath)) {
//                    fileSystem.delete(filePath)
//                    Napier.d("已删除本地过期文件: ${file.clientId}")
//                }
//                // 更新文件状态为已清理
//                filesRepository.updateFileStatus(file.id, FileStatus.DELETED)
//                Napier.d("已更新文件状态为已清理: ${file.clientId}")
//            }
//
//            Napier.d("过期文件清理完成")
//        } catch (e: Exception) {
//            Napier.e("清理过期文件失败", e)
//        }
    }
//
//    /**
//     * 检查文件是否存在（本地优先）
//     * @param fileId 文件ID
//     * @return 文件是否存在
//     */
//     fun isFileExists(fileId: String): Boolean {
//        FileStorageManager.isFileExists(fileId)
//     }

    /**
     * 生成文件目录
     * 使用当前日期创建子目录 如 2023-07
     */
    private fun generateFilePath(): Path {
        // 使用当前日期创建子目录 (年-月格式)，避免单个目录下文件过多
        val now = Clock.System.now().toLocalDateTime(TimeZone.Companion.currentSystemDefault())
        val yearMonth = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"

        return yearMonth.toPath()
    }

    /**
     * 生成安全且唯一的文件名
     * @param directoryPath 目录路径
     * @param originalFileName 原始文件名
     * @return 安全且唯一的文件名
     */
    private fun generateUniqueFileName(directoryPath: Path, originalFileName: String): String {
        // 确保文件名安全，替换可能引起问题的字符
        val safeFileName = originalFileName
            .replace("/", "_")
            .replace("\\", "_")
            .replace(":", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("\"", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")

        // 确保文件名唯一性，避免覆盖已存在的文件
        var uniqueFileName = safeFileName
        var counter = 1
        val fileExtension = safeFileName.substringAfterLast(".", "")
        val fileNameWithoutExtension = if (fileExtension.isNotEmpty()) {
            safeFileName.substringBeforeLast(".")
        } else {
            safeFileName
        }

        while (true) {
            val testFilePath = directoryPath / uniqueFileName
            if (!fileSystem.exists(testFilePath)) {
                break // 文件名唯一，可以使用
            }

            // 创建带编号的新文件名
            if (fileExtension.isNotEmpty()) {
                uniqueFileName = "${fileNameWithoutExtension}_$counter.$fileExtension"
            } else {
                uniqueFileName = "${safeFileName}_$counter"
            }
            counter++
        }

        return uniqueFileName
    }


    fun deleteFile(fileId: String): Boolean {
        return try {
            val fileRecord = filesRepository.getFile(fileId)
            if (fileRecord != null) {
                // 现在存储的是全路径，直接使用
                val filePath = fileRecord.storagePath.toPath()
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


/**
 * 文件类型检测器
 */
object FileTypeDetector {
    fun isImageFile(mimeType: String? = null, filename: String? = null): Boolean {
        mimeType?.let {
            if (it.startsWith("image/")) return true
        }
        filename?.let {
            val lower = it.lowercase()
            return lower.endsWith(".jpg") ||
                    lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") ||
                    lower.endsWith(".gif") ||
                    lower.endsWith(".bmp") ||
                    lower.endsWith(".webp") ||
                    lower.endsWith(".heic")
        }
        return false
    }

    fun isAudioFile(mimeType: String? = null, filename: String? = null): Boolean {
        mimeType?.let {
            if (it.startsWith("audio/")) return true
        }
        filename?.let {
            val lower = it.lowercase()
            return lower.endsWith(".mp3") ||
                    lower.endsWith(".wav") ||
                    lower.endsWith(".aac") ||
                    lower.endsWith(".flac") ||
                    lower.endsWith(".ogg") ||
                    lower.endsWith(".m4a")
        }
        return false
    }

    fun isVideoFile(mimeType: String? = null, filename: String? = null): Boolean {
        mimeType?.let {
            if (it.startsWith("video/")) return true
        }
        filename?.let {
            val lower = it.lowercase()
            return lower.endsWith(".mp4") ||
                    lower.endsWith(".mov") ||
                    lower.endsWith(".avi") ||
                    lower.endsWith(".mkv") ||
                    lower.endsWith(".flv") ||
                    lower.endsWith(".webm")
        }
        return false
    }

    fun getMessageType(mimeType: String? = null, filename: String? = null): MessageType {
        return when {
            isImageFile(mimeType, filename) -> MessageType.IMAGE
            isAudioFile(mimeType, filename) -> MessageType.VOICE
            isVideoFile(mimeType, filename) -> MessageType.VIDEO
            else -> MessageType.FILE
        }
    }
}





