package com.github.im.group.sdk

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.im.group.GlobalCredentialProvider
import java.io.InputStream

/**
 * 跨平台图像组件
 */
@Composable
actual fun CrossPlatformImage(
    file: File,
    modifier: Modifier,
    size: Int
) {
    val context = LocalContext.current
    val token = GlobalCredentialProvider.currentToken
    
    // 根据 PickedFile.data 的类型来决定如何加载图片
    when (val data = file.data) {
        is FileData.Bytes -> {
            // 使用字节数组加载
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(data.data)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = modifier
                    .padding(PaddingValues(3.dp, 12.dp))
            )
        }
        is FileData.Path -> {
            // 检查路径是否为Content URI
            if (data.path.startsWith("content://")) {
                // 处理Content URI
                val inputStream: InputStream? = try {
                    context.contentResolver.openInputStream(Uri.parse(data.path))
                } catch (e: Exception) {
                    null
                }
                
                if (inputStream != null) {
                    // 使用Coil加载Content URI
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(inputStream)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = modifier
                            .padding(PaddingValues(3.dp, 12.dp))
                    )
                } else {
                    // 如果无法加载，显示错误占位符
                    Image(
                        painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                        contentDescription = "无法加载图片",
                        modifier = modifier
                            .padding(PaddingValues(3.dp, 12.dp))
                    )
                }
            } else {
                // 普通文件路径
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(data.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = modifier
                        .padding(PaddingValues(3.dp, 12.dp))
                )
            }
        }
        is FileData.Uri -> {
            // 处理URI
            val inputStream: InputStream? = try {
                context.contentResolver.openInputStream(Uri.parse(data.uri))
            } catch (e: Exception) {
                null
            }
            
            if (inputStream != null) {
                // 使用Coil加载Content URI
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(inputStream)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = modifier
                        .padding(PaddingValues(3.dp, 12.dp))
                )
            } else {
                // 如果无法加载，显示错误占位符
                Image(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                    contentDescription = "无法加载图片",
                    modifier = modifier
                        .padding(PaddingValues(3.dp, 12.dp))
                )
            }
        }
        FileData.None -> {
            // 如果没有数据，显示错误占位符
            Image(
                painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                contentDescription = "无图片数据",
                modifier = modifier
                    .padding(PaddingValues(3.dp, 12.dp))
            )
        }
    }
}

/**
 * 获取Content URI的文件信息
 */
fun Context.getContentUriFileInfo(uri: String): Pair<String, Long>? {
    val contentUri = Uri.parse(uri)
    return try {
        val resolver: ContentResolver = this.contentResolver
        var name = "unknown"
        var size: Long = -1
        
        resolver.query(contentUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: "unknown"
                size = cursor.getLong(sizeIndex)
            }
        }
        
        Pair(name, size)
    } catch (e: Exception) {
        null
    }
}