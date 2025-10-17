package com.github.im.group.sdk

import ProxyConfig
import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.github.im.group.ui.video.VideoCallViewModel

/**
 * Android平台WebRTC视频通话实现
 */
@Composable
actual fun WebRTCVideoCall(
    modifier: Modifier,
    onCallStarted: () -> Unit,
    onCallEnded: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 初始化PeerConnectionFactory
    val peerConnectionFactory = remember {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }
    
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
    
    LaunchedEffect(peerConnectionFactory) {
        try {
            // 初始化通话
            onCallStarted()
        } catch (e: Exception) {
            onError(e.message ?: "未知错误")
        }
    }
}

/**
 * 本地视频流预览
 */
@Composable
actual fun LocalVideoPreview(
    modifier: Modifier
) {
    val context = LocalContext.current
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                surfaceView = this
            }
        },
        modifier = modifier,
        update = { view ->
            // 更新视图时的处理逻辑
            surfaceView = view
        }
    )
    
    // 在这里应该将本地视频流渲染到SurfaceView上
    // 由于这是一个简化实现，实际项目中需要将WebRTC的本地视频轨道与SurfaceView关联
}

/**
 * 远程视频流显示
 */
@Composable
actual fun RemoteVideoView(
    modifier: Modifier
) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

/**
 * WebRTC管理器实现
 */
class AndroidWebRTCManager(private val context: Context) : WebRTCManager {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localMediaStream: com.github.im.group.sdk.MediaStream? = null
    private var webSocket: WebSocket? = null
    private var userId: String = ""
    private var remoteUserId: String = ""
    
    // STOMP协议相关
    private var stompConnected = false
    private val stompSubscriptions = mutableMapOf<String, String>() // subscriptionId -> destination
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    override fun initialize() {
        // 初始化PeerConnectionFactory
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()
    }
    
    override fun createLocalMediaStream(): com.github.im.group.sdk.MediaStream? {
        // 创建本地媒体流
        localMediaStream = createLocalMediaStreamInternal()
        return localMediaStream
    }
    
    override fun connectToSignalingServer(serverUrl: String, userId: String) {
        this.userId = userId
        // 使用ProxyConfig中的host配置
        val host = ProxyConfig.host
        val port = 8080 // WebRTC服务端口固定为8080
        val request = Request.Builder()
            .url("ws://$host:$port/webrtc") // 使用ProxyConfig中的host
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebRTC", "WebSocket连接已建立")
                // 连接成功后发送STOMP连接帧
                connectToStomp()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebRTC", "收到消息: $text")
                // 处理STOMP消息
                handleStompMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebRTC", "WebSocket连接失败", t)
                stompConnected = false
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebRTC", "WebSocket连接已关闭: $reason")
                stompConnected = false
            }
        })
    }
    
    private fun connectToStomp() {
        // 发送STOMP连接帧
        val connectFrame = "CONNECT\n" +
                "accept-version:1.1,1.2\n" +
                "heart-beat:10000,10000\n" +
                "\n" +
                "\u0000"
        webSocket?.send(connectFrame)
    }
    
    private fun handleStompMessage(message: String) {
        if (message.startsWith("CONNECTED")) {
            Log.d("WebRTC", "STOMP连接成功")
            stompConnected = true
            // 订阅消息
            subscribeToMessages()
        } else if (message.startsWith("MESSAGE")) {
            // 解析并处理消息
            handleMessage(message)
        }
    }
    
    private fun subscribeToMessages() {
        // 订阅各种消息类型
        subscribe("/user/$userId/queue/webrtc/call", "call-subscription")
        subscribe("/user/$userId/queue/webrtc/answer", "answer-subscription")
        subscribe("/user/$userId/queue/webrtc/ice", "ice-subscription")
        subscribe("/user/$userId/queue/webrtc/hangup", "hangup-subscription")
        subscribe("/user/$userId/queue/webrtc/reject", "reject-subscription")
    }
    
    private fun subscribe(destination: String, subscriptionId: String) {
        val subscribeFrame = "SUBSCRIBE\n" +
                "id:$subscriptionId\n" +
                "destination:$destination\n" +
                "\n" +
                "\u0000"
        webSocket?.send(subscribeFrame)
        stompSubscriptions[subscriptionId] = destination
    }
    
    override fun initiateCall(remoteUserId: String) {
        this.remoteUserId = remoteUserId
        // 发送呼叫消息
        val message = WebrtcMessage(
            type = "OFFER",
            from = userId,
            to = remoteUserId,
            payload = null, // 实际应用中这里应该是SDP Offer
            timestamp = System.currentTimeMillis()
        )
        sendStompMessage("/app/webrtc/call", message)
    }
    
    override fun acceptCall(callId: String) {
        // 发送应答消息
        val message = WebrtcMessage(
            type = "ANSWER",
            from = userId,
            to = remoteUserId,
            payload = null, // 实际应用中这里应该是SDP Answer
            timestamp = System.currentTimeMillis()
        )
        sendStompMessage("/app/webrtc/answer", message)
    }
    
    override fun rejectCall(callId: String) {
        // 发送拒绝消息
        val message = WebrtcMessage(
            type = "REJECT",
            from = userId,
            to = remoteUserId,
            payload = null,
            timestamp = System.currentTimeMillis()
        )
        sendStompMessage("/app/webrtc/reject", message)
    }
    
    override fun endCall() {
        // 发送挂断消息
        val message = WebrtcMessage(
            type = "HANGUP",
            from = userId,
            to = remoteUserId,
            payload = null,
            timestamp = System.currentTimeMillis()
        )
        sendStompMessage("/app/webrtc/hangup", message)
        
        // 清理资源
        cleanup()
    }
    
    override fun switchCamera() {
        // 实际实现中需要切换摄像头
        Log.d("WebRTC", "切换摄像头")
    }
    
    override fun toggleCamera(enabled: Boolean) {
        // 控制摄像头开关
        (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.webRTCStream?.videoTracks?.forEach { track ->
            track.setEnabled(enabled)
        }
        Log.d("WebRTC", "摄像头状态: $enabled")
    }
    
    override fun toggleMicrophone(enabled: Boolean) {
        // 控制麦克风开关
        (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.webRTCStream?.audioTracks?.forEach { track ->
            track.setEnabled(enabled)
        }
        Log.d("WebRTC", "麦克风状态: $enabled")
    }
    
    override fun sendIceCandidate(candidate: com.github.im.group.sdk.IceCandidate) {
        // 发送ICE候选
        val message = WebrtcMessage(
            type = "ICE_CANDIDATE",
            from = userId,
            to = remoteUserId,
            payload = "${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}",
            timestamp = System.currentTimeMillis()
        )
        sendStompMessage("/app/webrtc/ice", message)
    }
    
    override fun release() {
        cleanup()
        client.dispatcher.executorService.shutdown()
    }
    
    private fun createLocalMediaStreamInternal(): com.github.im.group.sdk.MediaStream? {
        val peerConnectionFactory = peerConnectionFactory ?: return null
        val mediaStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        
        // 创建视频轨道
        val videoSource = peerConnectionFactory.createVideoSource(false)
        val eglBase = EglBase.create()
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread", 
            eglBase.eglBaseContext
        )
        val videoCapturer = createVideoCapturer()
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)
        
        val videoTrack = peerConnectionFactory.createVideoTrack("local_video_track", videoSource)
        videoTrack.setEnabled(true)
        mediaStream.addTrack(videoTrack)
        
        // 创建音频轨道
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory.createAudioTrack("local_audio_track", audioSource)
        audioTrack.setEnabled(true)
        mediaStream.addTrack(audioTrack)
        
        return WebRTCAdapter.MediaStreamAdapter(mediaStream, eglBase)
    }
    
    private fun createVideoCapturer(): VideoCapturer? {
        // 使用构造函数中传入的context
        val cameraEnumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
        
        val deviceNames = cameraEnumerator.deviceNames
        for (deviceName in deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                return cameraEnumerator.createCapturer(deviceName, null)
            }
        }
        
        // 如果没有前置摄像头，使用后置摄像头
        for (deviceName in deviceNames) {
            if (cameraEnumerator.isBackFacing(deviceName)) {
                return cameraEnumerator.createCapturer(deviceName, null)
            }
        }
        
        return null
    }
    
    private fun sendStompMessage(destination: String, message: WebrtcMessage) {
        val json = kotlinx.serialization.json.Json.encodeToString(WebrtcMessage.serializer(), message)
        
        val sendFrame = "SEND\n" +
                "destination:$destination\n" +
                "content-length:${json.length}\n" +
                "\n" +
                "$json\n" +
                "\u0000"
        webSocket?.send(sendFrame)
    }
    
    private fun handleMessage(stompMessage: String) {
        try {
            // 简单解析STOMP消息，提取JSON内容
            val lines = stompMessage.lines()
            var jsonStart = -1
            for (i in lines.indices) {
                if (lines[i].isBlank()) {
                    jsonStart = i + 1
                    break
                }
            }
            
            if (jsonStart != -1 && jsonStart < lines.size) {
                val jsonString = lines.subList(jsonStart, lines.size - 1).joinToString("\n")
                val message = kotlinx.serialization.json.Json.decodeFromString(WebrtcMessage.serializer(), jsonString)
                when (message.type) {
                    "OFFER" -> handleOffer(message)
                    "ANSWER" -> handleAnswer(message)
                    "ICE_CANDIDATE" -> handleIceCandidate(message)
                    "HANGUP" -> handleHangup(message)
                    "REJECT" -> handleReject(message)
                }
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "解析消息失败", e)
        }
    }
    
    private fun handleOffer(message: WebrtcMessage) {
        Log.d("WebRTC", "处理呼叫请求")
        // 实际实现中需要处理SDP Offer
    }
    
    private fun handleAnswer(message: WebrtcMessage) {
        Log.d("WebRTC", "处理应答")
        // 实际实现中需要处理SDP Answer
    }
    
    private fun handleIceCandidate(message: WebrtcMessage) {
        Log.d("WebRTC", "处理ICE候选")
        // 实际实现中需要处理ICE候选
        message.payload?.let { payload ->
            val parts = payload.split("|")
            if (parts.size == 3) {
                // 实际实现中需要添加ICE候选到PeerConnection
            }
        }
    }
    
    private fun handleHangup(message: WebrtcMessage) {
        Log.d("WebRTC", "处理挂断")
        // 实际实现中需要处理挂断
        cleanup()
    }
    
    private fun handleReject(message: WebrtcMessage) {
        Log.d("WebRTC", "处理拒绝")
        // 实际实现中需要处理拒绝
        cleanup()
    }
    
    private fun cleanup() {
        // 清理资源
        (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.webRTCStream?.videoTracks?.forEach { track ->
            track.dispose()
        }
        (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.webRTCStream?.audioTracks?.forEach { track ->
            track.dispose()
        }
        (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.eglBase?.release()
        localMediaStream = null
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        stompConnected = false
        stompSubscriptions.clear()
    }
}

/**
 * WebRTC适配器类，用于将WebRTC原生类型转换为我们自定义的接口
 */
object WebRTCAdapter {
    class MediaStreamAdapter(
        val webRTCStream: org.webrtc.MediaStream,
        val eglBase: EglBase
    ) : com.github.im.group.sdk.MediaStream {
        override val id: String = "media_stream_${System.currentTimeMillis()}"
        
        override val audioTracks: List<com.github.im.group.sdk.AudioTrack>
            get() = webRTCStream.audioTracks.map { AudioTrackAdapter(it) }
        
        override val videoTracks: List<com.github.im.group.sdk.VideoTrack>
            get() = webRTCStream.videoTracks.map { VideoTrackAdapter(it) }
    }
    
    class AudioTrackAdapter(private val webRTCAudioTrack: org.webrtc.AudioTrack) : com.github.im.group.sdk.AudioTrack {
        override val id: String = "audio_track_${System.currentTimeMillis()}"
        
        override val enabled: Boolean
            get() = webRTCAudioTrack.enabled()
        
        override fun setEnabled(enabled: Boolean) {
            webRTCAudioTrack.setEnabled(enabled)
        }
    }
    
    class VideoTrackAdapter(private val webRTCVideoTrack: org.webrtc.VideoTrack) : com.github.im.group.sdk.VideoTrack {
        override val id: String = "video_track_${System.currentTimeMillis()}"
        
        override val enabled: Boolean
            get() = webRTCVideoTrack.enabled()
        
        override fun setEnabled(enabled: Boolean) {
            webRTCVideoTrack.setEnabled(enabled)
        }
    }
}