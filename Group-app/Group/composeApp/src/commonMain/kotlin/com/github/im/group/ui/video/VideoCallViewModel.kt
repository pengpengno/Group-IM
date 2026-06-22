package com.github.im.group.ui.video

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.AudioTrack
import com.github.im.group.sdk.MediaStream
import com.github.im.group.sdk.VideoTrack
import com.github.im.group.sdk.WebRTCManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class VideoCallViewModel(
    val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private const val INCOMING_CALL_TIMEOUT_MILLIS = 30_000L
    }

    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState

    private val _isCameraEnabled = mutableStateOf(true)
    val isCameraEnabled: State<Boolean> = _isCameraEnabled

    private val _isMicrophoneEnabled = mutableStateOf(true)
    val isMicrophoneEnabled: State<Boolean> = _isMicrophoneEnabled

    private val _isFrontCamera = mutableStateOf(true)
    val isFrontCamera: State<Boolean> = _isFrontCamera

    private val _isSpeakerEnabled = mutableStateOf(true)
    val isSpeakerEnabled: State<Boolean> = _isSpeakerEnabled

    private val _localMediaStream = MutableStateFlow<MediaStream?>(null)
    val localMediaStream: StateFlow<MediaStream?> = _localMediaStream

    private val _remoteVideoTracks = MutableStateFlow<Map<String, VideoTrack>>(emptyMap())
    val remoteVideoTracks: StateFlow<Map<String, VideoTrack>> = _remoteVideoTracks

    private val _remoteAudioTracks = MutableStateFlow<Map<String, AudioTrack>>(emptyMap())
    val remoteAudioTracks: StateFlow<Map<String, AudioTrack>> = _remoteAudioTracks

    private var webRTCManager: WebRTCManager? = null
    private var videoCallStateJob: Job? = null
    private var remoteVideoTracksJob: Job? = null
    private var remoteAudioTracksJob: Job? = null

    private var currentCallId: String? = null
    private var isIncomingCallProcessing = false
    private var isEndingCall = false
    // We intentionally stage call setup in two phases:
    // 1) move UI into an outgoing/connecting state immediately
    // 2) ask for permission + build media/signaling after the call page is visible
    // This keeps the transition smooth on all clients and gives callees a stable
    // accept surface before camera/mic initialization starts.
    private var pendingCallSetup: PendingCallSetup? = null
    private var isPreparingCallSession = false

    private sealed interface PendingCallSetup {
        data class OutgoingMeeting(
            val roomId: String,
            val participantIds: List<String>
        ) : PendingCallSetup

        data class AcceptIncoming(
            val callId: String
        ) : PendingCallSetup
    }

    fun startCall(callee: UserInfo) {
        startMeeting(
            roomId = "call_${Clock.System.now().toEpochMilliseconds()}",
            participantIds = listOf(callee.userId.toString()),
            displayUser = callee
        )
    }

    fun startMeeting(roomId: String, participantIds: List<String>, displayUser: UserInfo? = null) {
        if (_videoCallState.value.callStatus.isActiveCall()) {
            Napier.w("Call already active: ${_videoCallState.value.callStatus}")
            return
        }

        resetSessionArtifacts()
        viewModelScope.launch {
            try {
                currentCallId = roomId
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.OUTGOING,
                    caller = displayUser ?: _videoCallState.value.caller,
                    callId = roomId,
                    callStartTime = Clock.System.now().toEpochMilliseconds(),
                    isMinimized = false,
                    isLocalVideoEnabled = _isCameraEnabled.value,
                    isMicrophoneEnabled = _isMicrophoneEnabled.value,
                    isSpeakerEnabled = _isSpeakerEnabled.value,
                    errorMessage = null,
                    sessionSummary = null
                )
                addActivity(
                    label = if (participantIds.size > 1) "Meeting invite sent" else "Calling started",
                    detail = displayUser?.username ?: participantIds.joinToString(", ")
                )

                // Render the outgoing screen first. Media permission and session
                // setup continue from the call surface so the transition feels
                // immediate and the user always sees a call UI before heavy work.
                pendingCallSetup = PendingCallSetup.OutgoingMeeting(
                    roomId = roomId,
                    participantIds = participantIds
                )
            } catch (e: Exception) {
                handleError("Failed to start meeting", e)
            }
        }
    }

    fun receiveCall(caller: UserInfo, callId: String? = null) {
        if (_videoCallState.value.callStatus.isActiveCall()) {
            webRTCManager?.rejectCall(callId.orEmpty())
            return
        }

        if (isIncomingCallProcessing) return

        isIncomingCallProcessing = true
        currentCallId = callId ?: "call_${Clock.System.now().toEpochMilliseconds()}"
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.INCOMING,
            caller = caller,
            participants = listOf(caller),
            callId = currentCallId,
            callStartTime = Clock.System.now().toEpochMilliseconds(),
            isMinimized = false,
            errorMessage = null,
            sessionSummary = null
        )
        addActivity("Incoming call", "${caller.username} is calling.")
        startIncomingCallTimeout()
    }

    fun acceptCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.INCOMING &&
            _videoCallState.value.callStatus != VideoCallStatus.PRE_JOIN
        ) return

        viewModelScope.launch {
            try {
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.CONNECTING,
                    isMinimized = false
                )
                addActivity("Joining call", "Preparing camera and microphone.")
                pendingCallSetup = PendingCallSetup.AcceptIncoming(currentCallId.orEmpty())
            } catch (e: Exception) {
                handleError("Failed to accept call", e)
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    fun preparePendingCallSession() {
        if (isPreparingCallSession) return
        val pendingSetup = pendingCallSetup ?: return

        viewModelScope.launch {
            isPreparingCallSession = true
            try {
                createLocalMediaStream()
                when (pendingSetup) {
                    is PendingCallSetup.OutgoingMeeting -> {
                        if (pendingSetup.participantIds.isEmpty()) {
                            webRTCManager?.joinMeeting(pendingSetup.roomId)
                        } else {
                            webRTCManager?.initiateMeeting(pendingSetup.roomId, pendingSetup.participantIds)
                        }
                    }

                    is PendingCallSetup.AcceptIncoming -> {
                        webRTCManager?.acceptCall(pendingSetup.callId)
                        _videoCallState.value = _videoCallState.value.copy(
                            callStatus = VideoCallStatus.ACTIVE,
                            isMinimized = false
                        )
                        addActivity("Call connected", "You joined the call.", CallActivityTone.SUCCESS)
                    }
                }
                pendingCallSetup = null
            } catch (e: Exception) {
                handleError("Failed to prepare call session", e)
            } finally {
                isPreparingCallSession = false
            }
        }
    }

    fun handlePendingCallPermissionDenied() {
        val pendingSetup = pendingCallSetup
        pendingCallSetup = null
        isPreparingCallSession = false

        when (pendingSetup) {
            is PendingCallSetup.AcceptIncoming -> rejectCall()
            is PendingCallSetup.OutgoingMeeting -> finishCall(
                status = VideoCallStatus.ENDED,
                errorMessage = "Camera and microphone permission denied",
                summary = buildSummary(
                    title = "Call cancelled",
                    detail = "Camera and microphone permission denied.",
                    endedBy = "system"
                )
            )
            null -> Unit
        }
    }

    fun rejectCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.INCOMING &&
            _videoCallState.value.callStatus != VideoCallStatus.PRE_JOIN
        ) return

        viewModelScope.launch {
            try {
                webRTCManager?.rejectCall(currentCallId.orEmpty())
                addActivity("Call declined", "You declined the incoming call.", CallActivityTone.WARNING)
                finishCall(
                    status = VideoCallStatus.ENDED,
                    summary = buildSummary(
                        title = "Call declined",
                        detail = "You declined the incoming call.",
                        endedBy = "local"
                    )
                )
            } catch (e: Exception) {
                handleError("Failed to reject call", e)
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    fun handleNotificationOpen(caller: UserInfo, callId: String) {
        if ((_videoCallState.value.callStatus == VideoCallStatus.INCOMING ||
            _videoCallState.value.callStatus == VideoCallStatus.PRE_JOIN) &&
            _videoCallState.value.callId == callId
        ) {
            return
        }
        currentCallId = callId
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.PRE_JOIN,
            caller = caller,
            participants = listOf(caller),
            callId = callId,
            callStartTime = Clock.System.now().toEpochMilliseconds(),
            isMinimized = false,
            errorMessage = null
        )
    }

    fun handleNotificationAccept(caller: UserInfo, callId: String) {
        if ((_videoCallState.value.callStatus != VideoCallStatus.INCOMING &&
            _videoCallState.value.callStatus != VideoCallStatus.PRE_JOIN) ||
            _videoCallState.value.callId != callId
        ) {
            handleNotificationOpen(caller, callId)
        }
        acceptCall()
    }

    fun handleNotificationReject(caller: UserInfo, callId: String) {
        if ((_videoCallState.value.callStatus == VideoCallStatus.INCOMING ||
            _videoCallState.value.callStatus == VideoCallStatus.PRE_JOIN) &&
            _videoCallState.value.callId == callId
        ) {
            rejectCall()
            return
        }

        currentCallId = callId
        _videoCallState.value = _videoCallState.value.copy(
            caller = caller,
            callId = callId
        )
        webRTCManager?.rejectCall(callId)
        finishCall(
            status = VideoCallStatus.ENDED,
            summary = buildSummary(
                title = "Call declined",
                detail = "You declined the incoming call.",
                endedBy = "local"
            )
        )
    }

    fun endCall() {
        if (isEndingCall || _videoCallState.value.callStatus == VideoCallStatus.ENDED) return

        isEndingCall = true
        viewModelScope.launch {
            try {
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.ENDING,
                    isMinimized = false
                )
                webRTCManager?.endCall()
                releaseMediaResources()
                finishCall(
                    status = VideoCallStatus.ENDED,
                    summary = buildSummary(
                        title = if ((_videoCallState.value.duration) > 0) "Call ended" else "Call cancelled",
                        detail = if ((_videoCallState.value.duration) > 0) {
                            "You ended the call after ${formatDuration(_videoCallState.value.duration)}."
                        } else {
                            "You ended the call before it connected."
                        },
                        endedBy = "local"
                    )
                )
            } catch (e: Exception) {
                handleError("Failed to end call", e)
                forceResetState()
            } finally {
                isEndingCall = false
            }
        }
    }

    fun minimizeCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.ACTIVE) return

        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.MINIMIZED,
            isMinimized = true
        )
    }

    fun maximizeCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.MINIMIZED) return

        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.ACTIVE,
            isMinimized = false
        )
    }

    fun toggleCamera() {
        viewModelScope.launch {
            val enabled = !_isCameraEnabled.value
            _isCameraEnabled.value = enabled
            _localMediaStream.value?.videoTracks?.forEach { it.setEnabled(enabled) }
            webRTCManager?.toggleCamera(enabled)
            _videoCallState.value = _videoCallState.value.copy(
                isLocalVideoEnabled = enabled
            )
        }
    }

    fun toggleMicrophone() {
        viewModelScope.launch {
            val enabled = !_isMicrophoneEnabled.value
            _isMicrophoneEnabled.value = enabled
            _localMediaStream.value?.audioTracks?.forEach { it.setEnabled(enabled) }
            webRTCManager?.toggleMicrophone(enabled)
            _videoCallState.value = _videoCallState.value.copy(
                isMicrophoneEnabled = enabled
            )
        }
    }

    fun toggleSpeaker() {
        val enabled = !_isSpeakerEnabled.value
        _isSpeakerEnabled.value = enabled
        _videoCallState.value = _videoCallState.value.copy(
            isSpeakerEnabled = enabled
        )
    }

    fun switchCamera() {
        viewModelScope.launch {
            _isFrontCamera.value = !_isFrontCamera.value
            webRTCManager?.switchCamera()
        }
    }

    fun setWebRTCManager(manager: WebRTCManager) {
        webRTCManager = manager
        observeWebRTCManager(manager)
    }

    private fun observeWebRTCManager(manager: WebRTCManager) {
        videoCallStateJob?.cancel()
        remoteVideoTracksJob?.cancel()
        remoteAudioTracksJob?.cancel()

        videoCallStateJob = viewModelScope.launch {
            manager.videoCallState.collect { state ->
                if (state.callStatus == VideoCallStatus.INCOMING && !isIncomingCallProcessing) {
                    receiveMeetingRequest(state)
                } else {
                    val current = _videoCallState.value
                    val shouldHoldUiFirstStatus =
                        pendingCallSetup != null &&
                            state.callStatus == VideoCallStatus.IDLE &&
                            current.callStatus in listOf(
                                VideoCallStatus.OUTGOING,
                                VideoCallStatus.CONNECTING,
                                VideoCallStatus.PRE_JOIN
                            )

                    val mergedCallStatus = when {
                        shouldHoldUiFirstStatus -> current.callStatus
                        current.isMinimized && state.callStatus == VideoCallStatus.ACTIVE -> VideoCallStatus.MINIMIZED
                        else -> state.callStatus
                    }

                    if (pendingCallSetup == null &&
                        state.callStatus == VideoCallStatus.IDLE &&
                        current.callStatus in listOf(
                            VideoCallStatus.OUTGOING,
                            VideoCallStatus.CONNECTING,
                            VideoCallStatus.ACTIVE,
                            VideoCallStatus.MINIMIZED
                        )
                    ) {
                        finalizeRemoteEndedSession(current, state.errorMessage)
                        return@collect
                    }

                    _videoCallState.value = current.copy(
                        // Until the deferred setup actually starts, keep the optimistic
                        // UI-first status instead of letting an idle manager snapshot
                        // collapse the call screen back to the chat page.
                        callStatus = mergedCallStatus,
                        caller = state.caller ?: current.caller,
                        callee = state.callee ?: current.callee,
                        participants = if (state.participants.isNotEmpty()) state.participants else current.participants,
                        callStartTime = state.callStartTime ?: current.callStartTime,
                        duration = if (state.duration > 0) state.duration else current.duration,
                        isRemoteVideoEnabled = state.isRemoteVideoEnabled,
                        errorMessage = state.errorMessage,
                        isMinimized = current.isMinimized || state.isMinimized,
                        callId = state.callId ?: current.callId
                    )
                }
            }
        }

        remoteVideoTracksJob = viewModelScope.launch {
            manager.remoteVideoTracks.collect { tracks ->
                _remoteVideoTracks.value = tracks
                _videoCallState.value = _videoCallState.value.copy(
                    remoteVideoTracks = tracks,
                    isRemoteVideoEnabled = tracks.isNotEmpty()
                )
            }
        }

        remoteAudioTracksJob = viewModelScope.launch {
            manager.remoteAudioTracks.collect { tracks ->
                _remoteAudioTracks.value = tracks
                _videoCallState.value = _videoCallState.value.copy(
                    remoteAudioTracks = tracks
                )
            }
        }
    }

    private fun receiveMeetingRequest(state: VideoCallState) {
        isIncomingCallProcessing = true
        currentCallId = state.callId
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.INCOMING,
            caller = state.caller ?: _videoCallState.value.caller,
            callee = state.callee ?: _videoCallState.value.callee,
            participants = if (state.participants.isNotEmpty()) state.participants else _videoCallState.value.participants,
            callId = state.callId ?: currentCallId,
            callStartTime = Clock.System.now().toEpochMilliseconds(),
            isMinimized = false,
            errorMessage = state.errorMessage
        )
        startIncomingCallTimeout()
    }

    private fun startIncomingCallTimeout() {
        viewModelScope.launch {
            delay(INCOMING_CALL_TIMEOUT_MILLIS)
            if (_videoCallState.value.callStatus == VideoCallStatus.INCOMING) {
                autoRejectCall("Call timed out")
            }
        }
    }

    private fun autoRejectCall(reason: String) {
        viewModelScope.launch {
            try {
                webRTCManager?.rejectCall(currentCallId.orEmpty())
                finishCall(
                    status = VideoCallStatus.ENDED,
                    errorMessage = reason,
                    summary = buildSummary(
                        title = "Missed call",
                        detail = reason,
                        endedBy = "system"
                    )
                )
            } catch (e: Exception) {
                Napier.e("Auto reject call failed", e)
                forceResetState()
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    private suspend fun createLocalMediaStream() {
        val mediaStream = webRTCManager?.createLocalMediaStream()
        _localMediaStream.value = mediaStream
        mediaStream?.videoTracks?.forEach { it.setEnabled(_isCameraEnabled.value) }
        mediaStream?.audioTracks?.forEach { it.setEnabled(_isMicrophoneEnabled.value) }
    }

    private fun releaseMediaResources() {
        releaseLocalMediaStream()
        releaseRemoteMediaTracks()
        resetControlStates()
    }

    private fun releaseLocalMediaStream() {
        _localMediaStream.value?.videoTracks?.forEach { it.setEnabled(false) }
        _localMediaStream.value?.audioTracks?.forEach { it.setEnabled(false) }
        _localMediaStream.value = null
    }

    private fun releaseRemoteMediaTracks() {
        _remoteVideoTracks.value.values.forEach { it.setEnabled(false) }
        _remoteAudioTracks.value.values.forEach { it.setEnabled(false) }
        _remoteVideoTracks.value = emptyMap()
        _remoteAudioTracks.value = emptyMap()
    }

    private fun resetControlStates() {
        _isCameraEnabled.value = true
        _isMicrophoneEnabled.value = true
        _isFrontCamera.value = true
        _isSpeakerEnabled.value = true
    }

    private fun resetSessionArtifacts() {
        _videoCallState.value = _videoCallState.value.copy(
            activityLog = emptyList(),
            sessionSummary = null,
            errorMessage = null
        )
    }

    private fun addActivity(
        label: String,
        detail: String? = null,
        tone: CallActivityTone = CallActivityTone.INFO
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val item = CallActivity(
            id = "$now-${(0..9999).random()}",
            label = label,
            detail = detail,
            tone = tone,
            timestamp = now
        )
        _videoCallState.value = _videoCallState.value.copy(
            activityLog = (_videoCallState.value.activityLog + item).takeLast(8)
        )
    }

    private fun buildSummary(title: String, detail: String, endedBy: String): CallSessionSummary {
        val duration = _videoCallState.value.duration
        return CallSessionSummary(
            title = title,
            detail = detail,
            durationSeconds = duration,
            connected = duration > 0,
            endedBy = endedBy,
            endedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    private fun finalizeRemoteEndedSession(current: VideoCallState, reason: String?) {
        releaseMediaResources()
        val connected = current.duration > 0
        val detail = when {
            !reason.isNullOrBlank() -> reason
            connected -> "${current.caller?.username ?: "The other side"} ended the call after ${formatDuration(current.duration)}."
            else -> "${current.caller?.username ?: "The other side"} ended the call before it connected."
        }
        addActivity(
            label = if (connected) "Call ended by other side" else "Call not answered",
            detail = detail,
            tone = if (connected) CallActivityTone.SUCCESS else CallActivityTone.WARNING
        )
        finishCall(
            status = VideoCallStatus.ENDED,
            errorMessage = reason,
            summary = buildSummary(
                title = if (connected) "Call ended by other side" else "Call not answered",
                detail = detail,
                endedBy = "remote"
            )
        )
    }

    fun dismissEndedSession() {
        forceResetState()
    }

    private fun finishCall(
        status: VideoCallStatus,
        errorMessage: String? = null,
        summary: CallSessionSummary? = null
    ) {
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = status,
            errorMessage = errorMessage,
            isMinimized = false,
            sessionSummary = summary ?: _videoCallState.value.sessionSummary
        )
        currentCallId = null
        isIncomingCallProcessing = false
    }

    private fun forceResetState() {
        releaseMediaResources()
        currentCallId = null
        isIncomingCallProcessing = false
        isEndingCall = false
        pendingCallSetup = null
        isPreparingCallSession = false
        _videoCallState.value = VideoCallState()
    }

    private fun handleError(message: String, error: Throwable) {
        Napier.e(message, error)
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.ERROR,
            errorMessage = "$message: ${error.message}",
            isMinimized = false
        )
        addActivity("Call error", error.message ?: message, CallActivityTone.WARNING)
    }

    override fun onCleared() {
        super.onCleared()
        videoCallStateJob?.cancel()
        remoteVideoTracksJob?.cancel()
        remoteAudioTracksJob?.cancel()
        webRTCManager?.release()
        releaseMediaResources()
    }
}

fun VideoCallStatus.isActiveCall(): Boolean {
    return this in listOf(
        VideoCallStatus.OUTGOING,
        VideoCallStatus.INCOMING,
        VideoCallStatus.PRE_JOIN,
        VideoCallStatus.CONNECTING,
        VideoCallStatus.ACTIVE,
        VideoCallStatus.MINIMIZED
    )
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}
