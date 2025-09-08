package com.github.im.group

import com.github.im.group.api.LoginApi
import com.github.im.group.config.SocketClient
import com.github.im.group.connect.AndroidSocketClient
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecorderFactory
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.TCPMessageViewModel
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {

    single<LoginApi> { LoginApi }

    single<FilePicker> { AndroidFilePicker(androidContext()) }

    single { Greeting() }
    single { UserRepository() }
    viewModelOf (::ChatViewModel)
    viewModelOf (::ChatMessageViewModel)
    single { ChatSessionManager() }
    single { TCPMessageViewModel(get()) }
    single { VoiceRecorderFactory.create()}
    single { AndroidSocketClient(get()) } bind SocketClient::class
    viewModelOf (::VoiceViewModel)
    single { (SenderSdk(get())) }


    viewModelOf(::UserViewModel)  // 注册为 ViewModel，由 Koin 自动管理生命周期



}