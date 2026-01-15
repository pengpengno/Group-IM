package com.github.im.group.sdk

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 文件数据类型枚举，用于支持多种数据类型
 */
sealed class FileData {
    data class Bytes(val data: ByteArray) : FileData()

    /**
     * @param path 支持本地路径  /   网络http 路径
     */
    data class Path(val path: String) : FileData(){
        /**
         * 判断是否 为网络路径
         */
        fun isHttpPath() = path.startsWith("http") || path.startsWith("https")
        fun isLocalPath() = !isHttpPath()
    }

    object None : FileData()
}


/**
 * 文件选择器 用于移动端
 */
interface FilePicker {
    /**
     * 选择图像
     */
    suspend fun pickImage(): List<File>

    /**
     * 选择视频
     */
    suspend fun pickVideo(): List<File>

    /**
     * 选取文件
     */
    suspend fun pickFile(): List<File>

    /**
     * 拍照
     * 需要返回 图片的字段内容
     */
    suspend fun takePhoto(): File?
    


    /**
     * 从 PickedFile 读取文件内容为字节数组
     */
    suspend fun readFileBytes(file: File): ByteArray
}

/**
 * 文件信息
 * 包含文件名、路径、MIME 类型、大小等信息
 *用于 处理本地存在的文件 会包括 路径与数据
 */
data class File(
    val name: String,
    val path: String,
    val mimeType: String?,
    val size: Long,
    val data : FileData = FileData.None  // 使用FileData类型以支持多种数据类型并确保类型安全
)


@Composable
fun FilePickerScreen(filePicker: FilePicker) {
    Column {
        Button(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                val files = filePicker.pickFile()
                Napier.d("Files: $files")
            }
        }) {
            Text("Pick File")
        }

        Button(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                val image = filePicker.takePhoto()
                Napier.d("Photo: $image")
            }
        }) {
            Text("Take Photo")
            CameraPreviewView()
        }


    }
}

@Composable
expect fun CameraPreviewView(): Unit

expect fun getPlatformFilePicker(): FilePicker