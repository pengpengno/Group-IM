package com.github.im.group.sdk

import android.annotation.SuppressLint


// File: androidMain/kotlin/FilePickerAndroid.kt

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidFilePicker(private val context: Context,
                        private val fileLauncher: ActivityResultLauncher<Intent>,
                        private val mediaLauncher: ActivityResultLauncher<Intent>,
                        private val cameraLauncher: ActivityResultLauncher<Intent>
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

fun initAndroidContext(ctx: Context) {
    androidContext = ctx
}


@Composable
fun FilePickerScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 使用 rememberLauncherForActivityResult 注册
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = mutableListOf<Uri>()
        result.data?.data?.let { uris.add(it) }
        result.data?.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                uris.add(clip.getItemAt(i).uri)
            }
        }
        filePicker?.onFilePicked(uris)
    }

    val dummyLauncher = fileLauncher // 提供给 Platform Code 初始化用
    val mediaLauncher = fileLauncher
    val cameraLauncher = fileLauncher

    LaunchedEffect(Unit) {
        if (!::filePicker.isInitialized) {
            filePicker = AndroidFilePicker(context, fileLauncher, mediaLauncher, cameraLauncher)
        }
    }

    Column {
        Button(onClick = {
            lifecycleOwner.lifecycleScope.launch {
                val files = filePicker?.pickFile()
                println("选中文件：$files")
            }
        }) {
            Text("选择文件")
        }

        Button(onClick = {
            filePicker?.takePhoto()
        }) {
            Text("拍照")
        }
    }
}

actual fun getPlatformFilePicker(): FilePicker = AndroidFilePicker(androidContext)