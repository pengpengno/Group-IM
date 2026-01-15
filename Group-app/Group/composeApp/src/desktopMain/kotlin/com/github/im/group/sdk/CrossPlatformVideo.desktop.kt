package com.github.im.group.sdk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun CrossPlatformVideo(
    file: File,
    modifier: Modifier,
    size: Dp,
    onClose: (() -> Unit)?
) {
    VideoThumbnail(
        file = file,
        modifier = modifier.size(size),
        onClick = {
            VideoPlayerManager.play(file)
        }
    )
}

@Composable
actual fun VideoThumbnail(
    file: File,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 显示占位符或视频缩略图
            Text(
                text = "视频",
                color = Color.White
            )

            // 播放图标
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(0.6f), CircleShape)
            )
        }
    }
}

actual object VideoPlayerManager {
    private var currentPlayer: Any? = null

    actual fun play(file: File) {
        // 桌面端播放逻辑
    }

    actual fun release() {
        currentPlayer = null
    }
    
    @Composable
    actual fun Render() {
        // 桌面端播放器渲染
    }
}