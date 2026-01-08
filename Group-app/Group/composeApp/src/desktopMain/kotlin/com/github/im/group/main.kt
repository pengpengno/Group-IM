package com.github.im.group

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.im.group.db.DatabaseDriverFactory
import com.github.im.group.listener.ConnectionLoginListener
import com.github.im.group.listener.WebRTCLoginListener
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.manager.LoginStateListener
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.manager.UserDataSyncListener
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.FriendRequestRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.DesktopFilePicker
import com.github.im.group.sdk.FilePicker
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.sdk.DesktopVoiceRecorder
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.ui.video.VideoCallViewModel
import com.github.im.group.viewmodel.ChatMessageViewModel
import com.github.im.group.viewmodel.ChatViewModel
import com.github.im.group.viewmodel.ContactsViewModel
import com.github.im.group.viewmodel.TCPMessageViewModel
import com.github.im.group.viewmodel.UserViewModel
import com.github.im.group.viewmodel.VoiceViewModel
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

fun main() = application {
    startKoin {
        modules(desktopModule)
    }

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

val desktopModule = module {
    single<DatabaseDriverFactory> { DesktopDatabaseDriverFactory() }
    single { get<DatabaseDriverFactory>().createDatabase() }

    single<FilePicker> { DesktopFilePicker() }
//    single<AudioPlayer> { com.github.im.group.sdk.AudioPlayer() }
//    single<WebRTCManager> { com.github.im.group.sdk.WebRTCManager() }
    single { UserRepository(get()) }
    single { ChatMessageRepository(get(), get()) }
    single { FilesRepository(get()) }
    single { FriendRequestRepository(get()) }
    single { MessageSyncRepository(get(), get(), get(), get()) }
    single { ConversationRepository(get()) }

    single {
        FileStorageManager(
            filesRepository = get(),
            fileSystem = FileSystem.SYSTEM,
            baseDirectory = "./files".toPath()
        )
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
            fileUploadService = get(),
            chatMessageBuilder = get(),
        )
    }

    single { ChatSessionManager(get(), get()) }
    single { TCPMessageViewModel(get()) }
    single { DesktopVoiceRecorder() }
//    single { MessageClient(get()) } bind SocketClient::class
    viewModel {
        VoiceViewModel(
            voiceRecorder = get(),
            audioPlayer = get(),
        )
    }

    single { SenderSdk(get(), get()) }

    single<List<LoginStateListener>> {
        listOf(
            UserDataSyncListener(get()),
            ConnectionLoginListener(get()),
            WebRTCLoginListener(get())
        )
    }
    single {
        val manager = LoginStateManager(get())

        get<List<LoginStateListener>>().forEach { manager.addListener(it) }
        manager
    }

    viewModel {
        UserViewModel(
            userRepository = get(),
            loginStateManager = get(),
            friendRequestRepository = get()
        )
    }

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