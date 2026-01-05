package com.github.im.group.sdk

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import io.github.aakira.napier.Napier

actual fun playAudio(bytes: ByteArray) {
    val context = VoiceRecorderFactory.context
}

/**
 * Android平台音频播放器实现
 * 集成播放控制和状态管理功能
 */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    
    companion object {
        // 全局播放器实例，确保同一时间只有一个音频在播放
        @Volatile
        private var currentPlayer: MediaPlayer? = null
        private var currentFilePath: String? = null
        private var isPlayingState = false
    }
    
    override val duration: Long
        get() = currentPlayer?.duration?.toLong() ?: 0L

    override val currentPosition: Long
        get() = currentPlayer?.currentPosition?.toLong() ?: 0L

    override val isPlaying: Boolean
        get() = isPlayingState
        
    fun isCurrentlyPlaying(filePath: String? = null): Boolean {
        return if (filePath != null) {
            isPlayingState && currentFilePath == filePath
        } else {
            isPlayingState
        }
    }
    
    override fun play(filePath: String) {
        synchronized(AndroidAudioPlayer::class.java) {
            try {
                // 如果正在播放相同的文件，则不执行任何操作
                if (currentFilePath == filePath && currentPlayer?.isPlaying == true) {
                    return
                }

                // 停止当前播放的音频
                stop()

                // 创建新的MediaPlayer实例
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.fromFile(java.io.File(filePath)))
                    prepare()
                    setOnCompletionListener { mp ->
                        isPlayingState = false
                        currentFilePath = null
                        // 播放完成后回到开头
                        try {
                            mp.seekTo(0)
                        } catch (e: Exception) {
                            Napier.e("重置播放位置失败", e)
                        }
                    }
                }

                // 更新状态
                currentPlayer = mediaPlayer
                currentFilePath = filePath
                isPlayingState = true

                // 开始播放
                mediaPlayer.start()
            } catch (e: Exception) {
                isPlayingState = false
                currentFilePath = null
                Napier.e("播放音频失败: $filePath", e)
                e.printStackTrace()
            }
        }
    }

    override fun pause() {
        synchronized(AndroidAudioPlayer::class.java) {
            try {
                currentPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        isPlayingState = false
                    }
                }
            } catch (e: Exception) {
                Napier.e("暂停音频失败", e)
            }
        }
    }

    override fun stop() {
        synchronized(AndroidAudioPlayer::class.java) {
            try {
                currentPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.reset()
                    player.release()
                }
            } catch (e: Exception) {
                Napier.e("停止音频失败", e)
            } finally {
                currentPlayer = null
                currentFilePath = null
                isPlayingState = false
            }
        }
    }

    override fun seekTo(positionMillis: Long) {
        synchronized(AndroidAudioPlayer::class.java) {
            try {
                currentPlayer?.seekTo(positionMillis.toInt())
            } catch (e: Exception) {
                Napier.e("跳转音频位置失败", e)
            }
        }
    }
    
    fun getCurrentFilePath(): String? = currentFilePath
}