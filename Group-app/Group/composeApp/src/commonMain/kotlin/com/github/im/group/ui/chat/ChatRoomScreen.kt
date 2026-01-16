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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.navigation.compose.rememberNavController
import com.github.im.group.api.FileMeta
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.toFile
import com.github.im.group.model.MessageItem
import com.github.im.group.sdk.File
import com.github.im.group.sdk.MediaFileView
import com.github.im.group.sdk.GalleryAwareMediaFileView
import com.github.im.group.ui.chat.MessageMediaManager

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
                                //TODO  获取在线状态接口
//
//                                Text(
//                                    text = "在线",
//                                    color = Color.Green,
//                                    fontSize = 12.sp
//                                )
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
                    items(state.messages, key = { message ->
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
                                messageViewModel.sendTextMessage(conversationId, text)
                            }
                        },
                        onRelease = {
                            //  停止后直接发送
                            voiceViewModel.getVoiceData()?.let {
                                    messageViewModel.sendVoiceMessage(conversationId,
                                    it)
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

    }
}

/**
 * 通用文件消息加载组件
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
    messageViewModel: ChatMessageViewModel,
    maxDownloadSize: Long = 50 * 1024 * 1024, // 默认50MB
    allMessages: List<MessageItem>? = null, // 添加所有消息列表参数
    onContentReady: @Composable (File, FileMeta) -> Unit,
    onLoading: @Composable () -> Unit,
    onError: @Composable (() -> Unit)? = null
) {

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
            file = messageViewModel.getFile(fileId)
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
                val fileExists = messageViewModel.isFileExists(fileId)
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
    val downloadStates by messageViewModel.fileDownloadStates.collectAsState()
    LaunchedEffect(downloadStates) {
        downloadStates[fileId]?.let { state ->
            if (state.isSuccess && !state.isDownloading) {
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
