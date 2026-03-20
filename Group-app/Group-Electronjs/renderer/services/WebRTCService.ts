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

// WebRTC Message Protocol
export interface WebrtcMessage {
  type: string;              // Message type: call/request, call/accept, call/end, offer, answer, candidate
  fromUser?: string;         // Sender ID
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
  callStartTime?: number;
  duration: number;
  isLocalVideoEnabled: boolean;
  isRemoteVideoEnabled: boolean;
  isMicrophoneEnabled: boolean;
  isSpeakerEnabled: boolean;
  errorMessage?: string;
}

const DEFAULT_ICE_SERVERS: RTCIceServer[] = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  { urls: 'stun:stun2.l.google.com:19302' }
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

  /**
   * Initialize with Redux store and create PC
   */
  public async initialize(store?: Store<RootState>, userId?: string): Promise<void> {
    if (store) this.store = store;
    if (userId) this.userId = userId;
    
    if (this.peerConnection) return;

    try {
      this.peerConnection = new RTCPeerConnection({
        iceServers: this.iceServers
      });
      
      this.setupPeerConnectionEvents();
      console.log('WebRTCService initialized');
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

      this.localStream = await navigator.mediaDevices.getUserMedia({
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
  public connectSignaling(host: string, port: number, userId: string, token: string): void {
    this.userId = userId;
    const protocol = host.startsWith('https') ? 'wss' : 'ws';
    const cleanHost = host.replace(/^https?:\/\//, '');
    const url = `${protocol}://${cleanHost}:${port}/ws?userId=${userId}&token=${token}`;

    console.log('Connecting to signaling server:', url);
    if (this.websocket) {
        this.websocket.close();
    }

    this.websocket = new WebSocket(url);

    this.websocket.onopen = () => {
      console.log('Signaling WebSocket connected');
      this.reconnectAttempts = 0;
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
      console.log('Signaling WebSocket closed:', event.code);
      if (!this.state.callStatus.includes('ENDED') && this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
        this.reconnectAttempts++;
        setTimeout(() => this.connectSignaling(host, port, userId, token), 3000);
      }
    };

    this.websocket.onerror = (error) => {
      console.error('Signaling WebSocket error:', error);
    };
  }

  private handleSignalingMessage(message: WebrtcMessage): void {
    console.log('Signaling Received:', message.type, 'from:', message.fromUser);

    switch (message.type) {
      case 'call/request':
        this.remoteUserId = message.fromUser || '';
        this.updateState({
          callStatus: VideoCallStatus.INCOMING,
          remoteUserId: this.remoteUserId
        });
        
        if (this.store) {
          this.store.dispatch(incomingCall({
            callId: `call-${Date.now()}`,
            remoteUser: {
              userId: this.remoteUserId,
              username: `User ${this.remoteUserId}`,
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

  public initiateCall(remoteUserId: string): void {
    this.remoteUserId = remoteUserId;
    this.updateState({
      callStatus: VideoCallStatus.OUTGOING,
      remoteUserId
    });

    this.sendSignalingMessage({
      type: 'call/request',
      fromUser: this.userId,
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
