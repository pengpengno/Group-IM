package com.github.im.group.ui.video

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.github.im.group.model.UserInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * 来电管理器
 * 负责管理多个来电请求，处理来电队列和冲突
 */
class IncomingCallManager(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        private const val DEFAULT_CALL_TIMEOUT_MILLIS = 30000L // 30秒默认超时
        private const val BUSY_REJECT_DELAY_MILLIS = 1000L    // 忙碌拒绝延迟
    }
    
    private val _pendingCalls = mutableMapOf<String, IncomingCall>()
    private val _activeCallId = mutableStateOf<String?>(null)
    private val _isBusy = mutableStateOf(false)
    private val timeoutJobs = mutableMapOf<String, Job>()
    
    val pendingCalls: Map<String, IncomingCall> get() = _pendingCalls.toMap()
    val activeCallId: State<String?> = _activeCallId
    val isBusy: State<Boolean> = _isBusy
    
    /**
     * 处理新的来电请求
     */
    fun handleIncomingCall(
        caller: UserInfo,
        callId: String,
        timeoutMillis: Long = DEFAULT_CALL_TIMEOUT_MILLIS,
        onNewCall: (IncomingCall) -> Unit,
        onBusyReject: (String) -> Unit = {}
    ) {
        Napier.d("处理新来电: ${caller.username}, callId: $callId")
        
        // 检查是否已有活跃通话
        if (_activeCallId.value != null) {
            Napier.d("已有活跃通话，拒绝新来电: $callId")
            coroutineScope.launch {
                delay(BUSY_REJECT_DELAY_MILLIS)
                onBusyReject(callId)
            }
            return
        }
        
        // 检查是否已经在待处理队列中
        if (_pendingCalls.containsKey(callId)) {
            Napier.d("来电已在处理队列中: $callId")
            return
        }
        
        // 创建新的来电对象
        val incomingCall = IncomingCall(
            callId = callId,
            caller = caller,
            receivedTime = Clock.System.now().toEpochMilliseconds(),
            timeoutMillis = timeoutMillis
        )
        
        // 添加到待处理队列
        _pendingCalls[callId] = incomingCall
        _isBusy.value = true
        
        // 启动超时计时器
        startCallTimeout(callId, timeoutMillis)
        
        // 通知UI有新来电
        onNewCall(incomingCall)
        
        Napier.d("新来电已加入处理队列: $callId")
    }
    
    /**
     * 接受来电
     */
    fun acceptCall(callId: String): Boolean {
        val call = _pendingCalls[callId] ?: run {
            Napier.w("找不到待处理的来电: $callId")
            return false
        }
        
        // 取消超时任务
        timeoutJobs[callId]?.cancel()
        timeoutJobs.remove(callId)
        
        // 移除待处理队列
        _pendingCalls.remove(callId)
        
        // 设置为活跃通话
        _activeCallId.value = callId
        _isBusy.value = true
        
        Napier.d("来电已接受: $callId")
        return true
    }
    
    /**
     * 拒绝来电
     */
    fun rejectCall(callId: String, reason: String = "用户拒绝"): Boolean {
        val call = _pendingCalls[callId] ?: run {
            Napier.w("找不到待处理的来电: $callId")
            return false
        }
        
        // 取消超时任务
        timeoutJobs[callId]?.cancel()
        timeoutJobs.remove(callId)
        
        // 从待处理队列移除
        _pendingCalls.remove(callId)
        
        // 更新忙碌状态
        updateBusyStatus()
        
        Napier.d("来电已拒绝 ($reason): $callId")
        return true
    }
    
    /**
     * 结束当前通话
     */
    fun endCurrentCall() {
        val currentCallId = _activeCallId.value ?: run {
            Napier.w("没有活跃的通话")
            return
        }
        
        _activeCallId.value = null
        _isBusy.value = false
        
        Napier.d("当前通话已结束: $currentCallId")
    }
    
    /**
     * 获取当前第一个待处理来电
     */
    fun getFirstPendingCall(): IncomingCall? {
        return _pendingCalls.values.firstOrNull()
    }
    
    /**
     * 检查是否有待处理来电
     */
    fun hasPendingCalls(): Boolean {
        return _pendingCalls.isNotEmpty()
    }
    
    /**
     * 清理所有待处理来电
     */
    fun clearAllPendingCalls() {
        // 取消所有超时任务
        timeoutJobs.values.forEach { it.cancel() }
        timeoutJobs.clear()
        
        // 清空待处理队列
        _pendingCalls.clear()
        _isBusy.value = _activeCallId.value != null
        
        Napier.d("已清理所有待处理来电")
    }
    
    /**
     * 启动来电超时计时器
     */
    private fun startCallTimeout(callId: String, timeoutMillis: Long) {
        val job = coroutineScope.launch {
            try {
                delay(timeoutMillis)
                // 检查来电是否仍然在待处理队列中
                if (_pendingCalls.containsKey(callId)) {
                    Napier.d("来电超时，自动拒绝: $callId")
                    rejectCall(callId, "超时未接听")
                }
            } catch (e: Exception) {
                Napier.e("来电超时任务异常: $callId", e)
            }
        }
        
        timeoutJobs[callId] = job
    }
    
    /**
     * 更新忙碌状态
     */
    private fun updateBusyStatus() {
        _isBusy.value = _activeCallId.value != null || _pendingCalls.isNotEmpty()
    }
    
    /**
     * 重置管理器状态
     */
    fun reset() {
        clearAllPendingCalls()
        _activeCallId.value = null
        _isBusy.value = false
        Napier.d("来电管理器已重置")
    }
}

/**
 * 来电数据类
 */
data class IncomingCall(
    val callId: String,
    val caller: UserInfo,
    val receivedTime: Long,
    val timeoutMillis: Long,
    val ringCount: Int = 0
) {
    val elapsedTime: Long
        get() = Clock.System.now().toEpochMilliseconds() - receivedTime
    
    val isTimedOut: Boolean
        get() = elapsedTime > timeoutMillis
    
    fun getFormattedElapsedTime(): String {
        val seconds = elapsedTime / 1000
        val minutes = seconds / 60
        return "${minutes.toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}

/**
 * 来电处理结果枚举
 */
sealed class CallHandlingResult {
    data class Accepted(val callId: String) : CallHandlingResult()
    data class Rejected(val callId: String, val reason: String) : CallHandlingResult()
    data class Busy(val callId: String) : CallHandlingResult()
    data class Timeout(val callId: String) : CallHandlingResult()
    data class Error(val callId: String, val error: String) : CallHandlingResult()
}