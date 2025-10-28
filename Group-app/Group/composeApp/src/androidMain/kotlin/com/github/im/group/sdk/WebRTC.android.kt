package com.github.im.group.sdk

import ProxyConfig
import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.sdk.addSinkCatching
import com.shepeliev.webrtckmp.AudioTrack
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrackKind
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.VideoTrack
import com.shepeliev.webrtckmp.WebRtc
import com.shepeliev.webrtckmp.audioTracks
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onTrack
import com.shepeliev.webrtckmp.videoTracks
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSink
import java.util.concurrent.TimeUnit

// Android平台的WebRTC Track和Stream实现
class AndroidVideoTrack(val webrtcVideoTrack: VideoTrack) : com.github.im.group.sdk.VideoTrack() {
    override val id: String
        get() = webrtcVideoTrack.id

    override val isEnabled: Boolean
        get() = webrtcVideoTrack.enabled

    override fun setEnabled(enabled: Boolean) {
        webrtcVideoTrack.enabled = enabled
    }

    suspend fun switchCamera() {
        webrtcVideoTrack.switchCamera()
    }
}

class AndroidAudioTrack(val webrtcAudioTrack: AudioTrack) : com.github.im.group.sdk.AudioTrack() {
    override val id: String
        get() = webrtcAudioTrack.id

    override val isEnabled: Boolean
        get() = webrtcAudioTrack.enabled

    override fun setEnabled(enabled: Boolean) {
        webrtcAudioTrack.enabled = enabled
    }
}

class AndroidMediaStream(private val webrtcMediaStream: MediaStream) : com.github.im.group.sdk.MediaStream() {
    override val id: String
        get() = webrtcMediaStream.id

    override val videoTracks: List<com.github.im.group.sdk.VideoTrack>
        get() = webrtcMediaStream.videoTracks.map { AndroidVideoTrack(it) }

    override val audioTracks: List<com.github.im.group.sdk.AudioTrack>
        get() = webrtcMediaStream.audioTracks.map { AndroidAudioTrack(it) }
}

/**
 * WebRTC管理器实现
 */
class AndroidWebRTCManager(private val context: Context) : WebRTCManager {
    private var localMediaStream: AndroidMediaStream? = null

    private var remoteMediaStream: AndroidMediaStream? = null
    private var webSocket: WebSocket? = null
    private var userId: String = ""
    private var remoteUserId: String = ""
    private var peerConnection: PeerConnection? = null
    private val iceServers = listOf(
        com.shepeliev.webrtckmp.IceServer(listOf("stun:stun.l.google.com:19302"))
    )
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    override suspend fun initialize() {
        // WebRTC-KMP 初始化在库内部完成
        Napier.d("WebRTC Manager initialized")
    }
    
    override suspend fun createLocalMediaStream(): AndroidMediaStream? {
        val stream = MediaDevices.getUserMedia(audio = true, video = true)
        localMediaStream = AndroidMediaStream(stream)
        return localMediaStream
    }
    
    override fun connectToSignalingServer(serverUrl: String, userId: String) {

        this.userId = userId
        val host = ProxyConfig.host
        val port = ProxyConfig.port
        val protocol = if (host.startsWith("https://")) "wss" else "ws"
        val cleanHost = host.replace(Regex("^https?://"), "")
        val request = Request.Builder()
            .url("$protocol://$cleanHost:$port/ws?userId=$userId")
            .build()
        Napier.d("WebRTC 创建WebSocket连接: $request")
            // TODO 鉴权
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebRTC", "WebSocket连接已建立")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebRTC", "收到消息: $text")
                handleWebSocketMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebRTC", "WebSocket连接失败", t)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebRTC", "WebSocket连接已关闭: $reason")
            }
        })
    }
    
    private fun handleWebSocketMessage(message: String) {
        try {
            val webrtcMessage = kotlinx.serialization.json.Json.decodeFromString(WebrtcMessage.serializer(), message)
            when (webrtcMessage.type) {
                "call/request" -> handleCallRequest(webrtcMessage)
                "call/accept" -> handleCallAccept(webrtcMessage)
                "offer" -> handleOffer(webrtcMessage)
                "answer" -> handleAnswer(webrtcMessage)
                "candidate" -> handleIceCandidate(webrtcMessage)
                "call/end" -> handleHangup(webrtcMessage)
                "call/failed" -> handleCallFailed(webrtcMessage)
                else -> Log.d("WebRTC", "未处理的WebSocket消息类型: ${webrtcMessage.type}")
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "解析消息失败", e)
        }
    }
    
    private fun handleCallRequest(message: WebrtcMessage) {
        Log.d("WebRTC", "收到呼叫请求，来自: ${message.fromUser}")
        remoteUserId = message.fromUser ?: ""
        createPeerConnection()
        val response = WebrtcMessage(
            type = "call/accept",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(response)
    }
    
    private fun handleCallAccept(message: WebrtcMessage) {
        Log.d("WebRTC", "呼叫被接受，目标: ${message.fromUser}")
        remoteUserId = message.fromUser ?: ""
        createAndSendOffer()
    }
    
    private fun createAndSendOffer() {
        val pc = peerConnection ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val offer = pc.createOffer(OfferAnswerOptions())
                pc.setLocalDescription(offer)
                
                val message = WebrtcMessage(
                    type = "offer",
                    fromUser = userId,
                    toUser = remoteUserId,
                    sdp = offer.sdp,
                    sdpType = "offer"
                )
                sendWebSocketMessage(message)
            } catch (e: Exception) {
                Log.e("WebRTC", "创建或发送Offer失败", e)
            }
        }
    }
    
    private fun handleOffer(message: WebrtcMessage) {
        Log.d("WebRTC", "处理Offer")
        createPeerConnection()
        
        message.sdp?.let { sdp ->
            val offer = SessionDescription(
                SessionDescriptionType.Offer,
                sdp
            )
            
            val pc = peerConnection ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    pc.setRemoteDescription(offer)
                    val answer = pc.createAnswer(OfferAnswerOptions(
                        offerToReceiveAudio = true,
                        offerToReceiveVideo = true,
                        voiceActivityDetection =  true

                    ))
                    pc.setLocalDescription(answer)
                    
                    val answerMessage = WebrtcMessage(
                        type = "answer",
                        fromUser = userId,
                        toUser = message.fromUser,
                        sdp = answer.sdp,
                        sdpType = "answer"
                    )
                    sendWebSocketMessage(answerMessage)
                } catch (e: Exception) {
                    Log.e("WebRTC", "处理Offer失败", e)
                }
            }
        }
    }
    
    private fun handleAnswer(message: WebrtcMessage) {
        Log.d("WebRTC", "处理Answer")
        message.sdp?.let { sdp ->
            val answer = SessionDescription(
                SessionDescriptionType.Answer,
                sdp
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    peerConnection?.setRemoteDescription(answer)
                } catch (e: Exception) {
                    Log.e("WebRTC", "设置远程Answer失败", e)
                }
            }
        }
    }
    
    private fun handleIceCandidate(message: WebrtcMessage) {
        Log.d("WebRTC", "处理ICE候选")
        message.candidate?.let { candidate ->
            val iceCandidate = IceCandidate(
                candidate.sdpMid ?: "",
                candidate.sdpMLineIndex,
                candidate.candidate
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    peerConnection?.addIceCandidate(iceCandidate)
                } catch (e: Exception) {
                    Log.e("WebRTC", "添加ICE候选失败", e)
                }
            }
        }
    }
    
    private fun handleHangup(message: WebrtcMessage) {
        Log.d("WebRTC", "处理挂断")
        cleanup()
    }
    
    private fun handleCallFailed(message: WebrtcMessage) {
        Log.d("WebRTC", "呼叫失败: ${message.reason}")
        cleanup()
    }
    
    private fun createPeerConnection() {


        peerConnection?.close()
//        val config = RtcConfiguration(iceServers)
        peerConnection = PeerConnection()
        
        // 添加本地媒体轨道到PeerConnection
        localMediaStream?.let { stream ->
            stream.videoTracks.filterIsInstance<AndroidVideoTrack>().forEach { track ->
                peerConnection?.addTrack(track.webrtcVideoTrack)
            }
            stream.audioTracks.filterIsInstance<AndroidAudioTrack>().forEach { track ->
                peerConnection?.addTrack(track.webrtcAudioTrack)
            }
        }

        CoroutineScope(Dispatchers.IO).launch{

            // 设置事件监听器
            peerConnection?.onIceCandidate?.onEach { candidate ->
                handleIceCandidateEvent(candidate)
            }?.launchIn(this)

            peerConnection?.onTrack ?.onEach{ event ->
                event.track?.let { track ->
                    when (track.kind) {
                        MediaStreamTrackKind.Audio -> {
                            val remoteAudioTrack = AndroidAudioTrack(track as AudioTrack)
                            // 创建远程媒体流（如果不存在）
                            if (remoteMediaStream == null) {
                                // 创建一个新的远程媒体流
//                                val remoteStream = com.shepeliev.webrtckmp.MediaStream("remote_stream")
//                                remoteMediaStream = AndroidMediaStream(remoteStream)
                            }
                            // 将音频轨道添加到远程媒体流中
                            // 注意：这里可能需要根据实际API调整
                        }
                        MediaStreamTrackKind.Video -> {
//                            val remoteVideoTrack = AndroidVideoTrack(track)
                            // 创建远程媒体流（如果不存在）
                            if (remoteMediaStream == null) {
                                // 创建一个新的远程媒体流
//                                val remoteStream = com.shepeliev.webrtckmp.MediaStream("remote_stream")
//                                remoteMediaStream = AndroidMediaStream(remoteStream)
                            }
                            // 将视频轨道添加到远程媒体流中
                            // 注意：这里可能需要根据实际API调整
                        }
                    }
                    Log.d("WebRTC", "收到远程轨道: ${track.kind}")
                }
                handleTrackEvent(event)
            }?.launchIn(this)
        }

    }
    
    private fun handleIceCandidateEvent(candidate: IceCandidate) {
        // 发送ICE候选

        val candi = com.github.im.group.sdk.IceCandidate(
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex,
            candidate = candidate.candidate
        )
        sendIceCandidate(candi)
    }
    
    private fun handleTrackEvent(event: com.shepeliev.webrtckmp.TrackEvent) {
        // 处理远程轨道事件
        Log.d("WebRTC", "收到远程轨道: ${event.track?.kind}")
        if (event.track != null){
            when (event.track?.kind) {
                MediaStreamTrackKind.Audio -> {
                    // 处理音频轨道
                }
                MediaStreamTrackKind.Video -> {
                    // 处理视频轨道
                }
                null -> TODO()
            }
        }
    }
    
    override fun initiateCall(remoteUserId: String) {
        this.remoteUserId = remoteUserId
        if (peerConnection == null) {
            createPeerConnection()
        }
        
        val message = WebrtcMessage(
            type = "call/request",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(message)
    }
    
    override fun acceptCall(callId: String) {
        if (peerConnection == null) {
            createPeerConnection()
        }
    }
    
    override fun rejectCall(callId: String) {
        val message = WebrtcMessage(
            type = "call/end",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(message)
    }
    
    override fun endCall() {
        val message = WebrtcMessage(
            type = "call/end",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(message)
        cleanup()
    }
    
    override suspend fun switchCamera() {
        localMediaStream?.videoTracks?.filterIsInstance<AndroidVideoTrack>()?.firstOrNull()?.switchCamera()
    }
    
    override fun toggleCamera(enabled: Boolean) {
        localMediaStream?.videoTracks?.forEach { track ->
            track.setEnabled(enabled)
        }
        Log.d("WebRTC", "摄像头状态: $enabled")
    }
    
    override fun toggleMicrophone(enabled: Boolean) {
        localMediaStream?.audioTracks?.forEach { track ->
            track.setEnabled(enabled)
        }
        Log.d("WebRTC", "麦克风状态: $enabled")
    }
    
    override fun sendIceCandidate(candidate: com.github.im.group.sdk.IceCandidate) {
        val candidateData = IceCandidateData(
            candidate = candidate.candidate,
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex
        )
        
        val message = WebrtcMessage(
            type = "candidate",
            fromUser = userId,
            toUser = remoteUserId,
            candidate = candidateData
        )
        sendWebSocketMessage(message)
    }
    
    override fun release() {
        try {
            cleanup()
        } catch (e: Exception) {
            Log.w("WebRTC", "清理资源时出错", e)
        }
        try {
            client.dispatcher.executorService.shutdown()
        } catch (e: Exception) {
            Log.w("WebRTC", "关闭HTTP客户端时出错", e)
        }
    }
    
    private fun sendWebSocketMessage(message: WebrtcMessage) {
        val json = kotlinx.serialization.json.Json.encodeToString(WebrtcMessage.serializer(), message)
        webSocket?.send(json)
    }
    
    private fun cleanup() {
        peerConnection?.close()
        peerConnection = null
        
        try {
            localMediaStream = null
            remoteMediaStream = null
        } catch (e: Exception) {
            Log.w("WebRTC", "释放本地媒体流时出错", e)
        }
        
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            Log.w("WebRTC", "关闭WebSocket时出错", e)
        }
        webSocket = null
    }
    
    // 获取远程媒体流的方法
    fun getRemoteMediaStream(): AndroidMediaStream? {
        return remoteMediaStream
    }
}

@Composable
actual fun VideoScreenView(
    modifier: Modifier,
    videoTrack: com.github.im.group.sdk.VideoTrack?,
    audioTrack: com.github.im.group.sdk.AudioTrack?
) {
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    val lifecycleEventObserver =
        remember(renderer, videoTrack) {
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        renderer?.also {
                            it.init(WebRtc.rootEglBase.eglBaseContext, null)
                            if (videoTrack is AndroidVideoTrack ){
                                videoTrack.webrtcVideoTrack.addSinkCatching(it)
                            }
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        renderer?.also {
                            if (videoTrack is AndroidVideoTrack ){
                                videoTrack.webrtcVideoTrack.removeSinkCatching(it)
                            }
                        }
                        renderer?.release()
                    }

                    else -> {
                        // ignore other events
                    }
                }
            }
        }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, lifecycleEventObserver) {
        lifecycle.addObserver(lifecycleEventObserver)

        onDispose {
            renderer?.let {
                if (videoTrack is AndroidVideoTrack ){
                    videoTrack.webrtcVideoTrack.removeSinkCatching(it)
                }
            }
            renderer?.release()
            lifecycle.removeObserver(lifecycleEventObserver)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                setScalingType(
                    RendererCommon.ScalingType.SCALE_ASPECT_BALANCED,
                    RendererCommon.ScalingType.SCALE_ASPECT_FIT,
                )
                renderer = this
            }
        },
    )
}
private fun VideoTrack.addSinkCatching(sink: VideoSink) {
    // runCatching as track may be disposed while activity was in pause
    runCatching { addSink(sink) }
}

private fun VideoTrack.removeSinkCatching(sink: VideoSink) {
    // runCatching as track may be disposed while activity was in pause
    runCatching { removeSink(sink) }
}
