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
//@Preview
//fun App() {
//    LoginScreen().Content()
//}
