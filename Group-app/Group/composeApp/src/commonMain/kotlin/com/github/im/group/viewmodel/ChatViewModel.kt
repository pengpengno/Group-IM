package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.ConversationApi
import com.github.im.group.api.ConversationRes
import com.github.im.group.config.SocketClient
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FilePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.aakira.napier.Napier



/**
 * 聊天界面的 ViewModel
 */
class ChatViewModel (
    val tcpClient: SocketClient,
    val userRepository: UserRepository,
    val filePicker: FilePicker,
): ViewModel() {


    private val _conversations = MutableStateFlow(listOf(ConversationRes()))

    val uiState:  StateFlow<List<ConversationRes>> = _conversations.asStateFlow()



    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading


    fun getConversations(uId: Long ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = ConversationApi.getActiveConversationsByUserId(uId)
                _conversations.value = response
            } catch (e: Exception) {
                Napier.e("加载会话列表失败", e)
            } finally {
                _loading.value = false
            }
        }

    }

    /**
     * 获取私聊会话
     */
    suspend fun getPrivateChat(friendId: Long): ConversationRes {
        val userInfo = userRepository.withLoggedInUser {
            it.user
        }

        Napier.d("开始获取私聊会话: 用户ID=$friendId")

        // 直接调用 API 并返回结果
        val conversation = ConversationApi.createOrGetConversation(userInfo.userId, friendId)
        
        Napier.d("成功获取私聊会话: 会话ID=${conversation.conversationId}")
        
        // 更新会话列表状态
        _conversations.update {
            // 根据 conversationId 比对是否存在，不存在则添加
            // 存在的话则提高其位置，将其放在前面
            val existingIndex = it.indexOfFirst { conv -> conv.conversationId == conversation.conversationId }
            if (existingIndex >= 0) {
                // 如果存在，将其移到列表前面
                val mutableList = it.toMutableList()
                mutableList.removeAt(existingIndex)
                mutableList.add(0, conversation)
                mutableList
            } else {
                // 如果不存在，添加到列表前面
                listOf(conversation) + it
            }
        }
        
        return conversation
    }




}