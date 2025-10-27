package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/**
 * WebRTC跨平台视频通话组件
 */

// 抽象类定义
abstract class VideoTrack {
    abstract val id: String
    abstract val isEnabled: Boolean
    abstract fun setEnabled(enabled: Boolean)
}

abstract class AudioTrack {
    abstract val id: String
    abstract val isEnabled: Boolean
    abstract fun setEnabled(enabled: Boolean)
}

abstract class MediaStream {
    abstract val id: String
    abstract val videoTracks: List<VideoTrack>
    abstract val audioTracks: List<AudioTrack>
}



// Note: WebRTC is not supported on desktop platform, so this is only for platforms that support WebRTC
@Composable
expect fun VideoScreenView(
    modifier: Modifier? = null,
    videoTrack: VideoTrack? = null,
    audioTrack: AudioTrack? = null
)

/**
 * WebRTC功能接口
 */
interface WebRTCManager {
    /**
     * 初始化WebRTC
     * 链接到远程服务器
     */
    suspend fun initialize()
    
    /**
     * 创建本地媒体流（音频+视频）
     */
    suspend fun createLocalMediaStream(): MediaStream?
    
    /**
     * 连接到信令服务器
     * 这里使用WebSocket
     */
    fun connectToSignalingServer(serverUrl: String, userId: String)
    
    /**
     * 发起呼叫
     */
    fun initiateCall(remoteUserId: String)
    
    /**
     * 接受呼叫
     */
    fun acceptCall(callId: String)
    
    /**
     * 拒绝呼叫
     */
    fun rejectCall(callId: String)
    
    /**
     * 结束通话
     */
    fun endCall()
    
    /**
     * 切换摄像头
     */
    suspend fun switchCamera()
    
    /**
     * 开关摄像头
     */
    fun toggleCamera(enabled: Boolean)
    
    /**
     * 开关麦克风
     */
    fun toggleMicrophone(enabled: Boolean)
    
    /**
     * 发送ICE候选
     */
    fun sendIceCandidate(candidate: IceCandidate)
    
    /**
     * 释放资源
     */
    fun release()
}

/**
 * WebRTC消息对象
 */
@Serializable
data class WebrtcMessage(
    val type: String,              // 消息类型: call/request, call/accept, call/end, offer, answer, candidate
    val fromUser: String? = null,  // 发送方用户ID
    val toUser: String? = null,    // 接收方用户ID
    val sdp: String? = null,       // SDP描述信息
    val sdpType: String? = null,   // SDP类型: offer/answer
    val candidate: IceCandidateData? = null,  // ICE候选信息
    val reason: String? = null     // 失败原因
)

/**
 * ICE候选数据
 */
@Serializable
data class IceCandidateData(
    val candidate: String,         // 候选描述
    val sdpMid: String,            // SDP中段标识
    val sdpMLineIndex: Int,        // SDP中媒体行索引
    val usernameFragment: String? = null, // usernameFragment字段
    val sdp: String? = null
)

@Serializable
data class IceCandidate(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)