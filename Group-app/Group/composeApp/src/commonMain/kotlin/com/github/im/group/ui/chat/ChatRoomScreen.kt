package com.github.im.group.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.api.ConversationType
import com.github.im.group.api.MeetingApi
import com.github.im.group.api.MeetingCreateRequest
import com.github.im.group.api.MeetingMessagePayLoad
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.theme.ThemeTokens
import com.github.im.group.ui.video.MeetingLauncher
import com.github.im.group.ui.video.VideoCallLauncher
import com.github.im.group.viewmodel.ChatRoomViewModel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

private val meetingPayloadJson = Json { ignoreUnknownKeys = true }

private fun extractMeetingPayload(message: MessageItem): MeetingMessagePayLoad? {
    val wrapper = message as? MessageWrapper
    val payload = wrapper?.messageDto?.payload
    if (payload is MeetingMessagePayLoad) return payload

    val raw = message.content
    if (raw.isBlank() || !raw.trim().startsWith("{")) return null
    return runCatching { meetingPayloadJson.decodeFromString<MeetingMessagePayLoad>(raw) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatRoomScreen(
    chatRoom: ChatRoom,
    navHostController: NavHostController = rememberNavController(),
    onBack: () -> Unit = {},
) {
    val userViewModel: UserViewModel = koinViewModel()
    val voiceViewModel: VoiceViewModel = koinViewModel()
    val chatRoomViewModel: ChatRoomViewModel = koinViewModel()

    val uiState by voiceViewModel.uiState.collectAsState()
    val chatUiState by chatRoomViewModel.uiState.collectAsState()
    val userInfo by userViewModel.currentLocalUserInfo.collectAsState()

    var showVideoCall by remember { mutableStateOf(false) }
    var showMeeting by remember { mutableStateOf(false) }
    var meetingRoomId by remember { mutableStateOf<String?>(null) }
    var meetingParticipantIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var meetingIsHost by remember { mutableStateOf(false) }
    var conversationId by remember { mutableStateOf<Long?>(null) }
    var remoteUser by remember { mutableStateOf<UserInfo?>(chatUiState.friend) }

    val isGroupConversation = chatUiState.conversation?.conversationType == ConversationType.GROUP
    val roomSubtitle = when {
        isGroupConversation -> "${chatUiState.conversation?.members?.size ?: 0} 位成员"
        remoteUser?.email?.isNotBlank() == true -> remoteUser?.email ?: ""
        else -> "私聊"
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = chatUiState.loading,
        onRefresh = {
            chatUiState.conversation?.conversationId?.let { id ->
                chatRoomViewModel.loadMessages(id)
            }
        }
    )

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var lastMarkedReadSeq by remember { mutableStateOf(0L) }

    LaunchedEffect(chatRoom) {
        chatRoomViewModel.initChatRoom(chatRoom)
    }

    LaunchedEffect(chatUiState.conversation?.conversationId, chatUiState.friend) {
        conversationId = chatUiState.conversation?.conversationId
        remoteUser = chatUiState.friend
    }

    DisposableEffect(conversationId) {
        onDispose {
            conversationId?.let { chatRoomViewModel.unregister(it) }
            voiceViewModel.reset()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0 && chatUiState.messages.isNotEmpty()) {
                    val oldestMessage = chatUiState.messages.lastOrNull()
                    oldestMessage?.seqId?.let { sequenceId ->
                        conversationId?.let { id ->
                            chatRoomViewModel.loadLatestMessages(id, sequenceId)
                        }
                    }
                }
            }
    }

    if (showVideoCall && remoteUser != null) {
        VideoCallLauncher(
            remoteUser = remoteUser!!,
            onCallEnded = { showVideoCall = false }
        )
    }

    if (showMeeting && meetingRoomId != null) {
        val roomId = meetingRoomId!!
        MeetingLauncher(
            roomId = roomId,
            participantIds = meetingParticipantIds,
            onCallEnded = {
                showMeeting = false
                val finalRoomId = meetingRoomId
                val shouldEnd = meetingIsHost
                meetingRoomId = null
                meetingParticipantIds = emptyList()
                meetingIsHost = false
                if (finalRoomId != null) {
                    scope.launch {
                        if (shouldEnd) {
                            MeetingApi.endMeeting(finalRoomId)
                        } else {
                            MeetingApi.leaveMeeting(finalRoomId)
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = ThemeTokens.BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val roomName = chatUiState.getRoomName()
                        UserAvatar(username = roomName, size = 32)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = roomName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isGroupConversation) Icons.Default.Groups else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = roomSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    if (remoteUser != null || isGroupConversation) {
                        IconButton(
                            onClick = {
                                if (isGroupConversation) {
                                    val conversation = chatUiState.conversation ?: return@IconButton
                                    val currentId = userInfo?.userId?.toString()
                                    val participantIds = conversation.members
                                        .map { it.userId.toString() }
                                        .filter { it != currentId }
                                    scope.launch {
                                        val meeting = MeetingApi.createMeeting(
                                            MeetingCreateRequest(
                                                conversationId = conversation.conversationId,
                                                title = conversation.groupName,
                                                participantIds = participantIds.mapNotNull { it.toLongOrNull() }
                                            )
                                        )
                                        meetingRoomId = meeting.roomId
                                        meetingParticipantIds = participantIds
                                        meetingIsHost = true
                                        showMeeting = true
                                    }
                                } else {
                                    showVideoCall = true
                                }
                            },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0EA5E9))
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = "发起视频通话", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ThemeTokens.BackgroundDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .pullRefresh(pullRefreshState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF1F5F9))
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = Color.White,
                tonalElevation = 0.dp
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(bottom = 100.dp, top = 20.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(chatUiState.messages, key = { message ->
                        if (message.seqId != 0L) "seq_${message.seqId}" else "client_${message.clientMsgId}"
                    }) { message ->
                        val isMyMessage = message.userInfo.userId == userInfo?.userId
                        MessageBubble(
                            isOwnMessage = isMyMessage,
                            msg = message,
                            onJoinMeeting = onJoinMeeting@{ payload ->
                                val roomId = payload.roomId ?: return@onJoinMeeting
                                scope.launch { MeetingApi.joinMeeting(roomId) }
                                meetingRoomId = roomId
                                meetingParticipantIds = payload.participantIds
                                    .map { it.toString() }
                                    .filter { it != userInfo?.userId?.toString() }
                                meetingIsHost = payload.hostId == userInfo?.userId
                                showMeeting = true
                            },
                        )
                    }

                if (!chatUiState.loading && chatUiState.messages.isEmpty()) {
                    item {
                        EmptyChatPlaceholder(isGroup = isGroupConversation)
                    }
                }
                }
            }

            val isAtBottom by remember {
                androidx.compose.runtime.derivedStateOf {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                }
            }
            LaunchedEffect(isAtBottom, conversationId, userInfo?.userId, chatUiState.messages) {
                val cid = conversationId ?: return@LaunchedEffect
                val uid = userInfo?.userId ?: return@LaunchedEffect
                if (!isAtBottom) return@LaunchedEffect

                val hasUnread = chatUiState.messages.any { it.userInfo.userId != uid && it.status == MessageStatus.SENT }
                if (!hasUnread) return@LaunchedEffect

                val lastSeq = chatUiState.messages.firstOrNull()?.seqId ?: 0L
                if (lastSeq <= 0L || lastSeq <= lastMarkedReadSeq) return@LaunchedEffect

                lastMarkedReadSeq = lastSeq
                chatRoomViewModel.markConversationAsRead(cid, uid)
            }

            LaunchedEffect(chatUiState.scrollToTop) {
                if (chatUiState.scrollToTop) {
                    listState.animateScrollToItem(0)
                    kotlinx.coroutines.delay(300)
                    chatRoomViewModel.resetScrollToTopFlag()
                }
            }

            PullRefreshIndicator(
                refreshing = chatUiState.loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (uiState is RecorderUiState.Recording) {
                val am by voiceViewModel.amplitude.collectAsState()
                VoiceControlOverlayWithRipple(amplitude = am)
            }

            val focusManager = LocalFocusManager.current
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                ChatInputArea(
                    onSendText = { text ->
                        if (text.isNotBlank()) chatRoomViewModel.sendText(text)
                        focusManager.clearFocus()
                    },
                    onRelease = {
                        voiceViewModel.getVoiceData()?.let { chatRoomViewModel.sendVoice(it) }
                    },
                    onFileSelected = { files ->
                        files.forEach { chatRoomViewModel.sendFile(it) }
                        focusManager.clearFocus()
                    }
                )
            }

            val belowCount by remember {
                androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex.coerceAtLeast(0) }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !isAtBottom,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 98.dp, end = 16.dp),
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = Color.White,
                    contentColor = ThemeTokens.TextMain,
                    modifier = Modifier.size(44.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (belowCount > 0) {
                                Badge {
                                    Text(text = belowCount.coerceAtMost(99).toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "回到底部",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (chatUiState.error != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = chatUiState.error ?: "", modifier = Modifier.weight(1f))
                        Button(onClick = { chatRoomViewModel.clearSessionCreationError() }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatPlaceholder(isGroup: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isGroup) "群聊已创建" else "对话已开启",
                    style = MaterialTheme.typography.titleSmall,
                    color = ThemeTokens.TextMain,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isGroup) "发一条消息，开始团队协作。" else "发一条消息，开始这段对话。",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeTokens.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    isOwnMessage: Boolean,
    msg: MessageItem,
    showAvatar: Boolean = true,
    onJoinMeeting: (MeetingMessagePayLoad) -> Unit = {},
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

                    // 娑堟伅闀挎寜鑿滃崟姘村钩鎺掑垪浠ラ槻閬尅
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
                                modifier = Modifier.weight(1f) // 浣垮叾鑳藉鍦ㄤ竴琛屾樉?
                            )
                            DropdownMenuItem(
                                text = { Text("转发", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isOwnMessage) {
                                DropdownMenuItem(
                                    text = { Text("撤回", fontSize = 14.sp, color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                    },
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








