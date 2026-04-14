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
            ChatRoomTopBar(
                roomName = chatUiState.getRoomName(),
                roomSubtitle = roomSubtitle,
                isGroupConversation = isGroupConversation,
                remoteUser = remoteUser,
                onBack = onBack,
                onStartVideoCall = {
                    if (isGroupConversation) {
                        val conversation = chatUiState.conversation ?: return@ChatRoomTopBar
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
                }
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
