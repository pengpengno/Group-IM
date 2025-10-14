package com.github.im.group.ui.chat

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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.model.MessageWrapper

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue

import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import com.github.im.group.sdk.CrossPlatformImage
import com.github.im.group.sdk.CrossPlatformVideo

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

    val imageUrl = "http://${ProxyConfig.host}:${ProxyConfig.port}/api/files/download/${content.imageId}"
    println(imageUrl)
    CrossPlatformImage(
        url = imageUrl,
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showPreview = true },
        size = 120.dp
    )

    // 点击后全屏预览
    if (showPreview) {
        Dialog(onDismissRequest = { showPreview = false }) {
            CrossPlatformImage(
                url = imageUrl,
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp)),
                size = 300.dp
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
//            .width(max = 250.dp)
            .clickable { /* TODO:
             文件在本地数据库中不存在   -》 下载
              文件在本地数据库中存在 且 路径正确 -》 打开
              文件在本地数据库中存在 且 路径错误 -》 提示不存在 ，重新下载
             下载 or 打开文件 */ },
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
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onclick() }
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
        Dialog(onDismissRequest = { showPreview = false }) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .clickable {
                        showPreview = false // 点击视频区域关闭对话框
                    }
            ) {
                CrossPlatformVideo(
                    url = videoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}