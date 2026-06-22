package com.github.im.group.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.AudioTrack
import com.github.im.group.sdk.MediaStream
import com.github.im.group.sdk.TryGetVideoCallPermissions
import com.github.im.group.sdk.VideoScreenView
import com.github.im.group.sdk.VideoTrack
import com.github.im.group.ui.UserAvatar
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VideoCallLauncher(remoteUser: UserInfo, onCallEnded: () -> Unit = {}) {
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()

    LaunchedEffect(remoteUser.userId, videoCallState.callStatus) {
        if (videoCallState.callStatus == VideoCallStatus.IDLE) {
            videoCallViewModel.startCall(remoteUser)
        }
    }

    HandleCallEnded(videoCallState.callStatus, onCallEnded)
    RenderCallSurface(
        fallbackUser = videoCallState.caller ?: remoteUser,
        videoCallState = videoCallState,
        videoCallViewModel = videoCallViewModel,
        onCallEnded = onCallEnded
    )
}

@Composable
fun MeetingLauncher(roomId: String, participantIds: List<String>, onCallEnded: () -> Unit = {}) {
    val videoCallViewModel: VideoCallViewModel = koinViewModel()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()

    LaunchedEffect(roomId, videoCallState.callStatus) {
        if (videoCallState.callStatus == VideoCallStatus.IDLE) {
            videoCallViewModel.startMeeting(roomId, participantIds)
        }
    }

    HandleCallEnded(videoCallState.callStatus, onCallEnded)
    RenderCallSurface(
        fallbackUser = videoCallState.caller ?: UserInfo(0, "Group", ""),
        videoCallState = videoCallState,
        videoCallViewModel = videoCallViewModel,
        onCallEnded = onCallEnded
    )
}

@Composable
private fun HandleCallEnded(callStatus: VideoCallStatus, onCallEnded: () -> Unit) {
    var hasShownCallUi by remember { mutableStateOf(false) }

    LaunchedEffect(callStatus) {
        if (callStatus != VideoCallStatus.IDLE) {
            hasShownCallUi = true
        } else if (hasShownCallUi) {
            delay(600)
            onCallEnded()
        }
    }
}

@Composable
private fun RenderCallSurface(
    fallbackUser: UserInfo,
    videoCallState: VideoCallState,
    videoCallViewModel: VideoCallViewModel,
    onCallEnded: () -> Unit
) {
    val localMediaStream by videoCallViewModel.localMediaStream.collectAsState()

    if ((videoCallState.callStatus == VideoCallStatus.OUTGOING || videoCallState.callStatus == VideoCallStatus.CONNECTING) &&
        localMediaStream == null
    ) {
        // Mount the call UI first, then request camera/mic permission and continue
        // session setup from inside the call surface. This keeps the callee/
        // caller experience aligned with web: page first, device activation second.
        TryGetVideoCallPermissions(
            onAllGranted = { videoCallViewModel.preparePendingCallSession() },
            onAnyDenied = {
                videoCallViewModel.handlePendingCallPermissionDenied()
            }
        )
    }

    when (videoCallState.callStatus) {
        VideoCallStatus.IDLE -> Unit
        VideoCallStatus.PRE_JOIN -> {
            VideoCallIncomingNotification(
                caller = videoCallState.caller ?: fallbackUser,
                subtitle = "Meeting invite opened from notification. Join when you are ready.",
                acceptLabel = "Join",
                rejectLabel = "Dismiss",
                onAccept = { videoCallViewModel.acceptCall() },
                onReject = { videoCallViewModel.rejectCall() }
            )
        }
        VideoCallStatus.INCOMING -> {
            VideoCallIncomingNotification(
                caller = videoCallState.caller ?: fallbackUser,
                onAccept = { videoCallViewModel.acceptCall() },
                onReject = { videoCallViewModel.rejectCall() }
            )
        }
        VideoCallStatus.OUTGOING, VideoCallStatus.CONNECTING -> {
            CallFullscreenDialog(
                remoteUser = videoCallState.caller ?: fallbackUser,
                videoCallState = videoCallState,
                videoCallViewModel = videoCallViewModel,
                onEndCall = { videoCallViewModel.endCall() }
            )
        }
        VideoCallStatus.MINIMIZED -> {
            MinimizedCallOverlay(
                remoteUser = videoCallState.caller ?: fallbackUser,
                videoCallViewModel = videoCallViewModel,
                onEndCall = { videoCallViewModel.endCall() }
            )
        }
        VideoCallStatus.ENDED -> {
            CallEndedDialog(
                remoteUser = videoCallState.caller ?: fallbackUser,
                videoCallState = videoCallState,
                onDismiss = {
                    videoCallViewModel.dismissEndedSession()
                    onCallEnded()
                }
            )
        }
        VideoCallStatus.ACTIVE, VideoCallStatus.ENDING, VideoCallStatus.ERROR -> {
            CallFullscreenDialog(
                remoteUser = videoCallState.caller ?: fallbackUser,
                videoCallState = videoCallState,
                videoCallViewModel = videoCallViewModel,
                onEndCall = { videoCallViewModel.endCall() }
            )
        }
    }
}

@Composable
private fun CallFullscreenDialog(
    remoteUser: UserInfo,
    videoCallState: VideoCallState,
    videoCallViewModel: VideoCallViewModel,
    onEndCall: () -> Unit
) {
    val localStream by videoCallViewModel.localMediaStream.collectAsState()
    val remoteVideoTracks by videoCallViewModel.remoteVideoTracks.collectAsState()
    val remoteAudioTracks by videoCallViewModel.remoteAudioTracks.collectAsState()
    val durationText = rememberCallDurationText(videoCallState.callStartTime)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050816))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                        )
                    )
            )

            RemoteVideoStage(
                remoteUser = remoteUser,
                videoCallState = videoCallState,
                remoteVideoTracks = remoteVideoTracks,
                remoteAudioTracks = remoteAudioTracks,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.38f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = remoteUser.username.ifBlank { "Video call" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (videoCallState.callStatus) {
                            VideoCallStatus.PRE_JOIN -> "Ready to join"
                            VideoCallStatus.OUTGOING -> "Calling"
                            VideoCallStatus.CONNECTING -> "Connecting"
                            VideoCallStatus.ENDING -> "Ending"
                            VideoCallStatus.ERROR -> videoCallState.errorMessage ?: "Connection issue"
                            else -> durationText
                        },
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            IconButton(
                onClick = { videoCallViewModel.minimizeCall() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.Default.Minimize,
                    contentDescription = "Minimize call",
                    tint = Color.White
                )
            }

            LocalPreviewCard(
                localStream = localStream,
                videoEnabled = videoCallState.isLocalVideoEnabled,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 86.dp, end = 20.dp)
            )

            CallControlPanel(
                videoCallState = videoCallState,
                videoCallViewModel = videoCallViewModel,
                onEndCall = onEndCall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )
        }
    }
}

@Composable
private fun CallEndedDialog(
    remoteUser: UserInfo,
    videoCallState: VideoCallState,
    onDismiss: () -> Unit
) {
    val summary = videoCallState.sessionSummary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0B1220)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Call recap",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = summary?.title ?: "Call finished",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary?.detail ?: "The call has ended.",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryChip("Duration", formatDuration(summary?.durationSeconds ?: videoCallState.duration))
                    SummaryChip("Result", if ((summary?.connected ?: false)) "Connected" else "Not connected")
                    SummaryChip("Peer", remoteUser.username.ifBlank { "Call" })
                }
                Spacer(modifier = Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    videoCallState.activityLog.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = item.label,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                item.detail?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it,
                                        color = Color.White.copy(alpha = 0.68f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.56f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MinimizedCallOverlay(
    remoteUser: UserInfo,
    videoCallViewModel: VideoCallViewModel,
    onEndCall: () -> Unit
) {
    val localStream by videoCallViewModel.localMediaStream.collectAsState()
    val remoteVideoTracks by videoCallViewModel.remoteVideoTracks.collectAsState()
    val remoteAudioTracks by videoCallViewModel.remoteAudioTracks.collectAsState()
    val videoCallState by videoCallViewModel.videoCallState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        VideoCallFloatingWindow(
            remoteUser = remoteUser,
            localMediaStream = localStream,
            remoteVideoTrack = remoteVideoTracks.entries.firstOrNull()?.value,
            remoteAudioTrack = remoteAudioTracks[remoteVideoTracks.entries.firstOrNull()?.key],
            isCameraEnabled = videoCallState.isLocalVideoEnabled,
            isMicrophoneEnabled = videoCallState.isMicrophoneEnabled,
            onExpand = { videoCallViewModel.maximizeCall() },
            onEndCall = onEndCall,
            onToggleCamera = { videoCallViewModel.toggleCamera() },
            onToggleMicrophone = { videoCallViewModel.toggleMicrophone() }
        )
    }
}

@Composable
private fun RemoteVideoStage(
    remoteUser: UserInfo,
    videoCallState: VideoCallState,
    remoteVideoTracks: Map<String, VideoTrack>,
    remoteAudioTracks: Map<String, AudioTrack>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (remoteVideoTracks.size > 1) {
            MultiPartyVideoGrid(
                videoTracks = remoteVideoTracks,
                audioTracks = remoteAudioTracks,
                participants = videoCallState.participants,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        val remoteEntry = remoteVideoTracks.entries.firstOrNull()
        val remoteTrack = remoteEntry?.value
        val remoteAudio = remoteAudioTracks[remoteEntry?.key]

        if (remoteTrack != null && videoCallState.isRemoteVideoEnabled) {
            VideoScreenView(
                modifier = Modifier.fillMaxSize(),
                videoTrack = remoteTrack,
                audioTrack = remoteAudio
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF111827)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UserAvatar(username = remoteUser.username, size = 132)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = remoteUser.username.ifBlank { "Remote user" },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalPreviewCard(
    localStream: MediaStream?,
    videoEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(width = 126.dp, height = 176.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF111827),
        shadowElevation = 10.dp
    ) {
        val localVideoTrack = localStream?.videoTracks?.firstOrNull()
        val localAudioTrack = localStream?.audioTracks?.firstOrNull()

        if (localVideoTrack != null && videoEnabled) {
            VideoScreenView(
                modifier = Modifier.fillMaxSize(),
                videoTrack = localVideoTrack,
                audioTrack = localAudioTrack
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1F2937)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Local preview",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(46.dp)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}

@Composable
private fun rememberCallDurationText(startTime: Long?): String {
    var value by remember(startTime) { mutableStateOf("00:00") }

    LaunchedEffect(startTime) {
        if (startTime == null) {
            value = "00:00"
            return@LaunchedEffect
        }

        while (true) {
            val seconds = ((Clock.System.now().toEpochMilliseconds() - startTime) / 1000).coerceAtLeast(0)
            value = formatDuration(seconds)
            delay(1_000)
        }
    }

    return value
}

@Composable
private fun CallControlPanel(
    videoCallState: VideoCallState,
    videoCallViewModel: VideoCallViewModel,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color.Black.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlIconButton(
                onClick = { videoCallViewModel.switchCamera() },
                icon = Icons.Default.Cameraswitch,
                contentDescription = "Switch camera"
            )
            ControlActionButton(
                onClick = { videoCallViewModel.toggleMicrophone() },
                isActive = videoCallState.isMicrophoneEnabled,
                activeIcon = Icons.Default.Mic,
                inactiveIcon = Icons.Default.MicOff,
                activeColor = Color.White.copy(alpha = 0.22f),
                inactiveColor = Color.White,
                iconColor = if (videoCallState.isMicrophoneEnabled) Color.White else Color.Black
            )
            FloatingActionButton(
                onClick = onEndCall,
                containerColor = Color(0xFFE11D48),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.size(68.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End call",
                    modifier = Modifier.size(34.dp)
                )
            }
            ControlActionButton(
                onClick = { videoCallViewModel.toggleCamera() },
                isActive = videoCallState.isLocalVideoEnabled,
                activeIcon = Icons.Default.Videocam,
                inactiveIcon = Icons.Default.VideocamOff,
                activeColor = Color.White.copy(alpha = 0.22f),
                inactiveColor = Color.White,
                iconColor = if (videoCallState.isLocalVideoEnabled) Color.White else Color.Black
            )
            ControlIconButton(
                onClick = { videoCallViewModel.toggleSpeaker() },
                icon = if (videoCallState.isSpeakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = "Toggle speaker"
            )
        }
    }
}

@Composable
private fun ControlIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun ControlActionButton(
    onClick: () -> Unit,
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    activeColor: Color,
    inactiveColor: Color,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else inactiveColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isActive) activeIcon else inactiveIcon,
            contentDescription = null,
            tint = iconColor
        )
    }
}

@Composable
fun MultiPartyVideoGrid(
    videoTracks: Map<String, VideoTrack>,
    audioTracks: Map<String, AudioTrack>,
    participants: List<UserInfo>,
    modifier: Modifier = Modifier
) {
    if (participants.isEmpty()) return

    val columns = if (participants.size <= 2) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.padding(top = 88.dp, bottom = 130.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(participants, key = { it.userId }) { participant ->
            val participantId = participant.userId.toString()
            val videoTrack = videoTracks[participantId]
            val audioTrack = audioTracks[participantId]

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (columns == 1) 4f / 3f else 1f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111827)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (videoTrack != null && videoTrack.isEnabled) {
                        VideoScreenView(
                            modifier = Modifier.fillMaxSize(),
                            videoTrack = videoTrack,
                            audioTrack = audioTrack
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = participant.username,
                                    tint = Color.White.copy(alpha = 0.32f),
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = participant.username.ifBlank { participantId },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = participant.username.ifBlank { participantId },
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    if (audioTrack == null || !audioTrack.isEnabled) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = "Muted",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.padding(8.dp).size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
