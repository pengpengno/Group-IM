package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 跨平台视频组件（内部使用，业务层建议使用MediaFileView）
 */
@Composable
expect fun CrossPlatformVideo(
    file: File,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onClose: (() -> Unit)? = null
)

/**
 * 全局视频播放器管理器，确保同时只有一个视频播放器实例
 */
expect object VideoPlayerManager {

    /**
     * 播放视频
     */
    fun play(file: File)

    /**
     * 释放播放器实例
     */
    fun release()

    /**
     * 渲染播放器
     */
    @Composable
    fun Render()
}