package com.github.im.group.sdk

import ProxyConfig
import com.github.im.group.config.SocketClient
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.PlatformType
import com.github.im.group.repository.UserRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SenderSdk(
    private val tcpClient: SocketClient,
    private val userRepository: UserRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _connected = MutableStateFlow(false)
    private val _host =ProxyConfig.host
    private val _port = ProxyConfig.tcp_port
    private var reconnectJob: Job? = null
    private val reconnectMutex = Mutex()

    /***
     * 用于 向远程长连接服务器建立连接
     */
    fun loginConnect() {
        CoroutineScope(Dispatchers.Main).launch {
            registerToRemote()
        }
    }

    /**
     * 发送消息
     * @param chatMessage 消息体
     */
    fun sendMessage(chatMessage: ChatMessage){
        val baseMessage = BaseMessagePkg(message = chatMessage)
        scope.launch {
            send(BaseMessagePkg.ADAPTER.encode(baseMessage))
        }
    }

    /**
     * 自动重连逻辑
     */
    private fun startAutoReconnect() {
        scope.launch {
            Napier.d("自动重连任务启动")
            reconnectMutex.withLock {
                // 如果已有重连任务在运行，则取消它
                reconnectJob?.cancel()
                
                // 启动新的重连任务
                reconnectJob = launch {
                    var retryCount = 0L
                    while (isActive) {
//                        Napier.d("正在检查连接状态...")
                        try {
                            val isConnected = tcpClient.isActive()
                            Napier.d("连接状态: $isConnected")
                            if (!isConnected) {
                                Napier.d("检测到断线，尝试重连...")
                                tcpClient.connect(_host, _port)
                                Napier.d("重连成功，重新启动接收协程")
                                // 重新注册到远程服务器（重新发送登录信息）
                                registerToRemoteSilently()
                                retryCount = 0 // 重置重试计数
                            }
                            _connected.value = true
                        } catch (e: Exception) {
                            Napier.d("自动重连异常: ${e.message}")
                            _connected.value = false
                            retryCount++
                            // 指数退避策略，最大延迟60秒
                            val delayTime = kotlin.math.min(60000L, 2000L * retryCount)
                            delay(delayTime)
                        }

                        // 每15秒检查一次连接状态
                        delay(15000)
                    }
                }
            }
        }
    }

    /**
     * 停止自动重连
     */
    fun stopAutoReconnect() {
        scope.launch {
            reconnectMutex.withLock {
                reconnectJob?.cancel()
                reconnectJob = null
            }
        }
    }

    /**
     * 发送数据
     */
    private suspend fun send(data: ByteArray){
        if (!tcpClient.isActive()){
            _connected.value = false
            // 不再在这里调用registerToRemoteSilently，避免与心跳包机制冲突
        }
        // 确保连接已建立后再发送数据
        while (!tcpClient.isActive()) {
            _connected.value = false
            delay(100) // 等待100毫秒再检查
        }
        _connected.value = true
        tcpClient.send(data)
    }

    /**
     * 注册身份并开始链接到远程服务器
     *
     */
    private suspend fun registerToRemote() {
        try {
            // 获取当前登录用户信息
            val currentUser = userRepository.requireLoggedInUser()
            
            // 首先 建立TCP 连接通道
            if (!tcpClient.isActive()){
                tcpClient.connect(_host,_port)
            }

            val pkg = BaseMessagePkg(accountInfo = currentUser.accountInfo)
            val message = BaseMessagePkg.ADAPTER.encode(pkg)
            tcpClient.send(message)
            _connected.value = true
            startAutoReconnect()

        } catch (e: Exception) {
            Napier.d("连接到远程服务器失败: ${e.message}")
            _connected.value = false
            startAutoReconnect()

        }
    }
    
    /**
     * 静默注册到远程服务器（用于重连）
     */
    private suspend fun registerToRemoteSilently() {
        try {
            Napier.d  ("重连时发送登录信息")

            // 获取当前登录用户信息
            val currentUser = userRepository.requireLoggedInUser()
            
            val pkg = BaseMessagePkg(accountInfo = currentUser.accountInfo)
            val message = BaseMessagePkg.ADAPTER.encode(pkg)
            tcpClient.send(message)
            
            _connected.value = true
        } catch (e: Exception) {
            Napier.d  ("重连时发送登录信息失败: ${e.message}")
            _connected.value = false
        }
    }
}