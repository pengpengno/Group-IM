package com.github.im.group

import androidx.compose.runtime.Composable
import com.github.im.group.ui.LoginScreen
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

@Composable
fun App() {
    Napier.base(DebugAntilog())
    LoginScreen()
}