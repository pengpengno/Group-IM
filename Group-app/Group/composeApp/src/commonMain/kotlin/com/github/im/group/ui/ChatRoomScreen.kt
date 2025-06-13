package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.api.ConversationRes
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.UserViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
//    conversation: ConversationRes,
    conversationId: Long,
    navHostController: NavHostController = rememberNavController(),
    userViewModel: UserViewModel,
//    userInfo: UserInfo,
    onBack: () -> Unit = {}
) {
    val chatViewModel: ChatViewModel = koinViewModel()
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    init {
        chatViewModel.getConversations(conversationId)
//        messageViewModel.loadMessages(conversationId)
    }

    val conversation by chatViewModel.uiState.collectAsState()

    val messages by messageViewModel.uiState.collectAsState()
    val userInfo  = userViewModel.getUser()
    LaunchedEffect(conversation.conversationId) {
        chatViewModel.loadMessages(conversation.conversationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation.getName(userInfo), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0088CC))
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendMessage(conversation.conversationId, messageText)
                        messageText = ""
                    }
                }) {
                    Text("发送")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.White)
        ) {
            messages.forEach { msg ->
                msg.content?.let {
                    if (userInfo != null) {
                        MessageBubble(
                            isOwnMessage = msg.fromAccountId == userInfo.userId,
                            content = it
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(isOwnMessage: Boolean, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isOwnMessage) Color(0xFFDCF8C6) else Color(0xFFECECEC),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(12.dp),
                color = Color.Black
            )
        }
    }
}
