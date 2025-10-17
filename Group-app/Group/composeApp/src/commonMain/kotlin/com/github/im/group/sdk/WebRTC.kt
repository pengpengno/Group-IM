package com.github.im.group.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/**
 * WebRTC跨平台接口定义
 */
@Composable
expect fun WebRTCVideoCall(
    modifier: Modifier = Modifier,
    onCallStarted: () -> Unit = {},
    onCallEnded: () -> Unit = {},
    onError: (String) -> Unit = {}
)

/**
 * 本地视频流预览
 */
@Composable
expect fun LocalVideoPreview(
    modifier: Modifier = Modifier
)

/**
 * 远程视频流显示
 */
@Composable
expect fun RemoteVideoView(
    modifier: Modifier = Modifier
)

/**
 * WebRTC功能接口
 */
interface WebRTCManager {
    /**
     * 初始化WebRTC
     */
    fun initialize()
    
    /**
     * 创建本地媒体流（音频+视频）
     */
    fun createLocalMediaStream(): MediaStream?
    
    /**
     * 连接到信令服务器
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
    fun switchCamera()
    
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
 * ICE候选信息
 */
data class IceCandidate(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String
)

/**
 * WebRTC消息对象
 */
@Serializable
data class WebrtcMessage(
    val type: String,        // 消息类型: OFFER, ANSWER, ICE_CANDIDATE, HANGUP, REJECT
    val from: String,        // 发送方用户ID
    val to: String,          // 接收方用户ID
    val payload: String?,    // 消息内容（SDP描述或ICE候选信息）
    val timestamp: Long?     // 时间戳
)

/**
 * 媒体流接口
 */
interface MediaStream {
    val id: String
    val audioTracks: List<AudioTrack>
    val videoTracks: List<VideoTrack>
}

/**
 * 音频轨道接口
 */
interface AudioTrack {
    val id: String
    val enabled: Boolean
    fun setEnabled(enabled: Boolean)
}

/**
 * 视频轨道接口
 */
interface VideoTrack {
    val id: String
    val enabled: Boolean
    fun setEnabled(enabled: Boolean)
}