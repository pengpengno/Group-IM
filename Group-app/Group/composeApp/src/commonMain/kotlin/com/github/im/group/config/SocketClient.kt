package com.github.im.group.config

// commonMain/kotlin/SocketClient.kt
interface SocketClient {


    suspend fun connect(host: String, port: Int)

    /**
     * 连接注册到远程服务器
     */
//    suspend fun registerToRemote(data: AccountInfo)

    /**
     * 发送消息
     */
    suspend fun send(data: ByteArray)
    suspend fun receive(): ByteArray

    fun isActive(): Boolean

    fun close()
}

//expect fun createSocketClient(): SocketClient
