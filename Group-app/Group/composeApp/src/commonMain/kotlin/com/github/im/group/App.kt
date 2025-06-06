package com.github.im.group

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.github.im.group.config.appModule
import com.github.im.group.ui.LoginScreen
import org.koin.compose.KoinApplication

@Composable
//@Preview
fun App() {

    KoinApplication(application = {
        modules(appModule)
    }){
        Navigator(LoginScreen())
    }

}