package com.github.im.group.config
import kotlinx.coroutines.flow.Flow

interface TcpClient {
    suspend fun connect()

//    suspend fun disconnect()

    suspend fun send(data: ByteArray)

    fun receive(): Flow<ByteArray>

    fun close()
}
//
//class KtorTcpClient(
//    private val host: String,
//    private val port: Int,
//    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
//) : TcpClient {
//
//    private var socket: Socket? = null
//    private var input: ByteReadChannel? = null
//    private var output: ByteWriteChannel? = null
//    private val selector = SelectorManager(dispatcher)
//    private val scope = CoroutineScope(dispatcher + SupervisorJob())
//    private val _incoming = MutableSharedFlow<ByteArray>()
//
//    override suspend fun connect() = withContext(dispatcher) {
//        socket = aSocket(selector).tcp().connect(host, port)
//        input = socket!!.openReadChannel()
//        output = socket!!.openWriteChannel(autoFlush = true)
//        scope.launch { readLoop() }
//    }
//
//    override suspend fun disconnect() = withContext(dispatcher) {
//        socket?.close(); selector.close()
//        scope.cancel()
//    }
//
//    override suspend fun send(data: ByteArray) = withContext(dispatcher) {
//        output?.writeFully(data)
//    }
//
//    override fun receive() = _incoming.asSharedFlow()
//
//    private suspend fun readLoop() {
//        val buf = ByteArray(4)
//        try {
//            while (true) {
//                input?.readFully(buf, 0, 4) ?: break
//                val len = java.nio.ByteBuffer.wrap(buf).int
//                val msg = ByteArray(len)
//                input!!.readFully(msg, 0, len)
//                _incoming.emit(msg)
//            }
//        } catch (e: Exception) {
//            // 读取终止或异常可处理
//        }
//    }
//}
