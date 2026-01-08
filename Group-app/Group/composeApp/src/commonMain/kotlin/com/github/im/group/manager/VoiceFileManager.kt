package com.github.im.group.manager

import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.time.Duration

/**
 * 语音文件管理器
 * 负责语音文件的存储和管理，使用专门的目录结构避免临时文件被清理
 */
class VoiceFileManager(
    private val fileSystem: FileSystem,
    private val baseDirectory: Path
) {
    companion object {
        const val VOICE_DIR_NAME = "voice_records"
    }

    init {
        // 初始化语音文件目录
        createVoiceDirectory()
    }

    /**
     * 创建语音文件目录
     */
    private fun createVoiceDirectory() {
        try {
            val voiceDir = baseDirectory / VOICE_DIR_NAME
            if (!fileSystem.exists(voiceDir)) {
                fileSystem.createDirectories(voiceDir)
            }
        } catch (e: Exception) {
            Napier.e("创建语音目录失败", e)
        }
    }

    /**
     * 生成语音文件路径
     * 使用日期子目录组织文件，避免单个目录文件过多
     */
    fun generateVoiceFilePath(fileName: String): Path {
        try {
            val yearMonthPath = generateFilePath()
            val directoryPath = baseDirectory / VOICE_DIR_NAME / yearMonthPath

            // 确保目录存在
            if (!fileSystem.exists(directoryPath)) {
                fileSystem.createDirectories(directoryPath)
            }

            val uniqueFileName = generateUniqueFileName(directoryPath, fileName)
            return directoryPath / uniqueFileName
        } catch (e: Exception) {
            Napier.e("生成语音文件路径失败", e)
            // 如果生成失败，返回基本路径
            return baseDirectory / VOICE_DIR_NAME / fileName
        }
    }

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

    /**
     * 获取语音文件的完整路径字符串
     */
    fun getVoiceFileAbsolutePath(fileName: String): String {
        val filePath = generateVoiceFilePath(fileName)
        return filePath.toString()
    }

    /**
     * 清理过期的语音文件
     * @param retentionDays 保留天数，默认30天
     */
    fun cleanupExpiredVoiceFiles(retentionDays: Int = 30) {
        try {
            val voiceDir = baseDirectory / VOICE_DIR_NAME
            if (!fileSystem.exists(voiceDir)) {
                return
            }

            val retentionTime = Clock.System.now()
                .minus(Duration.parse("${retentionDays}d"))
                .toEpochMilliseconds()

            // 遍历所有子目录和文件进行清理
            fileSystem.list(voiceDir).forEach { subDir ->
                if (fileSystem.metadata(subDir).isDirectory) {
                    fileSystem.list(subDir).forEach { file ->
                        try {
                            val metadata = fileSystem.metadata(file)
                            if (metadata.isRegularFile) {
                                // 检查文件修改时间
                                if (metadata.createdAtMillis ?: 0 < retentionTime) {
                                    fileSystem.delete(file)
                                    Napier.d("已删除过期语音文件: ${file.name}")
                                }
                            }
                        } catch (e: Exception) {
                            Napier.e("清理语音文件失败: ${file.name}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("清理过期语音文件失败", e)
        }
    }

    /**
     * 获取语音文件大小
     */
    fun getVoiceFileSize(filePath: String): Long {
        try {
            val path = filePath.toPath()
            if (fileSystem.exists(path)) {
                val metadata = fileSystem.metadata(path)
                return metadata.size ?: 0
            }
        } catch (e: Exception) {
            Napier.e("获取语音文件大小失败", e)
        }
        return 0
    }
}