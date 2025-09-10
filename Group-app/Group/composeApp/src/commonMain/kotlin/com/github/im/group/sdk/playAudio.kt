package com.github.im.group.sdk

expect fun playAudio(bytes: ByteArray)

/**
 * 音频播放
 */
interface AudioPlayer {
    /**
     * 播放
     */
    fun play(filePath: String)

    /**
     * 暂停
     */
    fun pause()
    fun stop()
    fun seekTo(positionMillis: Long)
    val duration: Long
    val currentPosition: Long
    val isPlaying: Boolean

}
data class PlaybackState(
    val filePath: String,
    val duration: Long,
    val currentPosition: Long,
    val isPlaying: Boolean
)
