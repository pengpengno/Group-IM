package com.github.im.group.ui.video

import com.github.im.group.config.ProxyConfig
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
import kotlinx.datetime.Clock

class VideoCallViewModel(
    val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private const val INCOMING_CALL_TIMEOUT_MILLIS = 30000L // 30秒超时
        private const val STATE_RESET_DELAY_MILLIS = 500L
        private const val RESOURCE_CLEANUP_TIMEOUT_MILLIS = 3000L
    }
    
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

    // 远程媒体流 (多人支持)
    private val _remoteVideoTracks = MutableStateFlow<Map<Long, VideoTrack>>(emptyMap())
    val remoteVideoTracks: StateFlow<Map<Long, VideoTrack>> = _remoteVideoTracks

    private val _remoteAudioTracks = MutableStateFlow<Map<Long, AudioTrack>>(emptyMap())
    val remoteAudioTracks: StateFlow<Map<Long, AudioTrack>> = _remoteAudioTracks

    // WebRTC管理器
    private var webRTCManager: com.github.im.group.sdk.WebRTCManager? = null

    // 当前来电ID和相关状态
    private var currentCallId: String? = null
    private var isIncomingCallProcessing = false
    private var isEndingCall = false

    init {
        // 监听远程视频轨道映射变化，更新状态
        viewModelScope.launch {
            _remoteVideoTracks.collect { tracks ->
                val hasRemoteVideo = tracks.isNotEmpty()
                if (_videoCallState.value.isRemoteVideoEnabled != hasRemoteVideo) {
                    _videoCallState.value = _videoCallState.value.copy(
                        isRemoteVideoEnabled = hasRemoteVideo
                    )
                }
            }
        }
        
        // 监听远程音频轨道映射变化，更新状态
        viewModelScope.launch {
            _remoteAudioTracks.collect { tracks ->
                val hasRemoteAudio = tracks.isNotEmpty()
                if (_videoCallState.value.isMicrophoneEnabled != hasRemoteAudio) {
                    _videoCallState.value = _videoCallState.value.copy(
                        isMicrophoneEnabled = hasRemoteAudio
                    )
                }
            }
        }
    }

    /**
     * 发起视频通话
     */
    fun startCall(callee: UserInfo) {
        if (_videoCallState.value.callStatus != VideoCallStatus.IDLE) {
            Napier.w("无法发起新通话：当前状态为 ${_videoCallState.value.callStatus}")
            return
        }
        
        viewModelScope.launch {
            try {
                // 更新状态为拨出
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.OUTGOING,
                    callee = callee,
                    participants = listOf(callee),
                    callStartTime = Clock.System.now().toEpochMilliseconds()
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
     * 接收到来电（改进版）
     */
    fun receiveCall(caller: UserInfo, callId: String? = null) {
        // 如果已经有活跃通话，拒绝新的来电
        if (_videoCallState.value.callStatus.isActiveCall()) {
            Napier.d("已有活跃通话，拒绝新来电")
            webRTCManager?.rejectCall(callId ?: "")
            return
        }
        
        // 防止重复处理同一通来电
        if (isIncomingCallProcessing) {
            Napier.d("正在处理来电，忽略重复请求")
            return
        }
        
        isIncomingCallProcessing = true
        currentCallId = callId ?: "call_${Clock.System.now().toEpochMilliseconds()}"
        
        _videoCallState.value = _videoCallState.value.copy(
            callStatus = VideoCallStatus.INCOMING,
            caller = caller,
            participants = listOf(caller),
            callStartTime = Clock.System.now().toEpochMilliseconds(),
            callId = currentCallId
        )
        
        // 启动来电超时计时器
        startIncomingCallTimeout()
        
        Napier.d("收到新来电：${caller.username}, callId: $currentCallId")
    }

    /**
     * 启动来电超时计时器
     */
    private fun startIncomingCallTimeout() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(INCOMING_CALL_TIMEOUT_MILLIS)
            if (_videoCallState.value.callStatus == VideoCallStatus.INCOMING) {
                Napier.d("来电超时，自动拒绝")
                autoRejectCall("超时未接听")
            }
        }
    }

    /**
     * 自动拒绝来电
     */
    private fun autoRejectCall(reason: String) {
        viewModelScope.launch {
            try {
                webRTCManager?.rejectCall(currentCallId ?: "")
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.ENDED,
                    errorMessage = reason
                )
                cleanupAfterCall()
            } catch (e: Exception) {
                Napier.e("自动拒绝来电失败", e)
                forceResetState()
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    /**
     * 接受来电（改进版）
     */
    fun acceptCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.INCOMING) {
            Napier.w("当前状态不是来电状态，无法接受")
            return
        }
        
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
                
                Napier.d("成功接受来电")
            } catch (e: Exception) {
                handleError("接受通话失败", e)
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    /**
     * 拒绝来电
     */
    fun rejectCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.INCOMING) {
            Napier.w("当前状态不是来电状态，无法拒绝")
            return
        }
        
        viewModelScope.launch {
            try {
                currentCallId?.let { callId ->
                    webRTCManager?.rejectCall(callId)
                }
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.ENDED
                )
                cleanupAfterCall()
            } catch (e: Exception) {
                handleError("拒绝通话失败", e)
            } finally {
                isIncomingCallProcessing = false
            }
        }
    }

    /**
     * 结束通话（强化版）
     */
    fun endCall() {
        // 防止重复调用
        if (isEndingCall || _videoCallState.value.callStatus == VideoCallStatus.ENDED) {
            Napier.d("通话已在结束过程中或已结束")
            return
        }
        
        isEndingCall = true
        
        viewModelScope.launch {
            try {
                // 1. 立即更新UI状态
                _videoCallState.value = _videoCallState.value.copy(
                    callStatus = VideoCallStatus.ENDING
                )
                
                Napier.d("开始结束通话流程")

                // 2. 通知远端结束通话（异步执行）
                webRTCManager?.endCall()

                // 3. 并行释放本地资源
                releaseLocalResources()

                // 4. 重置状态
                resetCallState()
                
                Napier.d("通话结束流程完成")
                
            } catch (e: Exception) {
                Napier.e("结束通话过程中发生错误", e)
                handleError("结束通话失败", e)
                // 即使出错也要确保状态重置
                forceResetState()
            } finally {
                isEndingCall = false
            }
        }
    }

    /**
     * 释放本地资源
     */
    private suspend fun releaseLocalResources() {
        try {
            Napier.d("开始释放本地资源")
            
            // 按顺序释放资源
            releaseLocalMediaStream()
            releaseRemoteMediaTracks()
            resetControlStates()
            
            Napier.d("本地资源释放完成")
        } catch (e: Exception) {
            Napier.e("释放本地资源失败", e)
        }
    }

    /**
     * 释放本地媒体流
     */
    private fun releaseLocalMediaStream() {
        _localMediaStream.value?.let { stream ->
            try {
                stream.videoTracks.forEach { track -> track.setEnabled(false) }
                stream.audioTracks.forEach { track -> track.setEnabled(false) }
                // 如果有release方法则调用
                if (stream is com.github.im.group.sdk.MediaStream) {
                    // stream.release() // 根据实际API决定是否调用
                }
            } catch (e: Exception) {
                Napier.e("释放本地媒体流轨道失败", e)
            }
        }
        _localMediaStream.value = null
    }

    /**
     * 释放远程媒体轨道
     */
    private fun releaseRemoteMediaTracks() {
        try {
            remoteVideoTracks.value.forEach { _remoteVideo ->
                _remoteVideo.value?.setEnabled(false)
            }
            remoteAudioTracks.value.forEach { _remoteAudio ->
                _remoteAudio.value?.setEnabled(false)

            }
            _remoteVideoTracks.value = emptyMap<Long, VideoTrack>()
            _remoteAudioTracks.value = emptyMap<Long, AudioTrack>()
        } catch (e: Exception) {
            Napier.e("释放远程媒体轨道失败", e)
        }
    }

    /**
     * 重置控制状态
     */
    private fun resetControlStates() {
        _isCameraEnabled.value = true
        _isMicrophoneEnabled.value = true
        _isFrontCamera.value = true
        _isSpeakerEnabled.value = true
    }

    /**
     * 重置通话状态
     */
    private fun resetCallState() {
        _videoCallState.value = VideoCallState(
            callStatus = VideoCallStatus.ENDED
        )
        currentCallId = null
        isIncomingCallProcessing = false
        
        // 延迟重置为IDLE状态
        viewModelScope.launch {
            kotlinx.coroutines.delay(STATE_RESET_DELAY_MILLIS)
            if (_videoCallState.value.callStatus == VideoCallStatus.ENDED) {
                _videoCallState.value = VideoCallState()
                Napier.d("状态已重置为IDLE")
            }
        }
    }

    /**
     * 强制重置状态（异常情况下使用）
     */
    private fun forceResetState() {
        try {
            releaseMediaResources()
            _videoCallState.value = VideoCallState()
            currentCallId = null
            isIncomingCallProcessing = false
            isEndingCall = false
            Napier.d("强制状态重置完成")
        } catch (e: Exception) {
            Napier.e("强制状态重置失败", e)
        }
    }

    /**
     * 通话结束后的清理工作
     */
    private fun cleanupAfterCall() {
        viewModelScope.launch {
            try {
                releaseMediaResources()
                currentCallId = null
                isIncomingCallProcessing = false
            } catch (e: Exception) {
                Napier.e("通话结束后清理失败", e)
            }
        }
    }

    /**
     * 最小化通话（进入悬浮窗模式）
     */
    fun minimizeCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.ACTIVE) {
            Napier.w("只有活跃通话才能最小化")
            return
        }
        
        viewModelScope.launch {
            _videoCallState.value = _videoCallState.value.copy(
                callStatus = VideoCallStatus.MINIMIZED,
                isMinimized = true
            )
            Napier.d("通话已最小化")
        }
    }

    /**
     * 最大化通话（从悬浮窗模式恢复）
     */
    fun maximizeCall() {
        if (_videoCallState.value.callStatus != VideoCallStatus.MINIMIZED) {
            Napier.w("当前不是最小化状态")
            return
        }
        
        viewModelScope.launch {
            _videoCallState.value = _videoCallState.value.copy(
                callStatus = VideoCallStatus.ACTIVE,
                isMinimized = false
            )
            Napier.d("通话已恢复最大化")
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
     * 切换扬声器
     */
    fun toggleSpeaker() {
        viewModelScope.launch {
            _isSpeakerEnabled.value = !_isSpeakerEnabled.value
            _videoCallState.value = _videoCallState.value.copy(
                isSpeakerEnabled = _isSpeakerEnabled.value
            )
            
            // 对于扬声器控制，通常在平台层处理，这里只是更新状态
            Napier.d("扬声器状态切换: ${_isSpeakerEnabled.value}")
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
            Napier.d("开始创建本地媒体流")
            val mediaStream = webRTCManager?.createLocalMediaStream()
            _localMediaStream.value = mediaStream
            
            if (mediaStream != null) {
                Napier.d("本地媒体流创建成功，视频轨道数: ${mediaStream.videoTracks.size}, 音频轨道数: ${mediaStream.audioTracks.size}")
            } else {
                Napier.w("本地媒体流创建返回null")
            }
        } catch (e: Exception) {
            Napier.e("创建本地媒体流失败", e)
            throw e
        }
    }

    /**
     * 释放媒体资源（向后兼容方法）
     */
    private fun releaseMediaResources() {
        try {
            releaseLocalMediaStream()
            releaseRemoteMediaTracks()
            resetControlStates()
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
        Napier.d("VideoCallViewModel正在清理资源")
        try {
            webRTCManager?.release()
            releaseMediaResources()
        } catch (e: Exception) {
            Napier.e("清理视频通话资源失败", e)
        }
    }
}

/**
 * 扩展函数：判断是否为活跃通话状态
 */
fun VideoCallStatus.isActiveCall(): Boolean {
    return this in listOf(VideoCallStatus.OUTGOING, VideoCallStatus.INCOMING, 
                         VideoCallStatus.CONNECTING, VideoCallStatus.ACTIVE)
}