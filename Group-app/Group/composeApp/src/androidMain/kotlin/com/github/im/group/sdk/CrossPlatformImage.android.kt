package com.github.im.group.sdk

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.im.group.GlobalCredentialProvider
import io.github.aakira.napier.Napier
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 跨平台图像组件
 */
@Composable
actual fun CrossPlatformImage(
    file: File,
    modifier: Modifier,
    size: Int,
    onLongClick: (() -> Unit)?
) {
    val context = LocalContext.current
    val path = file.dataPath()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    Napier.d("CrossPlatformImage: path=$path")

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存图片") },
            text = { Text("是否保存图片到本地？") },
            confirmButton = {
                Button(onClick = {
                    showSaveDialog = false
                    // 直接保存图片到本地
                    saveImageToLocal(context, path, file.name ?: "image_${'$'}${'$'}{System.currentTimeMillis()}.png")
                }) { Text("保存") }
            },
            dismissButton = {
                Button(onClick = { showSaveDialog = false }) { Text("取消") }
            }
        )
    }

    // 点击全屏展示
    if (showFullScreen) {
        FullScreenImage(path) { showFullScreen = false }
    }

    Box(
        modifier = modifier
            .padding(PaddingValues(3.dp, 12.dp))
            .combinedClickable(
                onClick = { 
                    showFullScreen = true
                    Napier.d { "click show full screen" }
                },   // 点击全屏
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        // 默认行为：显示保存对话框
                        showSaveDialog = true
                    }
                } // 长按处理
            )
    ) {
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(path)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            onLoading = { state ->
                isLoading = true
            },
            onError = { state ->
                isLoading = false
                isError = true
            },
            onSuccess = { state ->
                isLoading = false
                isError = false
            }
        )
        
        // 根据加载状态显示相应的覆盖内容
        if (isLoading && !isError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        } else if (isError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("加载失败", color = Color.White)
            }
        }
    }
}

/**
 * 全屏展示图片组件
 */
@Composable
fun FullScreenImage(imagePath: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss,
            properties = DialogProperties(
            usePlatformDefaultWidth = false
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            var isLoading by remember { mutableStateOf(true) }
            var isError by remember { mutableStateOf(false) }
            
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onLoading = { state ->
                    isLoading = true
                },
                onError = { state ->
                    isLoading = false
                    isError = true
                },
                onSuccess = { state ->
                    isLoading = false
                    isError = false
                }
            )
            
            // 根据加载状态显示相应的覆盖内容
            if (isLoading && !isError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(60.dp)
                    )
                }
            } else if (isError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载失败", color = Color.White)
                }
            }
            
            // 右上角关闭按钮
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.7f))
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

/**
 * 从 FileData 获取实际路径字符串
 */
private fun File.dataPath(): String {
    return when (val data = this.data) {
        is FileData.Path -> data.path
        is FileData.Bytes -> {
            val tmpFile = java.io.File.createTempFile("tmp_image", ".tmp")
            tmpFile.writeBytes(data.data)
            tmpFile.absolutePath
        }
        FileData.None -> ""
    }
}

/**
 * 保存图片到应用目录
 */
private fun saveImageToLocal(context: Context, path: String, fileName: String) {
    // 使用文件系统直接复制文件
    try {
        val sourceFile = java.io.File(path)
        if (sourceFile.exists()) {
            val destFile = java.io.File(context.getExternalFilesDir(null), fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            Napier.d("图片已保存到: ${destFile.absolutePath}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Napier.e("保存图片失败: ${e.message}")
    }
}

