package com.github.im.group.ui.video

import com.github.im.group.model.UserInfo

/**
 * 视频通话事件处理器接口
 */
interface VideoCallEventHandler {
    fun startCall(callee: UserInfo)
    fun receiveCall(caller: UserInfo)
    fun acceptCall()
    fun rejectCall()
    fun endCall()
    fun toggleCamera()
    fun toggleMicrophone()
    fun toggleSpeaker()
    fun minimizeCall()
    fun maximizeCall()
    fun switchCamera()
}

/**
 * 视频通话事件回调接口
 */
interface VideoCallEventCallback {
    fun onCallStarted()
    fun onCallConnected()
    fun onCallEnded()
    fun onCallRejected()
    fun onCallMissed()
    fun onError(error: String)
}