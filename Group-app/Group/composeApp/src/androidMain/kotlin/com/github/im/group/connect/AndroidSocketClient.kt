package com.github.im.group.connect

import com.github.im.group.config.SocketClient
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.viewmodel.TCPMessageViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class AndroidSocketClient(
    private val viewModel: TCPMessageViewModel
) : SocketClient {

    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var host: String = ""
    private var port: Int = 0
    private val connectionMutex = Mutex()
    private var isReconnecting = false

    /**
     * 连接一次 成功返回true
     */
    private suspend fun connectOnce(host: String, port: Int): Boolean {
        return try {
            connect(host, port)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun connect(host: String, port: Int) {
        connectionMutex.withLock {
            this.host = host
            this.port = port

            // 关闭现有连接
            close()

            socket = withContext(Dispatchers.IO) {
                Socket(host, port)
            }
            input = withContext(Dispatchers.IO) {
                socket?.getInputStream()
            }
            output = withContext(Dispatchers.IO) {
                socket?.getOutputStream()
            }

            startReceiving()
        }
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
            output?.write(lengthPrefix)
            output?.write(data)
            output?.flush()
        }
    }

    /**
     * 启动自动重连机制
     */
    private fun startAutoReconnect() {
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            while (isActive) {
                try {
                    if (!isActive() && !isReconnecting) {
                        isReconnecting = true
                        println("检测到断线，尝试重连...")
                        connect(host, port)
                        println("重连成功")
                        isReconnecting = false
                    }
                } catch (e: Exception) {
                    println("自动重连异常: ${e.message}")
                    isReconnecting = false
                    // 指数退避策略，最大延迟60秒
                    val delayTime = kotlin.math.min(60000L, 2000L * (reconnectJob?.hashCode()?.rem(10) ?: 1))
                    delay(delayTime)
                }
                delay(2000)
            }
        }
    }

    /**
     * 停止自动重连
     */
    private fun stopAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    /**
     * 接收消息
     */
    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    try {
                        // 先读取长度前缀
                        val length = readVarint32()
                        val data = ByteArray(length)
                        var read = 0
                        while (read < length) {
                            val r = input?.read(data, read, length - read) ?: throw Exception("Socket closed")
                            if (r == -1) throw Exception("Socket closed")
                            read += r
                        }
                        val message = BaseMessagePkg.ADAPTER.decode(data)
                        withContext(Dispatchers.Main) {
                            viewModel.onNewMessage(message)
                        }
                    } catch (e: SocketException) {
                        // 连接断开，触发重连
                        Napier.d("连接断开，触发重连: ${e.message}")
                        startAutoReconnect()
                        break
                    }
                }
            } catch (e: Exception) {
                Napier.d("接收出错: ${e.stackTrace}")
//                startAutoReconnect()
            }
        }
    }

    private fun readVarint32(): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input?.read() ?: throw Exception("Socket closed")
            if (b == -1) throw Exception("Socket closed")
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw Exception("Malformed varint32")
    }

    override suspend fun receive(): ByteArray {
        val length = readVarint32()
        val data = ByteArray(length)
        var read = 0
        while (read < length) {
            val r = input?.read(data, read, length - read) ?: throw Exception("Socket closed")
            if (r == -1) throw Exception("Socket closed")
            read += r
        }
        return data
    }

    override fun isActive(): Boolean {

        return socket != null && socket?.isConnected == true && socket?.isClosed == true
    }

    override fun close() {
        stopAutoReconnect()
        receiveJob?.cancel()
        receiveJob = null

        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (e: Exception) {
            println("关闭连接时出错: ${e.message}")
        }

        input = null
        output = null
        socket = null
    }
}