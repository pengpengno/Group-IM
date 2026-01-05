package com.github.im.group.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.CrossPlatformImage
import com.github.im.group.sdk.CrossPlatformVideo
import com.github.im.group.viewmodel.VoiceViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val imageId: String) : MessageContent()

    /**
     * @param audioPath 音频文件路径
     * @param duration 音频时长 ms 展示的需要转化为秒
     */
    data class Voice(val audioPath: String, val duration: Int) : MessageContent()
    data class File(val fileName: String, val fileSize: String, val fileUrl: String) : MessageContent()
    data class Video(val videoId: String) : MessageContent()
}


/**
 * 图片气泡
 */
@Composable
fun ImageMessage(content: MessageContent.Image) {
    var showPreview by remember { mutableStateOf(false) }

    CrossPlatformImage(
        url = content.imageId,
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showPreview = true },
        size = 200.dp
    )

    if (showPreview) {
        Dialog(onDismissRequest = { showPreview = false }) {
            CrossPlatformImage(
                url = content.imageId,
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp)),
                size = 300.dp
            )
        }
    }
}

/**
 * 语音消息
 */
@Composable
fun VoiceMessage(
    content: MessageContent.Voice, 
) {

    val audioPlayer: AudioPlayer = koinInject<AudioPlayer>()

    var isPlaying by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableStateOf(0f) }
    val audioPath = content.audioPath
    val bubbleWidth = (60 + content.duration / 1000 * 3).dp.coerceAtMost(200.dp) // 修正宽度计算，使用秒为单位

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(bubbleWidth)
            ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                if (!isPlaying) {
                    // 开始播放
                    audioPlayer.play(audioPath)
                } else {
                    // 暂停播放
                    audioPlayer.pause()
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color(0xFF0088CC),
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
        ) {
            VoiceWaveform(
                duration = content.duration.toLong(),
                isPlaying = isPlaying,
                playbackPosition = playbackPosition,
                onSeek = { position ->
                    playbackPosition = position
                    // 根据波形位置计算实际播放位置
                    if (audioPlayer.duration > 0) {
                        val seekPosition = (position * audioPlayer.duration).toLong()
                        audioPlayer.seekTo(seekPosition)
                    }
                }
            )
        }

        Text(
            text = "${content.duration / 1000}s",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
    
    // 监听音频播放状态变化
    androidx.compose.runtime.LaunchedEffect(audioPath, audioPlayer.isPlaying) {
        // 检查当前播放器是否正在播放当前音频
        val isCurrentPlaying = isCurrentlyPlaying(audioPlayer, audioPath)
        isPlaying = isCurrentPlaying
    }
    
    // 每100毫秒更新一次播放位置
    androidx.compose.runtime.LaunchedEffect(audioPath) {
        while (true) {
            val isCurrentPlaying = isCurrentlyPlaying(audioPlayer, audioPath)
            
            if (isCurrentPlaying) {
                val currentPosition = audioPlayer.currentPosition
                val duration = audioPlayer.duration
                if (duration > 0) {
                    playbackPosition = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
            }
            kotlinx.coroutines.delay(100) // 每100毫秒更新一次
        }
    }
}

/**
 * 语音波形图
 */
@Composable
fun VoiceWaveform(
    duration: Long,
    isPlaying: Boolean,
    playbackPosition: Float,
    onSeek: (Float) -> Unit
) {
    var currentPlaybackPosition by remember { mutableStateOf(playbackPosition) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val xPosition = change.position.x
                    val width = size.width
                    if (width > 0) {
                        val newPosition = (xPosition / width).coerceIn(0f, 1f)
                        currentPlaybackPosition = newPosition
                        onSeek(newPosition)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = 20
        val barWidth = width / barCount
        val centerY = height / 2

        // 根据时长生成更自然的波形模式
        for (i in 0 until barCount) {
            // 使用sin函数生成更自然的波形，根据索引和时长计算波形高度
            val progressRatio = i.toFloat() / barCount
            val sinValue = abs(sin(progressRatio * 2 * kotlin.math.PI.toFloat()))
            
            val barHeight = if (isPlaying && i < barCount * currentPlaybackPosition) {
                // 播放时的波形高度，使用更自然的波形模式
                (height * 0.7f) * (0.3f + 0.7f * sinValue)
            } else {
                // 静止时的波形高度，使用更自然的波形模式
                (height * 0.4f) * (0.3f + 0.7f * sinValue)
            }

            val x = i * barWidth + barWidth / 2
            val barColor = if (isPlaying && i < barCount * currentPlaybackPosition) {
                Color(0xFF0088CC) // 播放部分为蓝色
            } else {
                Color.Gray // 未播放部分为灰色
            }

            drawLine(
                color = barColor,
                start = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2),
                end = androidx.compose.ui.geometry.Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.6f
            )
        }

        // 绘制播放头
        if (isPlaying) {
            val playheadX = width * currentPlaybackPosition
            drawLine(
                color = Color.Red,
                start = androidx.compose.ui.geometry.Offset(playheadX, 0f),
                end = androidx.compose.ui.geometry.Offset(playheadX, height),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * 视频消息
 */
@Composable
fun VideoMessage(content: MessageContent.Video) {
    var showPreview by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showPreview = true }
            .background(Color.Gray)
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = "播放视频",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }

    if (showPreview) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CrossPlatformVideo(
                url = content.videoId,
                modifier = Modifier.fillMaxSize(),
                size = 300.dp,
                onClose = { showPreview = false }
            )
        }
    }
}

/**
 * 文本消息气泡
 */
@Composable
fun TextMessage(content: MessageContent.Text) {
    Text(
        text = content.text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        color = Color.Black
    )
}

/**
 * 文件消息
 */
@Composable
fun FileMessage(content: MessageContent.File) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .clickable { 
                // TODO:
                // 文件在本地数据库中不存在   -》 下载
                // 文件在本地数据库中存在 且 路径正确 -》 打开
                // 文件在本地数据库中存在 且 路径错误 -》 提示不存在 ，重新下载
                Napier.d("点击文件消息: ${content.fileName}")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = "文件",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(content.fileName, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(content.fileSize, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

/**
 * 发送中的圆形动画
 */
@Composable
fun SendingSpinner(modifier: Modifier = Modifier, color: Color = Color.Gray) {
    // 无限旋转动画
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing)
        )
    )

    Canvas(modifier.size(12.dp)) {
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
    }
}

@Composable
fun VideoBubble(content: MessageContent.Video) {
    var showPreview by remember { mutableStateOf(false) }
    
    val videoUrl = "http://${ProxyConfig.host}:${ProxyConfig.port}/api/files/download/${content.videoId}"

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                showPreview = true
            }
    ) {
        // 显示视频缩略图，这里暂时用一个占位图标
        Icon(
            // TODO  视频第一帧 缩略图
            Icons.Default.PlayCircle,
            contentDescription = "播放",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }

    // 点击后全屏预览
    if (showPreview) {
        FullScreenVideoPlayer(
            videoUrl = videoUrl,
            onClose = { showPreview = false }
        )
    }
}

/**
 * 全屏放大 视频播放
 */
@Composable
fun FullScreenVideoPlayer(videoUrl: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // 禁用默认宽度限制
        )
    ) {
        CrossPlatformVideo(
            url = videoUrl,
            modifier = Modifier.fillMaxSize(),
            size = 200.dp,
            onClose = onClose
        )
        
        // 关闭按钮
//        IconButton(
//            onClick = onClose,
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
//        ) {
//            Icon(
//                imageVector = Icons.Default.Close,
//                contentDescription = "关闭",
//                tint = Color.White
//            )
//        }
    }
}

/**
 * 检查音频播放器是否正在播放指定的音频文件
 */
expect fun isCurrentlyPlaying(audioPlayer: AudioPlayer, audioPath: String): Boolean
