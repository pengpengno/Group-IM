package com.github.im.group.sdk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// 桌面平台的WebRTC Track和Stream实现（占位符）
class DesktopVideoTrack : VideoTrack() {
    override val id: String = "desktop_video_track"
    override val isEnabled: Boolean = false
    override fun setEnabled(enabled: Boolean) {
        // 桌面平台不支持WebRTC，所以这个方法什么都不做
    }
}

class DesktopAudioTrack : AudioTrack() {
    override val id: String = "desktop_audio_track"
    override val isEnabled: Boolean = false
    override fun setEnabled(enabled: Boolean) {
        // 桌面平台不支持WebRTC，所以这个方法什么都不做
    }
}

class DesktopMediaStream : MediaStream() {
    override val id: String = "desktop_media_stream"
    override val videoTracks: List<VideoTrack> = emptyList()
    override val audioTracks: List<AudioTrack> = emptyList()
}

/**
 * 桌面平台WebRTC视频通话实现
 * 注意：桌面平台的WebRTC实现需要额外的库支持，这里提供一个基础框架
 */
@Composable
actual fun WebRTCVideoCall(
    modifier: Modifier,
    onCallStarted: () -> Unit,
    onCallEnded: () -> Unit,
    onError: (String) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("桌面平台WebRTC视频通话")
    }
}

/**
 * 本地视频预览
 */
@Composable
actual fun LocalVideoPreview(
    modifier: Modifier,
    localMediaStream: MediaStream?
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("本地视频预览(桌面平台)")
    }
}

/**
 * 远程视频显示
 */
@Composable
actual fun RemoteVideoView(
    modifier: Modifier,
    remoteVideoTrack: VideoTrack?


) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("远程视频显示")
    }
}

// Note: WebRTC is not supported on desktop platform with webrtc-kmp
// This is a placeholder implementation
@Composable
actual fun VideoScreenView(
    modifier: Modifier,
    videoTrack: VideoTrack?,
    audioTrack: AudioTrack?
) {
    Box(
        modifier = (modifier ?: Modifier).fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("桌面平台不支持WebRTC")
    }
}