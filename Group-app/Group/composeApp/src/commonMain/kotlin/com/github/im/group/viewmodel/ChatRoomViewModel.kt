package com.github.im.group.viewmodel

import ChatMessageBuilder
import UnauthorizedException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.ConversationType
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.api.GroupInfo
import com.github.im.group.api.UserApi
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.FileUploadService
import com.github.im.group.manager.getFile
import com.github.im.group.manager.getLocalFilePath
import com.github.im.group.manager.isFileExists
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.OfflineMessageRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecordingResult
import com.github.im.group.ui.ChatRoom
import db.OfflineMessage
import io.github.aakira.napier.Napier
import io.github.aakira.napier.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


/**
 * 会话创建状态
 */
sealed class SessionCreationState {
    object Idle : SessionCreationState()
    object Creating : SessionCreationState()
    object Pending : SessionCreationState()
    object Success : SessionCreationState()
    object Error : SessionCreationState()
}

/**
 * 聊天消息记录 状态管理
 */
data class ChatUiState(

    //  顺序为{ 老消息 }  {当前消息}  {新消息}
    val messages: MutableList<MessageItem> = mutableListOf(),
    val conversation: ConversationRes?=null ,
    val chatRoom : ChatRoom? = null,
    val loading: Boolean = false,
    val messageIndex: Int = 0,   // 消息的索引
    val sessionCreationState: SessionCreationState = SessionCreationState.Idle,
    val error: String? = null,

//    val roomName : String = "",
    val friend: UserInfo? = null,   // 单聊Room 下 这里只 放 好友

){
    /**
     * 获取群聊名称
     */
    fun getRoomName(): String {
        return when (chatRoom) {
            is ChatRoom.Conversation -> {
                if (conversation!!.type == ConversationType.PRIVATE_CHAT) {
                    friend?.username ?: ""
                } else {
                    conversation.groupName
                }
                friend?.username ?: ""
            }
            is ChatRoom.CreatePrivate -> {
                friend?.username ?: ""
            }
            else -> ""
        }
    }

}

/**
 * 文件下载状态
 */
data class FileDownloadState(
    val fileId: String,
    val isDownloading: Boolean = false,
    val isSuccess: Boolean = false,
    val progress: Float = 0f, // 添加进度信息，范围 0.0 - 1.0
//    val fileContent: ByteArray? = null,
    val error: String? = null
)

/***
 * 会话 聊天 消息 记录状态管理
 */
class ChatRoomViewModel(
    val userRepository: UserRepository,
    val chatSessionManager: ChatSessionManager,
    val chatMessageRepository: ChatMessageRepository,
    val messageSyncRepository: MessageSyncRepository,
    val filesRepository: FilesRepository, // 添加文件仓库依赖
    val conversationRepository: ConversationRepository, // 添加会话仓库依赖
    val offlineMessageRepository: OfflineMessageRepository, // 添加离线消息仓库依赖
    val filePicker: FilePicker,  // 通过构造函数注入FilePicker
    val fileStorageManager: FileStorageManager, // 文件存储管理器
    val senderSdk: SenderSdk,
    val chatMessageBuilder: ChatMessageBuilder,
    val fileUploadService: FileUploadService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState())

    val  uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    // 文件下载状态管理
    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())
    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()
    
    // 消息存储 - 用于管理聊天消息
    private val messageStore = mutableMapOf<String, MessageItem>()
    
    // 媒体消息存储 - 用于管理图片和视频等媒体消息
    private val mediaMessageStore = mutableMapOf<String, MessageItem>()
    
    // 媒体消息状态流 - 用于管理媒体消息列表
    private val _mediaMessages = MutableStateFlow<List<MessageItem>>(emptyList())
    val mediaMessages: StateFlow<List<MessageItem>> = _mediaMessages.asStateFlow()
    
    // 用于跟踪待创建的会话信息
    private var pendingSessionInfo: Pair<Long, Long>? = null // (userId, friendId)
    
    // 重试次数限制
    private val MAX_RETRY_COUNT = 3
    
    // 重试延迟时间（毫秒）
    private val RETRY_DELAY_MS = 2000L
    
    init {
        // 应用启动时初始化离线消息处理
        initializeWithOfflineMessages()
        
        // 启动定期同步任务
//        startPeriodicSync()
    }
    
    // 定期同步任务
    private var periodicSyncJob: kotlinx.coroutines.Job? = null
    
    /**
     * 启动定期同步任务
     */
    private fun startPeriodicSync() {
        periodicSyncJob?.cancel() // 取消之前的任务
        periodicSyncJob = viewModelScope.launch {
            while (true) {
                try {
                    delay(30000) // 每30秒检查一次离线消息
                    processOfflineMessages()
                } catch (e: Exception) {
                    Napier.e("定期同步离线消息失败", e)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        periodicSyncJob?.cancel()
    }

    fun scrollIndex(index : Int){
        _uiState.update {
            it.copy(messageIndex = index)
        }
    }



    /**
     * 初始化聊天室
     *
     * 此方法负责根据不同的聊天室类型初始化聊天界面状态。
     * 支持两种类型的聊天室：
     * 1. ChatRoom.Conversation - 已存在的会话（群聊或私聊）
     * 2. ChatRoom.CreatePrivate - 创建私聊会话
     *
     * 对于已存在的会话：
     * - 更新UI状态，设置聊天室信息
     * - 获取会话详细信息
     * - 从服务器加载消息
     * - 注册会话监听器以接收新消息
     *
     * 对于私聊会话创建：
     * - 尝试获取好友用户信息（先从本地缓存，本地没有则从远程获取并缓存）
     * - 如果获取用户信息成功，更新UI状态包含好友信息
     * - 如果获取用户信息失败，更新UI状态包含错误信息
     * - 实现了异常处理以确保界面稳定性
     *
     * 错误处理策略：
     * - 不直接抛出异常到UI层
     * - 将错误信息存储在UI状态的error字段中
     * - UI层可通过观察error字段来显示错误提示
     *
     * @param room 聊天室对象，可以是已存在的会话或创建私聊的请求
     */
    fun initChatRoom(room: ChatRoom){
        viewModelScope.launch {
            when(room){
                is ChatRoom.Conversation -> {
                    // 已经创建的会话
                    _uiState.update {
                        it.copy(
                            chatRoom = room,
                        )
                    }
                    val conversationId = room.conversationId
                    getConversation(conversationId) // 获取会话信息
                    loadMessages(conversationId)  //服务器加载更多消息
                    register(conversationId) // 注册会话，用于接收服务端推送的消息

                }
                is ChatRoom.CreatePrivate -> {
                    try {
                        val friendUserInfo = getUserById(room.friendUserId)
                        
                        if (friendUserInfo == null) {
                            // 用户信息获取失败，更新UI状态
                            _uiState.update {
                                it.copy(
                                    friend = null,
                                    chatRoom = room,
                                    error = "无法获取好友信息，请稍后重试"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    friend = friendUserInfo,
                                    chatRoom = room
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // 捕获任何意外异常
                        Napier.e("初始化聊天室时获取用户信息失败: ${room.friendUserId}", e)
                        _uiState.update {
                            it.copy(
                                friend = null,
                                chatRoom = room,
                                error = "获取好友信息时发生错误: ${e.message}"
                            )
                        }
                    }
                }
                else -> {
                    
                }
            }
//            val conversation = conversationRepository.getLocalConversationByMembers(userId, friendId)
//            if (conversation != null) {
//                _uiState.update {
//                    it.copy(conversation = conversation)
//                }
//            }
        }

    }
    /**
     * 接收到消息
     */
    fun onReceiveMessage(message: MessageItem){
        Napier.d("收到消息 $message")

        updateOrInsertMessage(message)
    }


    /**
     * 更新消息
     * 从本地数据库中获取最新消息数据
     * 更新 状态  ui
     *  TODO  设计为所有 PROTOBUF 的ACK
     *  1.  建立 发送缓存池
     *     a)  收到ACK 才会 remove
     *     b) 未 收到ACK  则在 一段时间内等待 ACK确认 超时 后重新发送
     *
     */
    fun receiveAckUpdateStatus (clientMsgId: String ,ackTimeStamp: Long) {
        // ACK 确认收到消息 表明发送成功， 更新数据库对应消息状态
        val ackLocalDateTime: LocalDateTime =
            Instant.fromEpochMilliseconds(ackTimeStamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
    }
    /**
     * 更新消息
     * 从数据库中获取最新消息数据
     * 1.  更新数据库对应消息状态
     * 2.  更新 ui
     */
    fun updateMessage(clientMsgId: String?) {
        if (clientMsgId == null) return

        // 从数据库中查询最新版本的消息
        val latestMessage = chatMessageRepository.getMessageByClientMsgId(clientMsgId) ?: return

        updateOrInsertMessage(latestMessage)
    }



    /**
     * 根据用户ID获取用户信息
     *
     * 此方法从本地数据库查询用户信息，如果本地数据库中存在对应的用户记录，
     * 则返回用户信息对象；否则返回null。此方法主要用于快速获取已知用户的
     * 基本信息，如用户名和邮箱等。
     *
     * 当前实现仅从本地缓存获取用户信息。如需实现"先从本地查询，本地查询不到
     * 从远程查询再存下来"的完整逻辑，可以扩展此方法如下：
     * 1. 首先尝试从本地数据库获取用户信息
     * 2. 如果本地不存在，向远程API发起请求获取用户信息
     * 3. 将远程获取的信息保存到本地数据库
     * 4. 返回用户信息
     *
     * @param userId 需要查询的用户ID
     * @return 如果找到对应的用户记录则返回UserInfo对象，否则返回null
     *
     * 注意：此方法目前只返回本地缓存的用户信息，不包含认证令牌信息
     * 这些敏感信息应当通过安全的认证流程获取和管理
     */
    suspend fun getUserById(userId: Long): UserInfo? {
        // 1. 首先尝试从本地数据库获取用户信息
        var user = userRepository.getUserById(userId)
        
        if (user != null) {
            // 本地已存在，直接返回
            return user
        }
        
        // 2. 本地不存在，向远程API发起请求获取用户信息
        try {
            user = UserApi.getUserBasicInfo(userId)
            
            // 3. 将远程获取的信息保存到本地数据库
            userRepository.addOrUpdateUser(user)
            
            // 4. 返回用户信息
            return user
        } catch (e: Exception) {
            // 如果远程请求失败，返回null
            Napier.e("获取用户信息失败: $userId", e)
            return null
        }
    }
    /**
     * 注册会话
     */
    fun register(conversationId: Long){
        chatSessionManager.register(conversationId,this)
    }

    fun unregister(conversationId: Long){
        chatSessionManager.unregister(conversationId)
    }

    /**
     * 加载本地现 有的消息
     */
    fun loadLocalMessages(conversationId: Long, limit: Long = 30) {

        // 然后再加载本地消息
        val messages = chatMessageRepository.getMessagesByConversation(conversationId,limit)
        Napier.d("读取到  ${messages.size} 条消息")

        // 应用消息到存储中
        applyMessages(messages)
    }

    /**
     * 加载远程消息
     * @param conversationId 会话id
     * @param limit 加载消息数量
     * 默认加载最新的 30 条消息
     */

    fun loadRemoteMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {


                // 先尝试从服务器同步新消息（增量同步）
                val newMessageCount = messageSyncRepository.syncMessages(conversationId)
                Napier.d("同步到 $newMessageCount 条新消息")

                // 然后再加载本地消息
                val messages = chatMessageRepository.getMessagesByConversation(conversationId,limit)
                Napier.d("读取到  ${messages.size} 条消息")

                // 应用消息到存储中
                applyMessages(messages)

            } catch (e: Exception) {
                Napier.e("加载消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 加载消息
     * @param conversationId 会话id
     * @param limit 加载消息数量
     * 默认加载最新的 30 条消息
     */
    fun loadMessages(conversationId: Long, limit: Long = 30) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {


                // 先尝试从服务器同步新消息（增量同步）
                val newMessageCount = messageSyncRepository.syncMessages(conversationId)
                Napier.d("同步到 $newMessageCount 条新消息")

                // 然后再加载本地消息
                val messages = chatMessageRepository.getMessagesByConversation(conversationId,limit)
                Napier.d("读取到  ${messages.size} 条消息")
                Napier.d("读取到  ${messages} ")

                // 应用消息到存储中
                applyMessages(messages)

            } catch (e: Exception) {
                Napier.e("加载消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 手动刷新消息（下拉刷新时调用）
     * @param conversationId 会话ID
     */
    fun refreshMessages(conversationId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                // 只执行同步操作，不重新加载本地数据
                val newMessageCount = messageSyncRepository.syncMessages(conversationId)
                Napier.d("刷新同步到 $newMessageCount 条新消息")
                
                // 如果有新消息，重新从本地加载所有消息
                if (newMessageCount > 0) {
                    val updatedMessages = chatMessageRepository.getMessagesByConversation(conversationId)
                    Napier.d("刷新后共有 ${updatedMessages.size} 条消息")
                    
                    // 应用消息到存储中
                    applyMessages(updatedMessages)
                }
            } catch (e: Exception) {
                Napier.e("刷新消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 加载更多历史消息（上拉加载更多）
     * @param conversationId 会话ID
     * @param beforeSequenceId 加载此序列号之前的消息
     */
    fun loadMoreMessages(conversationId: Long, beforeSequenceId: Long) {
        Napier.i("加载更多历史消息: beforeSequenceId=$beforeSequenceId")
        loadMessagesWithProgress(
            conversationId = conversationId,
            isFront = true,
            currentIndex = beforeSequenceId
        )
    }
    
    /**
     * 获取最新消息（下拉刷新获取新消息）
     * @param conversationId 会话ID
     * @param afterSequenceId 获取此序列号之后的消息
     */
    fun loadLatestMessages(conversationId: Long, afterSequenceId: Long) {
        Napier.i("加载最新消息: afterSequenceId=$afterSequenceId")
        loadMessagesWithProgress(
            conversationId = conversationId,
            isFront = false,
            currentIndex = afterSequenceId
        )
    }
    
    /**
     * 通用的消息加载方法，带有加载进度状态管理
     * @param conversationId 会话ID
     * @param isFront 是否是向前加载（历史消息）
     * @param currentIndex 当前索引
     */
    private fun loadMessagesWithProgress(
        conversationId: Long,
        isFront: Boolean,
        currentIndex: Long
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            try {
                val messages = messageSyncRepository.getMessagesWithStrategy(conversationId, currentIndex, isFront)
                Napier.d("加载了: ${messages.size} 条消息 currentIndex $currentIndex isFront $isFront")
                // 更新UI状态
                applyMessages(messages)
            } catch (e: Exception) {
                Napier.e("加载消息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 检查消息是否为媒体类型（图片或视频）
     */
    private fun MessageItem.isMediaType(): Boolean {
        return this.type == com.github.im.group.db.entities.MessageType.IMAGE || 
               this.type == com.github.im.group.db.entities.MessageType.VIDEO
    }

    /**
     * 统一更新UI消息列表
     */
    private fun emitUiMessages() {
        val sorted = messageStore.values
            .sortedByDescending { it.seqId.takeIf { it != 0L } ?: Long.MAX_VALUE }

        // 更新媒体消息列表
        updateMediaMessages(sorted)

        _uiState.update { state ->
            state.copy(messages = sorted.toMutableList())
        }
    }

    /**
     * 更新媒体消息列表
     */
    private fun updateMediaMessages(allMessages: List<MessageItem>) {
        val mediaMessages = allMessages.filter { it.isMediaType() }
        _mediaMessages.value = mediaMessages
    }

    /**
     * 检查是否为媒体类型消息
     */
    private fun isMediaType(type: com.github.im.group.db.entities.MessageType): Boolean {
        return type == com.github.im.group.db.entities.MessageType.IMAGE || 
               type == com.github.im.group.db.entities.MessageType.VIDEO
    }

    /**
     * 接收/ 更新 服务端发送的新的新的消息
     * 当消息是客户端上报发送的新消息时 ： 在 会话的消息list 中直接插入一条新的消息
     * 当消息是服务端发送的消息时  a) 如果是客户端发送的回传ACK ： 更新消息状态为已收到， 更新ui上的消息状态
     *                        b) 如果是别的用户发送的消息（即clientId）在本地不存在 ，则插入一条新的消息
     * 索引 index 0 则为 最新的消息
     *
     */
    private fun updateOrInsertMessage(message: MessageItem){
        chatMessageRepository.insertOrUpdateMessage(message)
        
        val key = message.uniqueKey()
        
        // 如果是 ACK，把 clientMsgId 版本替换成 seqId 版本
        if (message.seqId != 0L && message.clientMsgId.isNotBlank()) {
            messageStore.remove("C:${message.clientMsgId}")
        }
        
        messageStore[key] = message
        
        // 如果是媒体类型消息，也要添加到媒体消息存储中
        if (message.isMediaType()) {
            mediaMessageStore[key] = message
        } else {
            // 如果不是媒体类型，但之前在媒体消息存储中，则移除
            mediaMessageStore.remove(key)
        }
        
        emitUiMessages()
    }

    /**
     * 为MessageItem添加唯一键生成方法
     */
    private fun MessageItem.uniqueKey(): String =
        when {
            seqId != 0L -> "S:$seqId"
            clientMsgId.isNotBlank() -> "C:$clientMsgId"
            else -> error("Message has no identity: $this")
        }
    
    /**
     * 批量应用消息到存储中
     */
    private fun applyMessages(messages: List<MessageItem>) {
        messages.forEach { updateOrInsertMessage(it) }
    }


    /**
     * 删除消息
     */
    fun removeMessage(clientMsgId: String) {
        val key = messageStore.keys.find { 
            it.startsWith("C:") && messageStore[it]?.clientMsgId == clientMsgId 
        } ?: return
        
        messageStore.remove(key)
        emitUiMessages()
    }



    /**
     * 获取私聊会话
     *
     * 先查询本地是否存在 不存在则远程创建
     */
    suspend fun getOrCreatePrivateChat( userId:Long , friendId: Long): ConversationRes {
        Napier.d("开始获取私聊会话:  好友ID=$friendId")
        val localConversation = conversationRepository.getLocalConversationByMembers(
            userId = userId,
            friendId = friendId
        )

        if (localConversation != null) {
            Napier.d("本地已存在私聊会话: 会话ID=${localConversation.conversationId}")
            // 存在则直接返回
            _uiState.update {
                it.copy(conversation = localConversation)
            }
            return localConversation
        }
        // 不存在的话 直接调用 API 并返回结果
        return try {
            val result =  ConversationApi.createOrGetConversation( friendId)

            getConversation(result.conversationId)
             result

        } catch (e: UnauthorizedException) {
            // Token失效，通知登出
            Napier.e("Token失效，需要重新登录", e)
            throw e
        }
    }
    
    /**
     * 创建私聊会话，带重试机制
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @return Result<ConversationRes> 包含会话信息或错误
     */
    suspend fun createPrivateChatWithRetry(userId: Long, friendId: Long): Result<ConversationRes> {
        return try {
            // 设置状态为创建中
            _uiState.update { state ->
                state.copy(sessionCreationState = SessionCreationState.Creating)
            }
            
            // 尝试创建会话
            val result = ConversationApi.createOrGetConversation(friendId)
            
            // 更新状态为成功
            _uiState.update { state ->
                state.copy(
                    sessionCreationState = SessionCreationState.Success,
                    conversation = result
                )
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Napier.e("创建私聊会话失败", e)
            
            // 更新状态为错误
            _uiState.update { state ->
                state.copy(
                    sessionCreationState = SessionCreationState.Error,
                    error = e.message
                )
            }
            
            Result.failure(e)
        }
    }
    
    
    /**
     * 创建群聊会话
     * @param members 会话成员
     * @param groupName 群聊名称
     * @param desc 群聊描述
     *
     * @return 会话ID
     * 创建成功后，再从远程获取数据并更新UI
     *
     */
    fun createGroupChat(groupName:String , desc:String?,members : List<UserInfo>) {

        viewModelScope.launch {
            // 直接调用 API 并返回结果
            val conversation = try {

                ConversationApi.createGroupConversation(
                    GroupInfo(
                        groupName = groupName,
                        description = desc,
                        members = members
                    )
                )
            } catch (e: UnauthorizedException) {
                // Token失效，通知登出
                Napier.e("Token失效，需要重新登录", e)
                throw e
            }

            Napier.d("成功获取私聊会话: 会话ID=${conversation.conversationId}")


            _uiState.update {
                it.copy(conversation = conversation)
            }

        }



    }

    /**
     * 查询指定会话
     */
    fun getConversation (conversationId: Long ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(loading = true)
            }

            try {
                // 先从本地获取数据并显示
                val localConversation = conversationRepository.getLocalConversation(conversationId)
                if (localConversation != null) {
                    Napier.d("获取本地会话信息成功: $localConversation")
                    _uiState.update {
                        it.copy(conversation = localConversation)
                    }
                }

                // 在后台异步更新远程数据即可
                launch(Dispatchers.IO) {
                    try {
                        val remoteConversation = conversationRepository.getConversation(conversationId)
                        Napier.d("获取远程会话信息成功: $remoteConversation")
                        // 更新UI状态
                        _uiState.update {
                            it.copy(conversation = remoteConversation)
                        }
                    } catch (e: Exception) {
                        Napier.e("获取远程会话信息失败，保持本地数据", e)
                    }
                }

            } catch (e: Exception) {
                Napier.e("获取会话信息失败", e)
            } finally {
                _uiState.update {
                    it.copy(loading = false)
                }
            }
        }
    }

    /**
     * 发送私聊文本消息
     * 如果会话不存在则先创建会话再发送消息
     */
    fun sendPrivateTextMessage(userId: Long, friendId: Long, message: String) {
        viewModelScope.launch {
            try {
                val conversation = getOrCreatePrivateChat(userId, friendId)
                sendTextMessage(conversation.conversationId, message)
            } catch (e: Exception) {
                Napier.e("发送私聊文本消息失败", e)
            }
        }
    }

    /**
     * 发送私聊语音消息
     * 如果会话不存在则先创建会话再发送消息
     */
    fun sendPrivateVoiceMessage(userId: Long, friendId: Long, voice: VoiceRecordingResult) {
        viewModelScope.launch {
            try {
                val conversation = getOrCreatePrivateChat(userId, friendId)
                sendVoiceMessage(conversation.conversationId, voice)
            } catch (e: Exception) {
                Napier.e("发送私聊语音消息失败", e)
            }
        }
    }

    /**
     * 发送私聊文件消息
     * 如果会话不存在则先创建会话再发送消息
     */
    fun sendPrivateFileMessage(userId: Long, friendId: Long, file: File) {
        viewModelScope.launch {
            try {
                val conversation = getOrCreatePrivateChat(userId, friendId)
                sendFileMessage(conversation.conversationId, file)
            } catch (e: Exception) {
                Napier.e("发送私聊文件消息失败", e)
            }
        }
    }

    /**
     * 发送语音消息
     *
     * @param conversationId 会话ID
     * @param url 语音文件的URL地址
     */
    fun sendVoiceMessage(conversationId: Long,
                         voice  : VoiceRecordingResult
                         ) {
        log { "sendVoiceMessage " }
        sendFileMessage(conversationId,voice.file,voice.durationMillis,)
    }

    /**
     * 发送文件消息
     * 1. 预创建文件记录，获取服务端返回的fileId
     * 2. 上传文件
     * 3. 创建并发送消息（使用fileId作为content）
     * @param conversationId 会话ID
     * @param file 选择的文件
     */
    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    fun sendFileMessage(conversationId: Long, file: File, duration: Long=0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                log{"picked file name  : $file"}

                val fileData = try {
                    filePicker.readFileBytes(file)
                } catch (e: Exception) {
                    Napier.e("获取文件数据失败", e)
                    // 直接返回即可
                    return@launch
                }
                sendFileMessage(conversationId, fileData, file.name, file.size,file.path,duration)



            } catch (e: Exception) {
                // 处理上传失败的情况
                Napier.e("发送文件消息失败", e)
            }
        }
    }



    /**
     * 发送消息
     *
     * 1. 记录消息到本地数据库
     * 2. 通过长连接 发送消息到 服务器
     *
     * @param conversationId 会话
     * @param message 消息
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun sendChatMessage(chatMessage: ChatMessage){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 添加到本地消息列表
                val messageItem = MessageWrapper(message = chatMessage)
                updateOrInsertMessage(messageItem)
                // 发送消息
                senderSdk.sendMessage(chatMessage)


            } catch (e: Exception) {
                Napier.e("发送消息失败", e)
            }
        }
    }

    /**
     * 发送 文本消息
     *
     * @param conversationId 会话
     * @param message 消息
     */
    @OptIn(ExperimentalUuidApi::class)
    fun sendTextMessage(conversationId:Long, message:String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val textMessage = chatMessageBuilder.textMessage(conversationId, message)
                sendChatMessage(textMessage)
            } catch (e: Exception) {
                Napier.e("发送消息失败", e)
            }
        }
    }

    /**
     * 发送文件消息
     * @param conversationId 会话ID
     * @param data 文件数据
     * @param fileName 文件名
     * @param duration 时长（用于语音消息）
     * @param type 消息类型
     */
    private fun sendFileMessage(conversationId: Long, data: ByteArray, fileName: String, fileSize:Long, filePath:String,duration: Long = 0) {
        viewModelScope.launch {
            try {

                val chatMessage = chatMessageBuilder.fileMessage(conversationId, fileName,fileSize, duration)

                val fileId = chatMessage.content
                // 添加上传中的文件记录到数据库
                fileStorageManager.addPendingFile(fileId, fileName, duration,filePath)

                sendChatMessage(chatMessage)

                val uploadFileData = fileUploadService.uploadFileData(fileId, data, fileName, duration)


            } catch (e: Exception) {
                // 处理上传失败的情况
                Napier.e("发送文件消息失败", e)
            }
        }
    }

     suspend fun getFileMessageMeta(fileId: String): FileMeta{
         return FileApi.getFileMeta(fileId)
    }


    
    /**
     * 异步获取文件元信息（本地优先）
     * @param messageItem 消息项
     * @return 文件元数据的Flow
     */
    suspend fun getFileMessageMetaAsync(messageItem: MessageItem): FileMeta? {
        if(messageItem.type.isFile()){
            messageItem.fileMeta?.let {
                return it
            }
            log { "messageItem : $messageItem" }
            // 优先从本地数据库获取文件元数据
            filesRepository.getFileMeta(messageItem.content)?.let { 
                return it
            }
            // 如果本地没有，通过API获取并存储到本地
            try {
                val fileMeta = getFileMessageMeta(messageItem.content)
                // 将文件元数据存储到本地数据库
                filesRepository.addOrUpdateFile(fileMeta)
                return fileMeta
            } catch (e: Exception) {
                Napier.e("获取文件元信息失败", e)
                return null
            }
        }
        return null
    }
    
    /**
     * 检查文件是否存在（本地优先）
     * @param fileId 文件ID
     * @return 文件是否存在
     */
    fun isFileExists(fileId: String): Boolean {
        return fileStorageManager.isFileExists(fileId)
    }

    /**
     * 删除指定文件（本地和数据库记录）
     * @param fileId 文件ID
     * @return 删除是否成功
     */
    fun deleteFile(fileId: String): Boolean {
        return fileStorageManager.deleteFile(fileId)
    }

    /**
     * 清理过期文件
     */
    fun cleanupExpiredFiles() {
        fileStorageManager.cleanupExpiredFiles()
    }
    
    /**
     * 下载文件消息内容（实现本地优先策略）
     * 如果已经存在 就读取并且返回
     * 不存在那么就从远程下载
     * @param fileId 文件ID
     */
    fun downloadFileMessage(fileId: String) {
        Napier.d("开始下载文件: $fileId")
        viewModelScope.launch {
            try {
                // 检查是否已经在下载该文件
                val currentState = _fileDownloadStates.value[fileId]
                if (currentState?.isDownloading == true) {
                    Napier.d("文件已在下载中: $fileId")
                    return@launch
                }
                
                // 更新下载状态为正在下载
                _fileDownloadStates.update { currentStates ->
                    currentStates + (fileId to FileDownloadState(
                        fileId = fileId,
                        isDownloading = true,
                        progress = 0f  // 初始化进度为0
                    ))
                }
                
                // 使用流式下载方法获取文件路径（实现本地优先策略），并更新进度
                val filePath = fileStorageManager.getFileContentPathWithProgress(fileId) { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                    // 更新下载进度状态
                    _fileDownloadStates.update { currentStates ->
                        val currentState = currentStates[fileId] ?: FileDownloadState(fileId)
                        currentStates + (fileId to currentState.copy(
                            progress = progress.coerceIn(0f, 1f)  // 确保进度在0-1之间
                        ))
                    }
                }
                
                // 更新下载状态为成功
                _fileDownloadStates.update { currentStates ->
                    val currentState = currentStates[fileId] ?: FileDownloadState(fileId)
                    currentStates + (fileId to currentState.copy(
                        isDownloading = false,
                        isSuccess = filePath != null,
                        error = if (filePath == null) "Failed to download file" else null
                    ))
                }

                Napier.d("文件下载成功: $fileId")
            } catch (e: Exception) {
                // 区分协程取消异常和其他异常
                if (e !is kotlinx.coroutines.CancellationException) {
                    Napier.e("文件下载失败", e)
                    // 更新下载状态为失败
                    _fileDownloadStates.update { currentStates ->
                        val currentState = currentStates[fileId] ?: FileDownloadState(fileId)
                        currentStates + (fileId to currentState.copy(
                            isDownloading = false,
                            isSuccess = false,
                            error = e.message
                        ))
                    }
                } else {
                    // 协程被取消，不记录为错误
                    Napier.d("文件下载被取消: $fileId")
                }
            }
        }
    }
    
    /**
     * 清除指定文件的下载状态
     * @param fileId 文件ID
     */
    fun clearFileDownloadState(fileId: String) {
        _fileDownloadStates.update { currentStates ->
            currentStates - fileId
        }
    }
    
    /**
     * 获取本地文件路径
     * @param fileId 文件ID
     * @return 本地文件路径
     */
    fun getLocalFilePath(fileId: String): String? {
        return fileStorageManager.getLocalFilePath(fileId)
    }

    /**
     * 获取文件对象
     * @param fileId 文件ID
     */
    fun getFile(fileId: String): File?{

        return fileStorageManager.getFile(fileId)
    }

    /**
     * 获取媒体消息列表
     */
    fun getMediaMessages(): List<MessageItem> {
        return _mediaMessages.value
    }

    /**
     * 获取指定消息在媒体消息列表中的索引
     */
    fun getMediaMessageIndex(message: MessageItem): Int {
        val mediaMessages = getMediaMessages()
        return mediaMessages.indexOfFirst { 
            it.seqId == message.seqId && it.clientMsgId == message.clientMsgId 
        }
    }
    
    /**
     * 清理会话创建错误状态，允许重新尝试
     */
    fun clearSessionCreationError() {
        _uiState.update { state ->
            state.copy(
                sessionCreationState = SessionCreationState.Idle,
                error = null
            )
        }
    }
    
    /**
     * 重试创建会话（当处于Pending状态时）
     */
    fun retryPendingSession() {
        viewModelScope.launch {
            val pendingInfo = pendingSessionInfo
            if (pendingInfo != null) {
                val (userId, friendId) = pendingInfo
                Napier.d("重试创建会话: 用户ID=$userId, 好友ID=$friendId")
                
                val result = createPrivateChatWithRetry(userId, friendId)
                if (result.isSuccess) {
                    Napier.d("重试创建会话成功")
                } else {
                    Napier.e("重试创建会话失败", result.exceptionOrNull())
                }
            } else {
                Napier.w("没有待重试的会话")
            }
        }
    }
    
    /**
     * 保存离线消息到本地数据库
     */
    fun saveOfflineMessage(
        clientMsgId: String,
        conversationId: Long?,
        fromUserId: Long,
        toUserId: Long?,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        filePath: String? = null,
        fileSize: Long? = null,
        fileDuration: Int = 0
    ) {
        offlineMessageRepository.saveOfflineMessage(
            clientMsgId = clientMsgId,
            conversationId = conversationId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            content = content,
            messageType = messageType,
            filePath = filePath,
            fileSize = fileSize,
            fileDuration = fileDuration
        )
    }
    
    /**
     * 处理离线消息发送
     * 
     * 逻辑步骤:
     * 1. 获取所有待发送的离线消息
     * 2. 遍历每条离线消息并根据是否有会话ID分别处理:
     *    a) 有会话ID: 直接发送消息
     *    b) 无会话ID: 先创建会话再发送消息
     * 3. 根据消息类型分别处理不同类型的消息
     * 4. 发送成功后删除离线消息记录，失败则更新重试次数
     */
    fun processOfflineMessages() {
        viewModelScope.launch {
            try {
                // 步骤1: 获取所有待发送的离线消息
                val pendingMessages = offlineMessageRepository.getPendingOfflineMessages()
                
                Napier.d("发现 ${pendingMessages.size} 条待发送的离线消息")
                
                pendingMessages.forEach { offlineMessage ->
                    try {
                        Napier.d("处理离线消息: ${offlineMessage.client_msg_id}")
                        
                        // 步骤2: 根据是否有会话ID分别处理
                        if (offlineMessage.conversation_id != null) {
                            // 情况a: 已有会话ID，直接发送消息
                            handleOfflineMessageWithConversationId(offlineMessage)
                        } else {
                            // 情况b: 无会话ID，需先创建会话
                            handleOfflineMessageWithoutConversationId(offlineMessage)
                        }
                    } catch (e: Exception) {
                        Napier.e("处理离线消息失败: ${offlineMessage.client_msg_id}", e)
                        // 更新重试次数
                        offlineMessageRepository.updateOfflineMessageStatus(
                            offlineMessage.client_msg_id,
                            MessageStatus.FAILED,
                            incrementRetry = true
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("处理离线消息队列失败", e)
            }
        }
    }
    
    /**
     * 处理已有会话ID的离线消息
     * @param offlineMessage 离线消息对象
     */
    private suspend fun handleOfflineMessageWithConversationId(offlineMessage: OfflineMessage) {
        offlineMessage.conversation_id?.let {
            when (offlineMessage.message_type) {
                MessageType.TEXT -> {
                    sendTextMessage(offlineMessage.conversation_id, offlineMessage.content)
                }
                MessageType.VOICE -> {
                    // 需要从文件路径重新构建语音数据
                    val voiceFile = offlineMessage.file_path?.let {
                        fileStorageManager.readLocalFileFromPath(it)
                    }
                    if (voiceFile != null) {
                        val filename = offlineMessage.file_path.substringAfterLast("/")
                        when (val dataType = voiceFile.data) {
                            is FileData.Bytes -> {
                                val voiceData = VoiceRecordingResult(
                                    bytes = dataType.data,
                                    durationMillis = offlineMessage.file_duration ?: 0,
                                    name = filename,
                                    file = voiceFile
                                )
                                sendVoiceMessage(offlineMessage.conversation_id, voiceData)
                            }
                            else -> {
                                // 其他类型暂时不做处理
                            }
                        }
                    }
                }
                MessageType.IMAGE, MessageType.VIDEO, MessageType.FILE -> {
                    // 需要从文件路径重新构建文件数据
                    val fileData = offlineMessage.file_path?.let {
                        fileStorageManager.getFile(it)
                    }
                    if (fileData != null) {
                        sendFileMessage(offlineMessage.conversation_id, fileData)
                    }
                }
                else -> {
                    sendTextMessage(offlineMessage.conversation_id, offlineMessage.content)
                }
            }

            // 消息发送成功后，删除离线消息记录
            offlineMessageRepository.deleteOfflineMessage(offlineMessage.client_msg_id)
        }

    }
    
    /**
     * 处理无会话ID的离线消息
     * @param offlineMessage 离线消息对象
     */
    private suspend fun handleOfflineMessageWithoutConversationId(offlineMessage: OfflineMessage) {
        val toUserId = offlineMessage.to_user_id
        if (toUserId != null) {
            val result = createPrivateChatWithRetry(offlineMessage.from_user_id, toUserId)
            if (result.isSuccess) {
                val conversation = result.getOrNull()
                if (conversation != null) {
                    // 更新离线消息的会话ID
                    offlineMessageRepository.updateOfflineMessageConversationId(
                        offlineMessage.client_msg_id,
                        conversation.conversationId
                    )
                    
                    // 现在可以发送消息了
                    when (offlineMessage.message_type) {
                        MessageType.TEXT -> {
                            sendTextMessage(conversation.conversationId, offlineMessage.content)
                        }
                        MessageType.VOICE -> {
                            offlineMessage.file_path?.let {
                                val voiceFile = fileStorageManager.readLocalFileFromPath(it)
                                when (val dataType = voiceFile.data) {
                                    is FileData.Bytes -> {
                                        val voiceData = VoiceRecordingResult(
                                            bytes = dataType.data,
                                            durationMillis = offlineMessage.file_duration ?: 0,
                                            name = voiceFile.name,
                                            file = voiceFile
                                        )
                                        sendVoiceMessage(conversation.conversationId, voiceData)
                                    }
                                    else -> {

                                    }
                                }
                            }



                        }
                        MessageType.IMAGE, MessageType.VIDEO, MessageType.FILE -> {
                            val fileData = offlineMessage.file_path?.let { 
                                fileStorageManager.getFile(it) 
                            }
                            if (fileData != null) {
                                sendFileMessage(conversation.conversationId, fileData)
                            }
                        }
                        else -> {
                            sendTextMessage(conversation.conversationId, offlineMessage.content)
                        }
                    }
                    
                    // 消息发送成功后，删除离线消息记录
                    offlineMessageRepository.deleteOfflineMessage(offlineMessage.client_msg_id)
                }
            } else {
                // 发送失败，更新重试次数
                offlineMessageRepository.updateOfflineMessageStatus(
                    offlineMessage.client_msg_id,
                    MessageStatus.FAILED,
                    incrementRetry = true
                )
            }
        }
    }
    
    /**
     * 初始化时处理离线消息 即可
     */
    fun initializeWithOfflineMessages() {
        // 应用启动时处理之前未发送的离线消息
        processOfflineMessages()
    }
    
    /**
     * 发送私聊文本消息，带重试机制
     * 如果会话不存在则先创建会话再发送消息
     */
    fun sendPrivateTextMessageWithRetry(userId: Long, friendId: Long, message: String) {
        viewModelScope.launch {
            try {
                // 生成唯一的客户端消息ID
                val clientMsgId = java.util.UUID.randomUUID().toString()
                
                // 首先将消息保存到离线消息库
                saveOfflineMessage(
                    clientMsgId = clientMsgId,
                    conversationId = null, // 会话ID还未知
                    fromUserId = userId,
                    toUserId = friendId,
                    content = message,
                    messageType = MessageType.TEXT
                )
                
                // 然后尝试创建会话并发送消息
                val conversationResult = createPrivateChatWithRetry(userId, friendId)
                if (conversationResult.isSuccess) {
                    val conversation = conversationResult.getOrNull()
                    // 更新离线消息的会话ID
                    if (conversation != null) {
                        offlineMessageRepository.updateOfflineMessageConversationId(
                            clientMsgId,
                            conversation.conversationId
                        )
                    }
                    sendTextMessage(conversation?.conversationId ?: -1, message)
                    
                    // 消息发送成功后，删除离线消息记录
                    offlineMessageRepository.deleteOfflineMessage(clientMsgId)
                } else {
                    Napier.e("多次尝试后仍无法创建私聊会话，无法发送文本消息", conversationResult.exceptionOrNull())
                    // 在这里我们可以通知UI显示错误信息，让用户知道需要手动重试
                    _uiState.update { state ->
                        state.copy(
                            error = "无法建立会话，请检查网络连接后重试"
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("发送私聊文本消息失败", e)
            }
        }
    }

    /**
     * 发送私聊语音消息，带重试机制
     * 如果会话不存在则先创建会话再发送消息
     */
    fun sendPrivateVoiceMessageWithRetry(userId: Long, friendId: Long, voice: VoiceRecordingResult) {
        viewModelScope.launch {
            try {
                // 生成唯一的客户端消息ID
                val clientMsgId = java.util.UUID.randomUUID().toString()
                
                // 首先将消息保存到离线消息库
                saveOfflineMessage(
                    clientMsgId = clientMsgId,
                    conversationId = null, // 会话ID还未知
                    fromUserId = userId,
                    toUserId = friendId,
                    content = voice.file.name ?: "Voice Message",
                    messageType = MessageType.VOICE,
                    filePath = voice.file.path,
                    fileDuration = (voice.durationMillis / 1000).toInt() // 转换为秒
                )
                
                val conversationResult = createPrivateChatWithRetry(userId, friendId)
                if (conversationResult.isSuccess) {
                    val conversation = conversationResult.getOrNull()
                    // 更新离线消息的会话ID
                    if (conversation != null) {
                        offlineMessageRepository.updateOfflineMessageConversationId(
                            clientMsgId,
                            conversation.conversationId
                        )
                    }
                    sendVoiceMessage(conversation?.conversationId ?: -1, voice)
                    
                    // 消息发送成功后，删除离线消息记录
                    offlineMessageRepository.deleteOfflineMessage(clientMsgId)
                } else {
                    Napier.e("多次尝试后仍无法创建私聊会话，无法发送语音消息", conversationResult.exceptionOrNull())
                    // 在这里我们可以通知UI显示错误信息，让用户知道需要手动重试
                    _uiState.update { state ->
                        state.copy(
                            error = "无法建立会话，请检查网络连接后重试"
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("发送私聊语音消息失败", e)
            }
        }
    }

    /**
     * 发送私聊文件消息，带重试机制
     * 如果会话不存在则先创建会话再发送消息
     */
    fun sendPrivateFileMessageWithRetry(userId: Long, friendId: Long, file: File) {
        viewModelScope.launch {
            try {
                // 生成唯一的客户端消息ID
                val clientMsgId = java.util.UUID.randomUUID().toString()
                
                // 首先将消息保存到离线消息库
                saveOfflineMessage(
                    clientMsgId = clientMsgId,
                    conversationId = null, // 会话ID还未知
                    fromUserId = userId,
                    toUserId = friendId,
                    content = file.name ?: "File",
                    messageType = MessageType.FILE,
                    filePath = file.path,
                    fileSize = file.size
                )
                
                val conversationResult = createPrivateChatWithRetry(userId, friendId)
                if (conversationResult.isSuccess) {
                    val conversation = conversationResult.getOrNull()
                    // 更新离线消息的会话ID
                    if (conversation != null) {
                        offlineMessageRepository.updateOfflineMessageConversationId(
                            clientMsgId,
                            conversation.conversationId
                        )
                    }
                    sendFileMessage(conversation?.conversationId ?: -1, file)
                    
                    // 消息发送成功后，删除离线消息记录
                    offlineMessageRepository.deleteOfflineMessage(clientMsgId)
                } else {
                    Napier.e("多次尝试后仍无法创建私聊会话，无法发送文件消息", conversationResult.exceptionOrNull())
                    // 在这里我们可以通知UI显示错误信息，让用户知道需要手动重试
                    _uiState.update { state ->
                        state.copy(
                            error = "无法建立会话，请检查网络连接后重试"
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("发送私聊文件消息失败", e)
            }
        }
    }
}