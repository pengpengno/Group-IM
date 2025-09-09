package com.github.im.group.sdk

expect fun playAudio(bytes: ByteArray)


interface AudioPlayer {
    fun play(filePath: String)
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
