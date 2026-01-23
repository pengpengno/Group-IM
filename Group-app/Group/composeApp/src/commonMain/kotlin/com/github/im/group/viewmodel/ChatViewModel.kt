package com.github.im.group.viewmodel

import UnauthorizedException
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.api.GroupInfo
import com.github.im.group.db.entities.MessageType
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.UserRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * 用于聊天页面的会话列表
 */
data class ConversationUiState(
    val conversations: List<ConversationDisplayState> = emptyList(),
)

/**
 * 会话列表的显示状态
 */
data class ConversationDisplayState(
    val conversation: ConversationRes,
    /**
     * 最近的一次消息
     * 如果是 文件 / 语音 / 图片 / 就展示 文件消息/。。。。
     * 如果是文本  就展示 文本消息
     */
    val lastMessage: String = "",
    /**
     * 这里需要计算 处理
     * 如果是今天 那么就展示 时间和分钟
     * 如果是昨天 那么就展示昨天 时间和分钟
     * 如果是7天之内的 那么就展示星期几
     * 如果是更早的,没超过一年 那么就展示日期  格式 mm-dd
     * 如果超过了 一年那么久展示 年月日  格式 yyyy-mm-dd
     */
    val displayDateTime : String = "", // 展示时间日期
    
)
/**
 * Chat 聊天页面的状态管理
 * 包括 Conversation 列表、会话详情、消息列表
 */
class ChatViewModel (
    val userRepository: UserRepository,
    val loginStateManager: LoginStateManager,
    val messageRepository: ChatMessageRepository,
    val conversationRepository: ConversationRepository
): ViewModel() {


    private val _conversations = MutableStateFlow(emptyList<ConversationDisplayState>())
    val conversationState :  StateFlow<List<ConversationDisplayState>> = _conversations.asStateFlow()

    // 会话列表加载中状态
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()


    /***
     * 获取所有的会话列表
     */
    fun getConversations(uId: Long ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 首先从本地数据库获取会话列表并立即更新UI
                val localConversations = conversationRepository.getConversationsByUserId(uId)
                
                /**
                 * 获取消息的描述文本
                 *
                 * @param message 消息包装对象
                 * @return 返回对应消息类型的描述文本
                 */
                fun getMessageDesc(message: MessageWrapper): String {
                    return when (message.type) {
                        MessageType.TEXT -> message.content
                        MessageType.FILE -> "文件消息"
                        MessageType.VOICE -> "语音消息"
                        MessageType.VIDEO -> "视频消息"
                        MessageType.IMAGE -> "图片消息"
                    }
                }
                
                // 将本地会话转换为显示状态并更新UI
                val localConversationsWithLatestMessages = localConversations.map { conversation ->
                    val latestMessage = messageRepository.getLatestMessage(conversation.conversationId)
                    val lastMessageText = latestMessage?.let { message ->
                        getMessageDesc(message)
                    } ?: ""
                    val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) } ?: ""
                    ConversationDisplayState(
                        conversation = conversation,
                        lastMessage = lastMessageText,
                        displayDateTime = displayDateTime // 计算显示时间
                    )
                }
                
                // 立即将本地数据更新到UI
                _conversations.value = localConversationsWithLatestMessages
                
                // 然后从远程获取最新数据并更新
                val response = ConversationApi.getActiveConversationsByUserId(uId)
                
                // 将远程数据转换为显示状态
                val remoteConversationsWithLatestMessages = response.map { conversation ->
                    val latestMessage = messageRepository.getLatestMessage(conversation.conversationId)
                    val lastMessageText = latestMessage?.let { message ->
                        getMessageDesc(message)
                    } ?: ""
                    val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) } ?: ""
                    ConversationDisplayState(
                        conversation = conversation,
                        lastMessage = lastMessageText,
                        displayDateTime = displayDateTime // 计算显示时间
                    )
                }
                
                // 保存远程获取的会话到本地数据库（这会更新本地数据）
                response.forEach { conversation ->
                    conversationRepository.saveConversation(conversation)
                }
                
                // 更新UI为远程获取的最新数据
                _conversations.value = remoteConversationsWithLatestMessages
                
            } catch (e: UnauthorizedException) {
                // Token失效，通知登出
                Napier.e("Token失效，需要重新登录", e)
                // 更新用户仓库状态为登出
                userRepository.updateToLoggedOut()
                // 通知登录状态管理器用户已登出
                loginStateManager.setLoggedOut()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消，通常是页面跳转导致的正常取消，不记录为错误
                Napier.d("加载会话列表被取消")
            } catch (e: Exception) {
                Napier.e("加载会话列表失败", e)
                // 如果远程加载失败，至少保留本地缓存的数据
                if (_conversations.value.isEmpty()) {
                    // 如果之前没有加载过任何数据，尝试从本地加载
                    val localConversations = conversationRepository.getConversationsByUserId(uId)
                    val localConversationsWithLatestMessages = localConversations.map { conversation ->
                        val latestMessage = messageRepository.getLatestMessage(conversation.conversationId)
                        val lastMessageText = latestMessage?.let { message ->
                            when (message.type) {
                                MessageType.TEXT -> message.content
                                MessageType.FILE -> "文件消息"
                                MessageType.VOICE -> "语音消息"
                                MessageType.VIDEO -> "视频消息"
                                MessageType.IMAGE -> "图片消息"
                            }
                        } ?: ""
                        val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) } ?: ""
                        ConversationDisplayState(
                            conversation = conversation,
                            lastMessage = lastMessageText,
                            displayDateTime = displayDateTime
                        )
                    }
                    _conversations.value = localConversationsWithLatestMessages
                }
            } finally {
                _loading.value = false
            }
        }

    }


    /**
     * 获取私聊会话
     * 只需要
     * @param friendId 
     */
    fun getOrCreatePrivateChat(userId:Long, friendId: Long) {
        viewModelScope.launch {
            // 应该先在 本地查一下 是否存在
            Napier.d("开始获取私聊会话:  好友ID=$friendId")
            
            // 先尝试从本地数据库获取会话
            val localConversation = conversationRepository.getLocalConversationByMembers(userId, friendId)


            val conversation = if (localConversation != null) {
                Napier.d("从本地数据库找到会话: 会话ID=${localConversation.conversationId}")
                localConversation
            } else {
                Napier.d("本地数据库未找到会话，从远程创建: 好友ID=$friendId")
                // 直接调用 API 并返回结果
                try {
                    ConversationApi.createOrGetConversation(friendId)
                } catch (e: UnauthorizedException) {
                    // Token失效，通知登出
                    Napier.e("Token失效，需要重新登录", e)
                    // 更新用户仓库状态为登出
                    userRepository.updateToLoggedOut()
                    // 通知登录状态管理器用户已登出
                    loginStateManager.setLoggedOut()
                    throw e
                }
            }

            Napier.d("成功获取私聊会话: 会话ID=${conversation.conversationId}")

            // 保存到本地数据库
            conversationRepository.saveConversation(conversation)

            // 创建ConversationDisplayState对象
            val latestMessage = messageRepository.getLatestMessage(conversation.conversationId)
            val lastMessageText = latestMessage?.let { message ->
                when (message.type) {
                    MessageType.TEXT -> message.content
                    MessageType.FILE -> "文件消息"
                    MessageType.VOICE -> "语音消息"
                    MessageType.VIDEO -> "视频消息"
                    MessageType.IMAGE -> "图片消息"
                }
            } ?: ""
            val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) }?:""

            val conversationDisplayState = ConversationDisplayState(
                conversation = conversation,
                lastMessage = lastMessageText,
                displayDateTime = displayDateTime
            )

            // 更新会话列表状态
            _conversations.update {
                // 根据 conversationId 比对是否存在，不存在则添加
                // 存在的话则提高其位置，将其放在前面
                val existingIndex = it.indexOfFirst { conv -> conv.conversation.conversationId == conversation.conversationId }
                if (existingIndex >= 0) {
                    // 如果存在，将其移到列表前面
                    val mutableList = it.toMutableList()
                    mutableList.removeAt(existingIndex)
                    mutableList.add(0, conversationDisplayState)
                    mutableList
                } else {
                    // 如果不存在，添加到列表前面
                    listOf(conversationDisplayState) + it
                }
            }

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
    suspend fun createGroupChat(groupName:String , desc:String?,members : List<UserInfo>): ConversationRes {

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
            // 更新用户仓库状态为登出
            userRepository.updateToLoggedOut()
            // 通知登录状态管理器用户已登出
            loginStateManager.setLoggedOut()
            throw e
        }

        Napier.d("成功获取群聊会话: 会话ID=${conversation.conversationId}")

        // 保存到本地数据库
        conversationRepository.saveConversation(conversation)

        // 创建ConversationDisplayState对象
        val latestMessage = messageRepository.getLatestMessage(conversation.conversationId)
        val lastMessageText = latestMessage?.let { message ->
            when (message.type) {
                MessageType.TEXT -> message.content
                MessageType.FILE -> "文件消息"
                MessageType.VOICE -> "语音消息"
                MessageType.VIDEO -> "视频消息"
                MessageType.IMAGE -> "图片消息"
            }
        } ?: ""
        val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) }?:""

        val conversationDisplayState = ConversationDisplayState(
            conversation = conversation,
            lastMessage = lastMessageText,
            displayDateTime = displayDateTime
        )

        // 更新会话列表状态
        _conversations.update {
            // 根据 conversationId 比对是否存在，不存在则添加
            // 存在的话则提高其位置，将其放在前面
            val existingIndex = it.indexOfFirst { conv -> conv.conversation.conversationId == conversation.conversationId }
            if (existingIndex >= 0) {
                // 如果存在，将其移到列表前面
                val mutableList = it.toMutableList()
                mutableList.removeAt(existingIndex)
                mutableList.add(0, conversationDisplayState)
                mutableList
            } else {
                // 如果不存在，添加到列表前面
                listOf(conversationDisplayState) + it
            }
        }

        return conversation
    }




    /**
     * 同步会话信息到本地并更新UI
     * 当在其他页面（如ChatRoom）创建新会话后，可以通过此方法将新会话同步到会话列表
     * @param conversationId 会话ID
     */
    suspend fun syncConversationToUI(conversationId: Long) {
        try {
            // 从远程获取会话信息并保存到本地
            val conversation = conversationRepository.getConversation(conversationId)
            
            Napier.d("同步会话信息到UI: 会话ID=${conversation.conversationId}")
            
            // 创建ConversationDisplayState对象
            val latestMessage = messageRepository.getLatestMessage(conversation.conversationId)
            val lastMessageText = latestMessage?.let { message ->
                when (message.type) {
                    MessageType.TEXT -> message.content
                    MessageType.FILE -> "文件消息"
                    MessageType.VOICE -> "语音消息"
                    MessageType.VIDEO -> "视频消息"
                    MessageType.IMAGE -> "图片消息"
                }
            } ?: ""

            val displayDateTime = latestMessage?.let { calculateDisplayDateTime(it.time) }?:""

            val conversationDisplayState = ConversationDisplayState(
                conversation = conversation,
                lastMessage = lastMessageText,
                displayDateTime = displayDateTime
            )

            // 更新会话列表状态
            _conversations.update { currentList ->
                val existingIndex = currentList.indexOfFirst { conv -> conv.conversation.conversationId == conversation.conversationId }
                if (existingIndex >= 0) {
                    // 如果存在，将其移到列表前面
                    val mutableList = currentList.toMutableList()
                    mutableList.removeAt(existingIndex)
                    mutableList.add(0, conversationDisplayState)
                    mutableList
                } else {
                    // 如果不存在，添加到列表前面
                    listOf(conversationDisplayState) + currentList
                }
            }
        } catch (e: Exception) {
            Napier.e("同步会话信息失败", e)
        }
    }

    /**
     * 计算显示时间日期
     * 如果是今天 那么就展示 时间和分钟
     * 如果是昨天 那么就展示昨天 时间和分钟
     * 如果是7天之内的 那么就展示星期几
     * 如果是更早的,没超过一年 那么就展示日期  格式 mm-dd
     * 如果超过了 一年那么久展示 年月日  格式 yyyy-mm-dd
     */
    @RequiresApi(android.os.Build.VERSION_CODES.O)
    private fun calculateDisplayDateTime(createAt: kotlinx.datetime.LocalDateTime): String {
        return try {
            val dateTime = createAt
            val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            
            when {
                // 今天
                dateTime.date == now.date -> {
                    "${dateTime.hour}:${dateTime.minute.toString().padStart(2, '0')}"
                }
                // 昨天
                dateTime.date == now.date.minus(1, kotlinx.datetime.DateTimeUnit.DAY) -> {
                    "昨天 ${dateTime.hour}:${dateTime.minute.toString().padStart(2, '0')}"
                }
                // 7天之内
                (now.date.toEpochDays() - dateTime.date.toEpochDays()) <= 7 -> {
                    val dayOfWeek = when(dateTime.dayOfWeek) {
                        kotlinx.datetime.DayOfWeek.MONDAY -> "周一"
                        kotlinx.datetime.DayOfWeek.TUESDAY -> "周二"
                        kotlinx.datetime.DayOfWeek.WEDNESDAY -> "周三"
                        kotlinx.datetime.DayOfWeek.THURSDAY -> "周四"
                        kotlinx.datetime.DayOfWeek.FRIDAY -> "周五"
                        kotlinx.datetime.DayOfWeek.SATURDAY -> "周六"
                        kotlinx.datetime.DayOfWeek.SUNDAY -> "周日"
                        else -> "周${dateTime.dayOfWeek.value}"
                    }
                    dayOfWeek
                }
                // 更早的
                else -> {
                    val yearDiff = now.year - dateTime.year
                    if (yearDiff > 0) {
                        "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')}"
                    } else {
                        "${dateTime.monthNumber.toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')}"
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("解析时间失败", e)
            createAt.toString() // 返回原始时间字符串
        }
    }

}