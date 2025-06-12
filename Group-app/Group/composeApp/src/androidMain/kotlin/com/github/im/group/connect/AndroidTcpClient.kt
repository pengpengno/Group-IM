package com.github.im.group.connect

import com.github.im.group.config.TcpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket


class AndroidTcpClient(
    private val host: String,
    private val port: Int
) : TcpClient {
    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null

    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            socket = Socket(host, port)
            output = socket!!.getOutputStream()
            input = socket!!.getInputStream()
        }
    }

    override suspend fun send(data: ByteArray) {
        withContext(Dispatchers.IO) {
            output?.write(data)
            output?.flush()
        }
    }

    override  fun receive(): Flow<ByteArray> = callbackFlow {
        try {
            while (isActive) {
                val buffer = ByteArray(4096)
                val bytesRead = input?.read(buffer) ?: -1
                if (bytesRead == -1) {
                    close() // socket 关闭或断开
                } else {
                    trySend(buffer.copyOf(bytesRead)).isSuccess
                }
            }
        } catch (e: Exception) {
            close(e)
        }
    }

    override fun close() {
        socket?.close()
    }
}
