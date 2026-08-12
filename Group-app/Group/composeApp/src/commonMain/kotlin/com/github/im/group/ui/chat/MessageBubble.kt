package com.github.im.group.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.api.FileMeta
import com.github.im.group.api.MeetingMessagePayLoad
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.getPreviewUrl
import com.github.im.group.manager.toFile
import com.github.im.group.manager.toPreviewFile
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.ChatRoomViewModel
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

val meetingPayloadJson = Json { ignoreUnknownKeys = true }

fun extractMeetingPayload(message: MessageItem): MeetingMessagePayLoad? {
    val wrapper = message as? MessageWrapper
    val payload = wrapper?.messageDto?.payload
    if (payload is MeetingMessagePayLoad) return payload

    val raw = message.content
    if (raw.isBlank() || !raw.trim().startsWith("{")) return null
    return runCatching { meetingPayloadJson.decodeFromString<MeetingMessagePayLoad>(raw) }.getOrNull()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    isOwnMessage: Boolean,
    msg: MessageItem,
    showAvatar: Boolean = true,
    onJoinMeeting: (MeetingMessagePayLoad) -> Unit = {}
) {
    val messageViewModel: ChatRoomViewModel = koinViewModel()

    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val ownBubbleColor = Color(0xFF3B82F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            if (showAvatar) {
                UserAvatar(username = msg.userInfo.username, size = 36)
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Spacer(modifier = Modifier.width(46.dp))
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 270.dp),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            if (showAvatar && !isOwnMessage && msg.userInfo.username.isNotEmpty()) {
                Text(
                    text = msg.userInfo.username,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                if (isOwnMessage && msg.status == MessageStatus.SENDING) {
                    SendingSpinner(modifier = Modifier.padding(end = 4.dp).size(12.dp))
                }

                Box {
                    MaterialTheme(
                        colorScheme = if (isOwnMessage) {
                            MaterialTheme.colorScheme.copy(
                                primaryContainer = ownBubbleColor,
                                onPrimaryContainer = Color.White
                            )
                        } else {
                            MaterialTheme.colorScheme
                        }
                    ) {
                    Surface(
                        color = if (isOwnMessage) {
                            ownBubbleColor
                        } else {
                            Color.White
                        },
                        contentColor = if (isOwnMessage) Color.White else Color(0xFF0F172A),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isOwnMessage) 18.dp else 5.dp,
                            bottomEnd = if (isOwnMessage) 5.dp else 18.dp
                        ),
                        border = if (isOwnMessage) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        tonalElevation = 0.dp,
                        shadowElevation = if (isOwnMessage) 1.dp else 0.dp,
                        modifier = Modifier.combinedClickable(
                            onLongClick = { showMenu = true },
                            onClick = {}
                        )
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            when (msg.type) {
                                MessageType.TEXT -> BotCardMessage(
                                    rawText = msg.content,
                                    isOwnMessage = isOwnMessage,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                MessageType.BOT_CARD -> BotCardMessage(
                                    rawText = msg.content,
                                    isOwnMessage = isOwnMessage,
                                    isStructuredCard = true,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                MessageType.VOICE -> VoiceMessageContent(msg, messageViewModel, isOwnMessage)
                                MessageType.MEETING -> {
                                    val payload = extractMeetingPayload(msg)
                                    MeetingMessageBubble(
                                        payload = payload,
                                        isOwnMessage = isOwnMessage,
                                        onJoin = { if (payload != null) onJoinMeeting(payload) }
                                    )
                                }
                                MessageType.IMAGE, MessageType.VIDEO, MessageType.FILE ->
                                    FileMessageContent(message = msg, messageViewModel = messageViewModel, isOwnMessage = isOwnMessage)
                                else -> BotCardMessage(
                                    rawText = msg.content,
                                    isOwnMessage = isOwnMessage,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(if (isOwnMessage) (-16).dp else 16.dp, 0.dp),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("复制", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                if (msg.type == MessageType.TEXT) {
                                    clipboardManager.setText(AnnotatedString(msg.content))
                                }
                                showMenu = false
                            }
                        )
                        if (isOwnMessage) {
                            DropdownMenuItem(
                                text = { Text("撤回", fontSize = 14.sp, color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red) },
                                onClick = {
                                    messageViewModel.withdrawMessage(msg)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(msg.clientTime ?: msg.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    if (isOwnMessage) {
                    Spacer(modifier = Modifier.width(5.dp))
                    val statusIcon = when (msg.status) {
                        MessageStatus.SENT -> Icons.Default.Check
                        MessageStatus.READ -> Icons.Default.CheckCircle
                        MessageStatus.FAILED -> Icons.Default.Error
                        else -> Icons.Default.Check
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable(enabled = msg.status == MessageStatus.FAILED) {
                                messageViewModel.retryMessage(msg)
                            },
                        tint = when (msg.status) {
                            MessageStatus.FAILED -> MaterialTheme.colorScheme.error
                            MessageStatus.READ -> Color(0xFF3B82F6)
                            else -> Color(0xFF94A3B8)
                        }
                    )
                    }
                }
        }
    }
}

private fun formatMessageTime(time: LocalDateTime): String {
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

@Composable
private fun SendingSpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier, strokeWidth = 1.5.dp)
}

@Composable
private fun VoiceMessageContent(
    msg: MessageItem,
    messageViewModel: ChatRoomViewModel,
    isOwnMessage: Boolean
) {
    val cachedMeta = msg.fileMeta ?: messageViewModel.getCachedFileMeta(msg.content)
    val meta by produceState<FileMeta?>(initialValue = cachedMeta, key1 = msg.content) {
        value = cachedMeta ?: messageViewModel.getFileMessageMetaAsync(msg)
    }
    val resolvedFile = remember(meta) {
        meta?.let { fileMeta ->
            messageViewModel.getFile(fileMeta.fileId)
                ?: messageViewModel.getLocalFilePath(fileMeta.fileId)?.let(fileMeta::toFile)
                ?: fileMeta.takeIf { it.fileStatus == FileStatus.NORMAL }?.toFile()
        }
    }

    if (meta == null) {
        AttachmentStatusPlaceholder(text = "语音消息加载中...")
        return
    }
    val resolvedMeta = meta ?: return
    if (resolvedMeta.fileStatus != FileStatus.NORMAL || resolvedFile == null) {
        AttachmentStatusPlaceholder(
            text = when (resolvedMeta.fileStatus) {
                FileStatus.FAILED -> if (isOwnMessage) "语音上传失败" else "语音暂不可播放"
                else -> if (isOwnMessage) "语音上传中..." else "语音准备中..."
            }
        )
        return
    }

    VoiceMessage(
        content = MessageContent.Voice(
            audioPath = resolvedFile.path.ifBlank { resolvedMeta.getFileUrl().orEmpty() },
            duration = resolvedMeta.duration
        ),
        senderName = msg.userInfo.username,
        isOwnMessage = isOwnMessage,
        messageId = if (msg.seqId != 0L) "seq_${msg.seqId}" else "client_${msg.clientMsgId}"
    )
}

@Composable
private fun FileMessageContent(
    message: MessageItem,
    messageViewModel: ChatRoomViewModel,
    isOwnMessage: Boolean
) {
    val cachedMeta = message.fileMeta ?: messageViewModel.getCachedFileMeta(message.content)
    val meta by produceState<FileMeta?>(initialValue = cachedMeta, key1 = message.content) {
        value = cachedMeta ?: messageViewModel.getFileMessageMetaAsync(message)
    }

    if (meta == null) {
        AttachmentStatusPlaceholder(text = if (isOwnMessage) "文件准备中..." else "文件信息加载中...")
        return
    }
    val resolvedMeta = meta ?: return

    when (message.type) {
        MessageType.IMAGE, MessageType.VIDEO -> {
            val previewImagePath = remember(resolvedMeta, message.type) {
                resolvedMeta.takeIf { it.fileStatus == FileStatus.NORMAL }?.getPreviewUrl()
            }
            val resolvedFile = remember(resolvedMeta) {
                resolvedMeta.let { fileMeta ->
                    messageViewModel.getFile(fileMeta.fileId)
                        ?: messageViewModel.getLocalFilePath(fileMeta.fileId)?.let(fileMeta::toFile)
                        ?: fileMeta.takeIf {
                            it.fileStatus == FileStatus.NORMAL && message.type == MessageType.IMAGE
                        }?.toPreviewFile()
                        ?: fileMeta.takeIf { it.fileStatus == FileStatus.NORMAL }?.toFile()
                }
            }

            if (resolvedFile == null) {
                PendingMediaPlaceholder(
                    text = when (resolvedMeta.fileStatus) {
                        FileStatus.FAILED -> if (isOwnMessage) "媒体上传失败" else "媒体暂不可用"
                        else -> if (isOwnMessage) "媒体上传中..." else "媒体准备中..."
                    }
                )
                return
            }

            if (resolvedMeta.fileStatus == FileStatus.NORMAL) {
                MediaMessage(
                    file = resolvedFile,
                    previewImagePath = if (message.type == MessageType.VIDEO) previewImagePath else null,
                    onDownloadFile = messageViewModel::downloadFileMessage
                )
            } else {
                UploadingMediaBubble(
                    file = resolvedFile,
                    text = when (resolvedMeta.fileStatus) {
                        FileStatus.FAILED -> if (isOwnMessage) "上传失败" else "暂不可查看"
                        else -> if (isOwnMessage) "上传中..." else "对方正在上传"
                    },
                    failed = resolvedMeta.fileStatus == FileStatus.FAILED,
                    onDownloadFile = messageViewModel::downloadFileMessage
                )
            }
        }

        MessageType.FILE -> FileMessageBubble(
            meta = resolvedMeta,
            isOwnMessage = isOwnMessage,
            onDownloadFile = messageViewModel::downloadFileMessage
        )

        else -> BotCardMessage(
            rawText = message.content,
            isOwnMessage = isOwnMessage,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AttachmentStatusPlaceholder(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PendingMediaPlaceholder(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Box(
            modifier = Modifier.size(width = 160.dp, height = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            AttachmentStatusPlaceholder(text = text)
        }
    }
}

@Composable
private fun UploadingMediaBubble(
    file: com.github.im.group.sdk.File,
    text: String,
    failed: Boolean,
    onDownloadFile: ((String) -> Unit)? = null
) {
    Box {
        MediaMessage(file = file, modifier = Modifier.blur(8.dp), onDownloadFile = onDownloadFile)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (failed) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.5.dp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
