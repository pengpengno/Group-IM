package com.github.im.group

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        GlobalCredentialProvider.storage = AndroidCredentialStorage(applicationContext)

        setContent {
            App()
        }
    }
}
//
//@Composable
//fun App() {
//    Navigator(LoginScreen()
////    { userInfo, navigator ->
////        // 模拟好友数据
////        val mockFriends = listOf(
////            Friend(1, "Alice", true),
////            Friend(2, "Bob", true)
////        )
////
////        // 直接 push 到主界面
////        navigator.push(MainScreen(userInfo, mockFriends,
////            onFriendClick = {},
////            onLogout = {
////                navigator.popUntilRoot() // 登出返回登录页
////            }
////        ) as Screen)
////    }
//    )
//}
