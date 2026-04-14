package com.github.im.group.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.im.group.db.entities.MessageType
import com.github.im.group.model.MessageItem
import com.github.im.group.sdk.File
import com.github.im.group.viewmodel.ChatRoomViewModel

/**
 * V8 Architecture: MessageMediaManager (精简版)
 * 
 * - 职责: 仅管理媒体画廊的"当前浏览索引"与前进后退导航状态
 * - 媒体列表数据来源: 直接订阅 MessageStore.messages StateFlow, 派生出媒体子集
 * - 不再包含业务逻辑, 不直接依赖 ChatRoomViewModel 作为数据源
 */
@Stable
class MessageMediaManager(initialFiles: List<File>, initialIndex: Int = -1) {
    private var _mediaFiles by mutableStateOf(initialFiles)
    private var _currentIndex by mutableIntStateOf(initialIndex)

    val mediaFiles: List<File> get() = _mediaFiles
    val currentIndex: Int get() = _currentIndex

    val currentMediaFile: File?
        get() = _mediaFiles.getOrNull(_currentIndex)

    val hasNext: Boolean get() = _currentIndex < _mediaFiles.size - 1
    val hasPrevious: Boolean get() = _currentIndex > 0

    fun goToNext(): Boolean {
        if (!hasNext) return false
        _currentIndex++
        return true
    }

    fun goToPrevious(): Boolean {
        if (!hasPrevious) return false
        _currentIndex--
        return true
    }

    internal fun update(files: List<File>, currentFileId: String) {
        _mediaFiles = files
        _currentIndex = files.indexOfFirst { it.name == currentFileId || it.path.contains(currentFileId) }.coerceAtLeast(-1)
    }
}

/**
 * Compose 工厂函数
 * 订阅 MessageStore.messages StateFlow → 自动派生媒体列表 → 自动更新索引
 * 保证画廊与消息数据同步，无需手动回调串联
 */
@Composable
fun rememberMessageMediaManager(
    messageViewModel: ChatRoomViewModel,
    currentMessage: MessageItem
): MessageMediaManager {
    // 直接消费来自 MessageStore 的 StateFlow，Compose 自动重组
    val allMessages by messageViewModel.messageStore.messages.collectAsStateWithLifecycle()

    val mediaMessages = remember(allMessages) {
        allMessages.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }
    }

    val mediaFiles = remember(mediaMessages) {
        mediaMessages.mapNotNull { msg ->
            runCatching { messageViewModel.getFile(msg.content) }.getOrNull()
        }
    }

    val manager = remember { MessageMediaManager(mediaFiles) }

    // 当媒体列表变化时，同步更新 manager（保持 Compose stable 对象引用）
    remember(mediaFiles, currentMessage.content) {
        manager.update(mediaFiles, currentMessage.content)
    }

    return manager
}