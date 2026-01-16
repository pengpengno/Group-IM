package com.github.im.group.sdk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 媒体资源画廊浏览器
 * 
 * 用于在全屏模式下浏览媒体资源列表，支持左右滑动切换
 * 
 * @param mediaList 媒体资源列表
 * @param initialIndex 初始索引，默认为0
 * @param onDismiss 请求关闭浏览器的回调
 * @param onMediaClick 点击媒体资源时的回调
 */
@Composable
fun MediaGalleryBrowser(
    mediaList: List<File>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onMediaClick: (() -> Unit)? = null,
    onCurrentIndexChanged: ((Int) -> Unit)? = null
) {
    val galleryManager = remember(mediaList, initialIndex) {
        MediaGalleryManager.create(mediaList, initialIndex)
    }
    
    CompositionLocalProvider(LocalMediaGalleryManager provides galleryManager) {
        MediaGalleryBrowserContent(
            onDismiss = onDismiss,
            onMediaClick = onMediaClick,
            onCurrentIndexChanged = onCurrentIndexChanged
        )
    }
}

/**
 * 媒体资源画廊浏览器内容
 */
@Composable
private fun MediaGalleryBrowserContent(
    onDismiss: () -> Unit,
    onMediaClick: (() -> Unit)? = null,
    onCurrentIndexChanged: ((Int) -> Unit)? = null
) {
    val galleryManager = LocalMediaGalleryManager.current
    val currentMedia = galleryManager.currentMedia
    val currentIndex = galleryManager.currentIndex
    val hasNext = galleryManager.hasNext
    val hasPrevious = galleryManager.hasPrevious
    
    LaunchedEffect(currentIndex) {
        onCurrentIndexChanged?.invoke(currentIndex)
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
            // 显示当前媒体资源
            currentMedia?.let { media ->
                if (media.isImage()) {
                    // 显示图片
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        CrossPlatformImage(
                            file = media,
                            modifier = Modifier.fillMaxSize(),
                            onLongClick = null
                        )
                    }
                } else if (media.isVideo()) {
                    // 显示视频
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        CrossPlatformVideo(
                            file = media,
                            modifier = Modifier.fillMaxSize(),
                            onClose = null
                        )
                    }
                }
            }
            
            // 左侧按钮 - 上一张/上一个视频
            if (hasPrevious) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    IconButton(
                        onClick = { galleryManager.goToPrevious() },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "上一个",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            // 右侧按钮 - 下一张/下一个视频
            if (hasNext) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    IconButton(
                        onClick = { galleryManager.goToNext() },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "下一个",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            // 右上角关闭按钮
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
            
            // 底部显示当前索引和总数
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "${galleryManager.currentIndex + 1}/${galleryManager.mediaList.size}",
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 使用画廊管理器的媒体文件查看组件
 * 
 * @param file 当前文件
 * @param mediaList 媒体资源列表（可选），如果提供则启用画廊模式
 * @param currentIndex 当前索引（可选），配合mediaList使用
 */
@Composable
fun GalleryAwareMediaFileView(
    file: File,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    mediaList: List<File>? = null,
    currentIndex: Int = 0,
    onDownloadFile: ((String) -> Unit)? = null,
    onShowMenu: ((File) -> Unit)? = null
) {
    var showGallery by remember { mutableStateOf(false) }
    
    MediaFileView(
        file = file,
        modifier = modifier.size(size),
        onDownloadFile = onDownloadFile,
        onShowMenu = onShowMenu,
        onClick = {
            if (mediaList != null && mediaList.size > 1) {
                // 如果有多个媒体资源，显示画廊浏览器
                showGallery = true
            } else {
                // 否则按照默认行为处理（例如，如果是视频则播放）
                if (file.isVideo()) {
                    VideoPlayerManager.play(file)
                }
            }
        }
    )
    
    if (showGallery && mediaList != null) {
        MediaGalleryBrowser(
            mediaList = mediaList,
            initialIndex = currentIndex,
            onDismiss = { showGallery = false }
        )
    }
}