package com.github.im.group.sdk

expect fun playAudio(bytes: ByteArray)

/**
 * 音频播放
 */
interface AudioPlayer {
    /**
     * 播放
     * 路径必须可读
     * @param filePath 文件路径
     *
     */
    fun play(filePath: String)

    /**
     * 暂停
     */
    fun pause()

    /**
     * 停止播放
     */
    fun stop()

    /**
     * 跳转
     * @@param positionMillis 跳转的位置 milliseconds
     */
    fun seekTo(positionMillis: Long)

    /**
     * 时长 单位 Millis
     */
    val duration: Long
    val currentPosition: Long
    val isPlaying: Boolean
    
    /**
     * 检查是否正在播放指定路径的音频
     * @param filePath 音频文件路径，如果为null则检查是否有任何音频在播放
     */
    fun isCurrentlyPlaying(filePath: String? = null): Boolean

    /**
     * 获取当前播放的文件路径
     */
    fun getCurrentFilePath(): String?

}

/**
 * 回放状态设置
 */
data class PlaybackState(
    val filePath: String,
    val duration: Long,
    val currentPosition: Long,
    val isPlaying: Boolean
)
