package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 媒体资源画廊管理器，用于管理媒体资源列表和浏览状态
 */
@Stable
class MediaGalleryManager {
    private var _currentIndex by mutableStateOf(0)
    private var _mediaList by mutableStateOf(emptyList<File>())
    
    var currentIndex: Int
        get() = _currentIndex
        set(value) {
            if (value >= 0 && value < _mediaList.size) {
                _currentIndex = value
            }
        }
    
    var mediaList: List<File>
        get() = _mediaList
        set(value) {
            _mediaList = value
            // 重置索引为0，或保持在有效范围内
            _currentIndex = _currentIndex.coerceIn(0, (_mediaList.size - 1).coerceAtLeast(0))
        }
    
    val currentMedia: File?
        get() = if (_mediaList.isNotEmpty() && _currentIndex < _mediaList.size) {
            _mediaList[_currentIndex]
        } else {
            null
        }
    
    val hasNext: Boolean
        get() = _currentIndex < _mediaList.size - 1
    
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
    
    fun goToIndex(index: Int): Boolean {
        if (index >= 0 && index < _mediaList.size) {
            _currentIndex = index
            return true
        }
        return false
    }
    
    fun reset() {
        _currentIndex = 0
        _mediaList = emptyList()
    }
    
    companion object {
        fun create(initialList: List<File> = emptyList(), startIndex: Int = 0): MediaGalleryManager {
            val manager = MediaGalleryManager()
            manager.mediaList = initialList
            if (startIndex < initialList.size) {
                manager.currentIndex = startIndex
            }
            return manager
        }
    }
}

/**
 * 媒体资源画廊状态
 */
data class MediaGalleryState(
    val currentMedia: File?,
    val currentIndex: Int,
    val totalMediaCount: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * 提供全局媒体资源画廊管理器
 */
val LocalMediaGalleryManager = staticCompositionLocalOf<MediaGalleryManager> {
    MediaGalleryManager.create()
}