package com.github.im.group.viewmodel

import ChatMessageBuilder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.FileApi
import com.github.im.group.api.FileMeta
import com.github.im.group.manager.ChatSessionManager
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.MessageSyncRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FilePicker
import com.github.im.group.manager.FileStorageManager
import com.github.im.group.manager.FileUploadService
import com.github.im.group.manager.getFile
import com.github.im.group.manager.getLocalFilePath
import com.github.im.group.manager.isFileExists
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.sdk.File
import com.github.im.group.sdk.SenderSdk
import com.github.im.group.sdk.VoiceRecordingResult
import io.github.aakira.napier.Napier
import io.github.aakira.napier.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.Path
import okio.Path.Companion.toPath
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi


/**
 * 聊天消息记录model
 */
data class ChatUiState(

    //  { 老消息 }  {当前消息}  {新消息}
    val messages: MutableList<MessageItem> = mutableListOf(),
    val conversation: ConversationRes = ConversationRes(),
    val loading: Boolean = false,
    val messageIndex: Int = 0   // 消息的索引
)

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

class ChatMessageViewModel(
    val userRepository: UserRepository,
    val chatSessionManager: ChatSessionManager,
    val chatMessageRepository: ChatMessageRepository,
    val messageSyncRepository: MessageSyncRepository,
    val filesRepository: FilesRepository, // 添加文件仓库依赖
    val conversationRepository: ConversationRepository, // 添加会话仓库依赖
    val filePicker: FilePicker,  // 通过构造函数注入FilePicker
    val fileStorageManager: FileStorageManager, // 文件存储管理器
    val senderSdk: SenderSdk,
    val chatMessageBuilder: ChatMessageBuilder,
    val fileUploadService: FileUploadService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    // 使用LinkedHashMap作为唯一的消息存储来源，替代原来的多个索引
    private val messageStore = linkedMapOf<String, MessageItem>()

    // 专门的媒体资源列表，只包含图片和视频消息
    private val mediaMessageStore = linkedMapOf<String, MessageItem>()

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 多文件下载状态管理
    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())

    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()

    private val _loading = MutableStateFlow(false)

    // 媒体资源列表的StateFlow
    private val _mediaMessages = MutableStateFlow<List<MessageItem>>(emptyList())
    val mediaMessages: StateFlow<List<MessageItem>> = _mediaMessages.asStateFlow()

    fun scrollIndex(index : Int){
        _uiState.update {
            it.copy(messageIndex = index)
        }
    }

    /**
     * 接收到消息
     */
    fun onReceiveMessage(message: MessageItem){
        Napier.d("收到消息 $message")

        upsertMessage(message)
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
    fun updateMessage(clientMsgId: String?) {
        if (clientMsgId == null) return

        // 从数据库中查询最新版本的消息
        val latestMessage = chatMessageRepository.getMessageByClientMsgId(clientMsgId) ?: return

        upsertMessage(latestMessage)
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
    private fun upsertMessage(message: MessageItem){
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
        messages.forEach { upsertMessage(it) }
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
     * 查询指定会话（本地优先策略）
     */
    fun getConversation (conversationId: Long ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(loading = true)
            }

            try {
                // 使用本地优先策略获取会话信息
                val conversation = conversationRepository.getConversation(conversationId)
                Napier.d("获取会话信息成功: $conversation")
                _uiState.update {
                    it.copy(conversation = conversation)
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
                upsertMessage(messageItem)
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

}