package com.github.im.group.ui.video

import com.github.im.group.model.UserInfo

/**
 * 视频通话状态枚举
 */
enum class VideoCallStatus {
    IDLE,           // 空闲状态
    OUTGOING,       // 拨出通话
    INCOMING,       // 来电通话
    CONNECTING,     // 正在连接
    ACTIVE,         // 通话中
    ENDED,          // 已结束
    MINIMIZED,      // 已最小化（悬浮窗状态）
    ERROR           // 错误状态
}

/**
 * 视频通话状态数据类
 */
data class VideoCallState(
    val callStatus: VideoCallStatus = VideoCallStatus.IDLE,
    val caller: UserInfo? = null,           // 主叫用户（一对一通话）
    val callee: UserInfo? = null,           // 被叫用户（一对一通话）
    val participants: List<UserInfo> = emptyList(), // 参与者列表（支持群聊）
    val callStartTime: Long? = null,        // 通话开始时间
    val duration: Long = 0,                 // 通话时长（秒）
    val isLocalVideoEnabled: Boolean = true, // 本地视频是否启用
    val isRemoteVideoEnabled: Boolean = true, // 远程视频是否启用
    val isMicrophoneEnabled: Boolean = true,  // 麦克风是否启用
    val isSpeakerEnabled: Boolean = true,     // 扬声器是否启用
    val errorMessage: String? = null,         // 错误信息
    val isMinimized: Boolean = false          // 是否处于最小化状态
)