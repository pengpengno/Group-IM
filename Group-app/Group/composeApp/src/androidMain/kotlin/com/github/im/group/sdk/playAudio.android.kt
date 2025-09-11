package com.github.im.group.sdk

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

actual fun playAudio(bytes: ByteArray) {

    val context = VoiceRecorderFactory.context

}


class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var _isPlaying: Boolean = false

    override val duration: Long
        get() = mediaPlayer?.duration?.toLong() ?: 0L

    override val currentPosition: Long
        get() = mediaPlayer?.currentPosition?.toLong() ?: 0L

    override val isPlaying: Boolean
        get() = _isPlaying

    override fun play(filePath: String) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(java.io.File(filePath)))
                prepare()
                setOnCompletionListener {
                    _isPlaying = false
                    stop()
                }
            }
        }
        mediaPlayer?.start()
        _isPlaying = true
    }

    override fun pause() {
        mediaPlayer?.pause()
        _isPlaying = false
    }

    override fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying = false
    }

    override fun seekTo(positionMillis: Long) {
        mediaPlayer?.seekTo(positionMillis.toInt())
    }
}


