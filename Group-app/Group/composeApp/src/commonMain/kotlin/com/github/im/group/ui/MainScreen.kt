package com.github.im.group.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.github.im.group.model.Friend
import com.github.im.group.model.UserInfo

@OptIn(ExperimentalMaterial3Api::class)

class Main (
    private val userInfo: UserInfo,
    private val friends: List<Friend>,
    private val onFriendClick: (Friend) -> Unit,
    private val onLogout: () -> Unit
):Screen{
    @Composable
    override fun Content() {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("欢迎, ${userInfo.username}") },
                    actions = {
                        TextButton(onClick = onLogout) {
                            Text("退出登录", color = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FriendList(friends = friends, onFriendClick = onFriendClick, modifier = Modifier.weight(1f))
                // 聊天窗口预留区域
                Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                    Text(
                        "请选择一个好友开始聊天",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}



@Composable
fun FriendList(friends: List<Friend>, onFriendClick: (Friend) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxHeight()) {
        items(friends) { friend ->
            FriendItem(friend = friend, onClick = { onFriendClick(friend) })
            Divider()
        }
    }
}

@Composable
fun FriendItem(friend: Friend, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像示意，替换成真实图片加载逻辑
//        Image(
////            painter = painterResource("drawable/ic_avatar_placeholder.xml"),
//            painter = Icons.Default.Person,
//            contentDescription = "avatar",
//            modifier = Modifier.size(40.dp)
//        )
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "avatar",
            tint = Color.Gray, // 可以自定义颜色
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(friend.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (friend.online) "在线" else "离线",
                style = MaterialTheme.typography.bodySmall,
                color = if (friend.online) Color.Green else Color.Gray
            )
        }
    }
}


