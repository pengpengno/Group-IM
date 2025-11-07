package com.github.im.group.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.sdk.CrossPlatformImage
import com.github.im.group.sdk.CrossPlatformVideo
import io.github.aakira.napier.Napier

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val imageId: String) : MessageContent()
    data class Voice(val audioUrl: String, val duration: Int) : MessageContent()
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
fun VoiceMessage(content: MessageContent.Voice) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .clickable { /* TODO: 播放语音 */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "语音",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("${content.duration}\"", color = Color.Black)
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

@Composable
fun VoiceMessage(content: MessageContent.Voice, onclick: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { showDialog = true }
            .width((60 + content.duration * 5).dp.coerceAtMost(200.dp))
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "语音",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Text("${content.duration}\"", color = Color.Gray, fontSize = 12.sp)
    }
    
    // 点击后显示播放器
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                VoicePlayer(
                    duration = content.duration.toLong(),
                    onPlay = {
                        // 开始播放音频
                        onclick()
                    },
                    onPause = {
                        // 暂停播放音频
                    },
                    onSeek = { position ->
                        // 跳转到指定位置
                    }
                )
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