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
    val userRepository : UserRepository,
) : ViewModel(

) {
    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState

    private val _isCameraEnabled = mutableStateOf(true)
    val isCameraEnabled: androidx.compose.runtime.State<Boolean> = _isCameraEnabled

    private val _isMicrophoneEnabled = mutableStateOf(true)
    val isMicrophoneEnabled: androidx.compose.runtime.State<Boolean> = _isMicrophoneEnabled

    private val _isFrontCamera = mutableStateOf(true)
    val isFrontCamera: androidx.compose.runtime.State<Boolean> = _isFrontCamera

    // 本地媒体流
    private val _localMediaStream = MutableStateFlow<MediaStream?>(null)
    val localMediaStream: StateFlow<MediaStream?> = _localMediaStream

    // 远程媒体流
    private val _remoteMediaStream = MutableStateFlow<MediaStream?>(null)
    val remoteMediaStream: StateFlow<MediaStream?> = _remoteMediaStream

    // 远程视频流
    private val _remoteVideo = MutableStateFlow<VideoTrack?>(null)
    val remoteVideo: StateFlow<VideoTrack?> = _remoteVideo



    // 远程音频流
    private val _remoteAudio = MutableStateFlow<AudioTrack?>(null)
    val remoteAudio: StateFlow<AudioTrack?> = _remoteAudio


    // WebRTC管理器
    private var webRTCManager: com.github.im.group.sdk.WebRTCManager? = null


    fun setWebRTCManager(manager: com.github.im.group.sdk.WebRTCManager) {
        this.webRTCManager = manager

        // 绑定监听 remote track 更新
        viewModelScope.launch {
            manager.remoteVideoTrack.collect { videoTrack ->
                _remoteVideo.value = videoTrack
                Napier.d("收到远程视频轨道更新: ${videoTrack != null}")
            }
        }

        viewModelScope.launch {
            manager.remoteAudioTrack.collect { audioTrack ->
                _remoteAudio.value = audioTrack
                Napier.d("收到远程音频轨道更新: ${audioTrack != null}")
            }
        }

    }

    fun startVideoCall(remoteUser: UserInfo) {

        /**发起视频通话
         * 1. 向信令服务器 发起呼叫请求
         *
         */
        viewModelScope.launch {

            _videoCallState.value = _videoCallState.value.copy(
                callStatus = CallStatus.CONNECTING,
                remoteUser = remoteUser
            )

            Napier.d("startVideoCall")
            
            // 初始化WebRTC连接
            try {
                webRTCManager?.initialize()
                // 先初始化本地媒体流
                initializeLocalMediaStream()

                // 发送呼叫请求
                sendCallRequest(remoteUser.userId.toString())

                _remoteVideo.value = webRTCManager?.remoteVideoTrack?.value
                _remoteAudio.value = webRTCManager?.remoteAudioTrack?.value

                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = CallStatus.ACTIVE
                )
            } catch (e: Exception) {
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = CallStatus.ERROR,
                    errorMessage = e.message
                )
            }
        }
    }

    fun endCall() {
        viewModelScope.launch {
            _videoCallState.value = _videoCallState.value.copy(
                callStatus = CallStatus.ENDED
            )
            
            // 清理WebRTC连接
            cleanupWebRTCConnection()
        }
        // 状态恢复
    }

    fun minimizeCall() {
        viewModelScope.launch {
            _videoCallState.value = _videoCallState.value.copy(
                callStatus = CallStatus.MINIMIZED
            )
        }
    }

    fun toggleCamera() {
        _isCameraEnabled.value = !_isCameraEnabled.value
        // 控制实际的摄像头开关
        webRTCManager?.toggleCamera(_isCameraEnabled.value)
    }

    fun toggleMicrophone() {
        _isMicrophoneEnabled.value = !_isMicrophoneEnabled.value
        // 控制实际的麦克风开关
        webRTCManager?.toggleMicrophone(_isMicrophoneEnabled.value)
    }

    fun switchCamera() {
        viewModelScope.launch {
            _isFrontCamera.value = !_isFrontCamera.value
            // 切换前后摄像头
            webRTCManager?.switchCamera()
        }

    }

    /**
     * 初始化本地媒体流
     */
    private suspend fun initializeLocalMediaStream() {


        val mediaStream = webRTCManager?.createLocalMediaStream()
        _localMediaStream.value = mediaStream
    }


    private fun sendCallRequest(userId: String) {
        // 通过信令服务器发送呼叫请求
        webRTCManager?.initiateCall(userId)
    }

    private fun cleanupWebRTCConnection() {
        // 清理WebRTC连接
        webRTCManager?.endCall()
//        webRTCManager?.release()
    }
}

data class VideoCallState(
    val callStatus: CallStatus = CallStatus.IDLE,
    val remoteUser: UserInfo? = null,
    val errorMessage: String? = null
)

enum class CallStatus {
    IDLE,
    CONNECTING,
    ACTIVE,
    MINIMIZED,
    ENDED,
    ERROR
}