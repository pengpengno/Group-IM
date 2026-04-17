package com.github.im.group.ui.meetings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.im.group.api.MeetingRes
import com.github.im.group.ui.conversation
import com.github.im.group.viewmodel.MeetingsState
import com.github.im.group.viewmodel.MeetingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsUI(
    navHostController: NavHostController,
    viewModel: MeetingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchMeetings()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "会议中心",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchMeetings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            when (val currentState = state) {
                is MeetingsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MeetingsState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentState.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.fetchMeetings() }) {
                            Text("重试")
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
                                    // 点击进入会话或加入会议
                                    navHostController.navigate(conversation(meeting.conversationId))
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun MeetingCard(meeting: MeetingRes, onClick: () -> Unit) {
    val statusColor = when (meeting.status) {
        "ACTIVE" -> Color(0xFF10b981)
        "SCHEDULED" -> Color(0xFF3b82f6)
        else -> MaterialTheme.colorScheme.outline
    }
    
    val statusText = when (meeting.status) {
        "ACTIVE" -> "进行中"
        "SCHEDULED" -> "已预定"
        "ENDED" -> "已结束"
        else -> meeting.status ?: "未知"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                            text = meeting.scheduledAt.toString().replace("T", " ").substring(0, 16),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = meeting.title ?: "无标题会议",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "房间 ID: ${meeting.roomId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 参与者头像堆叠效果 (简单模拟)
                Row {
                    meeting.participants.take(3).forEachIndexed { index, p ->
                        Surface(
                            modifier = Modifier
                                .size(24.dp)
                                .offset(x = (index * (-8)).dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = p.username?.take(1)?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                val extra = meeting.participants.size - 3
                if (extra > 0) {
                    Text(
                        text = "+$extra ",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.offset(x = (-12).dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (meeting.participants.isNotEmpty()) 4.dp else 0.dp))
                
                Text(
                    text = "${meeting.participants.size} 人参与",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (meeting.status != "ENDED") {
                    Button(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("加入", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMeetingsView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VideoCall,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无会议记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = "您可以在聊天室发起会议或预定会议",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
