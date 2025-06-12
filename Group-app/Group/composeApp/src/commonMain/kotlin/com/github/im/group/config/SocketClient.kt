package com.github.im.group.config

// commonMain/kotlin/SocketClient.kt
interface SocketClient {
    suspend fun connect(host: String, port: Int)
    suspend fun send(data: ByteArray)
    suspend fun receive(): ByteArray
    fun close()
}

//expect fun createSocketClient(): SocketClient
