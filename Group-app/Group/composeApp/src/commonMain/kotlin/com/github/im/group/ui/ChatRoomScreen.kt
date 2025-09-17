package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.ui.chat.MessageContent
import com.github.im.group.ui.chat.TextMessage
import com.github.im.group.ui.chat.VoiceMessage
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.RecorderUiState
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
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
    val userInfo = userViewModel.getUser()
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
                onStopRecording = {
                    voiceViewModel.stopRecording()

                },
                onEmojiSelected = { emoji ->
                    // emoji 已经拼接在 ChatInputArea 内
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
                        content = content
                    )
                }
            }
        }

        // 录音时遮罩
        if (uiState is RecorderUiState.Recording) {
            RecordingOverlay(
                show = true,
                amplitude = amplitude,
                isCanceling = { voiceViewModel.cancel() }
            )
        }

        // 回放浮窗
        if (uiState is RecorderUiState.Playback) {
            val playback = uiState as RecorderUiState.Playback
            RecordingPlaybackOverlay(
                audioPlayer = voiceViewModel.audioPlayer,
                filePath = playback.filePath,
                onSend = {

                    voiceViewModel.getVoiceData()?.let {
                        // voice-{conversationId}-{dateTime}.m4a
                        val fileName = "voice-$conversationId-" + Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault()) + ".m4a"
                        println("durationMillis is ${it.durationMillis}")
                        messageViewModel.sendVoiceMessage(conversationId, it.bytes,fileName,it.durationMillis)
                    }
                },
                duration = voiceViewModel.getVoiceData()?.durationMillis ?: 0,
                onCancel = { voiceViewModel.cancel() }
            )
        }
    }
}

/**
 * 聊天气泡
 */
@Composable
fun MessageBubble(isOwnMessage: Boolean, msg : MessageItem, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isOwnMessage) Color(0xFFB3E5FC) else Color(0xFFF0F0F0),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp
        ) {
            val type = msg.type
            when(type)
                {
                    MessageType.TEXT -> TextMessage(MessageContent.Text(msg.content))
                    MessageType.VOICE -> VoiceMessage(MessageContent.Voice(msg.content,1),{})
                    MessageType.FILE -> println(msg)
                    MessageType.VIDEO -> TODO()
                    MessageType.IMAGE -> TODO()
            }

        }
    }
}

