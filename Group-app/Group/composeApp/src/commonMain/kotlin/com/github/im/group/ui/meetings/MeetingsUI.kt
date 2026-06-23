package com.github.im.group.ui.meetings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.MeetingApi
import com.github.im.group.api.MeetingRes
import com.github.im.group.ui.video.MeetingLauncher
import com.github.im.group.viewmodel.MeetingsState
import com.github.im.group.viewmodel.MeetingsViewModel
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsUI(
    navHostController: NavHostController,
    viewModel: MeetingsViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val groupConversations by viewModel.groupConversations.collectAsState()
    val creating by viewModel.creating.collectAsState()
    val currentUser by userViewModel.currentLocalUserInfo.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var activeMeeting by remember { mutableStateOf<MeetingRes?>(null) }

    LaunchedEffect(currentUser?.userId) {
        currentUser?.userId?.let(viewModel::fetchMeetings)
    }

    if (showCreateDialog) {
        CreateMeetingDialog(
            conversations = groupConversations,
            currentUserId = currentUser?.userId,
            creating = creating,
            error = createError,
            onDismiss = {
                createError = null
                showCreateDialog = false
            },
            onCreate = { conversationId, title, participantIds ->
                createError = null
                viewModel.createMeeting(
                    conversationId = conversationId,
                    title = title,
                    participantIds = participantIds,
                    onCreated = {
                        createError = null
                        showCreateDialog = false
                        viewModel.fetchMeetings(currentUser?.userId)
                        activeMeeting = it
                    },
                    onError = { message ->
                        createError = message
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Meetings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchMeetings(currentUser?.userId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create meeting")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
        ) {
            when (val currentState = state) {
                is MeetingsState.Loading -> MeetingsLoadingView()
                is MeetingsState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(currentState.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.fetchMeetings(currentUser?.userId) }) {
                            Text("Retry")
                        }
                    }
                }
                is MeetingsState.Success -> {
                    if (currentState.meetings.isEmpty()) {
                        EmptyMeetingsView()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentState.meetings) { meeting ->
                                MeetingCard(meeting) {
                                    scope.launch {
                                        runCatching { MeetingApi.joinMeeting(meeting.roomId) }
                                        activeMeeting = meeting
                                    }
                                }
                            }
                        }
                    }
                }
                MeetingsState.Idle -> Unit
            }
        }
    }

    activeMeeting?.let { meeting ->
        MeetingLauncher(
            roomId = meeting.roomId,
            participantIds = meeting.participants
                .map { it.userId.toString() }
                .filter { it != currentUser?.userId?.toString() },
            onCallEnded = {
                val finalMeeting = activeMeeting
                activeMeeting = null
                if (finalMeeting != null) {
                    scope.launch {
                        if (finalMeeting.hostId == currentUser?.userId) {
                            MeetingApi.endMeeting(finalMeeting.roomId)
                        } else {
                            MeetingApi.leaveMeeting(finalMeeting.roomId)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun CreateMeetingDialog(
    conversations: List<ConversationRes>,
    currentUserId: Long?,
    creating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (Long, String, List<Long>) -> Unit
) {
    var selectedConversationId by remember(conversations) {
        mutableStateOf(conversations.firstOrNull()?.conversationId)
    }
    var title by remember(conversations) {
        mutableStateOf(conversations.firstOrNull()?.groupName.orEmpty())
    }

    val selectedConversation = conversations.firstOrNull { it.conversationId == selectedConversationId }
    val participantIds = selectedConversation?.members
        ?.filter { it.userId != currentUserId }
        ?.map { it.userId }
        .orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (conversations.isEmpty()) {
                    Text(
                        "No group conversations available yet. Create a group chat first, then come back here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Choose a group conversation as the meeting room entry point.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(conversations) { item ->
                            val selected = item.conversationId == selectedConversationId
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedConversationId = item.conversationId
                                        if (title.isBlank() || title == selectedConversation?.groupName.orEmpty()) {
                                            title = item.groupName
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = item.groupName.ifBlank { "Untitled group" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.members.size} members",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Meeting title") },
                        singleLine = true
                    )

                    Text(
                        text = "Participants: ${participantIds.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val conversationId = selectedConversationId ?: return@Button
                    val finalTitle = title.ifBlank {
                        selectedConversation?.groupName?.ifBlank { "Meeting" } ?: "Meeting"
                    }
                    onCreate(conversationId, finalTitle, participantIds)
                },
                enabled = !creating && selectedConversationId != null && participantIds.isNotEmpty()
            ) {
                if (creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MeetingCard(meeting: MeetingRes, onClick: () -> Unit) {
    val statusColor = when (meeting.status) {
        "ACTIVE" -> Color(0xFF10B981)
        "SCHEDULED" -> Color(0xFF2563EB)
        "ENDED" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline
    }

    val statusText = when (meeting.status) {
        "ACTIVE" -> "In Progress"
        "SCHEDULED" -> "Scheduled"
        "ENDED" -> "Ended"
        else -> meeting.status ?: "Unknown"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                if (meeting.scheduledAt != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = meeting.scheduledAt.toString().replace("T", " ").take(16),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = meeting.title ?: "Untitled meeting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Room ID: ${meeting.roomId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row {
                    meeting.participants.take(3).forEachIndexed { index, participant ->
                        Surface(
                            modifier = Modifier
                                .size(26.dp)
                                .padding(start = if (index == 0) 0.dp else 0.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = participant.username?.take(1)?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (index < meeting.participants.take(3).lastIndex) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }

                if (meeting.participants.size > 3) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${meeting.participants.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "${meeting.participants.size} joined",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                if (meeting.status != "ENDED") {
                    Button(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingsLoadingView() {
    val transition = rememberInfiniteTransition(label = "meetings-loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meetings-loading-alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(18.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(22.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(16.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE2E8F0))
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .alpha(alpha)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBEAFE))
                            )
                            if (it < 2) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(32.dp)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD1FAE5))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMeetingsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoCall,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No meetings yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use the + button to create an instant meeting from one of your group conversations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
