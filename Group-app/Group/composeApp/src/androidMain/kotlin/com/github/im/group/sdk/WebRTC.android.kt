package com.github.im.group.sdk

import ProxyConfig
import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.webrtc.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import io.github.aakira.napier.Napier
import org.webrtc.audio.AudioDeviceModule


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
    modifier: Modifier,
    localMediaStream: MediaStream?
) {
    val context = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? = null
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                previewView = this
            }
        },
        modifier = modifier,
        update = { view ->
            // 更新视图时的处理逻辑
            previewView = view
        }
    )
    
    // 启动摄像头预览
    LaunchedEffect(localMediaStream, lifecycleOwner) {
        if (previewView != null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider!!, previewView!!, lifecycleOwner)
                } catch (e: Exception) {
                    Log.e("WebRTC", "启动摄像头预览失败", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    
    // 清理CameraX资源
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.e("WebRTC", "解除CameraX绑定失败", e)
            }
        }
    }
}

private fun bindPreview(cameraProvider: ProcessCameraProvider, previewView: PreviewView, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    val preview = Preview.Builder().build()
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        .build()
    
    preview.setSurfaceProvider(previewView.surfaceProvider)
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview
    )
}

/**
 * 远程视频流显示
 */
@Composable
actual fun RemoteVideoView(
    modifier: Modifier,
    remoteVideoTrack: com.github.im.group.sdk.VideoTrack? 
) {
    var surfaceView: SurfaceView? = null
    
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
            // 将远程视频轨道与SurfaceView关联
            if (remoteVideoTrack is WebRTCAdapter.VideoTrackAdapter && surfaceView != null) {
                remoteVideoTrack.webRTCVideoTrack.addSink(surfaceView as VideoSink)
            }
        }
    )
    
    // 在组件销毁时移除sink
    DisposableEffect(remoteVideoTrack) {
        onDispose {
            if (remoteVideoTrack is WebRTCAdapter.VideoTrackAdapter && surfaceView != null) {
                try {
                    remoteVideoTrack.webRTCVideoTrack.removeSink(surfaceView as VideoSink)
                } catch (e: Exception) {
                    Log.w("WebRTC", "移除视频轨道sink时出错", e)
                }
            }
        }
    }
}

/**
 * WebRTC管理器实现
 */
class AndroidWebRTCManager(private val context: Context) : WebRTCManager {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localMediaStream: MediaStream? = null
    private var webSocket: WebSocket? = null
    private var userId: String = ""
    private var remoteUserId: String = ""
    private var videoCapturer: VideoCapturer? = null // 添加视频捕获器引用
    private var surfaceTextureHelper: SurfaceTextureHelper? = null // 添加SurfaceTextureHelper引用
    
    // WebRTC相关
    private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private val eglBaseContext = EglBase.create()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    // WebRTC配置
    private val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        // 添加更多RTC配置选项以提高连接成功率
        enableRtpDataChannel = false
        enableDtlsSrtp = true
        tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
    }
    
    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
            Log.d("WebRTC", "Signaling state changed: $signalingState")
        }

        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
            Log.d("WebRTC", "ICE connection state changed: $iceConnectionState")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Log.d("WebRTC", "ICE connection receiving change: $receiving")
        }

        override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {
            Log.d("WebRTC", "ICE gathering state changed: $iceGatheringState")
        }

        override fun onIceCandidate(iceCandidate: org.webrtc.IceCandidate) {
            Log.d("WebRTC", "New ICE candidate")
            // 发送ICE候选到远程对等方
            val message = WebrtcMessage(
                type = "candidate",
                fromUser = userId,
                toUser = remoteUserId,
                candidate = com.github.im.group.sdk.IceCandidateData(
                    candidate = iceCandidate.sdp,
                    sdpMid = iceCandidate.sdpMid,
                    sdpMLineIndex = iceCandidate.sdpMLineIndex,
                    sdp = iceCandidate.sdp
                )
            )
            sendWebSocketMessage(message)
        }

        override fun onIceCandidatesRemoved(candidates: Array<out org.webrtc.IceCandidate>) {
            Log.d("WebRTC", "ICE candidates removed")
        }

        override fun onAddStream(mediaStream: org.webrtc.MediaStream) {
            Log.d("WebRTC", "Remote stream added")
        }

        override fun onRemoveStream(mediaStream: org.webrtc.MediaStream) {
            Log.d("WebRTC", "Remote stream removed")
        }

        override fun onDataChannel(dataChannel: DataChannel?) {
            Log.d("WebRTC", "New data channel")
        }

        override fun onRenegotiationNeeded() {
            Log.d("WebRTC", "Renegotiation needed")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) {
            Log.d("WebRTC", "New track added")
            // 处理远程视频轨道
            val track = rtpReceiver.track()
            if (track is VideoTrack) {
                Log.d("WebRTC", "Remote video track added: ${track.id()}")
                remoteVideoTrack = track
                // 在实际应用中，这里应该将远程视频轨道与UI组件关联
                // 例如，将track与RemoteVideoView组件关联
            }
        }
    }
    
    override fun initialize() {
        // 初始化PeerConnectionFactory
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        
        val options = PeerConnectionFactory.Options()
        // 创建支持多种编解码器的工厂，使用更现代的配置
        val videoEncoderFactory = DefaultVideoEncoderFactory(
            eglBaseContext.eglBaseContext,
            true,  // enableIntelVp8Encoder
            true   // enableH264HighProfile
        )


        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
//            .setAudioDeviceModule(AudioDeviceModule.buildAudioDeviceModule(context))
            .createPeerConnectionFactory()
    }
    
    override fun createLocalMediaStream(): com.github.im.group.sdk.MediaStream? {
        // 创建本地媒体流
        localMediaStream = createLocalMediaStreamInternal()
        return localMediaStream
    }
    
    override fun connectToSignalingServer(serverUrl: String, userId: String) {
        this.userId = userId
        // 使用ProxyConfig中的host配置连接到应用服务器
        val host = ProxyConfig.host
        val port = ProxyConfig.port // 应用服务器端口
        // 检查是否使用安全连接
        val protocol = if (host.startsWith("https://")) "wss" else "ws"
        val cleanHost = host.replace(Regex("^https?://"), "")
        val request = Request.Builder()
            .url("$protocol://$cleanHost:$port/ws?userId=$userId") // 连接到WebSocket端点
            .build()
        Napier.d("WebRTC 创建WebSocket连接: $request")
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebRTC", "WebSocket连接已建立")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebRTC", "收到消息: $text")
                // 处理WebSocket消息
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

    /**
     * 处理通话请求
     */
    private fun handleCallRequest(message: WebrtcMessage) {
        Log.d("WebRTC", "收到呼叫请求，来自: ${message.fromUser}")
        // 在实际应用中，这里应该显示来电界面并等待用户确认
        // 简化处理，直接接受呼叫
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
        // 不再重复创建PeerConnection，应该复用已有的
        createAndSendOffer()
    }
    
    private fun createAndSendOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            // 添加VP8编解码器偏好设置
            optional.add(MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d("WebRTC", "Offer创建成功")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        // 不需要实现
                    }

                    override fun onSetSuccess() {
                        Log.d("WebRTC", "本地描述设置成功")
                        // 发送Offer给远程对等方
                        val message = WebrtcMessage(
                            type = "offer",
                            fromUser = userId,
                            toUser = remoteUserId,
                            sdp = sdp?.description,
                            sdpType = sdp?.type?.canonicalForm()
                        )
                        sendWebSocketMessage(message)
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e("WebRTC", "创建本地描述失败: $error")
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "设置本地描述失败: $error")
                    }
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTC", "创建offer失败: $error")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    private fun handleOffer(message: WebrtcMessage) {
        Log.d("WebRTC", "处理Offer")
        // 只有在没有PeerConnection时才创建
        if (peerConnection == null) {
            createPeerConnection()
        }
        
        message.sdp?.let { sdp ->
            val sessionDescription = SessionDescription(
                message.sdpType?.let { SessionDescription.Type.fromCanonicalForm(it) } ?: SessionDescription.Type.OFFER, 
                sdp
            )
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    // 不需要实现
                }

                override fun onSetSuccess() {
                    Log.d("WebRTC", "远程offer设置成功")
                    // 创建Answer
                    val constraints = MediaConstraints().apply {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                        // 添加VP8编解码器偏好设置
                        optional.add(MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"))
                    }
                    
                    peerConnection?.createAnswer(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {
                            Log.d("WebRTC", "Answer创建成功")
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(sdp: SessionDescription?) {
                                    // 不需要实现
                                }

                                override fun onSetSuccess() {
                                    Log.d("WebRTC", "本地answer设置成功")
                                    // 发送Answer给远程对等方
                                    val answerMessage = WebrtcMessage(
                                        type = "answer",
                                        fromUser = userId,
                                        toUser = message.fromUser,
                                        sdp = sdp?.description,
                                        sdpType = sdp?.type?.canonicalForm()
                                    )
                                    sendWebSocketMessage(answerMessage)
                                }

                                override fun onCreateFailure(error: String?) {
                                    Log.e("WebRTC", "创建本地描述失败: $error")
                                }

                                override fun onSetFailure(error: String?) {
                                    Log.e("WebRTC", "设置本地描述失败: $error")
                                }
                            }, sdp)
                        }

                        override fun onCreateFailure(error: String?) {
                            Log.e("WebRTC", "创建answer失败: $error")
                        }

                        override fun onSetSuccess() {}
                        override fun onSetFailure(error: String?) {}
                    }, constraints)
                }

                override fun onCreateFailure(error: String?) {
                    Log.e("WebRTC", "创建远程描述失败: $error")
                }

                override fun onSetFailure(error: String?) {
                    Log.e("WebRTC", "设置远程offer失败: $error")
                }
            }, sessionDescription)
        }
    }
    
    private fun handleAnswer(message: WebrtcMessage) {
        Log.d("WebRTC", "处理Answer")
        message.sdp?.let { sdp ->
            val sessionDescription = SessionDescription(
                message.sdpType?.let { SessionDescription.Type.fromCanonicalForm(it) } ?: SessionDescription.Type.ANSWER,
                sdp
            )
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d("WebRTC", "远程answer设置成功")
                }

                override fun onSetFailure(error: String?) {
                    Log.e("WebRTC", "设置远程answer失败: $error")
                }
                
                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, sessionDescription)
        }
    }
    
    private fun handleIceCandidate(message: WebrtcMessage) {
        Log.d("WebRTC", "处理ICE候选")
        message.candidate?.let { candidate ->
            val iceCandidate = org.webrtc.IceCandidate(
                candidate.sdpMid,
                candidate.sdpMLineIndex,
                candidate.candidate
            )

            peerConnection?.addIceCandidate(iceCandidate)
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
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)

        // 添加本地媒体轨道到PeerConnection
        localMediaStream?.let { stream ->
            if (stream is WebRTCAdapter.MediaStreamAdapter) {
                val mediaStream = stream.webRTCStream
                val streamId = mediaStream.id  // 例如 "ARDAMS"

                // add audio tracks
                mediaStream.audioTracks.forEach { track ->
                    peerConnection?.addTrack(track, listOf(streamId))
                }

                // add video tracks
                mediaStream.videoTracks.forEach { track ->
                    peerConnection?.addTrack(track, listOf(streamId))
                }
            }
        }
    }
    
    override fun initiateCall(remoteUserId: String) {
        this.remoteUserId = remoteUserId
        // 只有在没有PeerConnection时才创建
        if (peerConnection == null) {
            createPeerConnection()
        }
        
        // 发送呼叫请求
        val message = WebrtcMessage(
            type = "call/request",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(message)
    }
    
    override fun acceptCall(callId: String) {
        // 只有在没有PeerConnection时才创建
        if (peerConnection == null) {
            createPeerConnection()
        }
    }
    
    override fun rejectCall(callId: String) {
        // 发送拒绝消息
        val message = WebrtcMessage(
            type = "call/end",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(message)
    }
    
    override fun endCall() {
        // 发送挂断消息
        val message = WebrtcMessage(
            type = "call/end",
            fromUser = userId,
            toUser = remoteUserId
        )
        sendWebSocketMessage(message)
        
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
    
    override fun sendIceCandidate(candidate: IceCandidateData) {
        // 发送ICE候选
        val message = WebrtcMessage(
            type = "candidate",
            fromUser = userId,
            toUser = remoteUserId,
            candidate = candidate
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

    /**
     * 构建 本地相机视频流
     */
    private fun createLocalMediaStreamInternal(): MediaStream? {
        val peerConnectionFactory = peerConnectionFactory ?: return null
        val mediaStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        
        // 创建视频轨道 - 使用CameraX
        val videoSource = peerConnectionFactory.createVideoSource(false)
        val eglBase = EglBase.create()
        
        surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread", 
            eglBase.eglBaseContext
        )
        videoCapturer = createVideoCapturer()
        try {
            videoCapturer?.initialize(surfaceTextureHelper!!, context, videoSource.capturerObserver)
            // 使用适中的分辨率和帧率以平衡质量和性能
            videoCapturer?.startCapture(1280, 720, 24)
        } catch (e: Exception) {
            Log.e("WebRTC", "初始化或启动摄像头捕获失败", e)
            // 出错时释放已创建的资源
            try {
                videoCapturer?.dispose()
            } catch (disposeException: Exception) {
                Log.w("WebRTC", "释放视频捕获器时出错", disposeException)
            }
            videoCapturer = null
            return null
        }
        
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

    /**
     * 创建视频捕获器
     */
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
        
        // 如果没有前置摄像头，使用后置摄像头  优先 前置相机
        for (deviceName in deviceNames) {
            if (cameraEnumerator.isBackFacing(deviceName)) {
                return cameraEnumerator.createCapturer(deviceName, null)
            }
        }
        
        return null
    }
    
    private fun sendWebSocketMessage(message: WebrtcMessage) {
        val json = kotlinx.serialization.json.Json.encodeToString(WebrtcMessage.serializer(), message)
        webSocket?.send(json)
    }
    
    private fun cleanup() {
        // 关闭PeerConnection
        peerConnection?.close()
        peerConnection = null
        
        // 清空远程视频轨道引用
        remoteVideoTrack = null
        
        // 停止并释放视频捕获器
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w("WebRTC", "停止视频捕获器时出错", e)
        }
        
        videoCapturer?.dispose()
        videoCapturer = null
        
        // 释放SurfaceTextureHelper
        try {
            surfaceTextureHelper?.dispose()
        } catch (e: Exception) {
            Log.w("WebRTC", "释放SurfaceTextureHelper时出错", e)
        }
        surfaceTextureHelper = null
        
        // 清理媒体流资源
        try {
            (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.webRTCStream?.videoTracks?.forEach { track ->
                track.dispose()
            }
        } catch (e: Exception) {
            Log.w("WebRTC", "释放视频轨道时出错", e)
        }
        
        try {
            (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.webRTCStream?.audioTracks?.forEach { track ->
                track.dispose()
            }
        } catch (e: Exception) {
            Log.w("WebRTC", "释放音频轨道时出错", e)
        }
        
        try {
            (localMediaStream as? WebRTCAdapter.MediaStreamAdapter)?.eglBase?.release()
        } catch (e: Exception) {
            Log.w("WebRTC", "释放EGL环境时出错", e)
        }
        localMediaStream = null
        
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            Log.w("WebRTC", "关闭WebSocket时出错", e)
        }
        webSocket = null
    }
    
    // 提供获取远程视频轨道的方法
    fun getRemoteVideoTrack(): com.github.im.group.sdk.VideoTrack? {
        return remoteVideoTrack?.let { WebRTCAdapter.VideoTrackAdapter(it as org.webrtc.VideoTrack) }
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
    
    class VideoTrackAdapter(val webRTCVideoTrack: org.webrtc.VideoTrack) : com.github.im.group.sdk.VideoTrack {
        override val id: String = "video_track_${System.currentTimeMillis()}"
        
        override val enabled: Boolean
            get() = webRTCVideoTrack.enabled()
        
        override fun setEnabled(enabled: Boolean) {
            webRTCVideoTrack.setEnabled(enabled)
        }
    }
}