package com.github.im.group.sdk

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.min
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * 跨平台视频组件（内部使用，业务层建议使用MediaFileView）
 */
@Composable
@OptIn(UnstableApi::class)
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

/**
 * 视频缩略图组件
 */
@Composable
@OptIn(UnstableApi::class)
actual fun VideoThumbnail(
    file: File,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var thumbnailLoadError by remember { mutableStateOf(false) }

    // 异步加载缩略图（带缓存）
    LaunchedEffect(file.path) {
        withContext(Dispatchers.IO) {
            val thumbnailPath = when (val data = file.data) {
                is FileData.Path -> data.path
                is FileData.Bytes -> file.path  // 对于字节数组，使用文件路径作为标识
                is FileData.None -> file.path
            }
            
            // 如果是网络资源，先尝试从缓存获取，否则生成缩略图
            val cachedBitmap = try {
                if (thumbnailPath.startsWith("http://") || thumbnailPath.startsWith("https://")) {
                    // 对于网络资源，尝试从缓存获取
                    var cachedBitmap = VideoCache.getThumbnail(context, thumbnailPath)
                    
                    // 如果缓存中没有，尝试提取网络视频的缩略图
                    if (cachedBitmap == null) {
                        val tempBitmap = extractVideoFrame(context, thumbnailPath, 0)
                        if (tempBitmap != null) {
                            cachedBitmap = tempBitmap.asImageBitmap()
                            // 将提取的缩略图缓存起来
                            VideoCache.saveThumbnail(context, thumbnailPath, cachedBitmap)
                        }
                    }
                    cachedBitmap
                } else {
                    // 对于本地资源，直接获取缩略图
                    VideoCache.getThumbnail(context, thumbnailPath)
                }
            } catch (e: OutOfMemoryError) {
                // 如果内存不足，返回null并记录错误
                io.github.aakira.napier.Napier.e("OOM while extracting thumbnail for: $thumbnailPath", e)
                thumbnailLoadError = true
                null
            } catch (e: Exception) {
                // 捕获其他异常
                io.github.aakira.napier.Napier.e("Error while extracting thumbnail for: $thumbnailPath", e)
                thumbnailLoadError = true
                null
            }
            
            thumbnailBitmap = cachedBitmap
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                // 根据是否有长按回调来决定使用哪种点击修饰符
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 显示缩略图
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
            } else if (thumbnailLoadError) {
                // 显示错误图标
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "视频缩略图加载失败",
                        tint = Color.Red,
                        modifier = Modifier
                            .size(48.dp)
                    )
                }
            } else {
                // 显示加载动画
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * 全局视频播放器管理器，确保同时只有一个视频播放器实例
 */
@OptIn(UnstableApi::class)
actual object VideoPlayerManager {
    private var currentPlayer: ExoPlayer? = null
    private var currentFile by mutableStateOf<File?>(null)
    private var dialogVisible by mutableStateOf(false)

    actual fun play(file: File) {
        // 释放之前的播放器
        release()
        
        // 设置当前要播放的文件
        currentFile = file
        dialogVisible = true
    }

    actual fun release() {
        currentPlayer?.release()
        currentPlayer = null
    }
    
    @Composable
    actual fun Render() {
        val file = currentFile
        if (dialogVisible && file != null) {
            val context = LocalContext.current
            val videoCache = remember { VideoCache.getInstance(context) }

            Dialog(
                onDismissRequest = { 
                    dialogVisible = false
                    release()
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                FullScreenVideoPlayer(
                    file = file,
                    onClose = { 
                        dialogVisible = false
                        release()
                    },
                    videoCache = videoCache
                )
            }
        }
    }
    
    @Composable
    private fun FullScreenVideoPlayer(
        file: File, 
        onClose: () -> Unit,
        videoCache: androidx.media3.datasource.cache.SimpleCache
    ) {
        val context = LocalContext.current

        var isPlaying by remember { mutableStateOf(false) }
        var position by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableStateOf(0L) }
        var showControls by remember { mutableStateOf(true) }
        var bufferedPercent by remember { mutableStateOf(0) }
        var isSeeking by remember { mutableStateOf(false) }

        val exoPlayer = remember(
            file.path,
            file.data::class,
            GlobalCredentialProvider.currentToken
        ) {
            val exoPlayerBuilder = ExoPlayer.Builder(context)
            
            // 配置播放器以减少内存使用
            exoPlayerBuilder
                .setSeekBackIncrementMs(5000)  // 5秒快退
                .setSeekForwardIncrementMs(5000)  // 5秒快进
            val player = exoPlayerBuilder.build()
            
            // 根据 file.data 类型来设置不同的数据源
            when (val data = file.data) {
                is FileData.Bytes -> {
                    val dataSourceFactory = DataSource.Factory { ByteArrayDataSource(data.data) }
                    val mediaSource = ProgressiveMediaSource.Factory(
                        dataSourceFactory
                    ).createMediaSource(MediaItem.Builder()
                        .setUri(Uri.EMPTY)
                        .setCustomCacheKey("bytearray-${file.hashCode()}")
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build())
                    player.setMediaSource(mediaSource)
                }
                is FileData.Path -> {
                    if (data.path.startsWith("content://")) {
                        // 处理 Android Content URI
                        val mediaSource = DefaultMediaSourceFactory(context)
                            .createMediaSource(MediaItem.fromUri(data.path))
                        player.setMediaSource(mediaSource)
                    } else if (data.path.startsWith("http://") || data.path.startsWith("https://")) {
                        // 处理网络视频资源 (HTTP/HTTPS)
                        val httpFactory = MediaHttpFactory.create(GlobalCredentialProvider.currentToken)
                        val cacheFactory = CacheDataSource.Factory()
                            .setCache(videoCache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
                            .createMediaSource(MediaItem.fromUri(data.path))
                        player.setMediaSource(mediaSource)
                    } else {
                        // 处理本地文件
                        val httpFactory = MediaHttpFactory.create(GlobalCredentialProvider.currentToken)
                        val cacheFactory = CacheDataSource.Factory()
                            .setCache(videoCache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
                            .createMediaSource(MediaItem.fromUri(data.path))
                        player.setMediaSource(mediaSource)
                    }
                }
                is FileData.None -> {
                    if (file.path.startsWith("content://")) {
                        // 处理 Android Content URI
                        val mediaSource = DefaultMediaSourceFactory(context)
                            .createMediaSource(MediaItem.fromUri(file.path))
                        player.setMediaSource(mediaSource)
                    } else if (file.path.startsWith("http://") || file.path.startsWith("https://")) {
                        // 处理网络视频资源 (HTTP/HTTPS)
                        val httpFactory = MediaHttpFactory.create(GlobalCredentialProvider.currentToken)
                        val cacheFactory = CacheDataSource.Factory()
                            .setCache(videoCache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
                            .createMediaSource(MediaItem.fromUri(file.path))
                        player.setMediaSource(mediaSource)

                    } else {
                        // 处理本地文件
                        val httpFactory = MediaHttpFactory.create(GlobalCredentialProvider.currentToken)
                        val cacheFactory = CacheDataSource.Factory()
                            .setCache(videoCache)
                            .setUpstreamDataSourceFactory(httpFactory)
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
                            .createMediaSource(MediaItem.fromUri(file.path))
                        player.setMediaSource(mediaSource)

                    }
                }
            }
            
            player.prepare()
            player.playWhenReady = true
            currentPlayer = player
            player
        }

        // 监听播放状态
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                    isPlaying = isPlayingValue
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
            
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()  // 确保在销毁时释放播放器
                if (currentPlayer === exoPlayer) {
                    currentPlayer = null
                }
            }
        }

        // 使用snapshotFlow监听播放状态变化，只在播放时更新位置
        LaunchedEffect(exoPlayer) {
            snapshotFlow { exoPlayer.isPlaying }
                .distinctUntilChanged()
                .collect { isPlaying ->
                    if (isPlaying && !isSeeking) {
                        // 当开始播放时，启动位置更新
                        while (currentCoroutineContext().isActive && exoPlayer.isPlaying && !isSeeking) {
                            position = exoPlayer.currentPosition
                            bufferedPercent = exoPlayer.bufferedPercentage
                            delay(500)
                        }
                    }
                }
        }

        // 自动隐藏控制栏
        LaunchedEffect(isPlaying, showControls, isSeeking) {
            if (isPlaying && showControls && !isSeeking) {
                delay(3000)
                showControls = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showControls = !showControls }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 控制栏
            if (showControls) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 顶部关闭按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(0.dp))

                    // 底部控制栏
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(formatTime(position), color = Color.White)
                        Box(modifier = Modifier.weight(1f)) {
                            Slider(
                                value = (duration * (bufferedPercent / 100f)).toFloat(),
                                onValueChange = {},
                                valueRange = 0f..duration.toFloat(),
                                enabled = false,
                                colors = SliderDefaults.colors(
                                    disabledThumbColor = Color.Transparent,
                                    disabledActiveTrackColor = Color.Gray.copy(alpha = 0.5f),
                                    disabledInactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )

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

                // 中央播放/暂停按钮
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
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
            }
        }
    }

    // 格式化时间
    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

// 添加单例对象用于HTTP数据源创建
object MediaHttpFactory {
    @OptIn(UnstableApi::class)
    fun create(token: String) =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)  // 增加连接超时时间
            .setReadTimeoutMs(60_000)     // 增加读取超时时间
            .setDefaultRequestProperties(
                mapOf(
                    "Authorization" to "Bearer $token",
                    "User-Agent" to "MyAppPlayer/1.0",
                    "Accept" to "video/*",  // 接受视频格式
                    "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8"  // 设置语言偏好
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
        when {
            path.startsWith("content://") -> {
                retriever.setDataSource(context, Uri.parse(path))
            }
            path.startsWith("http://") || path.startsWith("https://") -> {
                // 对于网络视频，设置自定义数据源以支持认证
                val token = GlobalCredentialProvider.currentToken
                val headers = mapOf(
                    "Authorization" to "Bearer $token",
                    "User-Agent" to "MyAppPlayer/1.0"
                )
                retriever.setDataSource(path, headers)
            }
            else -> {
                retriever.setDataSource(path)
            }
        }
        val bitmap = retriever.getFrameAtTime(
            timeMs * 1000L, // 使用Long类型，微秒单位
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        
        // 缩放位图为较小尺寸以节省内存
        bitmap?.let { originalBitmap ->
            val width = originalBitmap.width.coerceAtMost(320) // 限制最大宽度
            val height = originalBitmap.height.coerceAtMost(240) // 限制最大高度
            
            // 计算缩放比例
            val scaleWidth = width.toFloat() / originalBitmap.width
            val scaleHeight = height.toFloat() / originalBitmap.height
            val scale = minOf(scaleWidth, scaleHeight) // 保持宽高比
            
            if (scale < 1.0f) { // 只有在需要缩小时才进行缩放
                val scaledWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
                val scaledHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)
                
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                originalBitmap.recycle() // 释放原始位图
                scaledBitmap
            } else {
                originalBitmap // 如果不需要缩小，则返回原图
            }
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null // 内存不足时返回null
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            // 忽略释放异常
        }
    }
}