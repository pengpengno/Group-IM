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


sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val imageUrl: String) : MessageContent()
    data class Voice(val audioUrl: String, val duration: Int) : MessageContent()
    data class File(val fileName: String, val fileSize: String, val fileUrl: String) : MessageContent()
}


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
            .clickable { /* TODO: 下载 or 打开文件 */ },
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
fun VoiceMessage(content: MessageContent.Voice, onclick : () -> Unit) {
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
 * 图片消息
 */
@Composable
fun ImageMessage(content: MessageContent.Image) {
    Icon(
        imageVector = Icons.Default.Image,
        contentDescription = "图片",
        modifier = Modifier
            .size(120.dp)
            .padding(8.dp),
        tint = Color.Gray
    )
//    KamelImage(
//        resource = asyncPainterResource("https://example.com/image.png"),
//        contentDescription = "网络图片",
//        modifier = Modifier.size(120.dp)
//    )
}


@Composable
fun VideoBubble(content: MessageWrapper) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                // TODO: 播放视频 content.videoUrl
            }
    ) {

        Icon(
            Icons.Default.PlayCircle,
            contentDescription = "播放",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
    }
}

