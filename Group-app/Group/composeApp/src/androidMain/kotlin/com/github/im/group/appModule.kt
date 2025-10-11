package com.github.im.group

import android.content.Context
import com.github.im.group.api.LoginApi
import com.github.im.group.config.SocketClient
import com.github.im.group.connect.AndroidSocketClient
import com.github.im.group.db.AndroidDatabaseDriverFactory
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.AndroidAudioPlayer
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecorderFactory
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.TCPMessageViewModel
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {

    single<LoginApi> { LoginApi }

    single { AndroidDatabaseDriverFactory(get<Context>()) }  // 注册工厂
    single { get<AndroidDatabaseDriverFactory>().createDatabase() }  // 注册 AppDatabase 单例

    single<FilePicker> { AndroidFilePicker(androidContext()) }
    single<AudioPlayer> { AndroidAudioPlayer(androidContext()) }

    single { UserRepository(get()) }
    single { ChatMessageRepository(get()) }
    viewModelOf (::ChatViewModel)
    
    // 为ChatMessageViewModel添加所有必需的依赖项
    viewModel { 
        ChatMessageViewModel(
            userViewModel = get(),
            chatSessionManager = get(),
            chatMessageRepository = get(),
            senderSdk = get(),
            filePicker = get()  // 注入FilePicker依赖
        )
    }
    
    single { ChatSessionManager(get()) }
    single { TCPMessageViewModel(get()) }
    single { VoiceRecorderFactory.create()}
    single { AndroidSocketClient(get()) } bind SocketClient::class
    viewModel {
        VoiceViewModel(
            voiceRecorder = get(),
            audioPlayer = get()
        )
    }
    single { (SenderSdk(get())) }

    viewModelOf(::UserViewModel)  // 注册为 ViewModel，由 Koin 自动管理生命周期

}