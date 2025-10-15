package com.github.im.group.sdk

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.config.VideoCache

/**
 * 跨平台视频组件（带圆形播放控制按钮 + 自定义进度条）
 */
@Composable
@OptIn(UnstableApi::class)
actual fun CrossPlatformVideo(
    url: String,
    modifier: Modifier,
    size: Dp ,
    onClose: (() -> Unit)?
) {
    val context = LocalContext.current

    // 全局缓存实例
    val cache = remember { VideoCache.getInstance(context) }

    val cacheDataSourceFactory = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${GlobalCredentialProvider.currentToken}"))
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build().apply {
                setMediaItem(MediaItem.fromUri(url.toUri()))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // 播放状态与进度
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(false) } // 控制栏显示状态

    // 更新时间/时长
    LaunchedEffect(exoPlayer) {
        snapshotFlow { exoPlayer.isPlaying }.collect { isPlaying = it }
    }
    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            position = exoPlayer.currentPosition
            duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            kotlinx.coroutines.delay(200)
        }
    }

    // 格式化毫秒为 mm:ss
    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    // 播放进度 + 缓冲进度
    val bufferedPercentage by rememberUpdatedState(exoPlayer.bufferedPercentage)
    val bufferValue = duration * (bufferedPercentage / 100f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // 视频层
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },

            modifier = Modifier.fillMaxSize()
        )
        // 中间播放按钮 ✅ 必须用 Box + align
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center), // 居中显示
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        isPlaying = exoPlayer.isPlaying
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(formatTime(position), color = Color.White)
                Box(modifier = Modifier.weight(1f)) {
                    // 1️⃣ 缓冲进度条（背景灰）
                    Slider(
                        value = bufferValue,
                        onValueChange = {},
                        valueRange = 0f..duration.toFloat(),
                        enabled = false, // 不可操作
                        colors = SliderDefaults.colors(
                            disabledThumbColor = Color.Transparent,
                            disabledActiveTrackColor = Color.Gray.copy(alpha = 0.5f),
                            disabledInactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    // 2️⃣ 播放进度条（前景蓝/白）
                    Slider(
                        value = position.toFloat(),
                        onValueChange = { newPos ->
                            position = newPos.toLong()
                            exoPlayer.seekTo(position)
                        },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Cyan,
                            inactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
                Text(formatTime(duration), color = Color.White)
            }
        }


//        // 底部控制栏 ✅ AnimatedVisibility 正确放置
//        AnimatedVisibility(
//            visible = showControls,
//            enter = fadeIn(),
//            exit = fadeOut(),
//            modifier = Modifier.align(Alignment.BottomCenter)
//        ) {
//
//        }
    }


    // 自动隐藏控制栏
    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
}

