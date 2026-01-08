package com.github.im.group.sdk

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.net.URL

@Composable
actual fun CrossPlatformImage(
    file: File,
    modifier: Modifier,
    size: Int
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file.path) {
        try {
            when (val data = file.data) {
                is FileData.Path -> {
                    // 桌面平台使用文件路径
                    val file = java.io.File(data.path)
                    if (file.exists()) {
                        file.inputStream().use { stream ->
                            imageBitmap = loadImageBitmap(stream)
                        }
                    }
                }
                is FileData.Uri -> {
                    // 对于URI，尝试作为URL加载
                    URL(data.uri).openStream().use { stream ->
                        imageBitmap = loadImageBitmap(stream)
                    }
                }
                is FileData.Bytes -> {
                    // 直接使用字节数组
                    java.io.ByteArrayInputStream(data.data).use { stream ->
                        imageBitmap = loadImageBitmap(stream)
                    }
                }
                else -> {
                    // 尝试使用 pickedFile.path 作为 URL
                    URL(file.path).openStream().use { stream ->
                        imageBitmap = loadImageBitmap(stream)
                    }
                }
            }
        } catch (_: Exception) {
            imageBitmap = null
        }
    }

    Box(modifier = modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = null,
                modifier = Modifier.size(size.dp)
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}