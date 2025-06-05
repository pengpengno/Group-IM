package com.github.im.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.github.im.group.model.Friend
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.FriendList
import com.github.im.group.ui.Main


class MainScreenPreview(
    userInfo: UserInfo,
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    onLogout: () -> Unit
): Screen {

    // 手动调用 Content()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val mockUser = UserInfo(userId = 1,  email = "preview@example.com", username = "预览用户")
        val mockFriends = listOf(
            Friend(friendId = 1, name = "张三", online = true),
            Friend(friendId = 2, name = "李四", online = false),
            Friend(friendId = 3, name = "王五", online = true),
        )

        // Voyager 的方式：手动创建 Screen 实例
        val screen = Main(
            userInfo = mockUser,
            friends = mockFriends,
            onFriendClick = {},
            onLogout = {}
        )

        Scaffold(
            containerColor = Color(0xFFEFEFEF),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "欢迎, ${mockUser.username}",
                            color = Color.White
                        )
                    },
                    actions = {
                        TextButton(onClick = {  }) {
                            Text("退出登录", color = Color.White)
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0088CC)
                    )
                )
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
            ) {
                FriendList(
                    friends = mockFriends,
                    onFriendClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            "请选择一个好友开始聊天",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }


}
