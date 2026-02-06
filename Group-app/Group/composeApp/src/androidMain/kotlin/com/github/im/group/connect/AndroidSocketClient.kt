package com.github.im.group.connect

import com.github.im.group.config.SocketClient
import com.github.im.group.manager.MessageRouter
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.Heartbeat
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
    private val messageRouter: MessageRouter
) : SocketClient {

    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var host: String = ""
    private var port: Int = 0
    private val connectionMutex = Mutex()
    private var isReconnecting = false
    private var lastHeartbeatTime: Long = 0
    private val HEARTBEAT_INTERVAL = 25000L // 25秒发送一次心跳
    private val HEARTBEAT_TIMEOUT = 35000L // 35秒超时

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

            // 初始化心跳时间为当前时间，避免初始状态判断问题
            lastHeartbeatTime = System.currentTimeMillis()
            
            startReceiving()
            startHeartbeat()
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
            val lengthPrefix = encodeVarint32(data.size)
            output?.write(lengthPrefix)
            output?.write(data)
            output?.flush()
        }
    }

    /**
     * 启动心跳包发送任务
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    // 每25秒发送一次心跳包
                    delay(HEARTBEAT_INTERVAL)
                    if (isActive()) {
                        // 创建心跳消息 (ping = true)
                        val heartbeat = Heartbeat(ping = true)
                        val message = BaseMessagePkg(
                            heartbeat = heartbeat
                        )
                        val data = message.encode()
                        send(data)
                        Napier.d("已发送心跳包")
                        // 更新最近心跳发送时间
                        lastHeartbeatTime = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Napier.e("发送心跳包失败: ${e.message}")
                    // 心跳发送失败，可能连接已断开，触发重连
                    if (isActive) {
                        Napier.d("心跳发送失败，触发重连")
                        startAutoReconnect()
                    }
                }
            }
        }
    }

    /**
     * 处理接收到的心跳消息
     */
    private suspend fun handleHeartbeat(heartbeat: Heartbeat) {
        when {
            // 如果是服务端发来的ping请求，则回复pong
            heartbeat.ping -> {
                Napier.d("收到服务端心跳PING，回复PONG")
                val pong = Heartbeat(ping = false)
                val message = BaseMessagePkg(
                    heartbeat = pong
                )
                val data = message.encode()
                send(data)
            }
            // 如果是pong响应，记录日志
            else -> {
                Napier.d("收到心跳PONG响应")
                // 更新最近心跳时间
                lastHeartbeatTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * 启动自动重连机制
     */
    private fun startAutoReconnect() {
        // 如果已经在重连中，直接返回
        if (isReconnecting) return
        
        // 如果已有重连任务在运行，则取消它
        reconnectJob?.cancel()

        reconnectJob = scope.launch {
            isReconnecting = true
            try {
                var retryCount = 0L
                while (isActive) {
                    try {
                        Napier.d("尝试重连... (第${retryCount + 1}次)")
                        connect(host, port)
                        Napier.d("重连成功")
                        isReconnecting = false
                        break // 重连成功，退出循环
                    } catch (e: Exception) {
                        Napier.e("重连失败: ${e.message}")
                        retryCount++
                        // 指数退避策略，最大延迟60秒
                        val delayTime = kotlin.math.min(60000L, 2000L * retryCount)
                        delay(delayTime)
                    }
                }
            } finally {
                isReconnecting = false
            }
        }
    }

    /**
     * 停止自动重连
     */
    private fun stopAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        isReconnecting = false
    }

    /**
     * 接收消息
     * 无法读取 / 读取 数据出错会抛出异常, 并且自动出发重连
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
                            val r = input?.read(data, read, length - read) ?: throw SocketException("Socket closed")
                            if (r == -1) throw SocketException("Socket closed")
                            read += r
                        }
                        val message = BaseMessagePkg.ADAPTER.decode(data)
                        // 处理心跳消息
                        if (message.heartbeat != null) {
                            handleHeartbeat(message.heartbeat)
                        } else {
                            // 处理其他消息
                            withContext(Dispatchers.Main) {
                                messageRouter.routeMessage(message)
                            }
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
        val socketActive = socket != null && (socket?.channel?.isConnected
            ?: socket?.isConnected) == true && socket?.isClosed == false
        
        // 如果socket不活跃，直接返回false
        if (!socketActive) return false
        
        // 检查心跳是否超时（35秒内没有收到心跳响应）
        val currentTime = System.currentTimeMillis()
        val timeSinceLastHeartbeat = currentTime - lastHeartbeatTime
        val isHeartbeatTimeout = timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT
        
        if (isHeartbeatTimeout) {
            Napier.d("心跳超时: ${timeSinceLastHeartbeat}ms")
        }
        
        return !isHeartbeatTimeout
    }

    override fun close() {
        stopAutoReconnect()
        receiveJob?.cancel()
        heartbeatJob?.cancel()
        receiveJob = null
        heartbeatJob = null

        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (e: Exception) {
            Napier.e("关闭连接时出错: ${e.message}")
        }

        input = null
        output = null
        socket = null
    }
}