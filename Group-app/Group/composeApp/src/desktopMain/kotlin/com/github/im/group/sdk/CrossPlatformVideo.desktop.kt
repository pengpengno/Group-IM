package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Canvas
import javax.swing.JPanel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.print.attribute.standard.Media

//
//class VideoPlayer {
//    private val _isPlaying = MutableStateFlow(false)
//    actual val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
//
//    private var mediaPlayer: MediaPlayer? = null
//
//    init {
//        // 初始化 JavaFX
//        JFXPanel()
//    }
//
//    actual fun setSource(url: String) {
//        val media = Media(url)
//        mediaPlayer = MediaPlayer(media)
//    }
//
//    actual fun play() {
//        mediaPlayer?.play()
//        _isPlaying.value = true
//    }
//
//    actual fun pause() {
//        mediaPlayer?.pause()
//        _isPlaying.value = false
//    }
//
//    actual fun stop() {
//        mediaPlayer?.stop()
//        _isPlaying.value = false
//    }
//}

@Composable
actual fun CrossPlatformVideo(
    url: String,
    modifier: Modifier,
    size: Dp
) {
    val mediaPlayer = remember(url) {
        val factory = MediaPlayerFactory()
        factory.mediaPlayers().newEmbeddedMediaPlayer()
    }

    LaunchedEffect(url) {
        mediaPlayer.media().play(url)
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            JPanel().apply {
                val canvas = Canvas()
                add(canvas)
                mediaPlayer.attach(canvas)
            }
        }
    )
}