package com.github.im.group.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.im.group.api.MeetingMessagePayLoad
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.ui.UserAvatar
import com.github.im.group.viewmodel.ChatRoomViewModel
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            if (showAvatar) {
                UserAvatar(username = msg.userInfo.username, size = 36)
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(44.dp))
            }
        }

        Column(horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start) {
            if (showAvatar && !isOwnMessage && msg.userInfo.username.isNotEmpty()) {
                Text(
                    text = msg.userInfo.username,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                if (isOwnMessage && msg.status == MessageStatus.SENDING) {
                    SendingSpinner(modifier = Modifier.padding(end = 4.dp).size(12.dp))
                }

                Box {
                    Surface(
                        color = if (isOwnMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = if (isOwnMessage) 12.dp else 4.dp,
                            topEnd = if (isOwnMessage) 4.dp else 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        tonalElevation = if (isOwnMessage) 1.dp else 0.5.dp,
                        modifier = Modifier.combinedClickable(
                            onLongClick = { showMenu = true },
                            onClick = { /* Default click behavior */ }
                        )
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            when (msg.type) {
                                MessageType.TEXT -> TextMessage(MessageContent.Text(msg.content), isOwnMessage)
                                MessageType.VOICE -> {
                                    FileMessageLoader(
                                        msg = msg,
                                        messageViewModel = messageViewModel,
                                        onContentReady = { file, meta ->
                                            VoiceMessage(
                                                content = MessageContent.Voice(file.path, meta.duration),
                                                senderName = msg.userInfo.username,
                                                isOwnMessage = isOwnMessage,
                                                messageId = if (msg.seqId != 0L) "seq_${msg.seqId}" else "client_${msg.clientMsgId}"
                                            )
                                        },
                                        onLoading = { CircularProgressIndicator(modifier = Modifier.size(16.dp)) }
                                    )
                                }
                                MessageType.MEETING -> {
                                    val payload = extractMeetingPayload(msg)
                                    MeetingMessageBubble(
                                        payload = payload,
                                        isOwnMessage = isOwnMessage,
                                        onJoin = {
                                            if (payload != null) onJoinMeeting(payload)
                                        }
                                    )
                                }
                                MessageType.IMAGE, MessageType.VIDEO, MessageType.FILE -> {
                                    UnifiedFileMessage(message = msg, messageViewModel = messageViewModel)
                                }
                                else -> TextMessage(MessageContent.Text(msg.content), isOwnMessage)
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(if (isOwnMessage) (-16).dp else 16.dp, 0.dp),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                            DropdownMenuItem(
                                text = { Text("复制", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    if (msg.type == MessageType.TEXT) {
                                        clipboardManager.setText(AnnotatedString(msg.content))
                                    }
                                    showMenu = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DropdownMenuItem(
                                text = { Text("转发", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false },
                                modifier = Modifier.weight(1f)
                            )
                            if (isOwnMessage) {
                                DropdownMenuItem(
                                    text = { Text("撤回", fontSize = 14.sp, color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red) },
                                    onClick = { showMenu = false },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (isOwnMessage) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.time.toString().substring(11, 16),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val statusIcon = when (msg.status) {
                        MessageStatus.SENT -> Icons.Default.Check
                        MessageStatus.READ -> Icons.Default.CheckCircle
                        MessageStatus.FAILED -> Icons.Default.Error
                        else -> Icons.Default.Check
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (msg.status == MessageStatus.READ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
