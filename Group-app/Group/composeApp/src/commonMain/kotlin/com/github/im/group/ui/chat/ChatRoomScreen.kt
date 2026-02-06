package com.github.im.group.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.video.VideoCallLauncher
import com.github.im.group.viewmodel.ChatRoomViewModel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 聊天室屏幕组件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
    var conversationId by remember {  mutableStateOf<Long?>(null)}

    var remoteUser by remember { mutableStateOf<UserInfo?>(chatUiState.friend) } 

    val pullRefreshState = rememberPullRefreshState(
        refreshing = chatUiState.loading,
        onRefresh = {
            chatUiState.conversation?.conversationId?.let { id ->
                chatRoomViewModel.loadMessages(id)
            }
        }
    )

    val listState = rememberLazyListState()

    LaunchedEffect(chatRoom) {
        chatRoomViewModel.initChatRoom(chatRoom)
    }

    DisposableEffect(conversationId) {
        onDispose {
            conversationId?.let { chatRoomViewModel.unregister(it) }
            voiceViewModel.reset()
        }
    }

    /**
     * 滚动到顶部时加载更多历史消息
     *
     * 逻辑1: 监听列表滚动状态
     * 逻辑2: 检测是否滚动到顶部
     * 逻辑3: 获取最早的消息序列ID
     * 逻辑4: 加载更多历史消息
     */
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0 && chatUiState.messages.isNotEmpty()) {
                    // 用户滚动到了顶部，加载更多历史消息
                    val oldestMessage = chatUiState.messages.lastOrNull()
                    oldestMessage?.seqId?.let { sequenceId ->
                        conversationId?.let { id ->
                            chatRoomViewModel.loadLatestMessages(id, sequenceId)
                        }
                    }
                }
            }
    }

    // 视频通话界面
    if (showVideoCall && remoteUser != null) {
        VideoCallLauncher(
            remoteUser = remoteUser!!,
            onCallEnded = { showVideoCall = false }
        )
    }


    // 顶部聊天 tab 状态栏
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        chatUiState.getRoomName().let { name ->
                            UserAvatar(username = name, size = 32)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (remoteUser != null) {
                        IconButton(onClick = { showVideoCall = true }) {
                            Icon(Icons.Default.VideoCall, contentDescription = "视频通话")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pullRefresh(pullRefreshState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                )

                // 消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (chatUiState.loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    items(chatUiState.messages, key = { message ->
                        if (message.seqId != 0L) "seq_${message.seqId}" else "client_${message.clientMsgId}"
                    }) { message ->
                        val isMyMessage = message.userInfo.userId == userInfo?.userId
                        MessageBubble(
                            isOwnMessage = isMyMessage,
                            msg = message,
                        )
                    }
                }

                LaunchedEffect(chatUiState.scrollToTop) {
                    if (chatUiState.scrollToTop) {
                        listState.animateScrollToItem(0)
                        kotlinx.coroutines.delay(300)
                        chatRoomViewModel.resetScrollToTopFlag()
                    }
                }

                // 下拉刷新指示器
                PullRefreshIndicator(
                    refreshing = chatUiState.loading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (uiState is RecorderUiState.Recording) {
                    val am by voiceViewModel.amplitude.collectAsState()
                    VoiceControlOverlayWithRipple(amplitude = am)
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    ChatInputArea(
                        onSendText = { text -> if (text.isNotBlank()) chatRoomViewModel.sendText(text) },
                        onRelease = {
                            voiceViewModel.getVoiceData()?.let { chatRoomViewModel.sendVoice(it) }
                        },
                        onFileSelected = { files -> files.forEach { chatRoomViewModel.sendFile(it) } }
                    )
                }

                if (chatUiState.error != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = chatUiState.error ?: "", modifier = Modifier.weight(1f))
                            Button(onClick = { chatRoomViewModel.clearSessionCreationError() }) { Text("重试") }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MessageBubble(
    isOwnMessage: Boolean,
    msg: MessageItem,
    showAvatar: Boolean = true,
) {
    val messageViewModel: ChatRoomViewModel = koinViewModel()

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

                Surface(
                    color = if (isOwnMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = if (isOwnMessage) 12.dp else 4.dp,
                        topEnd = if (isOwnMessage) 4.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ),
                    tonalElevation = if (isOwnMessage) 1.dp else 0.5.dp
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        when (msg.type) {
                            MessageType.TEXT -> TextMessage(MessageContent.Text(msg.content), isOwnMessage)
                            MessageType.VOICE -> {
                                FileMessageLoader(
                                    msg = msg,
                                    messageViewModel = messageViewModel,
                                    onContentReady = { file, meta ->
                                        VoiceMessage(content = MessageContent.Voice(file.path, meta.duration),isOwnMessage)
                                    },
                                    onLoading = { CircularProgressIndicator(modifier = Modifier.size(16.dp)) }
                                )
                            }
                            MessageType.IMAGE, MessageType.VIDEO, MessageType.FILE -> {
                                UnifiedFileMessage(message = msg, messageViewModel = messageViewModel)
                            }
                            else -> TextMessage(MessageContent.Text(msg.content), isOwnMessage)
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
