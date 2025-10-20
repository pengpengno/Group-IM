package com.github.im.group.ui

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
import com.github.im.group.repository.UserState
import com.github.im.group.ui.chat.ImageMessage
import com.github.im.group.ui.chat.MessageContent
import com.github.im.group.ui.chat.SendingSpinner
import com.github.im.group.ui.chat.TextMessage
import com.github.im.group.ui.chat.VideoBubble
import com.github.im.group.ui.chat.VoiceMessage
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
    var remoteUser by remember { mutableStateOf<com.github.im.group.model.UserInfo?>(null) }


    LaunchedEffect(conversationId) {
        chatViewModel.getConversations(conversationId)
        messageViewModel.getConversation(conversationId)
        messageViewModel.loadMessages(conversationId)
        messageViewModel.register(conversationId)
    }


    // 加载完消息后自动滚动到底部

    val state by messageViewModel.uiState.collectAsState()

    val userInfo = userViewModel.getCurrentUser()
    
    // 获取VideoCallViewModel
    val videoCallViewModel: VideoCallViewModel = koinViewModel()

    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
        bottomBar = {
            ChatInputArea(
                onSendText = { text ->
                    messageViewModel.sendMessage(conversationId, text)
                },
                onStartRecording = {
                    voiceViewModel.startRecording(conversationId)
                },
                onStopRecording = { canceled, slideDirection ->
                    // 根据滑动方向处理不同的操作
                    when (slideDirection) {
                        SlideDirection.LEFT, SlideDirection.UP -> {
                            // 取消录音
                            voiceViewModel.cancel()
                        }
                        SlideDirection.RIGHT -> {
                            // 预览录音
                            voiceViewModel.stopRecording()

                        }
                        else -> {
                            // 默认 直接发送
                            voiceViewModel.stopRecording()
                            voiceViewModel.getVoiceData()?.let {
                                // voice-{conversationId}-{dateTime}.m4a
                                val fileName = "voice-$conversationId-" + Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault()) + ".m4a"
                                println("durationMillis is ${it.durationMillis}")
                                messageViewModel.sendVoiceMessage(conversationId, it.bytes,fileName,it.durationMillis)
                            }
                        }
                    }
                },
                onEmojiSelected = { emoji ->
                    // emoji 已经拼接在 ChatInputArea 内
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
                val content = msg.content
                if (userInfo != null) {
                    MessageBubble(
                        isOwnMessage = msg.userInfo.userId == userInfo.userId,
                        msg = msg,
                        onVoiceMessageClick = { voiceContent ->
                            // 处理语音消息点击事件，播放音频
                            // 这里需要根据实际的音频文件路径来播放
                            val audioUrl = "http://${ProxyConfig.host}:${ProxyConfig.port}/api/files/download/${voiceContent.audioUrl}"
                            // TODO: 实现实际的音频播放逻辑
                            println("播放音频: $audioUrl")
                        }
                    )
                }
            }
        }

        // 录音时遮罩
        val uiState by voiceViewModel.uiState.collectAsState()
        val amplitude by voiceViewModel.amplitude.collectAsState()
        if (uiState is com.github.im.group.viewmodel.RecorderUiState.Recording) {
            val recordingState = uiState as com.github.im.group.viewmodel.RecorderUiState.Recording
            RecordingOverlay(
                show = true,
                amplitude = amplitude,
                slideDirection = recordingState.slideDirection,
                onCancel = { voiceViewModel.cancel() },
                onPreview = { /* 预览功能 */ },
                onSend = { /* 发送功能 */
                    voiceViewModel.getVoiceData()?.let {
                        // voice-{conversationId}-{dateTime}.m4a
                        val fileName = "voice-$conversationId-" + kotlinx.datetime.Clock.System.now()
                            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()) + ".m4a"
                        println("durationMillis is ${it.durationMillis}")
                        messageViewModel.sendVoiceMessage(conversationId, it.bytes,fileName,it.durationMillis)
                    }
                }
            )
        }

        // 回放浮窗
        if (uiState is com.github.im.group.viewmodel.RecorderUiState.Playback) {
            val playback = uiState as com.github.im.group.viewmodel.RecorderUiState.Playback
            RecordingPlaybackOverlay(
                audioPlayer = voiceViewModel.audioPlayer,
                filePath = playback.filePath,
                onSend = {

                    voiceViewModel.getVoiceData()?.let {
                        // voice-{conversationId}-{dateTime}.m4a
                        val fileName = "voice-$conversationId-" + kotlinx.datetime.Clock.System.now()
                            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()) + ".m4a"
                        println("durationMillis is ${it.durationMillis}")
                        messageViewModel.sendVoiceMessage(conversationId, it.bytes,fileName,it.durationMillis)
                    }
                },
                duration = voiceViewModel.getVoiceData()?.durationMillis ?: 0,
                onCancel = { voiceViewModel.cancel() }
            )
        }
        
        // 视频通话界面
        if (showVideoCall) {
//            val videoCallState by videoCallViewModel.videoCallState.collectAsState()
            val localMediaStream by videoCallViewModel.localMediaStream

            VideoCallUI(
                navHostController = navHostController,
                remoteUser = remoteUser,
                localMediaStream = localMediaStream,
                onEndCall = { 
                    videoCallViewModel.endCall()
                    showVideoCall = false
                },
                onToggleCamera = { videoCallViewModel.toggleCamera() },
                onToggleMicrophone = { videoCallViewModel.toggleMicrophone() },
                onSwitchCamera = { videoCallViewModel.switchCamera() }
            )
        }
    }
}

/**
 * 聊天气泡
 */
@Composable
fun MessageBubble(isOwnMessage: Boolean, msg: MessageItem, onVoiceMessageClick: (MessageContent.Voice) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isOwnMessage && msg.status == MessageStatus.SENDING) {
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
                        val  duration = msg.fileMeta?.duration ?: 1
                        VoiceMessage(MessageContent.Voice(msg.content, duration)) {
                            onVoiceMessageClick(MessageContent.Voice(msg.content, duration))
                        }
                    }
                    MessageType.IMAGE -> ImageMessage(MessageContent.Image(msg.content))
                    MessageType.VIDEO -> VideoBubble(MessageContent.Video(msg.content))
                    MessageType.FILE -> FileMessageBubble(msg)
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
fun FileMessageBubble(msg: MessageItem) {
    val messageViewModel: ChatMessageViewModel = koinViewModel()

    // 显示文件消息
    val fileSize = messageViewModel.getFileMessageMeta(msg)?.size ?: 0
    val displaySize = if (fileSize > 1024 * 1024) {
        "${fileSize / 1024 / 1024}MB"
    } else if (fileSize > 1024) {
        "${fileSize / 1024}KB"
    } else {
        "${fileSize}B"
    }
    
    com.github.im.group.ui.chat.FileMessage(
        MessageContent.File(
            fileName = msg.content,
            fileSize = displaySize,
            fileUrl = "" // 这里需要文件URL
        )
    )
}