package com.github.im.group.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.api.FileMeta
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.CrossPlatformVideo
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.GalleryAwareMediaFileView
import com.github.im.group.sdk.MediaFileView
import com.github.im.group.viewmodel.ChatMessageViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.sin

/**
 * 从文件路径中提取文件ID
 * 这是一个简单的实现，实际情况可能需要根据实际URL结构调整
 */
fun extractFileIdFromPath(path: String): String {
    // 处理常见的API路径格式
    val pathSegments = path.split('/')
    // 查找 "files" 或 "api" 段后的ID
    for ((index, segment) in pathSegments.withIndex()) {
        if (segment.equals("files", ignoreCase = true) && index + 1 < pathSegments.size) {
            // 检查下一个段是否可能是文件ID（不是路径的一部分）
            val nextSegment = pathSegments[index + 1]
            if (nextSegment.isNotEmpty() && !nextSegment.contains(".")) { // 排除文件扩展名
                return nextSegment
            }
        }
    }
    
    // 如果上面的逻辑没找到，尝试从URL参数中提取
    if (path.contains("?")) {
        val queryString = path.substringAfter('?')
        val params = queryString.split('&')
        for (param in params) {
            if (param.startsWith("fileId=")) {
                return param.substringAfter('=')
            }
        }
    }
    
    // 如果仍然找不到，返回最后一个段（如果它看起来像是一个ID）
    val lastSegment = pathSegments.lastOrNull { it.isNotEmpty() }
    if (lastSegment != null && lastSegment.length > 5) { // 假设有效的文件ID至少有5个字符
        return lastSegment
    }
    
    return ""
}

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val file: com.github.im.group.sdk.File) : MessageContent()

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
    data class Video(val file: com.github.im.group.sdk.File) : MessageContent()
}


/**
 * 统一媒体消息气泡 - 处理图片和视频消息
 */
@Composable
fun MediaMessage(
    file: com.github.im.group.sdk.File,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 160.dp,
    mediaList: List<com.github.im.group.sdk.File>? = null,
    currentIndex: Int = 0,
    onDownloadFile: ((String) -> Unit)? = null,
    onShowMenu: ((com.github.im.group.sdk.File) -> Unit)? = null
) {
    // 获取下载状态
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    val downloadStates by messageViewModel.fileDownloadStates.collectAsState()
    
    // 提取文件ID
    val fileId = remember(file.path) {
        extractFileIdFromPath(file.path)
    }
    
    val downloadState = remember(fileId, downloadStates) {
        if (fileId.isNotEmpty()) downloadStates[fileId] else null
    }
    
    Box {
        if (mediaList != null && mediaList.size > 1) {
            // 如果有多个媒体资源，使用画廊感知的组件
            GalleryAwareMediaFileView(
                file = file,
                modifier = modifier.size(size),
                mediaList = mediaList,
                currentIndex = currentIndex,
                onDownloadFile = onDownloadFile,
                onShowMenu = onShowMenu
            )
        } else {
            // 否则使用普通的媒体文件视图
            MediaFileView(
                file = file,
                modifier = modifier.size(size),
                onDownloadFile = onDownloadFile,
                onShowMenu = onShowMenu
            )
        }
        
        // 显示下载进度
        if (downloadState?.isDownloading == true) {
            // 在右下角显示进度指示器
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                // 显示圆形进度条
                androidx.compose.material3.CircularProgressIndicator(
                    progress = downloadState.progress,
                    color = Color.Cyan,
                    strokeWidth = 3.dp
                )
                // 显示百分比文字
                Text(
                    text = "${(downloadState.progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = androidx.compose.ui.unit.TextUnit(10F, androidx.compose.ui.unit.TextUnitType.Sp),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * 图片气泡
 */
@Composable
fun ImageMessage(
    content: MessageContent.Image, 
    mediaList: List<MessageContent.Image>? = null,
    currentIndex: Int = 0,
    onDownloadFile: ((String) -> Unit)? = null, 
    onShowMenu: ((com.github.im.group.sdk.File) -> Unit)? = null
) {
    val actualMediaList = mediaList?.map { it.file } // 转换为File列表
    MediaMessage(
        file = content.file,
        mediaList = actualMediaList,
        currentIndex = currentIndex,
        onDownloadFile = onDownloadFile,
        onShowMenu = onShowMenu
    )
}

/**
 * 视频气泡
 */
@Composable
fun VideoBubble(
    content: MessageContent.Video, 
    mediaList: List<MessageContent.Video>? = null,
    currentIndex: Int = 0,
    onDownloadFile: ((String) -> Unit)? = null, 
    onShowMenu: ((com.github.im.group.sdk.File) -> Unit)? = null
) {
    val actualMediaList = mediaList?.map { it.file } // 转换为File列表
    MediaMessage(
        file = content.file,
        mediaList = actualMediaList,
        currentIndex = currentIndex,
        onDownloadFile = onDownloadFile,
        onShowMenu = onShowMenu
    )
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
fun FileMessageBubble(meta: FileMeta, onDownloadFile: ((String) -> Unit)? = null) {
    val messageViewModel: ChatMessageViewModel = koinViewModel()

    var showDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    // 创建一个虚拟的File对象，用于MediaFileView显示
    val file = File(
        name = meta.fileName,
        path = "",
        mimeType = meta.contentType,
        size = meta.size,
        data = FileData.Path(meta.getFileUrl() ?: "")
    )

    val displaySize = if (meta.size > 1024 * 1024) {
        "${meta.size / 1024 / 1024}MB"
    } else if (meta.size > 1024) {
        "${meta.size / 1024}KB"
    } else {
        "${meta.size}B"
    }

    fun downloadFile() {
        meta?.let {
            onDownloadFile?.invoke(it.fileId)
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(200.dp)
    ) {
        // 使用MediaFileView显示文件预览
        MediaFileView(
            file = file,
            modifier = Modifier.fillMaxWidth(),
            size = 120.dp,
            onDownloadFile = onDownloadFile
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 文件信息
        Column {
            Text(
                text = meta.fileName,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = displaySize,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
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
            fileName = meta.fileName,
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

/**
 * 统一的文件消息组件 - 处理所有类型的文件消息（图片、视频、普通文件）
 */
@Composable
fun UnifiedFileMessage(
    message: MessageItem,
    messageViewModel: ChatMessageViewModel
) {
    FileMessageLoader(
        msg = message,
        messageViewModel = messageViewModel,
        maxDownloadSize = 5 * 1024 * 1024,  // 5MB for all file types
        onContentReady = { pickedFile, meta ->
            val downloadFile = { fileId: String -> messageViewModel.downloadFileMessage(fileId) }
            val showMenu = { file: File ->
                // 显示文件操作菜单
                // 这里可以弹出一个BottomSheet或其他形式的菜单
                // 暂时使用现有的文件操作对话框
            }
            
            // 使用消息视图模型中的媒体消息管理器
            val mediaManager = rememberMessageMediaManager(
                messageViewModel = messageViewModel,
                currentMessage = message
            )
            
            // 使用统一的媒体消息组件
            MediaMessage(
                file = pickedFile,
                mediaList = mediaManager.mediaFiles,
                currentIndex = mediaManager.currentIndex,
                onDownloadFile = downloadFile,
                onShowMenu = showMenu
            )
        },
        onLoading = {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(4.dp),
                strokeWidth = 2.dp
            )
        },
        onError = {
            Text("${message.type.name}文件过大或加载失败")
        }
    )
}
