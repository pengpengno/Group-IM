package com.github.im.group

import ChatMessageBuilder
import ChatMessageBuilderImpl
import android.content.Context
import android.os.Environment
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
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.FriendRequestRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.AndroidAudioPlayer
import com.github.im.group.sdk.AndroidFilePicker
import com.github.im.group.sdk.AndroidWebRTCManager
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.FilePicker
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.FileUploadService
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.manager.VoiceFileManager
import com.github.im.group.sdk.AndroidVoiceRecorder
import com.github.im.group.sdk.VoiceRecorder
import com.github.im.group.sdk.WebRTCManager
import com.github.im.group.ui.video.VideoCallViewModel
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ContactsViewModel
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

val appmodule = module {


    single { AndroidDatabaseDriverFactory(get<Context>()) }  // 注册工厂
    single { get<AndroidDatabaseDriverFactory>().createDatabase() }  // 注册 AppDatabase 单例

    single<FilePicker> { AndroidFilePicker(androidContext()) }
    single<AudioPlayer> { AndroidAudioPlayer(androidContext()) }
    single<WebRTCManager> { AndroidWebRTCManager(androidContext()) }

    single { UserRepository(get()) }
    single { ChatMessageRepository(get(),get()) }
    single { FilesRepository(get()) }
    single { FriendRequestRepository(get()) }
    single { MessageSyncRepository(get(), get(), get(),get()) }

    single { ConversationRepository(get()) }

    single {
        val context = androidContext()

        FileStorageManager(
            filesRepository = get(),
            fileSystem = FileSystem.SYSTEM,
            baseDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath?.toPath()
                ?: context.filesDir.absolutePath.toPath(),
        )
    }
    
    single {
        val context = androidContext()
        val baseDirectory = context.filesDir.absolutePath.toPath()
        
        // 创建语音文件管理器
        val voiceFileManager = VoiceFileManager(
            fileSystem = FileSystem.SYSTEM,
            baseDirectory = baseDirectory
        )

        voiceFileManager
    }

    // 为ChatViewModel添加所有必需的依赖项
    viewModel {
        ChatViewModel(
            tcpClient = get(),
            userRepository = get(),
            filePicker = get(),
            loginStateManager = get(),
            messageRepository = get(),
        )
    }

    // 为ChatMessageViewModel添加所有必需的依赖项
    viewModel {
        ChatMessageViewModel(
            get(),
            chatSessionManager = get(),
            chatMessageRepository = get(),
            messageSyncRepository = get(),
            filesRepository = get(), // 添加文件仓库依赖
            conversationRepository = get(),
            senderSdk = get(),
            filePicker = get(),
            fileStorageManager = get(),
            chatMessageBuilder = get(),
            fileUploadService = get()
        )
    }


    single { FileUploadService(
        filePicker = get(),
        userRepository = get(),
        filesRepository = get(),
        chatMessageRepository = get(),
        fileStorageManager = get()
    ) }
    factory<ChatMessageBuilder> { ChatMessageBuilderImpl(
        userRepository = get(),
    ) }

    single { ChatSessionManager(get(),get()) }
    single { TCPMessageViewModel(get()) }
    single {
        AndroidVoiceRecorder(
            androidContext(),
            get(),
        )
    } bind VoiceRecorder::class

    single { AndroidSocketClient(get()) } bind SocketClient::class
    viewModel {
        VoiceViewModel(
            voiceRecorder = get(),
            audioPlayer = get(),
//            filesRepository = get()
        )
    }

    single { SenderSdk(get(),get()) }
    // 登录状态管理器和相关监听器

    single<List<LoginStateListener>> {
        listOf(
            UserDataSyncListener(get()),
            ConnectionLoginListener(get()),
            WebRTCLoginListener(get())
        )
    }
    single {

        val manager =  LoginStateManager(get())

        get<List<LoginStateListener>>().forEach { manager.addListener(it) }
        manager
    }


    viewModel {
        UserViewModel(
            userRepository = get(),
            loginStateManager = get(),
            friendRequestRepository = get()
        )
    }   // 注册为 ViewModel，由 Koin 自动管理生命周期

    // 注册VideoCallViewModel
    viewModel {
        val vm = VideoCallViewModel(get())
        // 注入WebRTC管理器
        vm.setWebRTCManager(get())
        vm
    }

    // 注册ContactsViewModel
    viewModel {
        ContactsViewModel(
            userRepository = get()
        )
    }

}