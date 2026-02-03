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
import androidx.compose.material3.ButtonDefaults
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
import com.github.im.group.viewmodel.SessionCreationState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel

/**
 * 聊天室屏幕组件
 * 
 * 逻辑1: 初始化聊天室信息
 * 逻辑2: 处理消息列表显示
 * 逻辑3: 实现下拉刷新功能
 * 逻辑4: 管理视频通话功能
 * 逻辑5: 处理会话创建状态
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
    var isCreatingConversation by remember {  mutableStateOf(true)}
    var conversationId by remember {  mutableStateOf<Long?>(null)}

    var remoteUser by remember { mutableStateOf<UserInfo?>(chatUiState.friend) } // 在群聊场景中可能为null，表示多个用户
    var groupName by remember { mutableStateOf<String>(chatUiState.getRoomName()) } // 群聊名称加载中

    /**
     * 实现下拉刷新逻辑
     *
     * 逻辑1: 优先从本地获取数据，提供即时反馈
     * 逻辑2: 必要时从远程获取最新数据
     * 逻辑3: 显示加载动画
     * 逻辑4: 使用会话ID执行刷新
     */
    val pullRefreshState = rememberPullRefreshState(
        refreshing = chatUiState.loading,
        onRefresh = {
            // 优先从本地获取数据，必要时从远程获取
            chatUiState.conversation?.conversationId?.let { conversationId ->
                chatRoomViewModel.loadMessages(conversationId)
            }
        }
    )
    val isRefreshing = pullRefreshState.progress

    val listState = rememberLazyListState()

    LaunchedEffect(chatRoom) {
        // 初始化下 信息
        chatRoomViewModel.initChatRoom(chatRoom)
    }


    DisposableEffect(conversationId) {
        onDispose {
            conversationId?.let {
                chatRoomViewModel.unregister(it) // 注销会话，避免内存泄漏
            }
            voiceViewModel.reset() // 重置
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
                        conversationId?.let {
                            chatRoomViewModel.loadLatestMessages(it, sequenceId)
                        }
                    }
                }
            }
    }

    // 视频通话界面
    if (showVideoCall && remoteUser != null) {
        VideoCallLauncher(
            remoteUser = remoteUser!!,
            onCallEnded = {
                showVideoCall = false
            }
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
                        // 显示对方用户头像和昵称或群组名称
                        chatUiState.getRoomName().let {
                            UserAvatar(
                                username = it,
                                size = 32,
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = it,
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
                    // 视频通话按钮
                    if (remoteUser != null) { // 只在单聊场景中显示视频通话按钮
                        IconButton(
                            onClick = {
                                showVideoCall = true
                                Napier.i ("start video call")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = "视频通话"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                // 消息列表背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                )

                // 消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true, // 最新消息在底部
                    contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp, start = 12.dp, end = 12.dp), // 为输入区域留出空间
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // 显示历史加载指示器
                    if (chatUiState.loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "加载中...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 显示消息列表
                    items(chatUiState.messages, key = { message ->
                        if (message.seqId != 0L) {
                            "seq_${message.seqId}"
                        } else {
                            "client_${message.clientMsgId}"
                        }
                    }) { message ->
                        // 根据消息方向显示不同的气泡样式
                        val isMyMessage = message.userInfo.userId == userInfo?.userId

                        MessageBubble(
                            isOwnMessage = isMyMessage,
                            msg = message,
                        )
                    }
                }

                // 处理滚动到最新消息
                LaunchedEffect(chatUiState.scrollToTop) {
                    if (chatUiState.scrollToTop) {
                        listState.animateScrollToItem(0) // 滚动到最新消息（reverseLayout=true时，最新消息在索引0）
                    }
                }

                // 监听滚动位置以更新消息索引
                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }
                        .collect { index ->
                            // 当用户滚动时，可以在这里处理消息索引逻辑
                            // 但注意不要覆盖发送消息时的自动滚动
                        }
                }

                // 监听滚动状态变化以动态更新消息索引
                LaunchedEffect(listState) {
                    snapshotFlow { listState.isScrollInProgress }
                        .collect { isScrolling ->
                            if (!isScrolling) {
                                // 滚动停止时，更新消息索引为当前可见的第一个消息
                                val firstVisibleIndex = listState.firstVisibleItemIndex
                                chatRoomViewModel.updateMessageIndex(firstVisibleIndex)
                            }
                        }
                }

                // 重置滚动到顶部标志，防止重复滚动
                LaunchedEffect(chatUiState.scrollToTop) {
                    if (chatUiState.scrollToTop) {
                        // 等待滚动完成后再重置标志
                        kotlinx.coroutines.delay(300) // 等待动画完成
                        chatRoomViewModel.resetScrollToTopFlag()
                    }
                }

                // 下拉刷新指示器
                PullRefreshIndicator(
                    refreshing = chatUiState.loading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                val am  = voiceViewModel.amplitude.collectAsState()
                // 录音遮罩
                if (uiState is RecorderUiState.Recording) {

                    VoiceControlOverlayWithRipple(
                        amplitude = am.value,
                    )
                }

                // 输入区域
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // Updated Input Area Logic in ChatRoomScreen.kt
                    ChatInputArea(
                        onSendText = { text ->
                            if (text.isNotBlank()) {
                                chatRoomViewModel.sendText(text)
                            }
                        },
                        onRelease = {
                            voiceViewModel.getVoiceData()?.let { voiceData ->
                                chatRoomViewModel.sendVoice(voiceData)
                            }
                        },
                        onFileSelected = { files ->
                            files.forEach { file ->
                                chatRoomViewModel.sendFile(file)
                            }
                        }
                    )
                }

                // 显示会话创建错误或状态
                if (chatUiState.sessionCreationState is SessionCreationState.Error ||
                    chatUiState.sessionCreationState is SessionCreationState.Pending ||
                    chatUiState.error != null) {

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                        ,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = chatUiState.error ?: "会话创建遇到问题",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    chatRoomViewModel.clearSessionCreationError()
//                                    chatRoomViewModel.retryPendingSession()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "重试",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }

                // 显示会话创建过程中的加载状态
                if (chatUiState.sessionCreationState is SessionCreationState.Creating) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "正在创建会话...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    )
}


/**
 * 判断是否应该显示头像
 * 
 * 逻辑1: 如果是第一条消息（最新的消息），显示头像
 * 逻辑2: 如果当前消息发送者与前一条消息发送者不同，显示头像
 * 逻辑3: 根据UI设计决定是否显示自己的头像
 * 
 * @param currentMsg 当前消息
 * @param allMessages 所有消息列表
 * @param currentUser 当前用户
 * @return 是否应该显示头像
 */
fun shouldShowAvatar(currentMsg: MessageItem, allMessages: List<MessageItem>, currentUser: UserInfo?): Boolean {
    val currentIndex = allMessages.indexOf(currentMsg)

    // 如果是第一条消息（最新的消息），显示头像
    if (currentIndex == 0) {
        return true
    }

    // 获取前一条消息
    val previousMsg = allMessages[currentIndex - 1]

    // 如果当前消息发送者与前一条消息发送者不同，显示头像
    if (currentMsg.userInfo.userId != previousMsg.userInfo.userId) {
        return true
    }

    // 如果是当前用户发送的消息，根据需要决定是否显示自己的头像
    // 这里可以根据UI设计决定是否显示自己的头像
    return false
}

/**
 * 聊天气泡
 * 
 * 逻辑1: 根据消息方向布局（自己消息靠右，他人消息靠左）
 * 逻辑2: 显示头像（对于他人消息）
 * 逻辑3: 根据消息类型显示内容（文本、语音、文件等）
 * 逻辑4: 显示消息状态（发送中、已发送、已读等）
 */
@Composable
fun MessageBubble(
    isOwnMessage: Boolean,
    msg: MessageItem,
    showAvatar: Boolean = true,
) {
    val messageViewModel: ChatRoomViewModel = koinViewModel()

    // 使用 Row 来包含头像和消息气泡
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // 对方消息显示头像在左侧
            if (showAvatar) {
                UserAvatar(
                    username = msg.userInfo.username,
                    size = 36
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                // 不显示头像时保留占位空间
                Spacer(modifier = Modifier.width(44.dp))
            }
        }

        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // 显示用户名（仅在群聊中且需要显示头像时）
            if (showAvatar && !isOwnMessage && msg.userInfo.username.isNotEmpty()) {
                Text(
                    text = msg.userInfo.username,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

                // 消息气泡
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnMessage && msg.status == MessageStatus.SENDING) {
                        // 发送中状态
                        SendingSpinner(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = if (isOwnMessage) 4.dp else 16.dp,
                            topEnd = if (isOwnMessage) 16.dp else 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        tonalElevation = if (isOwnMessage) 2.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            when (msg.type) {
                                MessageType.TEXT -> TextMessage(MessageContent.Text(msg.content))

                                MessageType.VOICE -> {
                                    FileMessageLoader(
                                        msg = msg,
                                        messageViewModel = messageViewModel,
                                        maxDownloadSize = 50 * 1024 * 1024, // 50MB 限制
                                        onContentReady = { pickedFile, meta ->
                                            VoiceMessage(
                                                content = MessageContent.Voice(
                                                    audioPath = pickedFile.path,
                                                    duration = meta.duration
                                                )
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
                                            Text("语音文件过大或加载失败")
                                        }
                                    )
                                }
                                MessageType.IMAGE ,
                                MessageType.VIDEO ,
                                MessageType.FILE -> {
                                    UnifiedFileMessage(
                                        message = msg,
                                        messageViewModel = messageViewModel
                                    )
                                }
                                else -> TextMessage(MessageContent.Text(msg.content))
                            }
                        }
                    }
                }

                // 显示消息时间戳和状态图标
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = if (isOwnMessage) 0.dp else 4.dp, end = if (isOwnMessage) 4.dp else 0.dp),
                    horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.time.toString().substring(11, 16), // 只显示小时和分钟
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    // 显示消息状态图标（仅对本人发送的消息）
                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (msg.status) {
                            MessageStatus.SENDING -> {
                                // 发送中状态（已存在）
                            }
                            MessageStatus.SENT -> {
                                // 已发送
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已发送",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            MessageStatus.READ -> {
                                // 已读
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "已读",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            MessageStatus.FAILED -> {
                                // 发送失败
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "发送失败",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                // 其他状态显示灰色勾
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已发送",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
