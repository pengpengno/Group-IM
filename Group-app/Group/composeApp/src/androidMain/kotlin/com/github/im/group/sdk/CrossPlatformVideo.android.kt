
package com.github.im.group.sdk

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.github.im.group.GlobalCredentialProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

/**
 * 跨平台的视频组件
 */
@Composable
@OptIn(UnstableApi::class)
actual fun CrossPlatformVideo(
    url: String,
    modifier: Modifier,
    size: Dp
) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {

        val datasourceFactory = DefaultHttpDataSource
            .Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${GlobalCredentialProvider.currentToken}"))
        val uri = url.toUri()
        println("video uri is : ${uri}")
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(datasourceFactory))
            .build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true  // 自动播放

        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

//    AndroidView(
//        factory = {
//            PlayerView(context).apply {
//                player = exoPlayer
//                layoutParams = FrameLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//                )
//                useController = false // 禁用默认控制器，实现更简洁的界面
//                // 设置背景为黑色
//                setBackgroundColor(android.graphics.Color.BLACK)
//            }
//        },
//        modifier = modifier
//    )

    // ExoPlayer 状态
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var duration by remember { mutableStateOf(0L) }
    var position by remember { mutableStateOf(0L) }

    // 监听 ExoPlayer
    LaunchedEffect(exoPlayer) {
        snapshotFlow { exoPlayer.isPlaying }.collect { isPlaying = it }
    }

    LaunchedEffect(exoPlayer) {
        snapshotFlow { exoPlayer.duration.takeIf { it > 0 } ?: 0L }
            .distinctUntilChanged()
            .collect { duration = it }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            position = exoPlayer.currentPosition
            kotlinx.coroutines.delay(200)
        }
    }

    Box(modifier = modifier) {
        // 播放器 Surface
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // 不使用默认控制器
//                    setBackgroundColor(Color.)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 自定义进度条 & 播放按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        ) {
            // 进度条
            if (duration > 0) {
                LinearProgressIndicator(
                    progress = {
                        position / duration.toFloat() // 这里直接传 Float，不要用 {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)) // 圆角
                        .progressSemantics(),
                    color = Color.Cyan, // 进度条颜色
                    trackColor = Color.DarkGray, // 背景轨道颜色
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 播放/暂停按钮
            Button(
                onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause()
                    else exoPlayer.play()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (isPlaying) "暂停" else "播放")
            }
        }
    }
}