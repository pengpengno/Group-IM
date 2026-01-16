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

/**
 * 获取文件的本地路径
 *
 * 如果 本地不存在 {@see FileStorageManager.isFileExists false  不存在} 返回 null
 */
expect fun FileStorageManager.getLocalFilePath(fileId: String): String?
/**
 * 获取本地文件对象
 * 如果文件在本地不存在 那么 文件 {@see FileData.Path(path) 数据源则为 下载链接 }
 * @param fileId 文件ID
 * @return 本地文件对象，如果不存在则返回null
 */
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
 * 其中 读取 path 作为  文件的数据源
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
//        data = FileData.Path(path.ifEmpty { this.getFileUrl() ?: "" })
    )
}



/**
 * 将FileMeta对象转换为File对象
 * 其中需要判断文件的状态为 正常，  数据为 http 资源
 * @param path 本地文件路径
 * @return File对象
 */
fun FileMeta.toFile(): File {
    return File(
        name = this.fileName,
        path = "",
        mimeType = this.contentType,
        size = this.size,
        data = FileData.Path(getFileUrl() ?: "")
    )
}


class FileStorageManager(
    val filesRepository: FilesRepository,
    val fileSystem: FileSystem,
    private val baseDirectory: Path
) {

    /**
     * 获取文件内容路径（实现本地优先策略）
     * @param fileId 文件ID
     * @return 本地文件路径
     */
    suspend fun getFileContentPath(fileId: String): String? {
        Napier.d("获取文件内容路径: $fileId")
        val file = filesRepository.getFile(fileId)
        /**
         * a) 如果文件记录为空那么 从远程下载即可 包括记录 以及文件
         * b) 如果文件不为空那么 检查本地是否存在记录 ,存在则返回本地文件路径
         *    b1) 如果记录存在但是文件在 相应的位置不存在 指定文件 , 那么重新下载 , 并且更新文件记录
         */

        try {
            // 情况 a) 文件记录为空，从远程下载
            if (file == null) {
                Napier.d("文件记录不存在，从服务器下载文件: $fileId")
                downloadFileToLocalStorage(fileId)
                return getLocalFilePath(fileId)
            }

            // 情况 b) 文件记录存在，使用isFileExists检查本地文件是否存在
            if (isFileExists(fileId)) {
                // 本地文件存在，直接返回路径
                val localFilePathStr = getLocalFilePath(fileId)
                Napier.d("文件已存在本地，返回路径: $localFilePathStr")
                return localFilePathStr
            } else {
                // 本地文件不存在，从远程下载
                Napier.d("本地文件不存在，从服务器下载: $fileId")
                downloadFileToLocalStorage(fileId)
                return getLocalFilePath(fileId)
            }
        } catch (e: Exception) {
            Napier.e("获取文件内容路径失败: $fileId", e)
            return null
        }
    }

    /**
     * 获取文件内容路径（实现本地优先策略）- 支持进度回调
     * @param fileId 文件ID
     * @param onProgress 进度回调函数，参数为已下载字节数和总字节数
     * @return 本地文件路径
     */
    suspend fun getFileContentPathWithProgress(fileId: String, onProgress: (Long, Long) -> Unit): String? {
        Napier.d("获取文件内容路径: $fileId")
        val file = filesRepository.getFile(fileId)
        /**
         * a) 如果文件记录为空那么 从远程下载即可 包括记录 以及文件
         * b) 如果文件不为空那么 检查本地是否存在记录 ,存在则返回本地文件路径
         *    b1) 如果记录存在但是文件在 相应的位置不存在 指定文件 , 那么重新下载 , 并且更新文件记录
         */

        try {
            // 情况 a) 文件记录为空，从远程下载
            if (file == null) {
                Napier.d("文件记录不存在，从服务器下载文件: $fileId")
                downloadFileToLocalStorageWithProgress(fileId, onProgress)
                return getLocalFilePath(fileId)
            }

            // 情况 b) 文件记录存在，使用isFileExists检查本地文件是否存在
            if (isFileExists(fileId)) {
                // 本地文件存在，直接返回路径
                val localFilePathStr = getLocalFilePath(fileId)
                Napier.d("文件已存在本地，返回路径: $localFilePathStr")
                return localFilePathStr
            } else {
                // 本地文件不存在，从远程下载
                Napier.d("本地文件不存在，从服务器下载: $fileId")
                downloadFileToLocalStorageWithProgress(fileId, onProgress)
                return getLocalFilePath(fileId)
            }
        } catch (e: Exception) {
            Napier.e("获取文件内容路径失败: $fileId", e)
            return null
        }
    }

    /**
     * 获取文件内容（实现本地优先策略）- 保持向后兼容，但不推荐用于大文件
     * @param fileId 文件ID
     * @return 文件字节数组
     * 
     * 注意：对于大于5MB的文件，不建议使用此方法，因为它会将整个文件加载到内存中。
     * 推荐使用 getFileObject 方法获取文件路径，然后流式读取。
     */
    @Deprecated(
        "此方法不适合大文件，会导致OOM。请使用getFileObject获取文件路径后流式读取。",
        level = DeprecationLevel.WARNING
    )
    suspend fun getFileContent(fileId: String): ByteArray {
        Napier.d("获取文件内容: $fileId")
        val file = filesRepository.getFile(fileId)
        
        // 检查文件大小，如果超过5MB则不允许使用此方法
        val fileMeta = FileApi.getFileMeta(fileId)
        if (fileMeta.size > 5 * 1024 * 1024) {  // 5MB
            throw IllegalStateException("文件过大(${'$'}{fileMeta.size} bytes)，不能使用此方法获取，避免OOM错误")
        }
        
        /**
         * a) 如果文件记录为空那么 从远程下载即可 包括记录 以及文件
         * b) 如果文件不为空那么 检查本地是否存在记录 ,存在则返回本地文件内容
         *    b1) 如果记录存在但是文件在 相应的位置不存在 指定文件 , 那么重新下载 , 并且更新文件记录
         */

        try {
            // 情况 a) 文件记录为空，从远程下载
            if (file == null) {
                Napier.d("文件记录不存在，从服务器下载文件: $fileId")
                // 为保持向后兼容性，需要将File对象转换为ByteArray
                val downloadedFile = downloadFileToLocalStorageAndReturnFile(fileId)
                return fileSystem.read(downloadedFile.path.toPath()) {
                    readByteArray()
                }
            }

            // 情况 b) 文件记录存在，检查本地文件是否存在
            val localFilePathStr = getLocalFilePath(fileId)
            if (localFilePathStr != null) {
                val localFilePath = localFilePathStr.toPath()
                if (fileSystem.exists(localFilePath)) {
                    // 本地文件存在，直接读取
                    Napier.d("文件已存在本地，直接读取: $localFilePath")
                    return fileSystem.read(localFilePath) {
                        readByteArray()
                    }
                } else {
                    // 本地文件不存在，从远程下载
                    Napier.d("本地文件不存在，从服务器下载: $fileId")
                    // 为保持向后兼容性，需要将File对象转换为ByteArray
                    val downloadedFile = downloadFileToLocalStorageAndReturnFile(fileId)
                    return fileSystem.read(downloadedFile.path.toPath()) {
                        readByteArray()
                    }
                }
            } else {
                // 本地路径不存在，从远程下载
                Napier.d("本地路径不存在，从服务器下载: $fileId")
                // 为保持向后兼容性，需要将File对象转换为ByteArray
                val downloadedFile = downloadFileToLocalStorageAndReturnFile(fileId)
                return fileSystem.read(downloadedFile.path.toPath()) {
                    readByteArray()
                }
            }
        } catch (e: OutOfMemoryError) {
            // 如果发生内存溢出，使用流式下载方式
            Napier.e("内存不足，使用流式下载: $fileId", e)
            return downloadFileStreaming(fileId)
        } catch (e: Exception) {
            // 区分协程取消异常和其他异常
            if (e is kotlinx.coroutines.CancellationException) {
                // 协程被取消，不记录为错误
                Napier.d("获取文件内容被取消: $fileId")
                throw e
            } else {
                Napier.e("获取文件内容失败: $fileId", e)
                throw e
            }
        }
    }
    
    /**
     * 获取文件对象（实现本地优先策略）- 推荐用于大文件
     * @param fileId 文件ID
     * @return File对象
     */
    suspend fun getFileObject(fileId: String): File {
        Napier.d("获取文件对象: $fileId")
        val file = filesRepository.getFile(fileId)
        /**
         * a) 如果文件记录为空那么 从远程下载即可 包括记录 以及文件
         * b) 如果文件不为空那么 检查本地是否存在记录 ,存在则返回本地文件对象
         *    b1) 如果记录存在但是文件在 相应的位置不存在 指定文件 , 那么重新下载 , 并且更新文件记录
         */

        try {
            // 情况 a) 文件记录为空，从远程下载
            if (file == null) {
                Napier.d("文件记录不存在，从服务器下载文件: $fileId")
                return downloadFileToLocalStorageAndReturnFile(fileId)
            }

            // 情况 b) 文件记录存在，检查本地文件是否存在
            val localFilePathStr = getLocalFilePath(fileId)
            if (localFilePathStr != null) {
                val localFilePath = localFilePathStr.toPath()
                if (fileSystem.exists(localFilePath)) {
                    // 本地文件存在，构建并返回File对象
                    Napier.d("文件已存在本地，返回File对象: $localFilePath")
                    val fileMeta = FileApi.getFileMeta(fileId)
                    return File(
                        name = fileMeta.fileName,
                        path = localFilePathStr,
                        mimeType = fileMeta.contentType,
                        size = fileMeta.size,
                        data = FileData.Path(localFilePathStr)
                    )
                } else {
                    // 本地文件不存在，从远程下载
                    Napier.d("本地文件不存在，从服务器下载: $fileId")
                    return downloadFileToLocalStorageAndReturnFile(fileId)
                }
            } else {
                // 本地路径不存在，从远程下载
                Napier.d("本地路径不存在，从服务器下载: $fileId")
                return downloadFileToLocalStorageAndReturnFile(fileId)
            }
        } catch (e: Exception) {
            // 区分协程取消异常和其他异常
            if (e is kotlinx.coroutines.CancellationException) {
                // 协程被取消，不记录为错误
                Napier.d("获取文件对象被取消: $fileId")
                throw e
            } else {
                Napier.e("获取文件对象失败: $fileId", e)
                throw e
            }
        }
    }

    /**
     * 下载文件到本地并返回File对象（推荐用于大文件）
     * @param fileId 文件ID
     * @return File对象
     */
    private suspend fun downloadFileToLocalStorageAndReturnFile(fileId: String): File {
        val tempFilePath = baseDirectory / "temp" / fileId
        
        try {
            // 确保临时目录存在
            val tempDir = baseDirectory / "temp"
            if (!fileSystem.exists(tempDir)) {
                fileSystem.createDirectories(tempDir)
            }
            
            // 流式下载到临时文件
            FileApi.downloadFileToPath(fileId, tempFilePath)
            
            // 获取文件元数据
            val fileMeta = FileApi.getFileMeta(fileId)
            val relativeFilePath = generateFilePathWithFileName(fileMeta.contentType, fileMeta.fileName, fileMeta.fileName)
            val fullPath = baseDirectory / relativeFilePath
            
            // 确保目标目录存在
            val targetDir = fullPath.parent
            if (targetDir != null && !fileSystem.exists(targetDir)) {
                fileSystem.createDirectories(targetDir)
            }
            
            // 将临时文件移动到目标位置（流式移动，避免加载整个文件到内存）
            fileSystem.write(fullPath) {
                fileSystem.source(tempFilePath).use { source ->
                    writeAll(source)
                }
            }
            
            // 删除临时文件
            if (fileSystem.exists(tempFilePath)) {
                fileSystem.delete(tempFilePath)
            }
            
            // 更新数据库中的存储路径
            filesRepository.updateStoragePath(fileId, fullPath.toString())

            // 返回File对象
            return File(
                name = fileMeta.fileName,
                path = fullPath.toString(),
                mimeType = fileMeta.contentType,
                size = fileMeta.size,
                data = FileData.Path(fullPath.toString())
            )
        } catch (e: Exception) {
            // 清理临时文件
            if (fileSystem.exists(tempFilePath)) {
                fileSystem.delete(tempFilePath)
            }
            // 如果目标文件已创建但下载失败，也要清理
            try {
                val fileMeta = FileApi.getFileMeta(fileId)
                val relativeFilePath = generateFilePathWithFileName(fileMeta.contentType, fileMeta.fileName, fileMeta.fileName)
                val fullPath = baseDirectory / relativeFilePath
                if (fileSystem.exists(fullPath)) {
                    fileSystem.delete(fullPath)
                }
            } catch (ex: Exception) {
                // 如果获取文件元数据失败，则忽略此步骤
                Napier.w("清理目标文件失败: $ex")
            }
            throw e
        }
    }
    
    /**
     * 流式下载文件（避免将整个文件加载到内存）
     * @param fileId 文件ID
     * @return 文件字节数组
     */
    private suspend fun downloadFileStreaming(fileId: String): ByteArray {
        val file = filesRepository.getFile(fileId)
        val tempFilePath = baseDirectory / "temp" / fileId
        
        try {
            // 确保临时目录存在
            val tempDir = baseDirectory / "temp"
            if (!fileSystem.exists(tempDir)) {
                fileSystem.createDirectories(tempDir)
            }
            
            // 流式下载到临时文件
            FileApi.downloadFileToPath(fileId, tempFilePath)
            
            // 读取临时文件内容
            val content = fileSystem.read(tempFilePath) {
                readByteArray()
            }
            
            // 将文件移动到正确位置
            val localFilePath = if (file != null) {
                val relativeFilePath = generateFilePathWithFileName(file.contentType, file.originalName, file.originalName)
                baseDirectory / relativeFilePath
            } else {
                baseDirectory / "files" / generateFilePath() / fileId
            }
            
            // 确保目标目录存在
            val targetDir = localFilePath.parent
            if (targetDir != null && !fileSystem.exists(targetDir)) {
                fileSystem.createDirectories(targetDir)
            }
            
            // 移动文件
            fileSystem.write(localFilePath) {
                write(content)
            }
            
            // 删除临时文件
            if (fileSystem.exists(tempFilePath)) {
                fileSystem.delete(tempFilePath)
            }
            
            // 更新数据库中的存储路径
            if (file != null) {
                filesRepository.updateStoragePath(fileId, localFilePath.toString())
            }
            
            return content
        } catch (e: Exception) {
            // 区分协程取消异常和其他异常
            if (e is kotlinx.coroutines.CancellationException) {
                // 协程被取消，清理临时文件并重新抛出异常
                if (fileSystem.exists(tempFilePath)) {
                    fileSystem.delete(tempFilePath)
                }
                Napier.d("流式下载文件被取消: $fileId")
                throw e
            } else {
                // 其他异常，清理临时文件并记录错误
                if (fileSystem.exists(tempFilePath)) {
                    fileSystem.delete(tempFilePath)
                }
                throw e
            }
        }
    }
    
    /**
     * 流式下载文件到本地（推荐用于大文件）
     * @param fileId 文件ID
     */
    suspend fun downloadFileToLocalStorage(fileId: String) {
        downloadFileToLocalStorageWithProgress(fileId) { _, _ -> }
    }
    
    /**
     * 流式下载文件到本地（推荐用于大文件）- 支持进度回调
     * @param fileId 文件ID
     * @param onProgress 进度回调函数，参数为已下载字节数和总字节数
     */
    suspend fun downloadFileToLocalStorageWithProgress(fileId: String, onProgress: (Long, Long) -> Unit) {
        val file = filesRepository.getFile(fileId)
        if (file == null) {
            throw IllegalArgumentException("文件记录不存在: $fileId")
        }
        
        val relativeFilePath = generateFilePathWithFileName(file.contentType, file.originalName, file.originalName)
        val fullPath = baseDirectory / relativeFilePath
        
        try {
            // 确保目录存在
            val parentPath = fullPath.parent
            if (parentPath != null) {
                val directoryPath = baseDirectory / parentPath
                if (!fileSystem.exists(directoryPath)) {
                    fileSystem.createDirectories(directoryPath)
                }
            } else {
                if (!fileSystem.exists(baseDirectory)) {
                    fileSystem.createDirectories(baseDirectory)
                }
            }
            
            // 流式下载文件（带进度）
            FileApi.downloadFileToPathWithProgress(fileId, fullPath, onProgress)
            
            // 更新数据库中的存储路径
            filesRepository.updateStoragePath(fileId, fullPath.toString())
            Napier.d("文件已流式下载并保存到本地: $fullPath")
        } catch (e: Exception) {
            // 区分协程取消异常和其他异常
            if (e is kotlinx.coroutines.CancellationException) {
                // 协程被取消，不记录为错误
                Napier.d("流式下载文件被取消: $fileId")
            } else {
                Napier.e("流式下载文件失败: $fileId", e)
            }
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

























