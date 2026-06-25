package com.github.im.group.sdk

import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.config.ProxyConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class AndroidMeetingSignalingClient {
    interface Listener {
        fun onConnected()
        fun onMessage(text: String)
        fun onDisconnected(code: Int, reason: String)
        fun onFailure(error: Throwable)
    }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MILLIS = 5_000L
        private const val MAX_PENDING_MESSAGES = 100
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val okHttpClient = OkHttpClient.Builder()
        .authenticator(
            Authenticator { _, response ->
                response.request.newBuilder()
                    .header("Authorization", GlobalCredentialProvider.currentToken)
                    .build()
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private var listener: Listener? = null
    private var webSocket: WebSocket? = null
    private var userId: String? = null
    private var reconnectAttempts = 0
    private var shouldReconnect = false
    // 明确记录当前信令链路是否已经真正进入可发送状态，
    // 方便区分“已排队等待连接”与“已经成功实时发出”两类场景。
    private var isConnected = false
    private val pendingMessages = ArrayDeque<WebrtcMessage>()

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun connect(userId: String) {
        this.userId = userId
        this.shouldReconnect = true
        Napier.d("AndroidMeetingSignalingClient connect requested for user=$userId")
        establishConnection()
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectAttempts = 0
        isConnected = false
        pendingMessages.clear()
        webSocket?.close(1000, "client_disconnect")
        webSocket = null
    }

    fun send(message: WebrtcMessage): Boolean {
        val socket = webSocket
        if (socket != null && isConnected && socket.send(json.encodeToString(message))) {
            Napier.d(
                "AndroidMeetingSignalingClient send success: " +
                    "type=${message.type}, roomId=${message.roomId}, toUser=${message.toUser}"
            )
            return true
        }

        if (pendingMessages.size >= MAX_PENDING_MESSAGES) {
            Napier.e("AndroidMeetingSignalingClient pending queue is full. Dropping ${message.type}")
            return false
        }

        pendingMessages.addLast(message)
        Napier.w(
            "AndroidMeetingSignalingClient queued message: " +
                "type=${message.type}, roomId=${message.roomId}, toUser=${message.toUser}, " +
                "isConnected=$isConnected, hasSocket=${socket != null}, queueSize=${pendingMessages.size}"
        )
        ensureConnectionForQueuedMessages()
        return true
    }

    private fun establishConnection() {
        val currentUserId = userId ?: return
        val request = Request.Builder()
            .url("${ProxyConfig.getWsBaseUrl()}?userId=$currentUserId&token=${GlobalCredentialProvider.currentToken}")
            .header("Authorization", GlobalCredentialProvider.currentToken)
            .build()

        Napier.d("AndroidMeetingSignalingClient connecting: $request")

        webSocket?.cancel()
        isConnected = false
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                 isConnected = true
                Napier.d("AndroidMeetingSignalingClient connected. Flushing ${pendingMessages.size} queued messages")
                listener?.onConnected()
                flushPendingMessages()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener?.onMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener?.onDisconnected(code, reason)
                if (shouldReconnect && code != 1000) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                listener?.onFailure(t)
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    /**
     * 发送时如果底层连接已经断开，主动拉起一次重连，
     * 避免只把消息压入队列，却迟迟没有新的连接来触发 flush。
     */
    private fun ensureConnectionForQueuedMessages() {
        if (!shouldReconnect || userId == null || isConnected) {
            return
        }

        if (webSocket == null) {
            Napier.d("AndroidMeetingSignalingClient re-establish connection for queued signaling messages")
            establishConnection()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Napier.e("AndroidMeetingSignalingClient reconnect limit reached")
            return
        }

        reconnectAttempts += 1
        CoroutineScope(Dispatchers.Main).launch {
            delay(RECONNECT_DELAY_MILLIS)
            if (shouldReconnect) {
                establishConnection()
            }
        }
    }

    private fun flushPendingMessages() {
        val socket = webSocket ?: return
        while (pendingMessages.isNotEmpty()) {
            val message = pendingMessages.removeFirst()
            val sent = socket.send(json.encodeToString(message))
            Napier.d(
                "AndroidMeetingSignalingClient flush queued message: " +
                    "type=${message.type}, roomId=${message.roomId}, toUser=${message.toUser}, sent=$sent"
            )
        }
    }
}
