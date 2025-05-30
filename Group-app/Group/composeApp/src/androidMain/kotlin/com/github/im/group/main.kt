package com.github.im.group

//import com.example.protobuf.MessageProtos
import java.io.OutputStream
import java.net.Socket

fun main() {
    val host = "127.0.0.1"
    val port = 5000

    Socket(host, port).use { socket ->
        val out: OutputStream = socket.getOutputStream()

//        // 构造 Protobuf 消息
//        val message = MessageProtos.ChatMessage.newBuilder()
//            .setSender("Alice")
//            .setContent("Hello from Kotlin TCP client!")
//            .build()
//
//        // 写入长度（前缀编码，4 字节，大端）
//        val messageBytes = message.toByteArray()
//        val lengthPrefix = messageBytes.size
//        out.write(
//            byteArrayOf(
//                (lengthPrefix shr 24).toByte(),
//                (lengthPrefix shr 16).toByte(),
//                (lengthPrefix shr 8).toByte(),
//                lengthPrefix.toByte()
//            )
//        )
//
//        // 写入 Protobuf 数据
//        out.write(messageBytes)
//        out.flush()

        println("消息已发送")
    }
}
