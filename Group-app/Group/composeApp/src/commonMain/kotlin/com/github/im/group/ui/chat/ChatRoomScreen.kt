package com.github.im.group.ui.chat

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoCall
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.video.VideoCallUI
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import com.github.im.group.ui.video.VideoCallViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.snapshotFlow
import com.github.im.group.ui.UserAvatar

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
    
    // 视频通话状态
    var showVideoCall by remember { mutableStateOf(false) }
    var remoteUser by remember { mutableStateOf<UserInfo?>(null) }

    // 录音时遮罩
    val uiState by voiceViewModel.uiState.collectAsState()
    val amplitude by voiceViewModel.amplitude.collectAsState()

    LaunchedEffect(conversationId) {
        chatViewModel.getConversations(conversationId)
        messageViewModel.getConversation(conversationId)
        // 先本地加载
        messageViewModel.loadLocalMessages(conversationId)
        // 再同步远程消息
        messageViewModel.loadRemoteMessages(conversationId)
        
        messageViewModel.register(conversationId)
    }


    // 加载完消息后自动滚动到底部

    val state by messageViewModel.uiState.collectAsState()
    val fileDownloadStates by messageViewModel.fileDownloadStates.collectAsState()

    val userInfo = userViewModel.getCurrentUser()

    
    // 获取VideoCallViewModel
    val videoCallViewModel: VideoCallViewModel = koinViewModel()

    val listState = rememberLazyListState()
    
    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
        isRefreshing = true
        messageViewModel.refreshMessages(conversationId)
    })

    // 上拉加载更多状态
//    var isLoadingMore by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            // 刷新到 指定 的消息索引
            listState.animateScrollToItem(state.messageIndex)
        }
    }

    // 监听刷新完成状态
    LaunchedEffect(state.loading) {
        if (!state.loading && isRefreshing) {
            isRefreshing = false
        }
    }

    // 监听列表滚动，实现上拉加载更多
    LaunchedEffect(listState) {
        Napier.d  ("ChatRoomScreen $listState")
        snapshotFlow { listState.firstVisibleItemIndex to listState.layoutInfo.totalItemsCount }
            .collect { (firstVisibleIndex, totalItemsCount) ->
                messageViewModel.scrollIndex(firstVisibleIndex)
                // 当滚动到顶部附近时，加载更多历史消息（向后翻页）
                Napier.d  ("ChatRoomScreen firstVisibleIndex $firstVisibleIndex totalItemsCount $totalItemsCount")
                val remainItemIndex = totalItemsCount - firstVisibleIndex
                val isLoadingMore = state.loading
                if (remainItemIndex < 5 && !isLoadingMore && state.messages.isNotEmpty()) {
                    // 获取最早的消息的序列号
                    val earliestMessage = state.messages.filter{ it.seqId > 0L }.minByOrNull { it.seqId }
                    Napier.d  ("earliestMessage  $earliestMessage ")

                    earliestMessage?.let {
                        messageViewModel.loadMoreMessages(conversationId, earliestMessage.seqId)
                    }
                }

            }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (uiState is RecorderUiState.Recording){
                Napier.d("close VoiceMessage")
                voiceViewModel.stopRecording(AnimatedContentTransitionScope.SlideDirection.Left)
            }
        }
    }


    /**
     * 发送语音消息
     */
    fun sendVoiceMessage() {
        voiceViewModel.getVoiceData()?.let {
            messageViewModel.sendVoiceMessage(
                conversationId,
                it.bytes,
                it.name,
                it.durationMillis
            )
            voiceViewModel.cancel() // 发送后重置状态
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.conversation.getName(userInfo), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    // 视频通话按钮
                    IconButton(onClick = {
                        // 设置远程用户（这里应该是从会话中获取对方用户信息）
                        remoteUser = state.conversation.getOtherUser(userInfo)
                        Napier.d("remoteUser: $remoteUser")
                        Napier.d("currentUser: $userInfo")
                        // 启动视频通话
                        remoteUser?.let { 
                            // 设置当前用户ID
                            videoCallViewModel.startVideoCall(it)
                            showVideoCall = true
                        }
                    }) {
                        Icon(Icons.Default.VideoCall, contentDescription = "视频通话", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0088CC))
            )
        },
        // 录音时候不展示 底部
        bottomBar = {
            ChatInputArea(
                onSendText = { text ->
                    messageViewModel.sendMessage(conversationId, text)
                },
                onRelease = { direction ->
                    Napier.d(" direction  : $direction")
                    when (direction) {
                        AnimatedContentTransitionScope.SlideDirection.Start -> {
                            voiceViewModel.stopRecording( direction)
                            Napier.d(" start  : $direction")
                            // 直接发送消息
                            sendVoiceMessage()
                        }
                        AnimatedContentTransitionScope.SlideDirection.Left -> {
                            Napier.d(" left  : $direction")

                            // 取消录音
                            voiceViewModel.cancel()
                        }

                        AnimatedContentTransitionScope.SlideDirection.Right -> {
                            // 取消录音
                            voiceViewModel.stopRecording(direction)
                        }else -> {
                        Napier.d(" else  : $direction")
                            // 取消录音
                            voiceViewModel.cancel()
                        }

                    }

                },
                onFileSelected = { files ->
                    // 处理选择的文件
                    files.forEach { file ->
                        messageViewModel.sendFileMessage(conversationId, file)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .pullRefresh(pullRefreshState)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true, // true: 新消息在底部，列表倒序排列
                contentPadding = PaddingValues(bottom = 40.dp) // 为输入框腾出空间
            ) {
                items(state.messages) { msg ->
                    // 计算是否应该显示头像
                    val showAvatar = shouldShowAvatar(msg, state.messages, userInfo)
                    
                    // 头像
                    MessageBubble(
                        isOwnMessage = msg.userInfo.userId == userInfo.userId,
                        msg = msg,
                        showAvatar = showAvatar,
                        onFileMessageClick = { fileMsg ->
                            // 处理文件消息点击事件
                            // 触发文件下载（实现本地优先策略）
                            Napier.d("点击文件消息: ${fileMsg.content}")
                            messageViewModel.downloadFileMessage(fileMsg.content)
                        }
                    )
                }
            }
            
            // 加载指示器
            if (state.loading && !isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "消息加载中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // 下拉刷新指示器
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // 录音控制浮窗
        val vm by voiceViewModel.amplitude.collectAsState()
        if (uiState is RecorderUiState.Recording) {
            VoiceControlOverlayWithRipple(
                amplitude = vm ,
                onFinish =  {
//                    voiceViewModel.stopRecording()
                },
            )
        }


        // 回放浮窗
        if (uiState is RecorderUiState.Playback) {

            VoiceReplay(
                onSend = {
                    sendVoiceMessage()
                }
            )
        }
        
        // 文件下载状态指示器（支持多个文件同时下载）
        fileDownloadStates.forEach { (fileId, downloadState) ->
            if (downloadState.isDownloading) {
                Dialog(onDismissRequest = { }) {
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
                            Text("文件下载中... ($fileId)")
                        }
                    }
                }
            }
            
            // 文件下载完成后的处理
            if (!downloadState.isDownloading && downloadState.isSuccess) {
//                Napier.d("文件下载完成，大小: ${downloadState.fileContent?.size ?: 0} 字节")
                // 这里可以添加文件保存或打开的逻辑
                // 下载完成后清除状态
                // messageViewModel.clearFileDownloadState(fileId)
            }
            
            // 文件下载失败的处理
            if (!downloadState.isDownloading && !downloadState.isSuccess) {
                Napier.e("文件下载失败: ${downloadState.error}")
                // 这里可以添加错误提示的逻辑
                // 下载失败后清除状态
                // messageViewModel.clearFileDownloadState(fileId)
            }
        }
        
        // 视频通话界面
        if (showVideoCall) {
            VideoCallUI(
                navHostController = navHostController,
                remoteUser = remoteUser,
                localMediaStream = null,
                onEndCall = {
                    videoCallViewModel.endCall()
                    showVideoCall = false
                },
                onToggleCamera = { videoCallViewModel.toggleCamera() },
                onToggleMicrophone = { videoCallViewModel.toggleMicrophone() },
                onSwitchCamera = { videoCallViewModel.switchCamera() },
                onMinimizeCall = {
                    videoCallViewModel.minimizeCall()
                    showVideoCall = false
                },
            )
        }
    }
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
                            // TODO TCP 推送的数据缺失了 FileMeta 信息 考虑下是否需要 TCP 将其都传入过来,保持 一致性
                            /***
                             *
                             * 目前通过  {@see com.github.im.group.viewmodel.ChatMessageViewModel
                             * .getFileMessageMeta(com.github.im.group.model.MessageItem) 获取文件元数据 }
                             * 来处理
                             */


                            val duration = messageViewModel.getFileMessageMeta(msg)?.duration ?: 1
                            VoiceMessage(MessageContent.Voice(msg.content, duration)) {
                                onVoiceMessageClick(MessageContent.Voice(msg.content, duration))
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

    // 显示文件消息
    val fileSize = messageViewModel.getFileMessageMeta(msg)?.size ?: 0
    val displaySize = if (fileSize > 1024 * 1024) {
        "${fileSize / 1024 / 1024}MB"
    } else if (fileSize > 1024) {
        "${fileSize / 1024}KB"
    } else {
        "${fileSize}B"
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
