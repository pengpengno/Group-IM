package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.AudioTrack
import com.github.im.group.sdk.MediaStream
import com.github.im.group.sdk.VideoScreenView
import com.github.im.group.sdk.VideoTrack

@Composable
fun VideoCallFloatingWindow(
    remoteUser: UserInfo,
    localMediaStream: MediaStream?,
    remoteVideoTrack: VideoTrack?,
    remoteAudioTrack: AudioTrack?,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    onExpand: () -> Unit,
    onEndCall: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMicrophone: () -> Unit,
    windowSize: Dp = 156.dp
) {
    Surface(
        modifier = Modifier.size(windowSize, windowSize * 1.45f),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF111827),
        shadowElevation = 16.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val previewTrack = remoteVideoTrack ?: localMediaStream?.videoTracks?.firstOrNull()
            val previewAudio = if (remoteVideoTrack != null) remoteAudioTrack else localMediaStream?.audioTracks?.firstOrNull()

            if (previewTrack != null) {
                VideoScreenView(
                    modifier = Modifier.fillMaxSize(),
                    videoTrack = previewTrack,
                    audioTrack = previewAudio
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1F2937)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = remoteUser.username.take(1).ifBlank { "?" },
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = remoteUser.username.ifBlank { "Video call" },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniActionButton(onClick = onToggleMicrophone) {
                    Icon(
                        imageVector = if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle microphone",
                        tint = Color.White
                    )
                }
                MiniActionButton(onClick = onToggleCamera) {
                    Icon(
                        imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle camera",
                        tint = Color.White
                    )
                }
                MiniActionButton(onClick = onExpand) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Expand call",
                        tint = Color.White
                    )
                }
                MiniActionButton(onClick = onEndCall, containerColor = Color(0xFFDC2626)) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniActionButton(
    onClick: () -> Unit,
    containerColor: Color = Color.Black.copy(alpha = 0.45f),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(containerColor)
    ) {
        content()
    }
}
