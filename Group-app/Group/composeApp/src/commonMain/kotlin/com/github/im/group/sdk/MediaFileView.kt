package com.github.im.group.sdk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image as FoundationImage

/**
 * 统一的媒体文件查看组件
 * 
 * 自动判断文件类型并选择合适的展示方式：
 * - 视频：显示缩略图 + 播放按钮，点击后全屏播放
 * - 图片：显示图片预览
 * - 其他：显示文件图标
 */
@Composable
fun MediaFileView(
    file: File,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onDownloadFile: ((String) -> Unit)? = null,  // 以函数式方式传递下载功能
    onShowMenu: ((File) -> Unit)? = null,        // 显示菜单的回调
    onClick: (() -> Unit)? = null                // 点击回调，用于自定义处理
) {
    when {
        file.isVideo() -> {
            VideoMediaView(
                file = file,
                modifier = modifier,
                size = size,
                onShowMenu = onShowMenu,
                onClick = onClick
            )
        }
        file.isImage() -> {
            ImageMediaView(
                file = file,
                modifier = modifier,
                size = size,
                onShowMenu = onShowMenu,
                onClick = onClick
            )
        }
        else -> {
            UnsupportedMediaView(
                file = file,
                modifier = modifier,
                size = size,
                onDownloadFile = onDownloadFile,
                onShowMenu = onShowMenu
            )
        }
    }
}

/**
 * 判断文件是否为视频
 */
fun File.isVideo(): Boolean {
    val name = this.name
    return name.endsWith(".mp4", true) ||
            name.endsWith(".mov", true) ||
            name.endsWith(".mkv", true) ||
            name.endsWith(".avi", true) ||
            name.endsWith(".webm", true) ||
            name.endsWith(".m4v", true) ||
            name.endsWith(".3gp", true)
}

/**
 * 判断文件是否为图片
 */
fun File.isImage(): Boolean {
    val name = this.name

    return name.endsWith(".jpg", true) ||
            name.endsWith(".jpeg", true) ||
            name.endsWith(".png", true) ||
            name.endsWith(".gif", true) ||
            name.endsWith(".webp", true) ||
            name.endsWith(".bmp", true)
}

/**
 * 视频媒体查看组件（跨平台）
 */
@Composable
private fun VideoMediaView(
    file: File,
    modifier: Modifier,
    size: Dp,
    onShowMenu: ((File) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    // 使用平台特定的视频缩略图组件
    VideoThumbnail(
        file = file,
        modifier = modifier.size(size),
        onClick = {
            if (onClick != null) {
                onClick()
            } else {
                // 默认行为：播放视频
                VideoPlayerManager.play(file)
            }
        },
        onLongClick = {
            onShowMenu?.invoke(file)
        }
    )
}

/**
 * 图片媒体查看组件
 */
@Composable
private fun ImageMediaView(
    file: File,
    modifier: Modifier,
    size: Dp,
    onShowMenu: ((File) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    CrossPlatformImage(
        file = file,
        modifier = modifier.size(size),
        onLongClick = {
            onShowMenu?.invoke(file)
        }
    )
}

/**
 * 不支持的媒体类型查看组件
 */
@Composable
private fun UnsupportedMediaView(
    file: File,
    modifier: Modifier,
    size: Dp,
    onDownloadFile: ((String) -> Unit)? = null,
    onShowMenu: ((File) -> Unit)? = null
) {
    var fileToShow by remember { mutableStateOf(file) }
    var isDownloading by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                // 如果文件数据源是HTTP链接且提供了下载函数，则尝试下载
                when (val fileData = fileToShow.data) {
                    is FileData.Path -> {
                        if (fileData.isHttpPath() && onDownloadFile != null) {
                            // 这里需要获取fileId，我们假设fileId可以从文件路径中解析出来
                            val fileId = extractFileIdFromPath(fileData.path)
                            if (fileId.isNotEmpty()) {
                                isDownloading = true
                                // 启动下载
                                onDownloadFile(fileId)
                            }
                        }
                    }
                    else -> {
                        // 其他类型的数据不做处理
                    }
                }
            }
            .then(
                // 添加长按功能
                if (onShowMenu != null) {
                    Modifier.combinedClickable(
                        onClick = { /* 已经有了click处理 */ },
                        onLongClick = {
                            onShowMenu(fileToShow)
                        }
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.Gray
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                // 显示下载进度指示器
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                when (val fileData = fileToShow.data) {
                    is FileData.Path -> {
                        if (fileData.isHttpPath()) {
                            Text(
                                text = "下载",
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "文件",
                                color = Color.White
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "文件",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 从文件路径中提取文件ID
 * 这是一个简单的实现，实际情况可能需要根据实际URL结构调整
 */
fun extractFileIdFromPath(path: String): String {
    // 处理常见的API路径格式
    val pathSegments = path.split('/')
    // 查找 "files" 或 "api" 段后的ID
    for ((index, segment) in pathSegments.withIndex()) {
        if (segment.equals("files", ignoreCase = true) && index + 1 < pathSegments.size) {
            // 检查下一个段是否可能是文件ID（不是路径的一部分）
            val nextSegment = pathSegments[index + 1]
            if (nextSegment.isNotEmpty() && !nextSegment.contains(".")) { // 排除文件扩展名
                return nextSegment
            }
        }
    }
    
    // 如果上面的逻辑没找到，尝试从URL参数中提取
    if (path.contains("?")) {
        val queryString = path.substringAfter('?')
        val params = queryString.split('&')
        for (param in params) {
            if (param.startsWith("fileId=")) {
                return param.substringAfter('=')
            }
        }
    }
    
    // 如果仍然找不到，返回最后一个段（如果它看起来像是一个ID）
    val lastSegment = pathSegments.lastOrNull { it.isNotEmpty() }
    if (lastSegment != null && lastSegment.length > 5) { // 假设有效的文件ID至少有5个字符
        return lastSegment
    }
    
    return ""
}

/**
 * 视频缩略图组件（平台特定实现）
 */
@Composable
expect fun VideoThumbnail(
    file: File,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
)
