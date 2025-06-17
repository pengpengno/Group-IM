package com.github.im.group.sdk

import ProxyConfig
import com.github.im.group.config.SocketClient
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.ChatMessage
import com.github.im.group.model.proto.PlatformType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SenderSdk(
    private val tcpClient: SocketClient,
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _connected = MutableStateFlow(false)
    private val _host = MutableStateFlow(ProxyConfig.host)
    private val _port = MutableStateFlow(ProxyConfig.tcp_port)
    private val _loginPkg = MutableStateFlow<BaseMessagePkg?>(null)
    private var reconnectJob: Job? = null


    /***
     * 用于 向远程长连接服务器建立连接
     */
    suspend fun loginConnect(userInfo: UserInfo) {

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

    fun sendMessage(chatMessage: ChatMessage){

        val baseMessage = BaseMessagePkg(message = chatMessage)

        CoroutineScope(Dispatchers.IO).launch {
            send(BaseMessagePkg.ADAPTER.encode(baseMessage))

        }
    }

    fun startAutoReconnect() {
        if (reconnectJob != null) return  // 防止重复启动
        reconnectJob = scope.launch {
            while (isActive) {
                try {
                    if (!tcpClient.isActive()) {
                        println("检测到断线，尝试重连...")
                         tcpClient.connect(_host.value, _port.value)
                        println("重连成功，重新启动接收协程")
                        _connected.value = true
                    } else {
                        _connected.value = true
                    }
                } catch (e: Exception) {
                    println("自动重连异常: ${e.message}")
                    _connected.value = false
                    delay(3000)
                }

                delay(2000)
            }
        }
    }




    private suspend fun send(data: ByteArray){

        if (!tcpClient.isActive()){
            registerToRemote()
        }
        tcpClient.send(data)

    }



    /**
     * 注册到远程服务器
     */
    private suspend fun registerToRemote() {
        // 首先 建立TCP 连接通道
        if (!tcpClient.isActive()){
            tcpClient.connect(_host.value,_port.value)
        }

        if (_loginPkg.value != null){
            val message = _loginPkg.value.let {
                if (it != null) {
                    BaseMessagePkg.ADAPTER.encode(it)
                }
                else {
                    null
                }
            }
            if (message != null){
                // 发送远程注册包
                tcpClient.send(message)

            }

        }

    }

}