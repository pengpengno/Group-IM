package com.github.im.group.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.im.group.api.FileMeta
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.getFile
import com.github.im.group.manager.isFileExists
import com.github.im.group.manager.toFile
import com.github.im.group.model.MessageItem
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.GalleryAwareMediaFileView
import com.github.im.group.sdk.MediaFileView
import com.github.im.group.viewmodel.ChatRoomViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.sin

/**
 * 从文件路径中提取文件ID
 */
fun extractFileIdFromPath(path: String): String {
    val pathSegments = path.split('/')
    for ((index, segment) in pathSegments.withIndex()) {
        if (segment.equals("files", ignoreCase = true) && index + 1 < pathSegments.size) {
            val nextSegment = pathSegments[index + 1]
            if (nextSegment.isNotEmpty() && !nextSegment.contains(".")) {
                return nextSegment
            }
        }
    }
    
    if (path.contains("?")) {
        val queryString = path.substringAfter('?')
        val params = queryString.split('&')
        for (param in params) {
            if (param.startsWith("fileId=")) {
                return param.substringAfter('=')
            }
        }
    }
    
    val lastSegment = pathSegments.lastOrNull { it.isNotEmpty() }
    if (lastSegment != null && lastSegment.length > 5) {
        return lastSegment
    }
    
    return ""
}

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val file: com.github.im.group.sdk.File) : MessageContent()
    data class Voice(val audioPath: String, val duration: Int) : MessageContent()
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
    val messageViewModel: ChatRoomViewModel = koinViewModel()
    val downloadStates by messageViewModel.fileDownloadStates.collectAsState()
    
    val fileId = remember(file.path) { extractFileIdFromPath(file.path) }
    val downloadState = remember(fileId, downloadStates) {
        if (fileId.isNotEmpty()) downloadStates[fileId] else null
    }
    
    Box {
        if (mediaList != null && mediaList.size > 1) {
            GalleryAwareMediaFileView(
                file = file,
                modifier = modifier.size(size),
                mediaList = mediaList,
                currentIndex = currentIndex,
                onDownloadFile = onDownloadFile,
                onShowMenu = onShowMenu
            )
        } else {
            MediaFileView(
                file = file,
                modifier = modifier.size(size),
                onDownloadFile = onDownloadFile,
                onShowMenu = onShowMenu
            )
        }
        
        if (downloadState?.isDownloading == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = downloadState.progress,
                    color = Color.Cyan,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "${(downloadState.progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ImageMessage(
    content: MessageContent.Image, 
    mediaList: List<MessageContent.Image>? = null,
    currentIndex: Int = 0,
    onDownloadFile: ((String) -> Unit)? = null, 
    onShowMenu: ((com.github.im.group.sdk.File) -> Unit)? = null
) {
    val actualMediaList = mediaList?.map { it.file }
    MediaMessage(
        file = content.file,
        mediaList = actualMediaList,
        currentIndex = currentIndex,
        onDownloadFile = onDownloadFile,
        onShowMenu = onShowMenu
    )
}

@Composable
fun VideoBubble(
    content: MessageContent.Video, 
    mediaList: List<MessageContent.Video>? = null,
    currentIndex: Int = 0,
    onDownloadFile: ((String) -> Unit)? = null, 
    onShowMenu: ((com.github.im.group.sdk.File) -> Unit)? = null
) {
    val actualMediaList = mediaList?.map { it.file }
    MediaMessage(
        file = content.file,
        mediaList = actualMediaList,
        currentIndex = currentIndex,
        onDownloadFile = onDownloadFile,
        onShowMenu = onShowMenu
    )
}

/**
 * 语音消息气泡 - 优化版
 */
@Composable
fun VoiceMessage(
    content: MessageContent.Voice,
    isOwnMessage: Boolean // 建议传入此参数以统一风格
) {
    val audioPlayer: AudioPlayer = koinInject<AudioPlayer>()
    var isPlaying by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableStateOf(0f) }
    val audioPath = content.audioPath

    // 根据时长动态计算宽度，但限制范围避免过长或过短
    val bubbleWidth = (140 + (content.duration / 1000) * 5).dp.coerceIn(160.dp, 260.dp)

    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 2.dp) // 减小外边距
            .width(bubbleWidth),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (audioPlayer.isCurrentlyPlaying(audioPath)) {
                    audioPlayer.pause()
                } else {
                    // AudioPlayer 内部应处理停止上一个播放的逻辑
                    audioPlayer.play(audioPath)
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            val iconColor =
                if (isOwnMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            VoiceWaveform(
                playbackPosition = playbackPosition,
                isOwnMessage = isOwnMessage,
                onSeek = { position ->
                    // 拖动过程中仅更新 UI 进度
                    playbackPosition = position
                },
                onSeekFinished = { position ->
                    // 逻辑：松开后自动定位并播放
                    if (audioPlayer.duration > 0) {
                        val seekTarget = (position * audioPlayer.duration).toLong()
                        audioPlayer.seekTo(seekTarget)
                        // 如果当前没在播放，松开后自动开始播放
                        if (!audioPlayer.isCurrentlyPlaying(audioPath)) {
                            audioPlayer.play(audioPath)
                        }
                    }
                }
            )
        }

        Text(
            text = "${content.duration / 1000}\"",
            style = MaterialTheme.typography.labelMedium,
            color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 8.dp)
        )
    }

    // 监听播放器状态：处理互斥停止和进度更新
    LaunchedEffect(audioPath) {
        while (true) {
            val currentlyPlayingThis = audioPlayer.isCurrentlyPlaying(audioPath)
            isPlaying = currentlyPlayingThis

            if (currentlyPlayingThis) {
                val dur = audioPlayer.duration
                if (dur > 0) {
                    playbackPosition =
                        (audioPlayer.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
                }
            } else {
                // 如果不是在播放当前音频，且进度已接近完成，则重置
                if (!audioPlayer.isPlaying && playbackPosition > 0.95f) {
                    playbackPosition = 0f
                }
            }
            delay(100)
        }
    }
}

/**
 * 语音波形图组件 - 支持点击与拖动松开播放
 */
@Composable
fun VoiceWaveform(
    playbackPosition: Float,
    isOwnMessage: Boolean,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit
) {
    val barColorActive = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary
    val barColorInactive = if (isOwnMessage) Color.White.copy(alpha = 0.4f) else Color.LightGray

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pos = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeekFinished(pos)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onSeekFinished(playbackPosition) },
                    onDragCancel = { onSeekFinished(playbackPosition) }
                ) { change, _ ->
                    val pos = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(pos)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = 35
        val barWidth = 3.dp.toPx()
        val gap = (width - (barCount * barWidth)) / (barCount - 1)
        val centerY = height / 2

        for (i in 0 until barCount) {
            val progressRatio = i.toFloat() / barCount
            // 生成比较自然的伪波形
            val waveFactor = abs(sin(progressRatio * 4f) * 0.4f + sin(progressRatio * 10f) * 0.6f)
            val barHeight =
                (height * 0.3f + height * 0.7f * waveFactor).coerceIn(4.dp.toPx(), height)

            val x = i * (barWidth + gap)
            val isPlayed = progressRatio <= playbackPosition

            drawRoundRect(
                color = if (isPlayed) barColorActive else barColorInactive,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
/**
 * 文本消息气泡
 */
@Composable
fun TextMessage(content: MessageContent.Text, isOwnMessage: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Text(
            text = content.text,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true }
                    )
                },
            color = if (isOwnMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    clipboardManager.setText(AnnotatedString(content.text))
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("转发") },
                onClick = {
                    // TODO: 转发逻辑
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.Forward, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

/**
 * 文件类型气泡
 */
@Composable
fun FileMessageBubble(meta: FileMeta, onDownloadFile: ((String) -> Unit)? = null) {
    var showDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .width(200.dp)
    ) {
        MediaFileView(
            file = file,
            modifier = Modifier.fillMaxWidth(),
            size = 100.dp,
            onDownloadFile = onDownloadFile
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column {
            Text(
                text = meta.fileName,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                fontSize = 14.sp
            )
            Text(
                text = displaySize,
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth().height(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("查看文件", fontSize = 12.sp)
        }
    }

    if (showDialog) {
        FileActionDialog(
            fileName = meta.fileName,
            fileSize = displaySize,
            onDismiss = { showDialog = false },
            onView = { showDialog = false },
            onDownload = {
                isDownloading = true
                onDownloadFile?.invoke(meta.fileId)
                isDownloading = false
            }
        )
    }

    if (isDownloading) {
        Dialog(onDismissRequest = { }) {
            Box(
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            color = MaterialTheme.colorScheme.surface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onView, modifier = Modifier.fillMaxWidth()) { Text("在线查看") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("下载文件") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("取消") }
            }
        }
    }
}

/**
 * 发送中的圆形动画
 */
@Composable
fun SendingSpinner(modifier: Modifier = Modifier, color: Color = Color.Gray) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing))
    )

    Canvas(modifier.size(12.dp)) {
        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}


@Composable
fun UnifiedFileMessage(
    message: MessageItem,
    messageViewModel: ChatRoomViewModel
) {
    FileMessageLoader(
        msg = message,
        messageViewModel = messageViewModel,
        maxDownloadSize = 5 * 1024 * 1024,
        onContentReady = { pickedFile, meta ->
            val downloadFile = { fileId: String -> messageViewModel.downloadFileMessage(fileId) }
            
            val mediaManager = rememberMessageMediaManager(
                messageViewModel = messageViewModel,
                currentMessage = message
            )
            
            Box {
                var showMenu by remember { mutableStateOf(false) }

                MediaMessage(
                    file = pickedFile,
                    mediaList = mediaManager.mediaFiles,
                    currentIndex = mediaManager.currentIndex,
                    onDownloadFile = downloadFile,
                    onShowMenu = { showMenu = true }
                )

                // 媒体文件的操作菜单
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("保存到本地") },
                        onClick = {
                            downloadFile(meta.fileId)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("转发") },
                        onClick = {
                            // TODO: 转发逻辑
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Forward, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        onLoading = {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp).padding(4.dp),
                strokeWidth = 2.dp
            )
        },
        onError = { Text("${message.type.name}加载失败", fontSize = 12.sp) }
    )
}


@Composable
fun FileMessageLoader(
    msg: MessageItem,
    messageViewModel: ChatRoomViewModel,
    maxDownloadSize: Long = 50 * 1024 * 1024,
    allMessages: List<MessageItem>? = null,
    onContentReady: @Composable (File, FileMeta) -> Unit,
    onLoading: @Composable () -> Unit,
    onError: @Composable (() -> Unit)? = null
) {
    val fileStorageManager = koinInject<FileStorageManager>()
    var fileMeta by remember { mutableStateOf<FileMeta?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var shouldDownload by remember { mutableStateOf(false) }
    var showDownloadButton by remember { mutableStateOf(false) }
    val fileId = msg.content

    LaunchedEffect(msg) {
        try {
            val meta = messageViewModel.getFileMessageMetaAsync(msg)
            file = fileStorageManager.getFile(fileId)
            fileMeta = meta

            meta?.let { fileMeta ->
                if (fileMeta.size > maxDownloadSize) {
                    showDownloadButton = true
                    file = fileMeta.toFile()
                    isLoading = false
                    return@LaunchedEffect
                }

                val fileExists = fileStorageManager.isFileExists(fileId)
                if (!fileExists) {
                    shouldDownload = true
                    file = fileMeta.toFile()
                } else {
                    val path = messageViewModel.getLocalFilePath(fileId)
                    path?.let { file = fileMeta.toFile(it) }
                }
            }
            isLoading = false
        } catch (e: Exception) {
            hasError = true
            isLoading = false
            Napier.e("加载文件消息失败", e)
        }
    }

    LaunchedEffect(shouldDownload) {
        if (shouldDownload) {
            try {
                messageViewModel.downloadFileMessage(fileId)
            } catch (e: Exception) {
                hasError = true
            }
        }
    }

    val downloadStates by  messageViewModel.fileDownloadStates.collectAsState()
    LaunchedEffect(downloadStates) {
        downloadStates[fileId]?.let { chatUiState ->
            if (chatUiState.isSuccess && !chatUiState.isDownloading) {
                val path = messageViewModel.getLocalFilePath(fileId)
                path?.let { file = fileMeta?.toFile(it) }
            }
        }
    }

    when {
        hasError -> onError?.invoke() ?: Text("加载失败", fontSize = 12.sp)
        isLoading -> onLoading()
        showDownloadButton -> {
            file?.let { fileObj ->
                fileMeta?.let { meta ->
                    Box {
                        MediaFileView(
                            file = fileObj,
                            modifier = Modifier.size(100.dp),
                            onDownloadFile = { shouldDownload = true }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        file != null  && fileMeta !=null-> onContentReady(file!!, fileMeta!!)
        else -> onLoading()
    }
}
