package com.github.im.group.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlin.math.roundToInt

/**
 * 语音消息播放器，支持播放/暂停和拖动进度
 */
@Composable
fun VoicePlayer(
    duration: Int, // 语音消息时长（秒）
    onPlay: () -> Unit, // 播放事件
    onPause: () -> Unit, // 暂停事件
    onSeek: (position: Float) -> Unit // 拖动进度事件
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0f) } // 当前播放位置（秒）
    
    // 波形数据 - 模拟数据，实际应该从音频文件分析得到
    val waveformData = remember { 
        List(50) { (Math.random() * 20 + 5).toFloat() } 
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 波形显示区域，支持拖动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val position = (offset.x / size.width).coerceIn(0f, 1f) * duration
                            currentPosition = position
                            onSeek(position)
                        },
                        onDrag = { change, dragAmount ->
                            val position = (change.position.x / size.width).coerceIn(0f, 1f) * duration
                            currentPosition = position
                            onSeek(position)
                            change.consume()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val position = (offset.x / size.width).coerceIn(0f, 1f) * duration
                        currentPosition = position
                        onSeek(position)
                    }
                }
        ) {
            // 绘制波形
            Canvas(modifier = Modifier.fillMaxWidth()) {
                val barWidth = size.width / waveformData.size
                waveformData.forEachIndexed { index, height ->
                    val x = index * barWidth + barWidth / 2
                    val barHeight = height.coerceAtMost(30f)
                    
                    // 根据是否已播放改变颜色
                    val isPlayed = (index.toFloat() / waveformData.size) <= (currentPosition / duration)
                    val color = if (isPlayed) Color(0xFF0088CC) else Color.Gray
                    
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x - 1.dp.toPx() / 2, (size.height - barHeight) / 2),
                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), barHeight)
                    )
                }
            }
            
            // 播放进度指示器
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (currentPosition / duration).coerceIn(0f, 1f) * 200.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0088CC))
            )
        }
        
        // 控制按钮和时间显示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                    if (isPlaying) {
                        onPlay()
                    } else {
                        onPause()
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
            
            Text(
                text = "${currentPosition.roundToInt()}\" / ${duration}\"",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}