package com.github.im.group.sdk

import ProxyConfig
import com.github.im.group.config.SocketClient
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.PlatformType
import com.github.im.group.viewmodel.TCPMessageViewModel
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
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _connected = MutableStateFlow(false)
    private val _host =ProxyConfig.host
    private val _port = ProxyConfig.tcp_port
    private val _loginPkg = MutableStateFlow<BaseMessagePkg?>(null)
    private var reconnectJob: Job? = null
    private val reconnectMutex = Mutex()


    /***
     * 用于 向远程长连接服务器建立连接
     */
    fun loginConnect(userInfo: UserInfo) {

        val accountInfo = AccountInfo(
            account = userInfo.username,
            accountName = userInfo.username,
            userId = userInfo.userId,
            eMail = userInfo.email,
            platformType = PlatformType.ANDROID,
        )

        _loginPkg.value = BaseMessagePkg(
            accountInfo = accountInfo
        )

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
            reconnectMutex.withLock {
                // 如果已有重连任务在运行，则取消它
                reconnectJob?.cancel()
                
                // 启动新的重连任务
                reconnectJob = launch {
                    while (isActive) {
                        try {
                            if (!tcpClient.isActive()) {
                                println("检测到断线，尝试重连...")
                                tcpClient.connect(_host, _port)
                                println("重连成功，重新启动接收协程")
                                _connected.value = true
                            } else {
                                _connected.value = true
                            }
                        } catch (e: Exception) {
                            println("自动重连异常: ${e.message}")
                            _connected.value = false
                            // 指数退避策略，最大延迟60秒
                            val delayTime = kotlin.math.min(60000L, 2000L * (reconnectJob?.hashCode()?.rem(10) ?: 1))
                            delay(delayTime)
                        }

                        // 每2秒检查一次连接状态
                        delay(2000)
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
            registerToRemote()
        }
        // 确保连接已建立后再发送数据
        while (!tcpClient.isActive()) {
            delay(100) // 等待100毫秒再检查
        }
        tcpClient.send(data)
    }

    /**
     * 注册到远程服务器
     * 开始连接
     */
    private suspend fun registerToRemote() {
        try {
            // 首先 建立TCP 连接通道
            if (!tcpClient.isActive()){
                tcpClient.connect(_host,_port)
            }

            _loginPkg.value?.let { pkg ->
                pkg.accountInfo?.let { accountInfo ->
                    val message = BaseMessagePkg.ADAPTER.encode(pkg)
                    tcpClient.send(message)
                }
            }
            
            _connected.value = true
            // 启动自动重连机制
            startAutoReconnect()
        } catch (e: Exception) {
            println("连接到远程服务器失败: ${e.message}")
            _connected.value = false
            // 启动自动重连机制以尝试恢复连接
            startAutoReconnect()
        }
    }
}