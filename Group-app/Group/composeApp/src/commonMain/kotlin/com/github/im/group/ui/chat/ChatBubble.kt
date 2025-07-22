package com.github.im.group.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.im.group.model.MessageWrapper

class ChatBubble {
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
//        AsyncImage(
//            model = content.thumbnailUrl,
//            contentDescription = "视频缩略图",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.matchParentSize()
//        )
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

