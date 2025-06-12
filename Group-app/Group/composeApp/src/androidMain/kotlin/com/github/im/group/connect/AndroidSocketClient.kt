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

        // 启动接收协程
        scope.launch {
            val adapter = BaseMessagePkg.ADAPTER
            val buffer = ByteArray(4096)

            while (true) {
                try {
                    val length = input.read(buffer)
                    if (length == -1) break
                    val msg = adapter.decode(buffer.copyOfRange(0, length))
                    withContext(Dispatchers.Main) {
                        viewModel.onNewMessage(msg)
                    }
                    println("接收到消息: $msg")
                    viewModel.updateMessage(msg)
                } catch (e: Exception) {
                    println("接收失败: ${e.message}")
                    break
                }
            }
        }
    }

    override suspend fun send(data: ByteArray) {
        withContext(Dispatchers.IO) {
            output.write(data)
            output.flush()
        }
    }


    /**
     * 开始 接收消息
     */
    private fun startReceiving() {
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

    override fun close() {
        socket.close()
    }
}
