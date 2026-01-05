package com.github.im.group.sdk

actual fun playAudio(bytes: ByteArray) {
}

class DesktopAudioPlayer : AudioPlayer {
    private var _isPlaying = false
    private var _duration = 0L
    private var _currentPosition = 0L
    private var _currentFilePath: String? = null

    override val duration: Long
        get() = _duration

    override val currentPosition: Long
        get() = _currentPosition

    override val isPlaying: Boolean
        get() = _isPlaying

    override fun isCurrentlyPlaying(filePath: String?): Boolean {
        return if (filePath != null) {
            _isPlaying && _currentFilePath == filePath
        } else {
            _isPlaying
        }
    }

    override fun getCurrentFilePath(): String? {
        return _currentFilePath
    }

    override fun play(filePath: String) {
        _isPlaying = true
        _currentFilePath = filePath
        // 桌面平台音频播放实现
    }

    override fun pause() {
        _isPlaying = false
    }

    override fun stop() {
        _isPlaying = false
        _currentFilePath = null
    }

    override fun seekTo(positionMillis: Long) {
        _currentPosition = positionMillis
    }
}