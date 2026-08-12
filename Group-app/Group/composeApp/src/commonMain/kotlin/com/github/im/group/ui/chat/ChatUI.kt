package com.github.im.group.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.github.im.group.api.AiBotApi
import com.github.im.group.ui.botConversation
import com.github.im.group.api.FriendshipDTO
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.UserAvatar
import com.github.im.group.ui.conversation
import com.github.im.group.ui.theme.ThemeTokens
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ConversationDisplayState
import com.github.im.group.viewmodel.UserViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatUI(
    navHostController: NavHostController,
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val userViewModel: UserViewModel = koinViewModel()

    val chatListState by chatViewModel.uiState.collectAsState()
    val searchResults by userViewModel.searchResults.collectAsState()
    val userInfo by userViewModel.currentLocalUserInfo.collectAsState()
    val friends by userViewModel.friends.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var userSearchQuery by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var actionConversation by remember { mutableStateOf<ConversationDisplayState?>(null) }

    DisposableEffect(lifecycleOwner, userInfo?.userId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userInfo?.userId?.let(chatViewModel::refreshCachedConversations)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(userInfo?.userId) {
        Napier.d { "Current user: $userInfo" }
        userInfo?.userId?.let { chatViewModel.loadConversations(it) }
    }

    LaunchedEffect(chatListState.realtimeHint?.conversationId, chatListState.realtimeHint?.unreadCount) {
        val hint = chatListState.realtimeHint ?: return@LaunchedEffect
        delay(4000)
        val latestHint = chatViewModel.uiState.value.realtimeHint
        if (latestHint?.conversationId == hint.conversationId &&
            latestHint.unreadCount == hint.unreadCount
        ) {
            chatViewModel.clearRealtimeHint(hint.conversationId)
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            friends = friends,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { groupName, desc, members ->
                showCreateGroupDialog = false
                scope.launch {
                    val createdConversation = chatViewModel.createGroupChat(groupName, desc, members)
                    navHostController.navigate(conversation(createdConversation.conversationId))
                }
            }
        )
    }

    actionConversation?.let { item ->
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { actionConversation = null },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ConversationActionSheet(
                conversation = item,
                onOpen = {
                    chatViewModel.clearRealtimeHint()
                    actionConversation = null
                    navHostController.navigate(conversation(item.conversation.conversationId))
                },
                onTogglePin = {
                    chatViewModel.togglePinConversation(item.conversation.conversationId)
                    actionConversation = null
                },
                onToggleMute = {
                    chatViewModel.toggleMuteConversation(item.conversation.conversationId)
                    actionConversation = null
                },
                onMuteEightHours = {
                    chatViewModel.muteConversationForEightHours(item.conversation.conversationId)
                    actionConversation = null
                },
                onMuteToday = {
                    chatViewModel.muteConversationUntilEndOfDay(item.conversation.conversationId)
                    actionConversation = null
                },
                onMuteForever = {
                    chatViewModel.muteConversationForever(item.conversation.conversationId)
                    actionConversation = null
                },
                onUnmute = {
                    chatViewModel.unmuteConversation(item.conversation.conversationId)
                    actionConversation = null
                },
                onMarkRead = {
                    chatViewModel.markConversationRead(item.conversation.conversationId)
                    actionConversation = null
                },
                onDismiss = { actionConversation = null }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeTokens.BackgroundDark)
    ) {
        if (chatListState.usedOfflineData) {
            OfflineStatusBanner()
        }

        AnimatedVisibility(
            visible = chatListState.realtimeHint != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            chatListState.realtimeHint?.let { hint ->
                RealtimeHintCard(
                    title = hint.title,
                    preview = hint.preview,
                    unreadCount = hint.unreadCount,
                    onOpen = {
                        chatViewModel.clearRealtimeHint()
                        navHostController.navigate(conversation(hint.conversationId))
                    },
                    onMarkRead = {
                        chatViewModel.markConversationRead(hint.conversationId)
                    },
                    onDismiss = { chatViewModel.clearRealtimeHint() }
                )
            }
        }

        if (chatListState.isSyncing && chatListState.conversations.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "正在同步会话列表…",
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.98f))
        ) {
            if (chatListState.isLoading && chatListState.conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                ConversationToolbar(
                    searchQuery = userSearchQuery,
                    unreadCount = chatListState.totalUnreadCount,
                    readAllInProgress = chatListState.readAllInProgress,
                    onQueryChange = {
                        userSearchQuery = it
                        userViewModel.searchUser(it)
                    },
                    onCreateGroup = {
                        userViewModel.loadFriendsIfNeeded()
                        showCreateGroupDialog = true
                    },
                    onOpenAssistant = {
                        scope.launch {
                            runCatching { AiBotApi.getConversation() }
                                .onSuccess { navHostController.navigate(botConversation(it.conversationId)) }
                                .onFailure { Napier.e("open durable assistant conversation failed", it) }
                        }
                    },
                    onMarkAllRead = {
                        chatViewModel.markAllConversationsRead()
                    }
                )

                if (userSearchQuery.isNotBlank()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { user ->
                            UserSearchItem(
                                user = user,
                                currentUser = userInfo,
                                onAddFriend = {}
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 72.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(chatListState.conversations, key = { it.conversation.conversationId }) { item ->
                            userInfo?.let { me ->
                                ChatItem(
                                    conversation = item,
                                    userInfo = me,
                                    onClick = {
                                        chatViewModel.clearRealtimeHint()
                                        navHostController.navigate(
                                            conversation(item.conversation.conversationId)
                                        )
                                    },
                                    onLongPress = {
                                        actionConversation = item
                                    },
                                    onMarkRead = {
                                        chatViewModel.markConversationRead(item.conversation.conversationId)
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color(0xFFF1F5F9)
                                )
                            }
                        }

                        if (chatListState.conversations.isEmpty() && !chatListState.isLoading) {
                            item { EmptyChatState() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationToolbar(
    searchQuery: String,
    unreadCount: Int,
    readAllInProgress: Boolean,
    onQueryChange: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenAssistant: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF7FBFF), Color.White)
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索联系人、群聊或消息", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = ThemeTokens.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = onCreateGroup) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = "创建群聊",
                        tint = ThemeTokens.PrimaryBlue
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF1F5F9),
                focusedContainerColor = Color(0xFFF1F5F9),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = ThemeTokens.PrimaryBlue.copy(alpha = 0.4f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = onOpenAssistant,
                label = {
                    Text("AI 助手")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFFF4F7EC),
                    labelColor = Color(0xFF365314)
                )
            )

            AssistChip(
                onClick = onMarkAllRead,
                enabled = unreadCount > 0 && !readAllInProgress,
                label = {
                    Text(
                        if (readAllInProgress) "处理中…" else "全部标为已读"
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFFEFF6FF),
                    labelColor = ThemeTokens.PrimaryBlue
                )
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF111827)
            ) {
                Text(
                    text = if (unreadCount > 0) "未读 $unreadCount" else "全部清爽",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RealtimeHintCard(
    title: String,
    preview: String,
    unreadCount: Int,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 10.dp,
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = preview,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFEF4444)
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            TextButton(onClick = onMarkRead) {
                Text("已读", color = Color.White)
            }
            TextButton(onClick = onDismiss) {
                Text("稍后", color = Color.White)
            }
        }
    }
}

@Composable
private fun OfflineStatusBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "网络有点不稳，当前先展示本地会话缓存。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EmptyChatState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有聊天会话",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "可以从联系人里开始一个新会话，或者创建一个群聊。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatItem(
    conversation: ConversationDisplayState,
    userInfo: UserInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onMarkRead: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            UserAvatar(username = conversation.conversation.getName(userInfo), size = 52)
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.conversation.getName(userInfo),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conversation.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ThemeTokens.PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "置顶",
                                color = ThemeTokens.PrimaryBlue,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (conversation.isMuted) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "免打扰",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = conversation.displayDateTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage.ifBlank { "暂无消息" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0) {
                        ThemeTokens.TextMain
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (conversation.unreadCount > 0) {
                        TextButton(
                            onClick = onMarkRead,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("已读", style = MaterialTheme.typography.labelMedium)
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 20.dp)
                                .background(Color(0xFFEF4444), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationActionSheet(
    conversation: ConversationDisplayState,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onMuteEightHours: () -> Unit,
    onMuteToday: () -> Unit,
    onMuteForever: () -> Unit,
    onUnmute: () -> Unit,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = conversation.conversation.groupName.ifBlank { "会话操作" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        ActionSheetItem(
            icon = Icons.Default.Search,
            title = "打开会话",
            description = "直接进入聊天页继续处理消息",
            onClick = onOpen
        )
        ActionSheetItem(
            icon = if (conversation.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
            title = if (conversation.isPinned) "取消置顶" else "置顶会话",
            description = if (conversation.isPinned) "恢复为按活跃时间排序" else "固定在会话列表顶部",
            onClick = onTogglePin
        )
        if (conversation.isMuted) {
            ActionSheetItem(
                icon = Icons.Default.Notifications,
                title = "恢复提醒",
                description = "重新接收这个会话的实时提示和通知",
                onClick = onUnmute
            )
        } else {
            ActionSheetItem(
                icon = Icons.Default.NotificationsOff,
                title = "8 小时免打扰",
                description = "适合临时专注，不影响明天继续提醒",
                onClick = onMuteEightHours
            )
            ActionSheetItem(
                icon = Icons.Default.NotificationsOff,
                title = "今天内免打扰",
                description = "今天剩余时间不再提醒，明天自动恢复",
                onClick = onMuteToday
            )
            ActionSheetItem(
                icon = Icons.Default.NotificationsOff,
                title = "永久免打扰",
                description = "保留未读数，但不再弹这个会话的提醒",
                onClick = onMuteForever
            )
        }
        if (conversation.unreadCount > 0) {
            ActionSheetItem(
                icon = Icons.Default.DoneAll,
                title = "标记已读",
                description = "立即清除这个会话的小红点",
                onClick = onMarkRead
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("取消")
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ThemeTokens.PrimaryBlue
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UserSearchItem(user: UserInfo, currentUser: UserInfo?, onAddFriend: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = user.username, size = 48)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (currentUser?.userId != user.userId) {
            Button(
                onClick = { onAddFriend(user.userId) },
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    friends: List<FriendshipDTO>,
    onDismiss: () -> Unit,
    onCreate: (groupName: String, desc: String?, members: List<UserInfo>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val selected = remember { mutableStateOf(setOf<Long>()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("创建群聊", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("群名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("群描述（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "选择成员（至少 2 人）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                val selectableFriends = friends.mapNotNull { it.friendUserInfo }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(selectableFriends, key = { it.userId }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        val next = selected.value.toMutableSet()
                                        if (!next.add(user.userId)) {
                                            next.remove(user.userId)
                                        }
                                        selected.value = next
                                    }
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = selected.value.contains(user.userId),
                                onCheckedChange = { checked ->
                                    val next = selected.value.toMutableSet()
                                    if (checked) next.add(user.userId) else next.remove(user.userId)
                                    selected.value = next
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            UserAvatar(username = user.username, size = 36)
                            Spacer(Modifier.width(10.dp))
                            Text(user.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    val canCreate = groupName.isNotBlank() && selected.value.size >= 2
                    Button(
                        onClick = {
                            val members = selectableFriends.filter { selected.value.contains(it.userId) }
                            onCreate(groupName.trim(), desc.trim().takeIf { it.isNotEmpty() }, members)
                        },
                        enabled = canCreate
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
