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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.abs

/**
 * 语音消息播放器，支持播放/暂停和拖动进度
 */
@Composable
fun VoicePlayer(
    duration: Long, // 语音消息时长（秒）
    audioBytes: ByteArray? = null, // 音频数据字节
    onPlay: () -> Unit, // 播放事件
    onPause: () -> Unit, // 暂停事件
    onSeek: (Float) -> Unit // 拖动进度事件
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0f) } // 当前播放位置（秒）
    
    // 波形数据 - 基于实际音频数据生成
    val waveformData = remember(audioBytes) { 
        if (audioBytes != null) {
            generateWaveformFromAudioData(audioBytes)
        } else {
            // 如果没有音频数据，使用默认的模拟数据
            generateWaveformFromAudioData(duration)
        }
    }

    // 当播放状态为true时，定期更新当前位置
    if (isPlaying) {
        androidx.compose.runtime.LaunchedEffect(isPlaying) {
            while (isPlaying && currentPosition < duration) {
                delay(1000) // 每秒更新一次位置
                if (isPlaying) {
                    currentPosition = (currentPosition + 1).coerceAtMost(duration.toFloat())
                }
            }
            // 播放完成后自动停止
            if (currentPosition >= duration) {
                isPlaying = false
            }
        }
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

/**
 * 根据音频数据生成波形数据
 */
fun generateWaveformFromAudioData(audioBytes: ByteArray): List<Float> {
    val barCount = 50
    val waveform = mutableListOf<Float>()
    
    // 将音频数据分组计算平均振幅
    val groupSize = audioBytes.size / barCount
    if (groupSize == 0) {
        // 如果音频数据太小，使用默认值
        return generateWaveformFromAudioData(1L)
    }
    
    for (i in 0 until barCount) {
        val startIndex = i * groupSize
        val endIndex = minOf((i + 1) * groupSize, audioBytes.size)
        
        if (startIndex < endIndex) {
            // 计算这一组的平均振幅
            var sum = 0L
            for (j in startIndex until endIndex) {
                sum += abs(audioBytes[j].toInt())
            }
            val average = sum.toFloat() / (endIndex - startIndex)
            // 将振幅映射到合适的高度范围 (5-30)
            val height = (average / 255f * 25f + 5f).coerceIn(5f, 30f)
            waveform.add(height)
        } else {
            waveform.add(5f) // 默认最小高度
        }
    }
    
    return waveform
}

/**
 * 根据音频时长生成模拟波形数据
 * 在实际应用中，这应该从真实的音频数据中提取
 */
fun generateWaveformFromAudioData(duration: Long): List<Float> {
    val barCount = 50
    val waveform = mutableListOf<Float>()
    
    // 生成基于正弦波的模拟波形数据
    for (i in 0 until barCount) {
        // 使用正弦函数生成波形，添加一些随机性使其更自然
        val sineValue = kotlin.math.sin(2 * kotlin.math.PI * i / barCount * 3)
        val randomFactor = (Math.random() * 0.5 + 0.5).toFloat()
        val height = (abs(sineValue) * 20 + 5) * randomFactor
        waveform.add(height.toFloat())
    }
    
    return waveform
}
