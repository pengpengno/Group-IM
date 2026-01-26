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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.api.FileMeta
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.getFile
import com.github.im.group.manager.isFileExists
import com.github.im.group.manager.toFile
import com.github.im.group.model.MessageItem
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.File
import com.github.im.group.sdk.MediaFileView
import com.github.im.group.ui.ChatRoom
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.video.VideoCallLauncher
import com.github.im.group.viewmodel.ChatRoomViewModel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.SessionCreationState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject
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

//        when(val room = chatRoom){
//            is ChatRoom.CreatePrivate -> {
//                // 设置初始状态为Idle，表示有待创建的会话
//                chatRoomViewModel.clearSessionCreationError()
//            }
//
//            is ChatRoom.Conversation -> {
//                conversationId = room.conversationId
//            }
//        }
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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 显示对方用户头像和昵称或群组名称
                        chatUiState.getRoomName().let {
                            UserAvatar(
                                username = it,
                                size = 32,
                            )

                            Spacer(modifier = Modifier.width(8.dp))
                            //
                            Column {
                                Text(
                                    text = it,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                            }
                        }


                        Spacer(modifier = Modifier.weight(1f))
                        Napier.i { "remoteUser  is  ...... $remoteUser" }
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
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // 消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true, // 最新消息在底部
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 8.dp, end = 8.dp) // 为输入区域留出空间
                ) {

                    // 显示历史加载指示器
                    if (chatUiState.loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ElevatedCard {
                                    Text("加载中...", modifier = Modifier.padding(12.dp))
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

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 100.dp) // 位于输入区域上方
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.Red)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = chatUiState.error ?: "会话创建遇到问题",
                                color = Color.White,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    chatRoomViewModel.clearSessionCreationError()
//                                    chatRoomViewModel.retryPendingSession()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                            ) {
                                Text(
                                    text = "重试",
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // 显示会话创建过程中的加载状态
                if (chatUiState.sessionCreationState is SessionCreationState.Creating) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 100.dp) // 位于输入区域上方
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.Blue)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "正在创建会话...",
                                color = Color.White,
                                fontSize = 14.sp
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
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // 对方消息显示头像在左侧
            if (showAvatar) {
                UserAvatar(
                    username = msg.userInfo.username,
                    size = 36
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                // 不显示头像时保留占位空间
                Spacer(modifier = Modifier.width(42.dp))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        // 显示消息时间戳（仅对本人发送的消息）
                        Text(
                            text = msg.time.toString().substring(11, 16), // 只显示小时和分钟
                            fontSize = 10.sp,
                            color = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(top = 4.dp)
//                                .align(Alignment.BottomEnd)
                        )
                    }

                    // 显示消息状态图标（仅对本人发送的消息）
                    if (isOwnMessage) {
                        Row(
                            modifier = Modifier
//                                .align(Alignment.CenterVertically)
                                .padding(end = 4.dp)
                        ) {
                            when (msg.status) {
                                MessageStatus.SENDING -> {
                                    // 发送中状态（已存在）
                                }
                                MessageStatus.SENT -> {
                                    // 已发送
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已发送",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(start = 2.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
//                            MessageStatus.DELIVERED -> {
//                                // 已送达
//                                Icon(
//                                    imageVector = Icons.Default.CheckCircle,
//                                    contentDescription = "已送达",
//                                    modifier = Modifier
//                                        .size(12.dp)
//                                        .padding(start = 4.dp),
//                                    tint = Color.Blue // 已读状态
//                                )
//                            }
                                MessageStatus.READ -> {
                                    // 已读
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "已读",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(start = 2.dp),
                                        tint = Color.Green // 已读状态
                                    )
                                }
                                MessageStatus.FAILED -> {
                                    // 发送失败
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "发送失败",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(start = 2.dp),
                                        tint = Color.Red
                                    )
                                }
//                            MessageStatus.PENDING -> {
//                                // 待发送（离线消息）
//                                Icon(
//                                    imageVector = Icons.Default.AccessTime,
//                                    contentDescription = "待发送",
//                                    modifier = Modifier
//                                        .size(12.dp)
//                                        .padding(start = 4.dp),
//                                    tint = Color.Orange
//                                )
//                            }
                                else -> {
                                    // 其他状态显示灰色勾
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已发送",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(start = 2.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

}

/**
 * 通用文件消息加载组件
 * 
 * 逻辑1: 获取文件元数据
 * 逻辑2: 检查文件大小是否超出限制
 * 逻辑3: 检查本地文件是否存在
 * 逻辑4: 根据需要触发下载流程
 * 逻辑5: 监听下载状态并更新UI
 * 
 * @param msg 消息对象
 * @param messageViewModel 消息视图模型
 * @param maxDownloadSize 最大下载大小限制（字节），默认为50MB
 * @param allMessages 所有消息列表，用于构建媒体画廊
 * @param onContentReady 内容准备就绪回调
 * @param onLoading 加载中状态
 * @param onError 错误状态（可选）
 */
@Composable
fun FileMessageLoader(
    msg: MessageItem,
    messageViewModel: ChatRoomViewModel,
    maxDownloadSize: Long = 50 * 1024 * 1024, // 默认50MB
    allMessages: List<MessageItem>? = null, // 添加所有消息列表参数
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
    var showDownloadButton by remember { mutableStateOf(false) }  // 新增状态：显示下载按钮
    val fileId = msg.content

    LaunchedEffect(msg) {
        try {
            // 异步获取文件元数据（这是所有文件类型都需要的步骤）
            val meta = messageViewModel.getFileMessageMetaAsync(msg)
            file = fileStorageManager.getFile(fileId)
            fileMeta = meta
            Napier.d { "查询文件的 元数据：$meta  file $file" }

            meta?.let { fileMeta ->
                // 检查文件大小是否超过限制
                if (fileMeta.size > maxDownloadSize) {
                    // 文件太大，不自动下载，显示下载按钮
                    showDownloadButton = true
                    file = fileMeta.toFile() // 使用HTTP链接作为数据源
                    isLoading = false
                    return@LaunchedEffect
                }

                // 检查文件是否存在，如果不存在 则 获取其 下载链接 作为数据源
                val fileExists = fileStorageManager.isFileExists(fileId)
                if (!fileExists) {
                    // 不存在则标记需要下载
                    shouldDownload = true
                    file = fileMeta.toFile()
                } else {
                    val path = messageViewModel.getLocalFilePath(fileId)
                    path?.let { file = fileMeta.toFile(it) }
                }

                Napier.d { "文件存在 ：$fileExists 文件ID： ${fileMeta.fileId} 的数据源为：${file?.data}" }
            }

            isLoading = false
        } catch (e: Exception) {
            hasError = true
            isLoading = false
            Napier.e("加载文件消息失败", e)
        }
    }

    // 当需要下载时，启动下载流程
    LaunchedEffect(shouldDownload) {
        if (shouldDownload) {
            try {
                messageViewModel.downloadFileMessage(fileId)
            } catch (e: Exception) {
                Napier.e("下载文件失败", e)
                hasError = true
            }
        }
    }

    // 监听下载状态变化，更新文件对象
    val downloadStates by  messageViewModel.fileDownloadStates.collectAsState()
    LaunchedEffect(downloadStates) {
        downloadStates[fileId]?.let { chatUiState ->
            if (chatUiState.isSuccess && !chatUiState.isDownloading) {
                // 下载完成后，获取本地路径并更新文件对象
                val path = messageViewModel.getLocalFilePath(fileId)
                path?.let {
                    file = fileMeta?.toFile(it)
                }
            }
        }
    }

    when {
        hasError -> {
            onError?.invoke() ?: Text("文件过大或加载失败")
        }
        isLoading -> {
            onLoading()
        }
        showDownloadButton -> {
            // 文件太大，显示缩略图和下载按钮
            file?.let { fileObj ->
                fileMeta?.let { meta ->
                    // 显示缩略图和下载按钮
                    Box {
                        MediaFileView(
                            file = fileObj,
                            modifier = Modifier.size(120.dp),
                            onDownloadFile = { fileId ->
                                // 启动下载
                                shouldDownload = true
                            }
                        )

                        // 显示文件大小和下载图标
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "下载",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // 显示文件大小
                        Text(
                            text = "${(meta.size / 1024 / 1024)}MB",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        file != null  && fileMeta !=null-> {
            onContentReady(file!!, fileMeta!!)
        }
        else -> {
            onLoading()
        }
    }
}