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
import coil3.request.ImageRequest
import io.github.aakira.napier.Napier

/**
 * Cross-platform image composable for Android.
 */
@Composable
actual fun CrossPlatformImage(
    file: File,
    modifier: Modifier,
    onLongClick: (() -> Unit)?
) {
    val context = LocalContext.current
    val path = file.dataPath()
    val isRemotePath = remember(path) { path.startsWith("http://") || path.startsWith("https://") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    Napier.d("CrossPlatformImage: path=$path")

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存图片") },
            text = { Text("是否保存图片到本地？") },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        saveImageToLocal(
                            context = context,
                            path = path,
                            fileName = file.name.ifBlank { "image_${System.currentTimeMillis()}.png" }
                        )
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Button(onClick = { showSaveDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

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
                },
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        showSaveDialog = true
                    }
                }
            )
    ) {
        ImageContent(
            context = context,
            path = path,
            isRemotePath = isRemotePath,
            modifier = Modifier.fillMaxSize(),
            loadingSize = 40.dp
        )
    }
}

@Composable
fun FullScreenImage(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isRemotePath = remember(imagePath) {
        imagePath.startsWith("http://") || imagePath.startsWith("https://")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            ImageContent(
                context = context,
                path = imagePath,
                isRemotePath = isRemotePath,
                modifier = Modifier.fillMaxSize(),
                loadingSize = 60.dp
            )

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

@Composable
private fun ImageContent(
    context: Context,
    path: String,
    isRemotePath: Boolean,
    modifier: Modifier,
    loadingSize: androidx.compose.ui.unit.Dp
) {
    var isLoading by remember(path) { mutableStateOf(true) }
    var isError by remember(path) { mutableStateOf(false) }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(path)
            .build(),
        contentDescription = null,
        modifier = modifier,
        onLoading = {
            isLoading = true
            isError = false
        },
        onError = {
            isLoading = false
            isError = true
        },
        onSuccess = {
            isLoading = false
            isError = false
        }
    )

    if (isLoading && !isError) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(loadingSize)
            )
        }
    } else if (isError) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRemotePath) "图片准备中..." else "加载失败",
                color = Color.White
            )
        }
    }
}

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

private fun saveImageToLocal(context: Context, path: String, fileName: String) {
    try {
        val sourceFile = java.io.File(path)
        if (sourceFile.exists()) {
            val destFile = java.io.File(context.getExternalFilesDir(null), fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            Napier.d("图片已保存到: ${destFile.absolutePath}")
        }
    } catch (e: Exception) {
        Napier.e("保存图片失败: ${e.message}")
    }
}
