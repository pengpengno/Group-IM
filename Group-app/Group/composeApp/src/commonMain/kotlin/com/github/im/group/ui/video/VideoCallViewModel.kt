package com.github.im.group.ui.video

import ProxyConfig
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.AudioTrack
import com.github.im.group.sdk.MediaStream
import com.github.im.group.sdk.VideoTrack
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoCallViewModel(
    val userRepository: UserRepository
) : ViewModel() {
    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState

    private val _isCameraEnabled = mutableStateOf(true)
    val isCameraEnabled: androidx.compose.runtime.State<Boolean> = _isCameraEnabled

    private val _isMicrophoneEnabled = mutableStateOf(true)
    val isMicrophoneEnabled: androidx.compose.runtime.State<Boolean> = _isMicrophoneEnabled

    private val _isFrontCamera = mutableStateOf(true)
    val isFrontCamera: androidx.compose.runtime.State<Boolean> = _isFrontCamera

    private val _isSpeakerEnabled = mutableStateOf(true)
    val isSpeakerEnabled: androidx.compose.runtime.State<Boolean> = _isSpeakerEnabled

    // 本地媒体流
    private val _localMediaStream = MutableStateFlow<MediaStream?>(null)
    val localMediaStream: StateFlow<MediaStream?> = _localMediaStream

    // 远程媒体流
    private val _remoteVideo = MutableStateFlow<VideoTrack?>(null)
    val remoteVideo: StateFlow<VideoTrack?> = _remoteVideo

    private val _remoteAudio = MutableStateFlow<AudioTrack?>(null)
    val remoteAudio: StateFlow<AudioTrack?> = _remoteAudio

    init {
        // 监听远程视频轨道变化，更新状态
        viewModelScope.launch {
            _remoteVideo.collect { videoTrack ->
                if (_videoCallState.value.isRemoteVideoEnabled != (videoTrack != null)) {
                    _videoCallState.value = _videoCallState.value.copy(
                        isRemoteVideoEnabled = videoTrack != null
                    )
                }
            }
        }
        
        // 监听远程音频轨道变化，更新状态
        viewModelScope.launch {
            _remoteAudio.collect { audioTrack ->
                if (_videoCallState.value.isMicrophoneEnabled != (audioTrack != null)) {
                    _videoCallState.value = _videoCallState.value.copy(
                        isMicrophoneEnabled = audioTrack != null
                    )
                }
            }
        }
    }

    // WebRTC管理器
    private var webRTCManager: com.github.im.group.sdk.WebRTCManager? = null

    // 当前来电ID
    private var currentCallId: String? = null

    init {
        // WebRTCManager 通过依赖注入设置，不需要在此处初始化
    }

    /**
     * 发起视频通话
     */
    fun startCall(callee: UserInfo) {
        viewModelScope.launch {
            try {
                // 更新 状态为 拨出
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.OUTGOING,
                    callee = callee,
                    participants = listOf(callee), // 添加被叫用户到参与者列表
                    callStartTime = System.currentTimeMillis()
                )

                // 创建本地媒体流
                createLocalMediaStream()

                // 通过WebRTC发起通话
                webRTCManager?.initiateCall(callee.userId.toString())
            } catch (e: Exception) {
                handleError("发起通话失败", e)
            }
        }
    }

    /**
     * 接收到来电
     */
    fun receiveCall(caller: UserInfo) {
        // 生成或接收callId
        currentCallId = "call_${'$'}{System.currentTimeMillis()}"
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.INCOMING,
            caller = caller,
            participants = listOf(caller), // 添加主叫用户到参与者列表
            callStartTime = System.currentTimeMillis()
        )
    }

    /**
     * 接受来电
     */
    fun acceptCall() {
        viewModelScope.launch {
            try {
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.CONNECTING
                )

                // 创建本地媒体流
                createLocalMediaStream()

                // 通过WebRTC接受通话
                webRTCManager?.acceptCall(currentCallId ?: "")
                
                // 更新状态为活跃通话
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.ACTIVE
                )
            } catch (e: Exception) {
                handleError("接受通话失败", e)
            }
        }
    }

    /**
     * 拒绝来电
     */
    fun rejectCall() {
        viewModelScope.launch {
            try {
                // 通过WebRTC拒绝通话
                currentCallId?.let { callId ->
                    webRTCManager?.rejectCall(callId)
                }
                endCall()
            } catch (e: Exception) {
                handleError("拒绝通话失败", e)
            }
        }
    }

    /**
     * 结束通话
     */
    fun endCall() {
        viewModelScope.launch {
            try {
                // 通知远端结束通话
                webRTCManager?.endCall()

                // 释放媒体资源
                releaseMediaResources()

                // 重置状态
                _videoCallState.value = VideoCallState(
                    callStatus = VideoCallStatus.ENDED
                )
                
                // 重置callId
                currentCallId = null
                
                // 延迟一小段时间后重置为IDLE状态，以便能够重新发起通话
                kotlinx.coroutines.delay(500) // 500毫秒延迟
                _videoCallState.value = VideoCallState()
            } catch (e: Exception) {
                handleError("结束通话失败", e)
            }
        }
    }

    /**
     * 最小化通话（进入悬浮窗模式）
     */
    fun minimizeCall() {
        viewModelScope.launch {
            _videoCallState.value = _videoCallState.value.copy(
                callStatus = VideoCallStatus.MINIMIZED,
                isMinimized = true
            )
        }
    }

    /**
     * 最大化通话（从悬浮窗模式恢复）
     */
    fun maximizeCall() {
        viewModelScope.launch {
            _videoCallState.value = _videoCallState.value.copy(
                callStatus = VideoCallStatus.ACTIVE,
                isMinimized = false
            )
        }
    }

    /**
     * 切换摄像头
     */
    fun toggleCamera() {
        viewModelScope.launch {
            _isCameraEnabled.value = !_isCameraEnabled.value
            _videoCallState.value = _videoCallState.value.copy(
                isLocalVideoEnabled = _isCameraEnabled.value
            )
            
            // 通知WebRTC切换摄像头
            webRTCManager?.toggleCamera(!_isCameraEnabled.value)
        }
    }

    /**
     * 切换麦克风
     */
    fun toggleMicrophone() {
        viewModelScope.launch {
            _isMicrophoneEnabled.value = !_isMicrophoneEnabled.value
            _videoCallState.value = _videoCallState.value.copy(
                isMicrophoneEnabled = _isMicrophoneEnabled.value
            )
            
            // 通知WebRTC切换麦克风
            webRTCManager?.toggleMicrophone(!_isMicrophoneEnabled.value)
        }
    }

    /**
     * 切换扬声器 - 这里映射到麦克风切换，因为WebRTCManager没有专门的扬声器控制
     */
    fun toggleSpeaker() {
        viewModelScope.launch {
            _isSpeakerEnabled.value = !_isSpeakerEnabled.value
            _videoCallState.value = _videoCallState.value.copy(
                isSpeakerEnabled = _isSpeakerEnabled.value
            )
            
            // 对于扬声器控制，通常在平台层处理，这里只是更新状态
        }
    }

    /**
     * 切换前后摄像头
     */
    fun switchCamera() {
        viewModelScope.launch {
            _isFrontCamera.value = !_isFrontCamera.value
            
            // 通知WebRTC切换摄像头
            webRTCManager?.switchCamera()
        }
    }

    /**
     * 创建本地媒体流
     */
    private suspend fun createLocalMediaStream() {
        try {
            val mediaStream = webRTCManager?.createLocalMediaStream()
            _localMediaStream.value = mediaStream
        } catch (e: Exception) {
            Napier.e("创建本地媒体流失败", e)
        }
    }

    /**
     * 释放媒体资源
     */
    private fun releaseMediaResources() {
        try {
            // 停止本地媒体流
            _localMediaStream.value?.let { stream ->
                stream.videoTracks.forEach { track -> track.setEnabled(false) }
                stream.audioTracks.forEach { track -> track.setEnabled(false) }
            }
            _localMediaStream.value = null

            // 释放远程媒体流
            _remoteVideo.value?.setEnabled(false)
            _remoteAudio.value?.setEnabled(false)
            _remoteVideo.value = null
            _remoteAudio.value = null

            // 重置状态
            _isCameraEnabled.value = true
            _isMicrophoneEnabled.value = true
            _isFrontCamera.value = true
            _isSpeakerEnabled.value = true
        } catch (e: Exception) {
            Napier.e("释放媒体资源失败", e)
        }
    }

    /**
     * 处理错误
     */
    private fun handleError(message: String, e: Exception) {
        Napier.e(message, e)
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.ERROR,
            errorMessage = "$message: ${e.message}"
        )
    }

    /**
     * 设置WebRTC管理器（通过依赖注入）
     */
    fun setWebRTCManager(manager: com.github.im.group.sdk.WebRTCManager) {
        this.webRTCManager = manager
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        try {
            webRTCManager?.release()
            releaseMediaResources()
        } catch (e: Exception) {
            Napier.e("清理视频通话资源失败", e)
        }
    }
}