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
        private const val STATE_RESET_DELAY_MILLIS = 500L
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
                    errorMessage = null
                )

                createLocalMediaStream()

                if (participantIds.isEmpty()) {
                    webRTCManager?.joinMeeting(roomId)
                } else {
                    webRTCManager?.initiateMeeting(roomId, participantIds)
                }
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
            errorMessage = null
        )
        startIncomingCallTimeout()
    }

    fun acceptCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.INCOMING) return

        viewModelScope.launch {
            try {
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.CONNECTING,
                    isMinimized = false
                )
                createLocalMediaStream()
                webRTCManager?.acceptCall(currentCallId.orEmpty())
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.ACTIVE,
                    isMinimized = false
                )
            } catch (e: Exception) {
                handleError("Failed to accept call", e)
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    fun rejectCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.INCOMING) return

        viewModelScope.launch {
            try {
                webRTCManager?.rejectCall(currentCallId.orEmpty())
                finishCall(VideoCallStatus.ENDED)
            } catch (e: Exception) {
                handleError("Failed to reject call", e)
            } finally {
                isIncomingCallProcessing = false
            }
        }
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
                finishCall(VideoCallStatus.ENDED)
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
                    _videoCallState.value = current.copy(
                        callStatus = if (current.isMinimized && state.callStatus == VideoCallStatus.ACTIVE) {
                            VideoCallStatus.MINIMIZED
                        } else {
                            state.callStatus
                        },
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
                finishCall(VideoCallStatus.ENDED, reason)
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

    private fun finishCall(status: VideoCallStatus, errorMessage: String? = null) {
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = status,
            errorMessage = errorMessage,
            isMinimized = false
        )
        currentCallId = null
        isIncomingCallProcessing = false

        viewModelScope.launch {
            delay(STATE_RESET_DELAY_MILLIS)
            if (_videoCallState.value.callStatus == status) {
                _videoCallState.value = VideoCallState()
            }
        }
    }

    private fun forceResetState() {
        releaseMediaResources()
        currentCallId = null
        isIncomingCallProcessing = false
        isEndingCall = false
        _videoCallState.value = VideoCallState()
    }

    private fun handleError(message: String, error: Throwable) {
        Napier.e(message, error)
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.ERROR,
            errorMessage = "$message: ${error.message}",
            isMinimized = false
        )
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
        VideoCallStatus.CONNECTING,
        VideoCallStatus.ACTIVE,
        VideoCallStatus.MINIMIZED
    )
}
