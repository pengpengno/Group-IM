package com.github.im.group.connect// androidMain/kotlin/SocketClientImpl.kt
import com.github.im.group.config.SocketClient
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.viewmodel.TCPMessageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class AndroidSocketClient(
    private val viewModel: TCPMessageViewModel
) : SocketClient {

    private lateinit var socket: Socket
    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null

//    private val _host = MutableStateFlow(ProxyConfig.host)
//    private val _port = MutableStateFlow(ProxyConfig.tcp_port)


    /**
     * 连接一次 成功返回true
     */
    private suspend fun connectOnce(host: String, port: Int): Boolean {
         return try {
             connect(host, port)
             true
         }catch (e: Exception)
         {
             false
         }
    }

//     suspend fun registerToRemote(data: AccountInfo) {
//
//        _loginPkg.value = data
//
//        if (connectOnce(_host.value, _port.value)) {
//            val pkg = BaseMessagePkg(accountInfo = data)
//            val bytes = pkg.encode()
//            send(bytes)
//        }
//
//    }

    override suspend fun connect(host: String, port: Int) {

        socket = withContext(Dispatchers.IO) {
            Socket(host, port)
        }
        input = withContext(Dispatchers.IO) {
            socket.getInputStream()
        }
        output = withContext(Dispatchers.IO) {
            socket.getOutputStream()
        }

        startReceiving()
        // 启动接收协程
    }


    /**
     * 重新启动接收协程
     */
    private fun restartReceiving() {
        receiveJob?.cancel()
        startReceiving()
    }

    override suspend fun send(data: ByteArray) {
        withContext(Dispatchers.IO) {
            // 首先判断当前是否 已经 连接了， 没连接就  重连
            output.write(data)
            output.flush()
        }
    }


    /**
     * 开始 接收消息
     */
    private fun startReceiving() {

        if(reconnectJob?.isActive == true){
            receiveJob?.cancel()
        }
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val buffer = ByteArray(4096)
                while (isActive) {
                    val len = input.read(buffer)
                    if (len == -1) break

                    val data = buffer.copyOf(len)

                    // 解析 Protobuf
                    val message = BaseMessagePkg.ADAPTER.decode(data)
                    withContext(Dispatchers.Main) {
                        viewModel.onNewMessage(message)
                    }
                }
            } catch (e: Exception) {
                println("接收出错: ${e.message}")
            }
        }
    }

    override suspend fun receive(): ByteArray {
        val buffer = ByteArray(4096)
        val length = withContext(Dispatchers.IO) {
            input.read(buffer)
        }
        return buffer.copyOfRange(0, length)
    }


    override fun isActive(): Boolean {
        return if (::socket.isInitialized) {
            socket.isConnected && !socket.isClosed
        } else {
            false
        }
    }

    override fun close() {
        socket.close()
    }
}
