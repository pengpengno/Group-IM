package com.github.im.group.sdk

import android.net.Uri
import androidx.annotation.OptIn
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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.config.VideoCache

/**
 * 跨平台视频组件（带圆形播放控制按钮 + 自定义进度条）
 */
@Composable
@OptIn(UnstableApi::class)
actual fun CrossPlatformVideo(
    file: File,
    modifier: Modifier,
    size: Dp,
    onClose: (() -> Unit)?
) {
    val context = LocalContext.current

    val cache = remember { VideoCache.getInstance(context) }

    val exoPlayer = remember(file.path) {
        val exoPlayerBuilder = ExoPlayer.Builder(context)
        
        // 根据 PickedFile.data 的类型来决定如何加载视频
        val mediaSourceFactory = when (val data = file.data) {
            is FileData.Path -> {
                if (data.path.startsWith("content://")) {
                    // Content URI 使用默认数据源
                    DefaultMediaSourceFactory(context)
                } else {
                    // HTTP URL 或本地文件路径使用带认证的数据源
                    val httpFactory = DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)
                        .setDefaultRequestProperties(
                            mapOf(
                                "Authorization" to "Bearer ${GlobalCredentialProvider.currentToken}",
                                "User-Agent" to "MyAppPlayer/1.0"
                            )
                        )
                    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                }
            }
            is FileData.Uri -> {
                // Content URI 使用默认数据源
                DefaultMediaSourceFactory(context)
            }
            else -> {
                // 对于其他类型（Bytes, None），尝试使用 pickedFile.path
                if (file.path.startsWith("content://")) {
                    DefaultMediaSourceFactory(context)
                } else {
                    val httpFactory = DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)
                        .setDefaultRequestProperties(
                            mapOf(
                                "Authorization" to "Bearer ${GlobalCredentialProvider.currentToken}",
                                "User-Agent" to "MyAppPlayer/1.0"
                            )
                        )
                    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                }
            }
        }
        
        exoPlayerBuilder.setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                // 根据 PickedFile.data 类型设置 MediaItem
                val mediaItem = when (val data = file.data) {
                    is FileData.Path -> {
                        if (data.path.startsWith("content://")) {
                            MediaItem.fromUri(Uri.parse(data.path))
                        } else {
                            MediaItem.fromUri(data.path.toUri())
                        }
                    }
                    is FileData.Uri -> {
                        MediaItem.fromUri(Uri.parse(data.uri))
                    }
                    else -> {
                        // 对于其他类型，尝试使用 pickedFile.path
                        if (file.path.startsWith("content://")) {
                            MediaItem.fromUri(Uri.parse(file.path))
                        } else {
                            MediaItem.fromUri(file.path.toUri())
                        }
                    }
                }
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // 播放状态与进度
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var position by remember { mutableLongStateOf(0L) }
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
        // 中间播放按钮
        if (showControls) {

            // TODO   缓冲中的 loading 样式
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