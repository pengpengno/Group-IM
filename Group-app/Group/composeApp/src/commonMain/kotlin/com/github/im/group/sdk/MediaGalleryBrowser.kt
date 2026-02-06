package com.github.im.group.sdk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
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
 * 
 * 职责：
 * 1. 使用 HorizontalPager 实现左右滑动切换媒体资源
 * 2. 支持图片和视频类型的展示
 * 3. 提供手动切换按钮（左/右）和关闭按钮
 * 4. 显示当前进度（n/total）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGalleryBrowserContent(
    onDismiss: () -> Unit,
    onMediaClick: (() -> Unit)? = null,
    onCurrentIndexChanged: ((Int) -> Unit)? = null
) {
    val galleryManager = LocalMediaGalleryManager.current
    
    // 使用 HorizontalPager 实现平滑的左右滑动效果
    val pagerState = rememberPagerState(
        initialPage = galleryManager.currentIndex.coerceIn(0, (galleryManager.mediaList.size - 1).coerceAtLeast(0))
    ) {
        galleryManager.mediaList.size
    }
    
    // 当页面切换时同步管理器状态
    LaunchedEffect(pagerState.currentPage) {
        galleryManager.currentIndex = pagerState.currentPage
        onCurrentIndexChanged?.invoke(pagerState.currentPage)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 全屏展示
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // 核心分页组件
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp, // 页面间距
                beyondViewportPageCount = 1 // 预加载前后页面
            ) { page ->
                val media = galleryManager.mediaList[page]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        media.isImage() -> {
                            CrossPlatformImage(
                                file = media,
                                modifier = Modifier.fillMaxSize(),
                                onLongClick = null
                            )
                        }
                        media.isVideo() -> {
                            CrossPlatformVideo(
                                file = media,
                                modifier = Modifier.fillMaxSize(),
                                onClose = null
                            )
                        }
                        else -> {
                            Text("不支持的媒体格式", color = Color.White)
                        }
                    }
                }
            }
            
            // 左侧快速切换按钮
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = { 
                        // 触发 Pager 滚动
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "上一个",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            // 右侧快速切换按钮
            if (pagerState.currentPage < galleryManager.mediaList.size - 1) {
                IconButton(
                    onClick = { 
                        // 触发 Pager 滚动
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "下一个",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            // 顶部操作栏
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
            
            // 底部页码指示器
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${galleryManager.mediaList.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 使用画廊管理器的媒体文件查看组件
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
        modifier = modifier,
        onDownloadFile = onDownloadFile,
        onShowMenu = onShowMenu,
        onClick = {
            if (mediaList != null && mediaList.isNotEmpty()) {
                showGallery = true
            } else if (file.isVideo()) {
                VideoPlayerManager.play(file)
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
