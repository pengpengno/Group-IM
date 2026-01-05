package com.github.im.group.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.chat.ChatInputArea
import com.github.im.group.ui.chat.VoiceControlOverlayWithRipple
import com.github.im.group.ui.video.VideoCallUI
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import com.github.im.group.ui.video.VideoCallViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.rememberNavController
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import io.github.aakira.napier.log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ChatRoomScreen(
    conversationId: Long,
    navHostController: NavHostController = rememberNavController(),
    onBack: () -> Unit = {},
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val userViewModel: UserViewModel = koinViewModel()
    val voiceViewModel: VoiceViewModel = koinViewModel()
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    
    val uiState by voiceViewModel.uiState.collectAsState()
    val state by messageViewModel.uiState.collectAsState()
    val userInfo = userViewModel.getCurrentUser()

    var showEmojiPanel by remember { mutableStateOf(false) }
    var showMorePanel by remember { mutableStateOf(false) }
    var showVideoCall by remember { mutableStateOf(false) }
    var remoteUser by remember { mutableStateOf<UserInfo?>(null) }
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.loading,
        onRefresh = { 
            messageViewModel.refreshMessages(conversationId)
        }
    )
    val isRefreshing = pullRefreshState.progress

    val listState = rememberLazyListState()
    
    LaunchedEffect(conversationId) {
        messageViewModel.loadMessages(conversationId)
        messageViewModel.getConversation(conversationId) // 获取会话信息
        messageViewModel.register(conversationId) // 注册会话，用于接收服务端推送的消息

        // 设置远程用户（这里应该是从会话中获取对方用户信息）
        remoteUser = state.conversation.getOtherUser(userInfo)
        Napier.i ("state ${state.conversation}")
    }
    
    DisposableEffect(conversationId) {
        onDispose {
            messageViewModel.unregister(conversationId) // 注销会话，避免内存泄漏
            voiceViewModel.reset() // 重置
        }
    }
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0 && state.messages.isNotEmpty()) {
                    // 用户滚动到了顶部，加载更多历史消息
                    val oldestMessage = state.messages.lastOrNull()
                    oldestMessage?.seqId?.let { sequenceId ->
                        messageViewModel.loadMoreMessages(conversationId, sequenceId)
                    }
                }
            }
    }
    
    // 视频通话界面
    if (showVideoCall) {
        VideoCallUI(
            remoteUser = remoteUser,
            localMediaStream = null,
            onEndCall = { 
                showVideoCall = false

            },
            onToggleCamera = { videoCallViewModel.toggleCamera() },
            onToggleMicrophone = { videoCallViewModel.toggleMicrophone() },
            onSwitchCamera = { videoCallViewModel.switchCamera() },
            onMinimizeCall = { showVideoCall = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 显示对方用户头像和昵称
                        state.conversation.getOtherUser(userInfo)?.let { otherUser ->
                            UserAvatar(
                                username = otherUser.username,
                                size = 32,
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = otherUser.username,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                // TODO: 显示在线状态
                                Text(
                                    text = "在线",
                                    color = Color.Green,
                                    fontSize = 12.sp
                                )
                            }
                        } ?: run {
                            // 如果没有获取到对方用户信息，显示群组名称
                            Text(state.conversation.groupName)
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // 视频通话按钮
                        IconButton(
                            onClick = { 
                                showVideoCall = true
                                // TODO: 初始化视频通话
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = "视频通话"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
                // 消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true, // 最新消息在底部
                    contentPadding = PaddingValues(bottom = 80.dp) // 为输入区域留出空间
                ) {
                    
                    // 显示历史加载指示器
                    if (state.loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("加载中...")
                            }
                        }
                    }
                    
                    // 显示消息列表
                    items(state.messages, key = { it.clientMsgId }) { message ->
                        // 根据消息方向显示不同的气泡样式
                        val isMyMessage = message.userInfo.userId == userInfo?.userId
                        
                        MessageBubble(
                            isOwnMessage = isMyMessage,
                            msg = message,
                            onFileMessageClick = { 
                                messageViewModel.downloadFileMessage(it.content)
                            }
                        )
                    }
                }
                
                // 下拉刷新指示器
                PullRefreshIndicator(
                    refreshing = state.loading,
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
                    ChatInputArea(
                        onSendText = { text ->
                            if (text.isNotBlank()) {
                                messageViewModel.sendMessage(conversationId, text)
                            }
                        },
                        onRelease = {

                            // 停止录音 后直接发送即可
                            log { "stop message recoder " }
                            //  停止后直接发送
                            voiceViewModel.getVoiceData()?.let {
                                    messageViewModel.sendVoiceMessage(conversationId,
                                        it.bytes, it.name, it.durationMillis)
                            }

                        },
                        onFileSelected = { files ->
                            files.forEach { file ->
                                messageViewModel.sendFileMessage(conversationId, file)
                            }
                        }
                    )
                }
            }
        }
    )
}

/**
 * 判断是否应该显示头像
 * 在以下情况下显示头像：
 * 1. 第一条消息（最新的消息）
 * 2. 当前消息发送者与前一条消息发送者不同
 * 3. 当前消息与前一条消息间隔时间较长（可选）
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
 */
@Composable
fun MessageBubble(
    isOwnMessage: Boolean,
    msg: MessageItem,
    showAvatar: Boolean = true,
    onVoiceMessageClick: (MessageContent.Voice) -> Unit = {},
    onFileMessageClick: (MessageItem) -> Unit = {}
) {
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    
    // 使用 Row 来包含头像和消息气泡
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // 对方消息显示头像在左侧
            if (showAvatar) {
                UserAvatar(
                    username = msg.userInfo.username,
                    size = 40
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                // 不显示头像时保留占位空间
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // 显示用户名（仅在群聊中且需要显示头像时）
            if (showAvatar && !isOwnMessage && msg.userInfo.username.isNotEmpty()) {
                Text(
                    text = msg.userInfo.username,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // 消息气泡
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwnMessage && msg.status == MessageStatus.SENDING) {
                    // 发送中状态
                    SendingSpinner(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                        color = Color.Gray
                    )
                }

                Surface(
                    color = if (isOwnMessage) Color(0xFFB3E5FC) else Color(0xFFF0F0F0),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp
                ) {
                    when (msg.type) {
                        MessageType.TEXT -> TextMessage(MessageContent.Text(msg.content))

                        MessageType.VOICE -> {
                            // 异步加载语音消息的持续时间
                            var duration by remember { mutableStateOf<Int>(0) }
                            var audioPath by remember { mutableStateOf<String?>(null) }
                            val fileId = msg.content
                            LaunchedEffect(msg) {
                                // 异步获取文件元数据，

                                var meta = messageViewModel.getFileMessageMetaAsync(msg)
                                meta?.let {

                                    var fileExists = messageViewModel.isFileExists(fileId)
                                    if (!fileExists){
                                        // 不存在则下载
                                        messageViewModel.downloadFileMessage(it.fileId)

                                    }
                                    audioPath =  messageViewModel.getLocalFilePath(fileId).toString()

                                    duration = it.duration

                                }

                            }

                            // 使用当前已获取到的持续时间，如果没有则显示加载状态
                            if (duration > 0 && audioPath != null) {
                                audioPath?.let {
                                    log { "audioPath: $it" }
                                    VoiceMessage(
                                        content = MessageContent.Voice(
                                            audioPath = it,
                                            duration = duration
                                        )
                                    )
                                }

                            } else {
                                // 显示加载状态
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(4.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        MessageType.IMAGE -> ImageMessage(MessageContent.Image(msg.content))
                        MessageType.VIDEO -> VideoBubble(MessageContent.Video(msg.content))
                        MessageType.FILE -> FileMessageBubble(msg, onFileMessageClick)
                        else -> TextMessage(MessageContent.Text(msg.content))
                    }
                }
            }
        }
        
        if (isOwnMessage) {
            // 自己发送的消息显示头像在右侧
            if (showAvatar) {
                Spacer(modifier = Modifier.width(8.dp))
                UserAvatar(
                    username = msg.userInfo.username,
                    size = 40
                )
            } else {
                // 不显示头像时保留占位空间
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

/**
 * 文件类型气泡
 */
@Composable
fun FileMessageBubble(msg: MessageItem, onClick: (MessageItem) -> Unit) {
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    
    var showDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    // 异步加载文件大小信息
    var fileSize by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(msg) {
        val meta = messageViewModel.getFileMessageMetaAsync(msg)
        fileSize = meta?.size
    }

    val displaySize = when (val size = fileSize) {
        null -> "加载中..."
        else -> if (size > 1024 * 1024) {
            "${size / 1024 / 1024}MB"
        } else if (size > 1024) {
            "${size / 1024}KB"
        } else {
            "${size}B"
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick(msg) }
            .width(200.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "文件",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = msg.content,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = displaySize,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
            fileName = msg.content,
            fileSize = displaySize,
            onDismiss = { showDialog = false },
            onView = {
                showDialog = false
                // 实际查看文件的逻辑
                // 可以打开浏览器下载或者在应用内查看
                onClick(msg)
            },
            onDownload = {
                isDownloading = true
                // 实际下载文件的逻辑
                // 这里应该调用文件下载服务
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
 * 定期清理过期文件
 */
fun scheduleFileCleanup() {
    // 可以通过协程定期执行文件清理任务
    // 示例：每隔一天清理一次过期文件
    /*
    viewModelScope.launch {
        while (true) {
            delay(24 * 60 * 60 * 1000) // 24小时
            messageViewModel.cleanupExpiredFiles()
        }
    }
    */
}
