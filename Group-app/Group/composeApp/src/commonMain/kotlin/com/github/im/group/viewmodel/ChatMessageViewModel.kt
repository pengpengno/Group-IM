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

    //   用于处理客户端 发送消息 后 返回数据的 ui 更新辅助
    private val messageIndex = mutableMapOf<String, Int>() // clientMsgId -> messages list index

    //  sequence  辅助 messageItem
    private val messageSequenceIdIndex = mutableMapOf<Long, Int>() // sequenceId -> messages list index

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 多文件下载状态管理
    private val _fileDownloadStates = MutableStateFlow<Map<String, FileDownloadState>>(emptyMap())

    val fileDownloadStates: StateFlow<Map<String, FileDownloadState>> = _fileDownloadStates.asStateFlow()

    private val _loading = MutableStateFlow(false)



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

        addNewMessage( message)
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

        val idx = messageIndex[clientMsgId] ?: return

        // 从数据库中查询最新版本的消息
        val latestMessage = chatMessageRepository.getMessageByClientMsgId(clientMsgId) ?: return

        // 更新 StateFlow
        _uiState.update { state ->
            val updatedList = state.messages.toMutableList()
            updatedList[idx] = latestMessage  // 替换为最新版本
            state.copy(messages = updatedList)
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

        _uiState.update {
            val messageList = mutableListOf<MessageItem>()
            messageList.addAll(messages)
            it.copy(messages = messageList)
        }
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

                _uiState.update {
                    val messageList = mutableListOf<MessageItem>()
                    messageList.addAll(messages)
                    it.copy(messages = messageList)
                }

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

                _uiState.update {
                    val messageList = mutableListOf<MessageItem>()
                    messageList.addAll(messages)
                    it.copy(messages = messageList)
                }

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
                    
                    _uiState.update {
                        val messageList = mutableListOf<MessageItem>()
                        updatedMessages.forEach { message ->
                            messageList.add(message)
                        }
                        it.copy(messages = messageList)
                    }
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
                _uiState.update { currentState ->
                    val list =  mutableListOf<MessageItem>()
                    val updatedMessages = currentState.messages
                    if (isFront) {
                        // 历史消息，添加到列表开头
                        list.addAll( messages)
                        list.addAll(updatedMessages)
//                        updatedMessages.addAll(0, messages)
                    } else {
                        // 最新消息，添加到列表末尾
                        list.addAll( updatedMessages)
                        list.addAll(messages)
//                        updatedMessages.addAll(messages)
                    }
                    currentState.copy(messages = list)
                }
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
     * 接收/ 更新 服务端发送的新的新的消息
     * 当消息是客户端上报发送的新消息时 ： 在 会话的消息list 中直接插入一条新的消息
     * 当消息是服务端发送的消息时  a) 如果是客户端发送的回传ACK ： 更新消息状态为已收到， 更新ui上的消息状态
     *                        b) 如果是别的用户发送的消息（即clientId）在本地不存在 ，则插入一条新的消息
     * 索引 index 0 则为 最新的消息
     *
     */
    private fun addNewMessage(message: MessageItem){
        chatMessageRepository.insertOrUpdateMessage(message)
        _uiState.update {
            // 更新消息列表
            val updatedMessages = it.messages.toMutableList()
            // 新来的消息，添加到列表开头
            // clientMsgId  建立索引
            val isExisted =  messageIndex.containsKey(message.clientMsgId)
            Napier.d("addNewMessage: isExisted $isExisted  clientMsgId  ${message.clientMsgId}")
            if(isExisted){
                //  如果已经存在 那么就 更新掉原始的版本
                val idx = messageIndex[message.clientMsgId]!!
                updatedMessages[idx] = message
            }else{
                // 不存在则 录入 clientMsgId 索引 并且添加到列表首位
                updatedMessages.add(0, message)
                // 更新所有消息的索引（因为头插法改变了所有消息的位置）
                messageIndex.clear()
                updatedMessages.forEachIndexed { index, msg ->
                    if (msg.clientMsgId .isNotBlank()){
                        messageIndex[msg.clientMsgId] = index
                    }
                }
            }
            // seqId 不为0 的消息，添加到索引
            if(message.seqId != 0L ){
                messageSequenceIdIndex[message.seqId] = 0 // 新消息在索引0位置
            }
            it.copy(messages = updatedMessages)
        }
    }


    /**
     * 删除消息
     */
    fun removeMessage(clientMsgId: String) {
        val idx = messageIndex.remove(clientMsgId) ?: return
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()
            updatedMessages.removeAt(idx)
            // 重新建立索引
            updatedMessages.forEachIndexed { i, msg ->
                messageIndex[msg.clientMsgId] = i
            }
            state.copy(messages = updatedMessages)
        }
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
                addNewMessage(MessageWrapper(message = chatMessage))
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
                        isDownloading = true
                    ))
                }
                
                // 从文件存储管理器获取文件内容（实现本地优先策略）
                val fileContent = fileStorageManager.getFileContent(fileId)
                
                // 更新下载状态为成功
                _fileDownloadStates.update { currentStates ->
                    currentStates + (fileId to FileDownloadState(
                        fileId = fileId,
                        isDownloading = false,
                        isSuccess = true,
                    ))
                }

                Napier.d("文件下载成功: $fileId")
            } catch (e: Exception) {
                Napier.e("文件下载失败", e)
                // 更新下载状态为失败
                _fileDownloadStates.update { currentStates ->
                    currentStates + (fileId to FileDownloadState(
                        fileId = fileId,
                        isDownloading = false,
                        isSuccess = false,
                        error = e.message
                    ))
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
    fun getLocalFilePath(fileId: String): Path? {
        return fileStorageManager.getLocalFilePath(fileId)?.toPath()
    }



}