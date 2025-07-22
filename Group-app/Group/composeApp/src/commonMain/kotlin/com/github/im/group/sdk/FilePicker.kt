package com.github.im.group.sdk

/**
 * 文件选择器 用于移动端
 */
interface FilePicker {
    suspend fun pickImage(): List<PickedFile>
    suspend fun pickVideo(): List<PickedFile>
    suspend fun pickFile(): List<PickedFile>
}

data class PickedFile(
    val name: String,
    val path: String,
    val mimeType: String?,
    val size: Long
)

expect fun getPlatformFilePicker(): FilePicker
