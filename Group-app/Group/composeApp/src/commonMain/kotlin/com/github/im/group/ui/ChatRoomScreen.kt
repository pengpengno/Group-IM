package com.github.im.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.UserViewModel
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
    val messageViewModel: ChatMessageViewModel = koinViewModel()
    var messageText by remember { mutableStateOf("") }



    LaunchedEffect(conversationId) {
        chatViewModel.getConversations(conversationId)
        messageViewModel.getConversation(conversationId)
        messageViewModel.loadMessages(conversationId)
        messageViewModel.register(conversationId)
//        onDispose {
//            messageViewModel.unregister(conversationId)
//        }
    }
//    onDispose {
//        messageViewModel.unregister(conversationId)
//    }

    // 加载完消息后自动滚动到底部


    val state by messageViewModel.uiState.collectAsState()
    val userInfo = userViewModel.getUser()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

//    val tcpViewModel: TCPMessageViewModel = GlobalContext.get()
//    LaunchedEffect(Unit) {
//        tcpViewModel.uiState.collect { message ->
//            // 根据类型主动通知 ChatMessageViewModel
//            when {
//                message.message != null -> {
//                    messageViewModel.addMessage(message.message)
//                }
//            }
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.conversation.getName(userInfo), color = Color.White) },
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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息") },
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            messageViewModel.sendMessage(conversationId, messageText)
                            // 发送完毕后展示再页面上
                            messageText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC))
                ) {
                    Text("发送", color = Color.White)
                }
            }
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
                        content = content
                    )
                }
            }
        }
//        LazyColumn(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//                .background(Color.White),
//            verticalArrangement = Arrangement.Bottom
//        ) {
//            Spacer(modifier = Modifier.height(8.dp))
//            state.messages.forEach { msg ->
//                msg.content.let {
//                    if (userInfo != null) {
//                        MessageBubble(
//                            isOwnMessage = msg.userInfo.userId == userInfo.userId,
//                            content = it
//                        )
//                    }
//                }
//            }
//            Spacer(modifier = Modifier.height(70.dp)) // 留出输入框空间
//        }
    }
}

/**
 * 聊天气泡
 */
@Composable
fun MessageBubble(isOwnMessage: Boolean, content: String) {
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
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.Black
            )
        }
    }
}