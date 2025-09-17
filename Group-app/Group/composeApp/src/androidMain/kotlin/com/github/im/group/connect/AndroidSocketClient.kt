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
    private var host: String = ""
    private var port: Int = 0


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

    override suspend fun connect(host: String, port: Int) {
        this.host = host
        this.port = port

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
     * 编码
     */
    private fun encodeVarint32(value: Int): ByteArray {
        var v = value
        val buffer = mutableListOf<Byte>()
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                buffer.add(v.toByte())
                break
            } else {
                buffer.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
        }
        return buffer.toByteArray()
    }

    override suspend fun send(data: ByteArray) {
        withContext(Dispatchers.IO) {
            // 首先判断当前是否 已经 连接了， 没连接就  重连
            if (!isActive()) {
                val isConnected = connectOnce(host, port)
                println("连接成功: $isConnected")
            }

            val lengthPrefix = encodeVarint32(data.size)
            output.write(lengthPrefix)
            output.write(data)
            output.flush()

        }
    }


    /**
     * 接收消息
     */
    private fun startReceiving() {

        if(reconnectJob?.isActive == true){
            receiveJob?.cancel()
        }
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    // 先读取长度前缀
                    val length = readVarint32(input)
                    val data = ByteArray(length)
                    var read = 0
                    while (read < length) {
                        val r = input.read(data, read, length - read)
                        if (r == -1) throw Exception("Socket closed")
                        read += r
                    }
                    val message = BaseMessagePkg.ADAPTER.decode(data)
                    withContext(Dispatchers.Main) {
                        viewModel.onNewMessage(message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("接收出错: ${e}")
            }
        }
    }
    private fun readVarint32(input: InputStream): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input.read()
            if (b == -1) throw Exception("Socket closed")
//            if (b == -1) break
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw Exception("Malformed varint32")
    }

    override suspend fun receive(): ByteArray {
        val length = readVarint32(input)
        val data = ByteArray(length)
        var read = 0
        while (read < length) {
            val r = input.read(data, read, length - read)
            if (r == -1) throw Exception("Socket closed")
            read += r
        }
        return data
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
