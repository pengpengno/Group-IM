package com.github.im.group.sdk

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 文件选择器 用于移动端
 */
interface FilePicker {
    /**
     * 选择图像
     */
    suspend fun pickImage(): List<PickedFile>

    /**
     * 选择视频
     */
    suspend fun pickVideo(): List<PickedFile>

    /**
     * 选取文件
     */
    suspend fun pickFile(): List<PickedFile>

    /**
     * 拍照
     */
    suspend fun takePhoto(): PickedFile?
    
    /**
     * 读取文件内容为字节数组
     */
    suspend fun readFileBytes(file: PickedFile): ByteArray?
}

data class PickedFile(
    val name: String,
    val path: String,
    val mimeType: String?,
    val size: Long
)


@Composable
fun FilePickerScreen(filePicker: FilePicker) {
    Column {
        Button(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                val files = filePicker.pickFile()
                println("Files: $files")
            }
        }) {
            Text("Pick File")
        }

        Button(onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                val image = filePicker.takePhoto()
                println("Photo: $image")
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