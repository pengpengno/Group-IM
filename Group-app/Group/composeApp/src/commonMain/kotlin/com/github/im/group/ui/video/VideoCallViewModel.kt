package com.github.im.group.ui.video

import ProxyConfig
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.MediaStream
import com.github.im.group.sdk.WebRTCManager
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
    private val _localMediaStream = mutableStateOf<MediaStream?>(null)
    val localMediaStream: androidx.compose.runtime.State<MediaStream?> = _localMediaStream

    // WebRTC管理器
    private var webRTCManager: WebRTCManager? = null


    fun setWebRTCManager(manager: WebRTCManager) {
        this.webRTCManager = manager
    }

    fun startVideoCall(remoteUser: UserInfo) {
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
                
                // 再建立信令连接
                connectSignalingServer()
                
                // 发送呼叫请求
                sendCallRequest(remoteUser.userId.toString())
                
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
        _isFrontCamera.value = !_isFrontCamera.value
        // 切换前后摄像头
        webRTCManager?.switchCamera()
    }

    private fun initializeLocalMediaStream() {
        // 初始化本地媒体流
        val mediaStream = webRTCManager?.createLocalMediaStream()
        _localMediaStream.value = mediaStream
        // 可以在这里处理返回的媒体流对象
    }

    /**
     *webrtc 信令服务器连接
     * TODO 登录后立即连接  退出 登录后也是退出连接
     */
    private fun connectSignalingServer() {
        // 连接到信令服务器，使用ProxyConfig中的host配置
        val host = ProxyConfig.host
        val port = ProxyConfig.port
        // 检查是否使用安全连接
        val protocol = if (host.startsWith("https://")) "wss" else "ws"
        val cleanHost = host.replace(Regex("^https?://"), "")

        val currentUserId = userRepository.withLoggedInUser { it.user.userId .toString()}
        webRTCManager?.connectToSignalingServer("$protocol://$cleanHost:$port/webrtc", currentUserId)
    }

    private fun sendCallRequest(userId: String) {
        // 通过信令服务器发送呼叫请求
        webRTCManager?.initiateCall(userId)
    }

    private fun cleanupWebRTCConnection() {
        // 清理WebRTC连接
        webRTCManager?.endCall()
        webRTCManager?.release()
        _localMediaStream.value = null
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