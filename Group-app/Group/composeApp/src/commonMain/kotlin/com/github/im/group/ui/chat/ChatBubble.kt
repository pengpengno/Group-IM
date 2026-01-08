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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.api.FileMeta
import com.github.im.group.model.MessageItem
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.CrossPlatformImage
import com.github.im.group.sdk.CrossPlatformVideo
import com.github.im.group.viewmodel.ChatMessageViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.sin

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val imageId: String) : MessageContent()

    /**
     * @param audioPath 音频文件路径
     * @param duration 音频时长 ms 展示的需要转化为秒
     */
    data class Voice(val audioPath: String, val duration: Int) : MessageContent()

    /**
     * 文件信息
     * @param fileMeta 文件元信息
     */
    data class File(val fileMeta: FileMeta) : MessageContent()
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
        val isCurrentPlaying = audioPlayer.isCurrentlyPlaying( audioPath)
        isPlaying = isCurrentPlaying
    }
    
    // 每100毫秒更新一次播放位置 - 优化更新频率
    androidx.compose.runtime.LaunchedEffect(audioPath) {
        while (true) {
            val isCurrentPlaying = audioPlayer.isCurrentlyPlaying( audioPath)
            
            if (isCurrentPlaying) {
                val currentPosition = audioPlayer.currentPosition
                val duration = audioPlayer.duration
                if (duration > 0) {
                    playbackPosition = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
            }
            kotlinx.coroutines.delay(50) // 提高更新频率到每50毫秒更新一次
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
    var selectedPlaybackPosition by remember { mutableStateOf(playbackPosition) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp) // 增加高度
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        val xPosition = change.position.x
                        val width = size.width
                        if (width > 0) {
                            val newPosition = (xPosition / width).coerceIn(0f, 1f)
                            selectedPlaybackPosition = newPosition
                            onSeek(newPosition)
                        }
                    },
                    onDragEnd = {
                        // 拖动结束后更新当前播放位置
                        currentPlaybackPosition = selectedPlaybackPosition
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = 48 // 增加波形条数以获得更好的视觉效果
        val barWidth = width / barCount
        val centerY = height / 2

        // 生成更自然的波形，增强视觉效果
        for (i in 0 until barCount) {
            // 使用更复杂的波形算法，模拟真实音频波形
            val progressRatio = i.toFloat() / barCount
            val baseWave = abs(sin(progressRatio * 5 * kotlin.math.PI.toFloat()))
            val secondWave = abs(sin(progressRatio * 9 * kotlin.math.PI.toFloat())) * 0.4f
            val thirdWave = abs(sin(progressRatio * 13 * kotlin.math.PI.toFloat())) * 0.2f
            val waveValue = (baseWave + secondWave + thirdWave) / 2f // 组合多个波形
            
            // 增强波形高度对比，使其更明显
            val baseHeight = if (isPlaying && i < barCount * currentPlaybackPosition) {
                // 播放时的波形高度，使用组合波形，增强高度对比
                height * 0.9f * (0.1f + 0.9f * waveValue)
            } else {
                // 静止时的波形高度，使用组合波形，增强高度对比
                height * 0.6f * (0.1f + 0.7f * waveValue)
            }

            val x = i * barWidth + barWidth / 2
            val barColor = if (isPlaying && i < barCount * currentPlaybackPosition) {
                Color(0xFF0088CC) // 播放部分为蓝色
            } else {
                Color(0xFF888888) // 未播放部分为深灰色，更明显
            }

            drawLine(
                color = barColor,
                start = androidx.compose.ui.geometry.Offset(x, centerY - baseHeight / 2),
                end = androidx.compose.ui.geometry.Offset(x, centerY + baseHeight / 2),
                strokeWidth = barWidth * 0.7f // 调整线宽以增强视觉效果
            )
        }

        // 绘制播放头
        val playheadX = width * currentPlaybackPosition
        drawLine(
            color = Color.Red,
            start = androidx.compose.ui.geometry.Offset(playheadX, 0f),
            end = androidx.compose.ui.geometry.Offset(playheadX, height),
            strokeWidth = 2f
        )
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
 * 文件类型气泡
 */
@Composable
fun FileMessageBubble(meta: FileMeta) {
    val messageViewModel: ChatMessageViewModel = koinViewModel()

    var showDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    // 异步加载文件大小信息
    var fileSize by remember { mutableStateOf<Long?>(null) }
    var fileName by remember { mutableStateOf<String>(meta.fileName) }


    val displaySize = when (val size = fileSize) {
        null -> "加载中..."
        else -> if (size > 1024 * 1024) {
            "${size / 1024 / 1024}MB"
        } else if (size > 1024) {
            "${size / 1024}KB"
        } else {
            "${size}B"
        }
    }

    fun downloadFile() {
        meta?.let {
            messageViewModel.downloadFileMessage(it.fileId)
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable {
                downloadFile()
            }
            .width(200.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "文件",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = displaySize,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 操作按钮
        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("查看文件")
        }
    }

    // 点击后显示文件操作对话框
    if (showDialog) {
        FileActionDialog(
            fileName = fileName,
            fileSize = displaySize,
            onDismiss = { showDialog = false },
            onView = {
                showDialog = false
                // 实际查看文件的逻辑
                // 可以打开浏览器下载或者在应用内查看
//                onClick(msg) TODO
            },
            onDownload = {
                isDownloading = true
                downloadFile()
                isDownloading = false
            }
        )
    }

    // 下载进度指示器
    if (isDownloading) {
        Dialog(onDismissRequest = { /* 不允许取消 */ }) {
            Box(
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("文件下载中...")
                }
            }
        }
    }
}

/**
 * 文件操作对话框
 */
@Composable
fun FileActionDialog(
    fileName: String,
    fileSize: String,
    onDismiss: () -> Unit,
    onView: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = fileName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = fileSize,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onView,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2),
                        contentColor = Color.White
                    )
                ) {
                    Text("在线查看")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Text("下载文件")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("取消")
                }
            }
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
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    val videoUrl = messageViewModel.getLocalFilePath(content.videoId).toString()

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
