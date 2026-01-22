package com.github.im.group.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.im.group.model.MessageItem
import com.github.im.group.sdk.File
import com.github.im.group.viewmodel.ChatRoomViewModel

/**
 * 消息媒体资源管理器
 * 用于管理聊天会话中的所有媒体资源
 */
@Stable
class MessageMediaManager {
    private var _mediaFiles by mutableStateOf<List<File>>(emptyList())
    private var _currentIndex by mutableStateOf(-1)
    
    var mediaFiles: List<File>
        get() = _mediaFiles
        set(value) {
            _mediaFiles = value
        }
    
    var currentIndex: Int
        get() = _currentIndex
        set(value) {
            if (value >= 0 && value < _mediaFiles.size) {
                _currentIndex = value
            }
        }
    
    val currentMediaFile: File?
        get() = if (_currentIndex >= 0 && _currentIndex < _mediaFiles.size) {
            _mediaFiles[_currentIndex]
        } else {
            null
        }
    
    val hasNext: Boolean
        get() = _currentIndex < _mediaFiles.size - 1
    
    val hasPrevious: Boolean
        get() = _currentIndex > 0
    
    fun goToNext(): Boolean {
        if (hasNext) {
            _currentIndex++
            return true
        }
        return false
    }
    
    fun goToPrevious(): Boolean {
        if (hasPrevious) {
            _currentIndex--
            return true
        }
        return false
    }
    
    fun setCurrentFile(file: File): Boolean {
        val index = _mediaFiles.indexOf(file)
        if (index != -1) {
            _currentIndex = index
            return true
        }
        return false
    }
    
    companion object {
        fun create(mediaMessages: List<MessageItem>, currentMessage: MessageItem, messageViewModel: ChatRoomViewModel): MessageMediaManager {
            val manager = MessageMediaManager()
            
            // 异步获取所有媒体文件
            val mediaFiles = mediaMessages
                .mapNotNull { message ->
                    // 获取对应文件
                    try {
                        messageViewModel.getFile(message.content)
                    } catch (e: Exception) {
                        null
                    }
                }
            
            manager.mediaFiles = mediaFiles
            
            // 设置当前文件的索引
            val currentFile = try {
                messageViewModel.getFile(currentMessage.content)
            } catch (e: Exception) {
                null
            }
            
            if (currentFile != null) {
                val currentIndex = mediaFiles.indexOf(currentFile)
                if (currentIndex != -1) {
                    manager.currentIndex = currentIndex
                }
            }
            
            return manager
        }
    }
}

/**
 * 记住消息媒体管理器
 */
@Composable
fun rememberMessageMediaManager(
    messageViewModel: ChatRoomViewModel,
    currentMessage: MessageItem
): MessageMediaManager {
    val mediaMessages by messageViewModel.mediaMessages.collectAsStateWithLifecycle()
    
    return remember(mediaMessages, currentMessage) {
        val mediaFiles = mediaMessages.mapNotNull { message ->
            runCatching {
                messageViewModel.getFile(message.content)
            }.getOrNull()
        }
        
        val currentFile = runCatching {
            messageViewModel.getFile(currentMessage.content)
        }.getOrNull()
        
        val manager = MessageMediaManager()
        manager.mediaFiles = mediaFiles
        
        if (currentFile != null) {
            val currentIndex = mediaFiles.indexOf(currentFile)
            if (currentIndex != -1) {
                manager.currentIndex = currentIndex
            }
        }
        
        manager
    }
}