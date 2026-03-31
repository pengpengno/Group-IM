import { EventEmitter } from 'events';
import { Store } from '@reduxjs/toolkit';
import { 
  VideoCallStatus, 
  incomingCall, 
  callConnected, 
  callEnded, 
  callError,
  setLocalStreamId,
  setRemoteStreamId 
} from '../features/video-call/videoCallSlice';
import { RootState } from '../store';
import { webrtcAPI } from './api/apiClient';

// WebRTC Message Protocol
export interface WebrtcMessage {
  type: string;              // Message type: call/request, call/accept, call/end, offer, answer, candidate
  fromUser?: string;         // Sender ID
  fromUserName?: string;     // Sender Name (Metadata)
  fromAvatar?: string;       // Sender Avatar (Metadata)
  toUser?: string;           // Receiver ID
  sdp?: string;              // SDP description
  sdpType?: string;          // SDP type: offer/answer
  candidate?: IceCandidateData; // ICE candidate
  reason?: string;           // Failure reason
}

export interface IceCandidateData {
  candidate: string;
  sdpMid: string;
  sdpMLineIndex: number;
}

// Internal Call State for the Service
export interface CallInternalState {
  callStatus: VideoCallStatus;
  remoteUserId?: string;
  remoteUserName?: string;
  remoteAvatar?: string;
  callStartTime?: number;
  duration: number;
  isLocalVideoEnabled: boolean;
  isRemoteVideoEnabled: boolean;
  isMicrophoneEnabled: boolean;
  isSpeakerEnabled: boolean;
  errorMessage?: string;
}

const DEFAULT_ICE_SERVERS: RTCIceServer[] = [
  { urls: 'stun:stun.l.google.com:19302' }
];

export class WebRTCService extends EventEmitter {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private websocket: WebSocket | null = null;
  private store: Store<RootState> | null = null;
  private userId: string = '';
  private remoteUserId: string = '';
  private iceServers: RTCIceServer[];

  private state: CallInternalState = {
    callStatus: VideoCallStatus.IDLE,
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: true,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true
  };

  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_ATTEMPTS = 5;
  private durationInterval: any = null;
  private heartbeatInterval: any = null;
  private reconnectTimer: any = null;
  private isConnecting: boolean = false;

  constructor(iceServers: RTCIceServer[] = DEFAULT_ICE_SERVERS) {
    super();
    this.iceServers = iceServers;
  }

  public getState(): CallInternalState {
    return { ...this.state };
  }

  public getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  public getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }

  private async requestUserMedia(constraints: MediaStreamConstraints): Promise<MediaStream> {
    if (typeof navigator === 'undefined') {
      throw new Error('Media devices are unavailable in the current runtime.');
    }

    if (navigator.mediaDevices?.getUserMedia) {
      return navigator.mediaDevices.getUserMedia(constraints);
    }

    const legacyNavigator = navigator as Navigator & {
      getUserMedia?: (
        constraints: MediaStreamConstraints,
        successCallback: (stream: MediaStream) => void,
        errorCallback: (error: Error) => void
      ) => void;
      webkitGetUserMedia?: (
        constraints: MediaStreamConstraints,
        successCallback: (stream: MediaStream) => void,
        errorCallback: (error: Error) => void
      ) => void;
      mozGetUserMedia?: (
        constraints: MediaStreamConstraints,
        successCallback: (stream: MediaStream) => void,
        errorCallback: (error: Error) => void
      ) => void;
      msGetUserMedia?: (
        constraints: MediaStreamConstraints,
        successCallback: (stream: MediaStream) => void,
        errorCallback: (error: Error) => void
      ) => void;
    };

    const legacyGetUserMedia =
      legacyNavigator.getUserMedia ||
      legacyNavigator.webkitGetUserMedia ||
      legacyNavigator.mozGetUserMedia ||
      legacyNavigator.msGetUserMedia;

    if (legacyGetUserMedia) {
      return new Promise((resolve, reject) => {
        legacyGetUserMedia.call(navigator, constraints, resolve, reject);
      });
    }

    const secureContextHint =
      typeof window !== 'undefined' && !window.isSecureContext
        ? ' Remote web calls require HTTPS/WSS (or localhost) to access camera and microphone.'
        : '';

    throw new Error(`getUserMedia is not available in this environment.${secureContextHint}`);
  }

  /**
   * Initialize with Redux store and create PC
   */
  public async initialize(store?: Store<RootState>, userId?: string): Promise<void> {
    if (store) this.store = store;
    if (userId) this.userId = userId;
    
    if (this.peerConnection) return;

    try {
      // 动态获取最新的 ICE Servers（STUN/TURN）
      console.log('Fetching ICE servers from backend...');
      try {
        const response = await webrtcAPI.getIceServers();
        if (response.data && Array.isArray(response.data)) {
          // 适配后端模型 IceServerConfig
          this.iceServers = response.data.map((s: any) => ({
            urls: s.url,
            username: s.username,
            credential: s.credential
          }));
          console.log('Fetched ICE servers successfully:', this.iceServers);
        }
      } catch (err) {
        console.warn('Failed to fetch ICE servers, using defaults:', err);
      }

      this.peerConnection = new RTCPeerConnection({
        iceServers: this.iceServers
      });
      
      this.setupPeerConnectionEvents();
      console.log('WebRTCService initialized with PC');
    } catch (error) {
      console.error('Failed to initialize WebRTCService:', error);
      throw error;
    }
  }

  private setupPeerConnectionEvents(): void {
    if (!this.peerConnection) return;

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendSignalingMessage({
          type: 'candidate',
          fromUser: this.userId,
          toUser: this.remoteUserId,
          candidate: {
            candidate: event.candidate.candidate,
            sdpMid: event.candidate.sdpMid || '',
            sdpMLineIndex: event.candidate.sdpMLineIndex || 0
          }
        });
      }
    };

    this.peerConnection.ontrack = (event) => {
      console.log('Received remote track:', event.track.kind);
      if (event.streams && event.streams[0]) {
        this.remoteStream = event.streams[0];
        this.emit('remote-stream', this.remoteStream);
        this.updateState({ isRemoteVideoEnabled: true });
        
        if (this.store) {
          this.store.dispatch(setRemoteStreamId(this.remoteStream.id));
        }
      }
    };

    this.peerConnection.onconnectionstatechange = () => {
      const connState = this.peerConnection?.connectionState;
      console.log('WebRTC Connection State:', connState);
      
      if (connState === 'connected') {
        this.updateState({ callStatus: VideoCallStatus.ACTIVE, callStartTime: Date.now() });
        this.startDurationTimer();
        if (this.store) {
          this.store.dispatch(callConnected());
        }
      } else if (connState === 'failed' || connState === 'disconnected') {
        this.handleError(new Error(`WebRTC Connection ${connState}`));
      }
    };
  }

  public async acquireLocalMedia(): Promise<MediaStream> {
    try {
      if (this.localStream) {
        this.localStream.getTracks().forEach(t => t.stop());
      }

      this.localStream = await this.requestUserMedia({
        video: { width: 1280, height: 720, frameRate: 30 },
        audio: true
      });

      if (this.peerConnection) {
        // Clear existing senders if any
        const senders = this.peerConnection.getSenders();
        senders.forEach(sender => this.peerConnection?.removeTrack(sender));

        this.localStream.getTracks().forEach(track => {
          this.peerConnection!.addTrack(track, this.localStream!);
        });
      }

      if (this.store) {
        this.store.dispatch(setLocalStreamId(this.localStream.id));
      }

      return this.localStream;
    } catch (error) {
      console.error('Failed to acquire local media:', error);
      throw error;
    }
  }

  /**
   * Connect to the WebSocket signaling server
   */
  public connectSignaling(host: string, port: number, userId: string, token: string, pageProtocol: string = 'http:'): void {
    // 如果已经连接或是正在连接到同一个服务器且同一个用户，就不重复发起
    const protocol = pageProtocol === 'https:' || pageProtocol === 'wss:' ? 'wss' : 'ws';
    const cleanHost = host.replace(/^https?:\/\//, '');
    const url = `${protocol}://${cleanHost}:${port}/ws?userId=${userId}&token=${token}`;

    if (this.websocket && (this.websocket.readyState === WebSocket.OPEN || this.websocket.readyState === WebSocket.CONNECTING)) {
        if (this.websocket.url === url) {
            console.log('Already connected or connecting to signaling server:', url);
            return;
        }
    }

    if (this.isConnecting) {
        console.log('Signaling connection already in progress...');
        return;
    }

    this.userId = userId;
    console.log('Connecting to signaling server:', url);
    
    // 清理之前的定时器
    if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
    }
    
    // 清理旧的 WebSocket 及其监听器，防止调用此方法手动关闭旧连接时触发 onclose 的重连逻辑
    if (this.websocket) {
        this.websocket.onopen = null;
        this.websocket.onmessage = null;
        this.websocket.onclose = null;
        this.websocket.onerror = null;
        this.websocket.close();
        this.websocket = null;
    }

    this.isConnecting = true;
    this.websocket = new WebSocket(url);

    this.websocket.onopen = () => {
      console.log('Signaling WebSocket connected');
      this.reconnectAttempts = 0;
      this.isConnecting = false;
      this.startHeartbeat();
    };

    this.websocket.onmessage = (event) => {
      try {
        const message: WebrtcMessage = JSON.parse(event.data);
        this.handleSignalingMessage(message);
      } catch (e) {
        console.error('Failed to parse signaling message:', e);
      }
    };

    this.websocket.onclose = (event) => {
      console.log('Signaling WebSocket closed:', event.code, 'Reason:', event.reason);
      this.isConnecting = false;
      this.stopHeartbeat();
      
      // 如果不是因为 ended 而关闭，且重连次数没超过限制，就尝试重连
      if (!this.state.callStatus.includes('ENDED') && 
          this.state.callStatus !== VideoCallStatus.IDLE && // 如果手动设置为 IDLE 也不应重连
          this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
        
        console.log(`Scheduling reconnect... attempts: ${this.reconnectAttempts + 1}/${this.MAX_RECONNECT_ATTEMPTS}`);
        this.reconnectAttempts++;
        this.reconnectTimer = setTimeout(() => {
            this.reconnectTimer = null;
            this.connectSignaling(host, port, userId, token, pageProtocol);
        }, 5000); // 稍微增加重连间隔
      }
    };

    this.websocket.onerror = (error) => {
      console.error('Signaling WebSocket error:', error);
      this.isConnecting = false;
    };
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatInterval = setInterval(() => {
        if (this.websocket?.readyState === WebSocket.OPEN) {
            // 发送心跳包
            this.websocket.send(JSON.stringify({ type: 'ping', fromUser: this.userId }));
        }
    }, 30000); // 每30秒发送一次心跳
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
        clearInterval(this.heartbeatInterval);
        this.heartbeatInterval = null;
    }
  }

  private handleSignalingMessage(message: WebrtcMessage): void {
    console.log('Signaling Received:', message.type, 'from:', message.fromUser);

    switch (message.type) {
      case 'call/request':
        this.remoteUserId = message.fromUser || '';
        this.updateState({
          callStatus: VideoCallStatus.INCOMING,
          remoteUserId: this.remoteUserId,
          remoteUserName: message.fromUserName,
          remoteAvatar: message.fromAvatar
        });
        
        if (this.store) {
          this.store.dispatch(incomingCall({
            callId: `call-${Date.now()}`,
            remoteUser: {
              userId: this.remoteUserId,
              username: message.fromUserName || `User ${this.remoteUserId}`,
              avatar: message.fromAvatar,
              email: '',
              status: 'online'
            }
          }));
        }

        this.emit('incoming-call', { callerId: this.remoteUserId });
        break;

      case 'call/accept':
        this.remoteUserId = message.fromUser || '';
        this.createOffer();
        break;

      case 'offer':
        this.handleOffer(message);
        break;

      case 'answer':
        this.handleAnswer(message);
        break;

      case 'candidate':
        this.handleIceCandidate(message);
        break;

      case 'call/end':
        this.handleRemoteHangup();
        break;

      case 'call/failed':
        this.handleError(new Error(message.reason || 'Remote call failed'));
        break;
    }
  }

  private async createOffer(): Promise<void> {
    try {
      if (!this.peerConnection) await this.initialize();
      if (!this.localStream) await this.acquireLocalMedia();

      const offer = await this.peerConnection!.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true
      });
      await this.peerConnection!.setLocalDescription(offer);

      this.sendSignalingMessage({
        type: 'offer',
        fromUser: this.userId,
        toUser: this.remoteUserId,
        sdp: offer.sdp,
        sdpType: 'offer'
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (e) {
      this.handleError(e as Error);
    }
  }

  private async handleOffer(message: WebrtcMessage): Promise<void> {
    try {
      if (!this.peerConnection) await this.initialize();
      if (!this.localStream) await this.acquireLocalMedia();

      await this.peerConnection!.setRemoteDescription(new RTCSessionDescription({
        type: 'offer',
        sdp: message.sdp
      }));

      const answer = await this.peerConnection!.createAnswer();
      await this.peerConnection!.setLocalDescription(answer);

      this.sendSignalingMessage({
        type: 'answer',
        fromUser: this.userId,
        toUser: this.remoteUserId,
        sdp: answer.sdp,
        sdpType: 'answer'
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (e) {
      this.handleError(e as Error);
    }
  }

  private async handleAnswer(message: WebrtcMessage): Promise<void> {
    try {
      if (this.peerConnection && message.sdp) {
        await this.peerConnection.setRemoteDescription(new RTCSessionDescription({
          type: 'answer',
          sdp: message.sdp
        }));
      }
    } catch (e) {
      this.handleError(e as Error);
    }
  }

  private handleIceCandidate(message: WebrtcMessage): void {
    if (this.peerConnection && message.candidate) {
      this.peerConnection.addIceCandidate(new RTCIceCandidate({
        candidate: message.candidate.candidate,
        sdpMid: message.candidate.sdpMid,
        sdpMLineIndex: message.candidate.sdpMLineIndex
      })).catch(e => console.error('Error adding ICE candidate:', e));
    }
  }

  public initiateCall(remoteUserId: string, remoteUserName?: string): void {
    this.remoteUserId = remoteUserId;
    
    // Attempt to get our own metadata from the store if it exists
    const currentUser = this.store?.getState().auth.user;

    this.updateState({
      callStatus: VideoCallStatus.OUTGOING,
      remoteUserId,
      remoteUserName
    });

    this.sendSignalingMessage({
      type: 'call/request',
      fromUser: this.userId,
      fromUserName: currentUser?.username,
      fromAvatar: currentUser?.avatar,
      toUser: remoteUserId
    });
  }

  public acceptCall(): void {
    if (!this.remoteUserId) return;

    this.sendSignalingMessage({
      type: 'call/accept',
      fromUser: this.userId,
      toUser: this.remoteUserId
    });

    this.updateState({ callStatus: VideoCallStatus.CONNECTING });
  }

  public endCall(): void {
    if (this.remoteUserId) {
      this.sendSignalingMessage({
        type: 'call/end',
        fromUser: this.userId,
        toUser: this.remoteUserId
      });
    }
    this.cleanup();
  }

  private handleRemoteHangup(): void {
    this.cleanup();
    this.emit('call-ended', { remoteId: this.remoteUserId });
  }

  private sendSignalingMessage(message: WebrtcMessage): void {
    if (this.websocket?.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(message));
    } else {
      console.warn('Cannot send signaling message: WebSocket not open');
    }
  }

  private updateState(updates: Partial<CallInternalState>): void {
    this.state = { ...this.state, ...updates };
    this.emit('state-change', this.state);
  }

  private handleError(error: Error): void {
    console.error('WebRTCService Error:', error);
    this.updateState({
      callStatus: VideoCallStatus.ERROR,
      errorMessage: error.message
    });

    if (this.store) {
      this.store.dispatch(callError(error.message));
    }

    this.emit('error', error);
  }

  private startDurationTimer(): void {
    this.stopDurationTimer();
    this.durationInterval = setInterval(() => {
      if (this.state.callStartTime) {
        const duration = Math.floor((Date.now() - this.state.callStartTime) / 1000);
        this.updateState({ duration });
      }
    }, 1000);
  }

  private stopDurationTimer(): void {
    if (this.durationInterval) {
      clearInterval(this.durationInterval);
      this.durationInterval = null;
    }
  }

  private cleanup(): void {
    this.stopDurationTimer();
    this.stopHeartbeat();
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }
    if (this.localStream) {
      this.localStream.getTracks().forEach(t => t.stop());
      this.localStream = null;
    }
    this.remoteStream = null;
    this.updateState({
      callStatus: VideoCallStatus.IDLE,
      remoteUserId: undefined,
      duration: 0,
      callStartTime: undefined
    });

    if (this.store) {
      this.store.dispatch(callEnded());
    }
  }

  public destroy(): void {
    this.cleanup();
    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
    }
    this.removeAllListeners();
  }

  public toggleCamera(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach(t => t.enabled = enabled);
      this.updateState({ isLocalVideoEnabled: enabled });
    }
  }

  public toggleMicrophone(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach(t => t.enabled = enabled);
      this.updateState({ isMicrophoneEnabled: enabled });
    }
  }
}

export const webRTCService = new WebRTCService();
