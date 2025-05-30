package com.github.im.group

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Group",
        undecorated = true
    ) {

        MaterialTheme { // ← 这里全局包裹 MaterialTheme
            App()
        }
    }
}