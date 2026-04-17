package com.github.im.group.sdk

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.config.ProxyConfig
import com.github.im.group.ui.video.VideoCallState
import com.github.im.group.ui.video.VideoCallStatus
import com.shepeliev.webrtckmp.AudioTrack
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrackKind
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
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

    fun release(){
        webrtcMediaStream.release()
    }
}

/**
 * Android WebRTC管理器实现 (Mesh多人架构)
 */
class AndroidWebRTCManager(private val context: Context) : WebRTCManager {
    private var localMediaStream: AndroidMediaStream? = null

    private val _connectionState = MutableStateFlow(VideoCallState())
    override val videoCallState: StateFlow<VideoCallState> = _connectionState

    private val _remoteVideoTracks = MutableStateFlow<Map<String, com.github.im.group.sdk.VideoTrack>>(emptyMap())
    override val remoteVideoTracks: StateFlow<Map<String, com.github.im.group.sdk.VideoTrack>> = _remoteVideoTracks

    private val _remoteAudioTracks = MutableStateFlow<Map<String, com.github.im.group.sdk.AudioTrack>>(emptyMap())
    override val remoteAudioTracks: StateFlow<Map<String, com.github.im.group.sdk.AudioTrack>> = _remoteAudioTracks

    // 向后兼容 1v1 的轨道
    private val _remoteVideoTrack = MutableStateFlow<AndroidVideoTrack?>(null)
    override val remoteVideoTrack: StateFlow<AndroidVideoTrack?> = _remoteVideoTrack

    private val _remoteAudioTrack = MutableStateFlow<AndroidAudioTrack?>(null)
    override val remoteAudioTrack: StateFlow<AndroidAudioTrack?> = _remoteAudioTrack

    private var webSocket: WebSocket? = null
    private var userId: String = ""
    private var roomId: String? = null
    
    // 多人连接管理: Map<RemoteUserId, PeerConnection>
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val pendingIceCandidates = mutableMapOf<String, MutableList<IceCandidate>>()
    
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMillis = 5000L
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder().authenticator(
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
    
    override suspend fun initialize() {
        Napier.d("AndroidWebRTCManager initialized")
    }
    
    override suspend fun createLocalMediaStream(): AndroidMediaStream? {
        if (localMediaStream != null) return localMediaStream
        val stream = MediaDevices.getUserMedia(audio = true, video = true)
        localMediaStream = AndroidMediaStream(stream)
        return localMediaStream
    }
    
    override fun connectToSignalingServer(serverUrl: String, userId: String) {
        this.userId = userId
        establishWebSocketConnection()
    }
    
    private fun establishWebSocketConnection() {
        val request = Request.Builder()
            .url("${ProxyConfig.getWsBaseUrl()}?userId=$userId&token=${GlobalCredentialProvider.currentToken}")
            .header("Authorization", GlobalCredentialProvider.currentToken)
            .build()
        Napier.d("WebRTC Establishing WebSocket: $request")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebRTC", "WebSocket Open")
                reconnectAttempts = 0
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebRTC", "WebSocket Failure", t)
                attemptReconnect()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (code != 1000) attemptReconnect()
            }
        })
    }
    
    private fun attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(reconnectDelayMillis)
                establishWebSocketConnection()
            }
        }
    }

    private fun handleWebSocketMessage(text: String) {
        try {
            val msg = json.decodeFromString<WebrtcMessage>(text)
            when (msg.type) {
                "meeting/request", "call/request" -> handleCallRequest(msg)
                "meeting/join" -> handleMeetingJoin(msg)
                "meeting/participants" -> handleMeetingParticipants(msg)
                "meeting/participant-joined" -> handleParticipantJoined(msg)
                "meeting/participant-left" -> handleParticipantLeft(msg)
                "meeting/reject", "call/failed" -> handleCallFailed(msg)
                "offer" -> handleOffer(msg)
                "answer" -> handleAnswer(msg)
                "candidate" -> handleIceCandidate(msg)
                "meeting/leave", "call/end" -> handleHangup(msg)
                "meeting/end" -> cleanup()
                else -> Log.d("WebRTC", "Unknown type: ${msg.type}")
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "Signal parse error: $text", e)
        }
    }

    private fun handleCallRequest(msg: WebrtcMessage) {
        this.roomId = msg.roomId
        val callerInfo = com.github.im.group.model.UserInfo(
            userId = msg.fromUser?.toLongOrNull() ?: 0L,
            username = msg.fromUserName ?: "User-${msg.fromUser}",
            email = "",
        )
        
        val participantInfos = msg.participants?.map { 
            com.github.im.group.model.UserInfo(
                userId = it.userId.toLongOrNull() ?: 0L,
                username = it.userName ?: "User-${it.userId}",
                email = ""
            )
        } ?: listOf(callerInfo)

        _connectionState.value = _connectionState.value.copy(
            callStatus = VideoCallStatus.INCOMING,
            caller = callerInfo,
            participants = participantInfos,
            callId = msg.roomId
        )
    }

    private fun handleMeetingParticipants(msg: WebrtcMessage) {
        Log.d("WebRTC", "Received participants: ${msg.participants?.size}")
        val participantInfos = msg.participants?.map { 
            com.github.im.group.model.UserInfo(
                userId = it.userId.toLongOrNull() ?: 0L,
                username = it.userName ?: "User-${it.userId}",
                email = ""
            )
        } ?: emptyList()
        
        _connectionState.value = _connectionState.value.copy(
            participants = participantInfos
        )
        
        // As a newcomer, I wait for offers from existing members to avoid glare.
        // Or I can send offers to them if we use a tie-breaker. 
        // For simplicity, we match Web client behavior: existing members send offers to newcomer.
    }

    private fun handleMeetingJoin(msg: WebrtcMessage) {
        Log.d("WebRTC", "Meeting joined: ${msg.roomId}")
    }

    private fun handleParticipantJoined(msg: WebrtcMessage) {
        val remoteId = msg.fromUser ?: return
        if (remoteId == userId) return
        Log.d("WebRTC", "Participant joined: $remoteId, sending offer (Mesh)")
        CoroutineScope(Dispatchers.IO).launch {
            createAndSendOffer(remoteId)
        }
    }

    private fun handleParticipantLeft(msg: WebrtcMessage) {
        val remoteId = msg.fromUser ?: return
        removePeerConnection(remoteId)
    }

    private suspend fun createAndSendOffer(remoteUserId: String) {
        try {
            val pc = ensurePeerConnection(remoteUserId)
            val offer = pc.createOffer(OfferAnswerOptions(offerToReceiveVideo = true, offerToReceiveAudio = true))
            pc.setLocalDescription(offer)
            sendWebSocketMessage(WebrtcMessage(
                type = "offer", fromUser = userId, toUser = remoteUserId,
                roomId = roomId, sdp = offer.sdp, sdpType = "offer"
            ))
        } catch (e: Exception) {
            Log.e("WebRTC", "Create Offer Failed: $remoteUserId", e)
        }
    }
    
    private fun handleOffer(msg: WebrtcMessage) {
        val remoteId = msg.fromUser ?: return
        val sdp = msg.sdp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pc = ensurePeerConnection(remoteId)
                pc.setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, sdp))
                pendingIceCandidates[remoteId]?.forEach { pc.addIceCandidate(it) }
                pendingIceCandidates.remove(remoteId)
                
                val answer = pc.createAnswer(OfferAnswerOptions(offerToReceiveAudio = true, offerToReceiveVideo = true))
                pc.setLocalDescription(answer)
                sendWebSocketMessage(WebrtcMessage(
                    type = "answer", fromUser = userId, toUser = remoteId,
                    roomId = roomId, sdp = answer.sdp, sdpType = "answer"
                ))
            } catch (e: Exception) {
                Log.e("WebRTC", "Handle Offer Failed: $remoteId", e)
            }
        }
    }
    
    private fun handleAnswer(msg: WebrtcMessage) {
        val remoteId = msg.fromUser ?: return
        val sdp = msg.sdp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pc = peerConnections[remoteId] ?: return@launch
                pc.setRemoteDescription(SessionDescription(SessionDescriptionType.Answer, sdp))
                pendingIceCandidates[remoteId]?.forEach { pc.addIceCandidate(it) }
                pendingIceCandidates.remove(remoteId)
            } catch (e: Exception) {
                Log.e("WebRTC", "Handle Answer Failed: $remoteId", e)
            }
        }
    }
    
    private fun handleIceCandidate(msg: WebrtcMessage) {
        val remoteId = msg.fromUser ?: return
        val data = msg.candidate ?: return
        val candidate = IceCandidate(data.sdpMid ?: "", data.sdpMLineIndex, data.candidate)
        val pc = peerConnections[remoteId]
        if (pc?.remoteDescription != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    pc.addIceCandidate(candidate)
                } catch (e: Exception) {
                    Log.e("WebRTC", "Add ICE Failed: $remoteId", e)
                }
            }
        } else {
            pendingIceCandidates.getOrPut(remoteId) { mutableListOf() }.add(candidate)
        }
    }
    
    private fun handleHangup(msg: WebrtcMessage) {
        val status = _connectionState.value.callStatus
        if (status == VideoCallStatus.INCOMING || status == VideoCallStatus.OUTGOING) {
            Log.d("WebRTC", "Hangup during ring/call phase, cleaning up")
            cleanup()
            return
        }
        msg.fromUser?.let { removePeerConnection(it) } ?: cleanup()
    }
    
    private fun handleCallFailed(msg: WebrtcMessage) {
        if (peerConnections.isEmpty()) cleanup()
    }
    
    private suspend fun ensurePeerConnection(remoteUserId: String): PeerConnection {
        peerConnections[remoteUserId]?.let { return it }
        val pc = PeerConnection()
        peerConnections[remoteUserId] = pc
        
        localMediaStream?.let { stream -> 
            stream.videoTracks.forEach { if (it is AndroidVideoTrack) pc.addTrack(it.webrtcVideoTrack) }
            stream.audioTracks.forEach { if (it is AndroidAudioTrack) pc.addTrack(it.webrtcAudioTrack) }
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            pc.onIceCandidate.onEach { 
                sendIceCandidate(com.github.im.group.sdk.IceCandidate(it.sdpMid, it.sdpMLineIndex, it.candidate), remoteUserId)
            }.launchIn(this)
            
            pc.onTrack.onEach { event -> 
                event.track?.let { handleRemoteTrack(remoteUserId, it) }
            }.launchIn(this)
        }
        return pc
    }
    
    private fun handleRemoteTrack(remoteUserId: String, track: com.shepeliev.webrtckmp.MediaStreamTrack) {
        Log.d("WebRTC", "Remote track from $remoteUserId: ${track.kind}")
        when (track.kind) {
            MediaStreamTrackKind.Audio -> {
                val audioTrack = AndroidAudioTrack(track as AudioTrack)
                _remoteAudioTracks.value = _remoteAudioTracks.value + (remoteUserId to audioTrack)
                _remoteAudioTrack.value = audioTrack
            }
            MediaStreamTrackKind.Video -> {
                val videoTrack = AndroidVideoTrack(track as VideoTrack)
                _remoteVideoTracks.value = _remoteVideoTracks.value + (remoteUserId to videoTrack)
                _remoteVideoTrack.value = videoTrack
            }
        }
    }

    private fun removePeerConnection(remoteUserId: String) {
        peerConnections[remoteUserId]?.close()
        peerConnections.remove(remoteUserId)
        pendingIceCandidates.remove(remoteUserId)
        _remoteVideoTracks.value = _remoteVideoTracks.value - remoteUserId
        _remoteAudioTracks.value = _remoteAudioTracks.value - remoteUserId
        if (peerConnections.isEmpty()) {
            // Optional: reset state if no one left
        }
    }
    
    override fun initiateCall(remoteUserId: String) {
        val rid = "call_${Clock.System.now().toEpochMilliseconds()}"
        initiateMeeting(rid, listOf(remoteUserId))
    }

    override fun initiateMeeting(roomId: String, participantIds: List<String>) {
        this.roomId = roomId
        _connectionState.value = _connectionState.value.copy(callStatus = VideoCallStatus.OUTGOING, callId = roomId)
        
        val participants = participantIds.map { ParticipantInfo(userId = it) }
        
        participantIds.forEach { targetId ->
            sendWebSocketMessage(WebrtcMessage(
                type = "call/request", fromUser = userId, toUser = targetId,
                roomId = roomId, participants = participants
            ))
        }
        joinMeeting(roomId)
    }
    
    override fun joinMeeting(roomId: String) {
        this.roomId = roomId
        sendWebSocketMessage(WebrtcMessage(type = "meeting/join", fromUser = userId, roomId = roomId))
        _connectionState.value = _connectionState.value.copy(callStatus = VideoCallStatus.CONNECTING, callId = roomId)
    }

    override fun leaveMeeting() {
        sendWebSocketMessage(WebrtcMessage(type = "meeting/leave", fromUser = userId, roomId = roomId))
        cleanup()
    }

    override fun acceptCall(callId: String) { joinMeeting(callId) }
    override fun rejectCall(callId: String) {
        sendWebSocketMessage(WebrtcMessage(type = "call/end", fromUser = userId, roomId = callId))
        cleanup()
    }
    override fun endCall() { leaveMeeting() }
    override suspend fun switchCamera() { 
        localMediaStream?.videoTracks?.filterIsInstance<AndroidVideoTrack>()?.firstOrNull()?.switchCamera() 
    }
    override fun toggleCamera(enabled: Boolean) { 
        localMediaStream?.videoTracks?.forEach { it.setEnabled(enabled) } 
    }
    override fun toggleMicrophone(enabled: Boolean) { 
        localMediaStream?.audioTracks?.forEach { it.setEnabled(enabled) } 
    }
    override fun sendIceCandidate(candidate: com.github.im.group.sdk.IceCandidate, toUser: String?) {
        sendWebSocketMessage(WebrtcMessage(
            type = "candidate", fromUser = userId, toUser = toUser, roomId = roomId,
            candidate = IceCandidateData(candidate.candidate, candidate.sdpMid, candidate.sdpMLineIndex)
        ))
    }
    
    override fun release() { cleanup() }
    
    private fun sendWebSocketMessage(msg: WebrtcMessage) {
        val text = json.encodeToString(msg)
        webSocket?.send(text)
    }
    
    private fun cleanup() {
        peerConnections.forEach { (_, pc) -> pc.close() }
        peerConnections.clear()
        pendingIceCandidates.clear()
        localMediaStream?.release()
        localMediaStream = null
        _remoteAudioTracks.value = emptyMap()
        _remoteVideoTracks.value = emptyMap()
        _remoteAudioTrack.value = null
        _remoteVideoTrack.value = null
        _connectionState.value = VideoCallState()
    }
}

@Composable
actual fun VideoScreenView(
    modifier: Modifier,
    videoTrack: com.github.im.group.sdk.VideoTrack?,
    audioTrack: com.github.im.group.sdk.AudioTrack?
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> 
            SurfaceViewRenderer(context).apply {
                init(WebRtc.rootEglBase.eglBaseContext, null)
                setScalingType(
                    RendererCommon.ScalingType.SCALE_ASPECT_BALANCED,
                    RendererCommon.ScalingType.SCALE_ASPECT_FIT,
                )
                setEnableHardwareScaler(true)
            }
        },
        update = { view ->
            if (videoTrack is AndroidVideoTrack) {
                // To avoid multiple sinks, remove then add
                try {
                    videoTrack.webrtcVideoTrack.removeSink(view)
                } catch (e: Exception) {
                    // Ignore
                }
                try {
                    videoTrack.webrtcVideoTrack.addSink(view)
                } catch (e: Exception) {
                    Napier.e("Failed to add sink", e)
                }
            }
        },
        onRelease = { view ->
            if (videoTrack is AndroidVideoTrack) {
                try {
                    videoTrack.webrtcVideoTrack.removeSink(view)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            view.release()
        }
    )
}