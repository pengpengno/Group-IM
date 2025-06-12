package com.github.im.group

import com.github.im.group.api.LoginApi
import com.github.im.group.api.SpaceXApi
import com.github.im.group.config.SocketClient
import com.github.im.group.connect.AndroidSocketClient
import com.github.im.group.db.SpaceXSDK
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.TCPMessageViewModel
import com.github.im.group.viewmodel.UserViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<SpaceXApi> { SpaceXApi() }
    single<SpaceXSDK> {
        SpaceXSDK(
            databaseDriverFactory = AndroidDatabaseDriverFactory(
                androidContext()
            ), api = get()
        )
    }

    single<LoginApi> { LoginApi }

    single { Greeting() }
    single { (ChatViewModel()) }
    single { (TCPMessageViewModel()) }
    single { AndroidSocketClient(get()) } bind SocketClient::class
    single { (SenderSdk(get())) }
    single { (UserViewModel(get())) } // 注册为 ViewModel，由 Koin 自动管理生命周期


}