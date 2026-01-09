package com.github.im.group.sdk

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.config.VideoCache
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    val videoCache = remember { VideoCache.getInstance(context) }

    Napier.d("CrossPlatformVideo: file.path = ${file.path}")
    Napier.d("CrossPlatformVideo: file.data type = ${file.data}")

    // 视频封面相关状态
    var coverBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showCover by remember { mutableStateOf(true) }
    
    // 播放状态与进度
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(false) } // 控制栏显示状态
    
    // 缓冲百分比
    var bufferedPercent by remember { mutableStateOf(0) }
    // 是否正在拖动进度条
    var isSeeking by remember { mutableStateOf(false) }

    // 创建协程作用域用于更新UI状态
    val mainScope = rememberCoroutineScope()

    // 获取视频首帧作为封面（处理所有数据类型）
    LaunchedEffect(file.path, file.data) {
        withContext(Dispatchers.IO) {
            val bitmap = when (val data = file.data) {
                is FileData.Bytes -> {
                    // 对于字节数组数据，创建临时文件再提取帧（使用稳定key）
                    val fileName = "video_${file.hashCode()}.mp4"
                    val tempFile = java.io.File(context.cacheDir, fileName)
                    try {
                        if (!tempFile.exists()) {
                            tempFile.writeBytes(data.data)
                        }
                        extractVideoFrame(context, tempFile.absolutePath)
                    } finally {
                        // 不立即删除文件，让缓存策略处理
                    }
                }
                is FileData.Path -> {
                    extractVideoFrame(context, data.path)
                }
                else -> {
                    extractVideoFrame(context, file.path)
                }
            }
            bitmap?.let { coverBitmap = it.asImageBitmap() }
        }
    }

    val exoPlayer = remember(
        file.path,
        file.data::class,
        GlobalCredentialProvider.currentToken
    ) {
        val exoPlayerBuilder = ExoPlayer.Builder(context)
        
        exoPlayerBuilder.build().apply {
            // 根据 file.data 类型来设置不同的数据源
            when (val data = file.data) {
                is FileData.Bytes -> {
                    // 处理字节数据 - 使用正确的 DataSource.Factory
                    val dataSourceFactory = DataSource.Factory { ByteArrayDataSource(data.data) }
                    val mediaSource = ProgressiveMediaSource.Factory(
                        dataSourceFactory
                    ).createMediaSource(MediaItem.Builder()
                        .setUri(Uri.EMPTY) // 使用空URI
                        .setCustomCacheKey("bytearray-${file.hashCode()}") // 使用自定义cache key
                        .setMimeType(MimeTypes.VIDEO_MP4) // 设置合适的MIME类型
                        .build())
                    setMediaSource(mediaSource)
                }
                is FileData.Path -> {
                    if (data.path.startsWith("content://")) {
                        // Content URI 使用默认数据源
                        val mediaSource = DefaultMediaSourceFactory(context)
                            .createMediaSource(MediaItem.fromUri(data.path))
                        setMediaSource(mediaSource)
                    } else {
                        // HTTP URL 或本地文件路径使用带认证的数据源
                        val httpFactory = MediaHttpFactory.create(GlobalCredentialProvider.currentToken)
                        val cacheFactory = CacheDataSource.Factory()
                            .setCache(videoCache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
                            .createMediaSource(MediaItem.fromUri(data.path))
                        setMediaSource(mediaSource)
                    }
                }
                is FileData.None -> {
                    // 如果没有数据，尝试使用 file.path
                    val mediaSource = if (file.path.startsWith("content://")) {
                        DefaultMediaSourceFactory(context)
                            .createMediaSource(MediaItem.fromUri(file.path))
                    } else {
                        val httpFactory = MediaHttpFactory.create(GlobalCredentialProvider.currentToken)
                        val cacheFactory = CacheDataSource.Factory()
                            .setCache(videoCache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        
                        ProgressiveMediaSource.Factory(cacheFactory)
                            .createMediaSource(MediaItem.fromUri(file.path))
                    }
                    setMediaSource(mediaSource)
                }
            }
            
            prepare()
            // 设置播放器属性
            playWhenReady = false // 先不自动播放
        }
    }

    // 使用 DisposableEffect 来监听播放状态变化 - 统一处理所有事件
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
                if (isPlayingValue) showCover = false
            }
            
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0)
                }
                bufferedPercent = exoPlayer.bufferedPercentage
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                position = exoPlayer.currentPosition
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // 使用更符合Media3官方范式的播放进度更新方式
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying && !isSeeking) {
                position = exoPlayer.currentPosition
                bufferedPercent = exoPlayer.bufferedPercentage
            }
            kotlinx.coroutines.delay(500)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // 格式化毫秒为 mm:ss
    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    // 播放进度 + 缓冲进度
    val bufferValue = duration * (bufferedPercent / 100f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = !showCover) {  // 只在封面隐藏时响应点击
                showControls = !showControls
            }
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

        // 视频封面层（真正的缩略图）
        if (showCover && coverBitmap != null) {
            Image(
                bitmap = coverBitmap!!,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {  // 封面层负责播放
                        exoPlayer.play()
                        showCover = false
                    }
            )

            // 中间播放按钮
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(0.6f), CircleShape)
            )
        }

        // 中间播放按钮（当视频正在播放但封面隐藏时）
        if (!showCover && showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center), // 居中显示
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        // 移除手动设置isPlaying，仅由Player.Listener控制
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
                            isSeeking = true
                            position = newPos.toLong()
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(position)
                            isSeeking = false
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
    }

    // 自动隐藏控制栏 - 优化UX，避免在拖动进度条时隐藏
    LaunchedEffect(isPlaying, showControls, isSeeking) {
        if (isPlaying && showControls && !isSeeking) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
}

// 添加单例对象用于HTTP数据源创建
object MediaHttpFactory {
    @OptIn(UnstableApi::class)
    fun create(token: String) =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setDefaultRequestProperties(
                mapOf(
                    "Authorization" to "Bearer $token",
                    "User-Agent" to "MyAppPlayer/1.0"
                )
            )
}

// 实用级工具方法：提取视频帧
fun extractVideoFrame(
    context: android.content.Context,
    path: String,
    timeMs: Long = 0
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        if (path.startsWith("content://")) {
            retriever.setDataSource(context, Uri.parse(path))
        } else {
            retriever.setDataSource(path)
        }
        retriever.getFrameAtTime(
            timeMs * 1000L, // 使用Long类型，微秒单位
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    } catch (e: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            // 忽略释放异常
        }
    }
}