package com.github.im.group.sdk


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 文件 Picker
 */
interface FilePickerLauncher {
    suspend fun pick(mimeType: String): List<Uri>
}

/**
 * 创建并记住一个文件选择器启动器
 * @return 返回一个 FilePickerLauncher 对象，用于启动文件选择并获取结果
 */
@Composable
fun rememberFilePickerLauncher(): FilePickerLauncher {
    // 创建一个 MutableSharedFlow 用于发射文件选择结果
    val resultFlow = remember { MutableSharedFlow<List<Uri>>() }

    // 创建相机启动器（当前未实现具体功能）
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // handle photo result here
    }
    
    // 创建文件选择启动器，用于处理文件选择结果
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        // 当文件选择完成后，通过 CoroutineScope 发射结果
        CoroutineScope(Dispatchers.Main).launch {
            resultFlow.emit(uris)
        }
    }

    // 返回一个 FilePickerLauncher 对象
    return object : FilePickerLauncher {
        /**
         * 挂起函数，用于启动文件选择器并返回选择的文件 URI 列表
         * @param mimeType 需要选择的文件类型
         * @return 返回一个包含选择文件 URI 的列表
         */
        override suspend fun pick(mimeType: String): List<Uri> {
            // 创建 Intent 用于打开文档
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = mimeType
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            // 启动文件选择器
            launcher.launch(arrayOf(mimeType))
            // 等待并返回选择结果
            return resultFlow.first()
        }
    }
}


class AndroidFilePicker(private val context: Context,

    ) : FilePicker {

    override suspend fun pickImage(): List<PickedFile> {

        return pickFiles("image/*")
    }

    override suspend fun pickVideo(): List<PickedFile> {
        return pickFiles("video/*")
    }

    override suspend fun pickFile(): List<PickedFile> {
        return pickFiles("*/*")
    }

    override suspend fun takePhoto(): PickedFile? {
        TODO("Not yet implemented")
    }

    /**
     * 拍照
     */
    fun takePhoto(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        launcher.launch(intent)
    }

    /**
     * 图片/视频选择
     */
    fun selectMedia(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/* video/*"
        }
        launcher.launch(intent)
    }

    private suspend fun pickFiles(mimeType: String): List<PickedFile> {
        val result = CompletableDeferred<List<Uri>>()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = mimeType
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        (context as? Activity)?.let { activity ->
            activity.startActivityForResult(intent, 999)
            // 用 registerForActivityResult 替代较好，但需绑定生命周期
            // 简化处理为模拟返回
            // 实际情况需你配合回调机制返回数据并 resume Coroutine
        }

        // 📝 实际项目中，这里要使用 registerForActivityResult + 回调挂起机制

        return listOf() // TODO: 填写获取 Uri 并转为 PickedFile
    }

    private suspend fun uriToPickedFile(uri: Uri): PickedFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = "unknown"
        var size: Long = -1
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
                size = cursor.getLong(sizeIndex)
            }
        }

        val mime = resolver.getType(uri)
        val path = uri.toString()

        PickedFile(name, path, mime, size)
    }
}

@SuppressLint("StaticFieldLeak")
lateinit var androidContext: Context

/**
 * 初始化 安卓 上下文
 */
fun initAndroidContext(ctx: Context) {
    androidContext = ctx
}

/**
 * 音视频预览
 */
@Composable
@Preview
actual fun CameraPreviewView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraController = LifecycleCameraController(ctx)
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            previewView.controller = cameraController

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}


actual fun getPlatformFilePicker(): FilePicker = AndroidFilePicker(androidContext)