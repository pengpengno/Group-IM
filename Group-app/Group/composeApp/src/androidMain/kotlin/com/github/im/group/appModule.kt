package com.github.im.group

import android.content.Context
import com.github.im.group.api.LoginApi
import com.github.im.group.config.SocketClient
import com.github.im.group.connect.AndroidSocketClient
import com.github.im.group.db.AndroidDatabaseDriverFactory
import com.github.im.group.listener.ConnectionLoginListener
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.manager.LoginStateListener
import com.github.im.group.manager.UserDataSyncListener
import com.github.im.group.listener.WebRTCLoginListener
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.AndroidAudioPlayer
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.AndroidWebRTCManager
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.FileStorageManager
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecorderFactory
import com.github.im.group.sdk.WebRTCManager
import com.github.im.group.ui.video.VideoCallViewModel
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.TCPMessageViewModel
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import okio.FileSystem
import okio.Path.Companion.toPath
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
    single<WebRTCManager> { AndroidWebRTCManager(androidContext()) }

    single { UserRepository(get()) }
    single { ChatMessageRepository(get()) }
    single { FilesRepository(get()) }
    single { 
        FileStorageManager(
            filesRepository = get(),
            fileSystem = FileSystem.SYSTEM,
            baseDirectory = androidContext().filesDir.absolutePath.toPath()
        ) 
    }
    viewModelOf (::ChatViewModel)
    
    // 为ChatMessageViewModel添加所有必需的依赖项
    viewModel { 
        ChatMessageViewModel(
            get(),
            chatSessionManager = get(),
            chatMessageRepository = get(),
            senderSdk = get(),
            filePicker = get() ,
            fileStorageManager = get()
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

    single { SenderSdk(get(),get()) }
    // 登录状态管理器和相关监听器
//    // 可以继续添加其他LoginStateListener实现
//    single<LoginStateListener> { UserDataSyncListener(get()) }
//    single<LoginStateListener> { ConnectionLoginListener(get()) }
//    single<LoginStateListener> { WebRTCLoginListener(get()) }

    single<List<LoginStateListener>> {
        listOf(
            UserDataSyncListener(get()),
            ConnectionLoginListener(get()),
            WebRTCLoginListener(get())
        )
    }
    single {

       val manager =  LoginStateManager(get())
//        getAll<LoginStateListener>().forEach { listener ->
//            Napier.log(LogLevel.INFO, message="Adding login state listener: ${listener::class.simpleName}")
//            manager.addListener(listener)
//        }

        get<List<LoginStateListener>>().forEach { manager.addListener(it) }
        manager
    }


    viewModelOf(::UserViewModel)  // 注册为 ViewModel，由 Koin 自动管理生命周期
    
    // 注册VideoCallViewModel
    viewModel { 
        val vm = VideoCallViewModel(get())
        // 注入WebRTC管理器
        vm.setWebRTCManager(get())
        vm
    }
    

}