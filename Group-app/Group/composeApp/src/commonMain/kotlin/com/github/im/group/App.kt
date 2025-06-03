package com.github.im.group

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.github.im.group.model.UserInfo
import com.github.im.group.ui.MainScreen

@Composable
@Preview
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    val loginResponse = remember { mutableStateOf<UserInfo?>(null) }

    if (!isLoggedIn) {
        LoginScreen { userInfo ->
            loginResponse.value = userInfo
            isLoggedIn = true
        }
    } else {
        loginResponse.value?.let { response ->
            MainScreen(
                userInfo = response,
                friends = emptyList(),
                onFriendClick = { /* 示例中的空操作 */ },
                onLogout = { isLoggedIn = false }
            )
        }
    }
}