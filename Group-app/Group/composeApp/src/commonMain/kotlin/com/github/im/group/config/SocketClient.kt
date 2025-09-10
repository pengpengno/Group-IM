package com.github.im.group.config

// commonMain/kotlin/SocketClient.kt
interface SocketClient {


    suspend fun connect(host: String, port: Int)



    /**
     * 发送消息
     * @param data 字节流数据 如果是 Protobuf的 数据请注意编解码
     */
    suspend fun send(data: ByteArray)

    suspend fun receive(): ByteArray

    fun isActive(): Boolean

    fun close()
}

//expect fun createSocketClient(): SocketClient
