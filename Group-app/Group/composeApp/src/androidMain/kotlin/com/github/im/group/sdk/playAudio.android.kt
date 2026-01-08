package com.github.im.group.sdk

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.aakira.napier.Napier


/**
 * Android平台音频播放器实现
 * 使用ExoPlayer替代MediaPlayer以获得更好的性能和控制
 */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    
    companion object {
        // 全局播放器实例，确保同一时间只有一个音频在播放
        @Volatile
        private var currentPlayer: ExoPlayer? = null
        private var currentFilePath: String? = null
        private var isPlayingState = false
    }
    
    override val duration: Long
        get() = currentPlayer?.duration ?: 0L

    override val currentPosition: Long
        get() = currentPlayer?.currentPosition ?: 0L

    override val isPlaying: Boolean
        get() = isPlayingState
        
    override fun isCurrentlyPlaying(filePath: String?): Boolean {
        return if (filePath != null) {
            isPlayingState && currentFilePath == filePath
        } else {
            isPlayingState && currentPlayer?.playWhenReady == true
        }
    }
    
    override fun getCurrentFilePath(): String? = currentFilePath
        

    
    override fun play(filePath: String) {
        synchronized(AndroidAudioPlayer::class.java) {
            try {
                // 如果正在播放相同的文件，则不执行任何操作
                if (currentFilePath == filePath && currentPlayer?.playWhenReady == true) {
                    return
                }

                // 如果是同一个文件但暂停状态，则继续播放
                if (currentFilePath == filePath && currentPlayer != null) {
                    currentPlayer?.play()
                    isPlayingState = true
                    return
                }

                // 停止当前播放的音频
                stop()

                // 创建新的ExoPlayer实例
                val exoPlayer = ExoPlayer.Builder(context).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                // 播放完成
                                isPlayingState = false
                                currentFilePath = null
                                // 播放完成后回到开头
                                try {
                                    seekTo(0)
                                } catch (e: Exception) {
                                    Napier.e("重置播放位置失败", e)
                                }
                            } else if (playbackState == Player.STATE_READY) {
                                isPlayingState = playWhenReady
                            }
                        }
                        
                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            isPlayingState = playWhenReady
                        }
                    })
                }

                // 设置媒体源
                val mediaItem = MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(filePath)))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                
                // 开始播放
                exoPlayer.play()

                // 更新状态
                currentPlayer = exoPlayer
                currentFilePath = filePath
                isPlayingState = true
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
                currentPlayer?.pause()
                isPlayingState = false
            } catch (e: Exception) {
                Napier.e("暂停音频失败", e)
            }
        }
    }

    override fun stop() {
        synchronized(AndroidAudioPlayer::class.java) {
            try {
                currentPlayer?.let { player ->
                    player.stop()
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
                currentPlayer?.seekTo(positionMillis)
            } catch (e: Exception) {
                Napier.e("跳转音频位置失败", e)
            }
        }
    }
    
}