import { EventEmitter } from 'events';
import { VideoCallStatus } from './videoCallSlice';

// WebRTC 消息协议

// WebRTC 消息协议
export interface WebrtcMessage {
  type: string;              // 消息类型: call/request, call/accept, call/end, offer, answer, candidate
  fromUser?: string;         // 发送方用户ID
  toUser?: string;           // 接收方用户ID
  sdp?: string;              // SDP描述信息
  sdpType?: string;          // SDP类型: offer/answer
  candidate?: IceCandidateData; // ICE候选信息
  reason?: string;           // 失败原因
}

export interface IceCandidateData {
  candidate: string;
  sdpMid: string;
  sdpMLineIndex: number;
}

// 视频通话状态
export interface VideoCallState {
  callStatus: VideoCallStatus;
  remoteUserId?: string;
  callStartTime?: number;
  duration: number;
  isLocalVideoEnabled: boolean;
  isRemoteVideoEnabled: boolean;
  isMicrophoneEnabled: boolean;
  isSpeakerEnabled: boolean;
  errorMessage?: string;
}

const DEFAULT_ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' }
];

export class VideoCallManager extends EventEmitter {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private websocket: WebSocket | null = null;
  private userId: string = '';
  private remoteUserId: string = '';
  private iceServers: RTCIceServer[];

  private callState: VideoCallState = {
    callStatus: VideoCallStatus.IDLE,
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: true,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true
  };

  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_ATTEMPTS = 5;

  constructor(iceServers: RTCIceServer[] = DEFAULT_ICE_SERVERS) {
    super();
    this.iceServers = iceServers;
  }

  public getCallState(): VideoCallState {
    return { ...this.callState };
  }

  public getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  public getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }

  public async initialize(): Promise<void> {
    if (this.peerConnection) return;

    this.peerConnection = new RTCPeerConnection({
      iceServers: this.iceServers
    });

    this.setupPeerConnectionEvents();
  }

  private setupPeerConnectionEvents(): void {
    if (!this.peerConnection) return;

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendMessage({
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
      this.remoteStream = event.streams[0];
      this.emit('remote-stream', this.remoteStream);
      this.updateCallState({ isRemoteVideoEnabled: true });
    };

    this.peerConnection.onconnectionstatechange = () => {
      console.log('WebRTC connection state:', this.peerConnection?.connectionState);
      if (this.peerConnection?.connectionState === 'connected') {
        this.updateCallState({ callStatus: VideoCallStatus.ACTIVE });
      } else if (this.peerConnection?.connectionState === 'failed' || this.peerConnection?.connectionState === 'disconnected') {
        this.handleError(new Error('WebRTC connection failed'));
      }
    };
  }

  public async createLocalStream(): Promise<MediaStream> {
    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
      });

      if (this.peerConnection) {
        this.localStream.getTracks().forEach(track => {
          this.peerConnection!.addTrack(track, this.localStream!);
        });
      }

      return this.localStream;
    } catch (error) {
      console.error('Failed to get user media:', error);
      throw error;
    }
  }

  public connectSignaling(host: string, port: number, userId: string, token: string): void {
    this.userId = userId;
    const protocol = host.startsWith('https') ? 'wss' : 'ws';
    const cleanHost = host.replace(/^https?:\/\//, '');
    const url = `${protocol}://${cleanHost}:${port}/ws?userId=${userId}`;

    console.log('Connecting to signaling server:', url);
    this.websocket = new WebSocket(url);

    // Auth header is usually handled via URL param or cookies in standard Browser WebSocket,
    // but Android implementation used a header. Browser WebSocket doesn't support custom headers.
    // However, if the server expects it in URL or session, we follow.

    this.websocket.onopen = () => {
      console.log('Signaling connection opened');
      this.reconnectAttempts = 0;
    };

    this.websocket.onmessage = (event) => {
      try {
        const message: WebrtcMessage = JSON.parse(event.data);
        this.handleMessage(message);
      } catch (e) {
        console.error('Failed to parse signaling message:', e);
      }
    };

    this.websocket.onclose = () => {
      console.log('Signaling connection closed');
      if (this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
        this.reconnectAttempts++;
        setTimeout(() => this.connectSignaling(host, port, userId, token), 5000);
      }
    };

    this.websocket.onerror = (error) => {
      console.error('Signaling error:', error);
    };
  }

  private handleMessage(message: WebrtcMessage): void {
    console.log('Received signaling message:', message.type);

    switch (message.type) {
      case 'call/request':
        this.remoteUserId = message.fromUser || '';
        this.updateCallState({
          callStatus: VideoCallStatus.INCOMING,
          remoteUserId: this.remoteUserId
        });
        this.emit('incoming-call', { callerId: this.remoteUserId });
        break;

      case 'call/accept':
        this.remoteUserId = message.fromUser || '';
        this.initiateOffer();
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
        this.cleanup();
        this.emit('call-ended', { remoteId: message.fromUser });
        break;

      case 'call/failed':
        this.handleError(new Error(message.reason || 'Call failed'));
        break;
    }
  }

  private async initiateOffer(): Promise<void> {
    try {
      if (!this.peerConnection) await this.initialize();
      if (!this.localStream) await this.createLocalStream();

      const offer = await this.peerConnection!.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true
      });
      await this.peerConnection!.setLocalDescription(offer);

      this.sendMessage({
        type: 'offer',
        fromUser: this.userId,
        toUser: this.remoteUserId,
        sdp: offer.sdp,
        sdpType: 'offer'
      });

      this.updateCallState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (e) {
      this.handleError(e as Error);
    }
  }

  private async handleOffer(message: WebrtcMessage): Promise<void> {
    try {
      if (!this.peerConnection) await this.initialize();
      if (!this.localStream) await this.createLocalStream();

      await this.peerConnection!.setRemoteDescription(new RTCSessionDescription({
        type: 'offer',
        sdp: message.sdp
      }));

      const answer = await this.peerConnection!.createAnswer();
      await this.peerConnection!.setLocalDescription(answer);

      this.sendMessage({
        type: 'answer',
        fromUser: this.userId,
        toUser: this.remoteUserId,
        sdp: answer.sdp,
        sdpType: 'answer'
      });

      this.updateCallState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (e) {
      this.handleError(e as Error);
    }
  }

  private async handleAnswer(message: WebrtcMessage): Promise<void> {
    try {
      if (this.peerConnection) {
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

  public initiateCall(remoteUserId: string): void {
    this.remoteUserId = remoteUserId;
    this.updateCallState({
      callStatus: VideoCallStatus.OUTGOING,
      remoteUserId
    });

    this.sendMessage({
      type: 'call/request',
      fromUser: this.userId,
      toUser: remoteUserId
    });
  }

  public acceptCall(): void {
    if (!this.remoteUserId) return;

    this.sendMessage({
      type: 'call/accept',
      fromUser: this.userId,
      toUser: this.remoteUserId
    });

    this.updateCallState({ callStatus: VideoCallStatus.CONNECTING });
  }

  public rejectCall(): void {
    this.endCall();
  }

  public endCall(): void {
    if (this.remoteUserId) {
      this.sendMessage({
        type: 'call/end',
        fromUser: this.userId,
        toUser: this.remoteUserId
      });
    }
    this.cleanup();
  }

  private sendMessage(message: WebrtcMessage): void {
    if (this.websocket?.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(message));
    }
  }

  private updateCallState(updates: Partial<VideoCallState>): void {
    this.callState = { ...this.callState, ...updates };
    this.emit('state-change', this.callState);
  }

  private handleError(error: Error): void {
    console.error('VideoCallManager Error:', error);
    this.updateCallState({
      callStatus: VideoCallStatus.ERROR,
      errorMessage: error.message
    });
    this.emit('error', error);
  }

  private cleanup(): void {
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }
    if (this.localStream) {
      this.localStream.getTracks().forEach(t => t.stop());
      this.localStream = null;
    }
    this.remoteStream = null;
    this.updateCallState({
      callStatus: VideoCallStatus.ENDED,
      remoteUserId: undefined
    });
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
      this.updateCallState({ isLocalVideoEnabled: enabled });
    }
  }

  public toggleMicrophone(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach(t => t.enabled = enabled);
      this.updateCallState({ isMicrophoneEnabled: enabled });
    }
  }
}