package com.github.im.group.manager

import com.github.im.group.sdk.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 全局播放状态
 */
data class AudioPlaybackState(
    val messageId: String? = null,
    val senderName: String? = null,
    val audioPath: String? = null,
    val duration: Long = 0,
    val currentPosition: Long = 0,
    val isPlaying: Boolean = false,
    val sessionName: String? = null
)

/**
 * 全局音频播放管理器，管理所有音频播放逻辑，保证单实例播放
 */
class AudioPlaybackManager(private val audioPlayer: AudioPlayer) {
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * 播放语音
     */
    fun play(messageId: String, audioPath: String, senderName: String, duration: Long, sessionName: String? = null) {
        // 如果正在播放同一个音频，且是同一个消息，则切换播放/暂停
        if (_playbackState.value.messageId == messageId && _playbackState.value.audioPath == audioPath) {
            if (audioPlayer.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        // 停止之前的播放
        stop()

        // 更新状态并播放新音频
        _playbackState.value = AudioPlaybackState(
            messageId = messageId,
            senderName = senderName,
            audioPath = audioPath,
            duration = duration,
            isPlaying = true,
            sessionName = sessionName
        )
        
        audioPlayer.play(audioPath)
        startPolling()
    }

    fun pause() {
        audioPlayer.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        val path = _playbackState.value.audioPath
        if (path != null) {
            audioPlayer.play(path)
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startPolling()
        }
    }

    fun stop() {
        audioPlayer.stop()
        _playbackState.value = AudioPlaybackState()
        stopPolling()
    }

    fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
        _playbackState.value = _playbackState.value.copy(currentPosition = position)
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                val playerPlaying = audioPlayer.isPlaying
                val currentPos = audioPlayer.currentPosition
                val dur = audioPlayer.duration
                
                // 更新播放进度和状态
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = playerPlaying,
                    currentPosition = currentPos,
                    duration = if (dur > 0) dur else _playbackState.value.duration
                )
                
                // 如果播放已停止且已回到开头，说明播放结束
                if (!playerPlaying && currentPos == 0L && _playbackState.value.currentPosition > 0) {
                     // 暂不自动重置 metadata，保留最后一条播放记录在 bar 上
                }
                
                delay(100)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
