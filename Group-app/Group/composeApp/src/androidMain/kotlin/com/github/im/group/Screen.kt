package com.github.im.group

sealed class Screen {
    object Login : Screen()
    object Main : Screen()
//    data class Main(val userInfo: UserInfo) : Screen()
}
