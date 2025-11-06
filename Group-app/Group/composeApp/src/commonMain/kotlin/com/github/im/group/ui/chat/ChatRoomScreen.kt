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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        messageViewModel.loadMessages(conversationId)
        messageViewModel.register(conversationId)
    }


    // 加载完消息后自动滚动到底部

    val state by messageViewModel.uiState.collectAsState()
    val fileDownloadStates by messageViewModel.fileDownloadStates.collectAsState()

    val userInfo = userViewModel.getCurrentUser()


    
    // 获取VideoCallViewModel
    val videoCallViewModel: VideoCallViewModel = koinViewModel()

    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White),
            state = listState,
            reverseLayout = false, // false: 新消息在底部
            contentPadding = PaddingValues(bottom = 40.dp) // 为输入框腾出空间
        ) {
            items(state.messages) { msg ->
                // 头像

                MessageBubble(
                    isOwnMessage = msg.userInfo.userId == userInfo.userId,
                    msg = msg,
                    onFileMessageClick = { fileMsg ->
                        // 处理文件消息点击事件
                        // 触发文件下载（实现本地优先策略）
                        Napier.d("点击文件消息: ${fileMsg.content}")
                        messageViewModel.downloadFileMessage(fileMsg.content)
                    }
                )
            }
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
                Napier.d("文件下载完成，大小: ${downloadState.fileContent?.size ?: 0} 字节")
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
 * 聊天气泡
 */
@Composable
fun MessageBubble(isOwnMessage: Boolean, msg: MessageItem,
                  onVoiceMessageClick: (MessageContent.Voice) -> Unit = {},
                  onFileMessageClick: (MessageItem) -> Unit = {}) {

    val messageViewModel: ChatMessageViewModel = koinViewModel()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp,
                vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
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
