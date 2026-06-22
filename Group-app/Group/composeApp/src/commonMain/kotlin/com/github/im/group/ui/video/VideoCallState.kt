package com.github.im.group.ui.video

import com.github.im.group.model.UserInfo
import com.github.im.group.sdk.AudioTrack
import com.github.im.group.sdk.VideoTrack

enum class VideoCallStatus {
    IDLE,
    PRE_JOIN,
    OUTGOING,
    INCOMING,
    CONNECTING,
    ACTIVE,
    ENDING,
    ENDED,
    MINIMIZED,
    ERROR
}

enum class CallActivityTone {
    INFO,
    SUCCESS,
    WARNING
}

data class CallActivity(
    val id: String,
    val label: String,
    val detail: String? = null,
    val tone: CallActivityTone = CallActivityTone.INFO,
    val timestamp: Long
)

data class CallSessionSummary(
    val title: String,
    val detail: String,
    val durationSeconds: Long,
    val connected: Boolean,
    val endedBy: String,
    val endedAt: Long
)

data class VideoCallState(
    val callStatus: VideoCallStatus = VideoCallStatus.IDLE,
    val caller: UserInfo? = null,
    val callee: UserInfo? = null,
    val participants: List<UserInfo> = emptyList(),
    val remoteVideoTracks: Map<String, VideoTrack> = emptyMap(),
    val remoteAudioTracks: Map<String, AudioTrack> = emptyMap(),
    val callStartTime: Long? = null,
    val duration: Long = 0,
    val isLocalVideoEnabled: Boolean = true,
    val isRemoteVideoEnabled: Boolean = true,
    val isMicrophoneEnabled: Boolean = true,
    val isSpeakerEnabled: Boolean = true,
    val errorMessage: String? = null,
    val isMinimized: Boolean = false,
    val callId: String? = null,
    val activityLog: List<CallActivity> = emptyList(),
    val sessionSummary: CallSessionSummary? = null
)
