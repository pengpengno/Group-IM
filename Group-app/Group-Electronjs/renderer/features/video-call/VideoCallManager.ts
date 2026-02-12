import { EventEmitter } from 'events';

// Video call status enumeration
export enum VideoCallStatus {
  IDLE = 'idle',
  OUTGOING = 'outgoing',
  INCOMING = 'incoming',
  CONNECTING = 'connecting',
  ACTIVE = 'active',
  ENDED = 'ended',
  ERROR = 'error'
}

// Video call state interface
export interface VideoCallState {
  callStatus: VideoCallStatus;
  callerId?: string;
  calleeId?: string;
  callStartTime?: number;
  duration: number;
  isLocalVideoEnabled: boolean;
  isRemoteVideoEnabled: boolean;
  isMicrophoneEnabled: boolean;
  isSpeakerEnabled: boolean;
  errorMessage?: string;
}

// WebRTC configuration
interface WebRTCConfig {
  iceServers: RTCIceServer[];
}

// Default WebRTC configuration
const DEFAULT_WEBRTC_CONFIG: WebRTCConfig = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ]
};

export class VideoCallManager extends EventEmitter {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private websocket: WebSocket | null = null;
  private config: WebRTCConfig;
  private callState: VideoCallState = {
    callStatus: VideoCallStatus.IDLE,
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: true,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true
  };

  constructor(config: Partial<WebRTCConfig> = {}) {
    super();
    this.config = { ...DEFAULT_WEBRTC_CONFIG, ...config };
  }

  // Get current call state
  public getCallState(): VideoCallState {
    return { ...this.callState };
  }

  // Get local media stream
  public getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  // Get remote media stream
  public getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }

  // Initialize video call manager
  public async initialize(): Promise<void> {
    try {
      // Create peer connection
      this.peerConnection = new RTCPeerConnection({
        iceServers: this.config.iceServers
      });

      // Set up event listeners
      this.setupPeerConnectionEvents();
      
      console.log('VideoCallManager initialized successfully');
    } catch (error) {
      console.error('Failed to initialize VideoCallManager:', error);
      throw error;
    }
  }

  // Create local media stream
  public async createLocalMediaStream(
    enableVideo: boolean = true,
    enableAudio: boolean = true
  ): Promise<MediaStream> {
    try {
      const constraints: MediaStreamConstraints = {
        video: enableVideo ? { facingMode: 'user' } : false,
        audio: enableAudio
      };

      this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
      
      // Add tracks to peer connection
      if (this.peerConnection && this.localStream) {
        this.localStream.getTracks().forEach(track => {
          this.peerConnection!.addTrack(track, this.localStream!);
        });
      }

      this.updateCallState({ isLocalVideoEnabled: enableVideo });
      console.log('Local media stream created successfully');
      
      return this.localStream;
    } catch (error) {
      console.error('Failed to create local media stream:', error);
      throw error;
    }
  }

  // Start outgoing video call
  public async startVideoCall(calleeId: string): Promise<void> {
    try {
      // Ensure we have local stream
      if (!this.localStream) {
        await this.createLocalMediaStream();
      }

      // Update state
      this.updateCallState({
        callStatus: VideoCallStatus.OUTGOING,
        calleeId,
        callStartTime: Date.now()
      });

      // Create offer
      const offer = await this.peerConnection!.createOffer();
      await this.peerConnection!.setLocalDescription(offer);

      // Send offer through signaling
      this.sendSignalingMessage({
        type: 'offer',
        sdp: offer.sdp,
        to: calleeId
      });

      console.log(`Started video call to ${calleeId}`);
    } catch (error) {
      console.error('Failed to start video call:', error);
      this.handleError(error as Error);
    }
  }

  // Accept incoming video call
  public async acceptVideoCall(callerId: string): Promise<void> {
    try {
      // Ensure we have local stream
      if (!this.localStream) {
        await this.createLocalMediaStream();
      }

      // Update state
      this.updateCallState({
        callStatus: VideoCallStatus.CONNECTING,
        callerId,
        callStartTime: Date.now()
      });

      console.log(`Accepted video call from ${callerId}`);
    } catch (error) {
      console.error('Failed to accept video call:', error);
      this.handleError(error as Error);
    }
  }

  // Reject video call
  public rejectVideoCall(callerId: string): void {
    this.sendSignalingMessage({
      type: 'reject',
      from: callerId
    });

    this.endVideoCall();
  }

  // End current video call
  public endVideoCall(): void {
    try {
      // Close peer connection
      if (this.peerConnection) {
        this.peerConnection.close();
        this.peerConnection = null;
      }

      // Stop local stream
      if (this.localStream) {
        this.localStream.getTracks().forEach(track => track.stop());
        this.localStream = null;
      }

      // Clear remote stream
      this.remoteStream = null;

      // Close websocket
      if (this.websocket) {
        this.websocket.close();
        this.websocket = null;
      }

      // Update state
      this.updateCallState({
        callStatus: VideoCallStatus.ENDED,
        callStartTime: undefined,
        duration: 0
      });

      console.log('Video call ended');
    } catch (error) {
      console.error('Error ending video call:', error);
    }
  }

  // Toggle camera
  public toggleCamera(enabled: boolean): void {
    if (this.localStream) {
      const videoTrack = this.localStream.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = enabled;
        this.updateCallState({ isLocalVideoEnabled: enabled });
        
        // Notify remote peer
        this.sendSignalingMessage({
          type: 'control',
          controlType: 'camera',
          enabled
        });
      }
    }
  }

  // Toggle microphone
  public toggleMicrophone(enabled: boolean): void {
    if (this.localStream) {
      const audioTrack = this.localStream.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = enabled;
        this.updateCallState({ isMicrophoneEnabled: enabled });
        
        // Notify remote peer
        this.sendSignalingMessage({
          type: 'control',
          controlType: 'microphone',
          enabled
        });
      }
    }
  }

  // Toggle speaker
  public toggleSpeaker(enabled: boolean): void {
    this.updateCallState({ isSpeakerEnabled: enabled });
    // Speaker toggle logic would depend on the specific audio output device API
  }

  // Connect to signaling server
  public connectToSignalingServer(url: string, userId: string): void {
    this.websocket = new WebSocket(`${url}?userId=${userId}`);
    
    this.websocket.onopen = () => {
      console.log('Connected to signaling server');
    };

    this.websocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleSignalingMessage(message);
    };

    this.websocket.onerror = (error) => {
      console.error('Signaling server error:', error);
      this.handleError(new Error('Signaling server connection failed'));
    };

    this.websocket.onclose = () => {
      console.log('Disconnected from signaling server');
    };
  }

  // Handle incoming signaling messages
  private handleSignalingMessage(message: any): void {
    switch (message.type) {
      case 'offer':
        this.handleOffer(message);
        break;
      case 'answer':
        this.handleAnswer(message);
        break;
      case 'ice-candidate':
        this.handleIceCandidate(message);
        break;
      case 'call-request':
        this.handleCallRequest(message);
        break;
      case 'call-accepted':
        this.handleCallAccepted(message);
        break;
      case 'call-rejected':
        this.handleCallRejected(message);
        break;
      case 'call-ended':
        this.handleCallEnded(message);
        break;
      case 'control':
        this.handleControlMessage(message);
        break;
    }
  }

  // Handle offer from remote peer
  private async handleOffer(message: any): Promise<void> {
    try {
      if (!this.peerConnection) {
        await this.initialize();
      }

      const offer = new RTCSessionDescription({
        type: 'offer',
        sdp: message.sdp
      });

      await this.peerConnection!.setRemoteDescription(offer);

      // Create and send answer
      const answer = await this.peerConnection!.createAnswer();
      await this.peerConnection!.setLocalDescription(answer);

      this.sendSignalingMessage({
        type: 'answer',
        sdp: answer.sdp,
        to: message.from
      });

      this.updateCallState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (error) {
      console.error('Error handling offer:', error);
      this.handleError(error as Error);
    }
  }

  // Handle answer from remote peer
  private async handleAnswer(message: any): Promise<void> {
    try {
      const answer = new RTCSessionDescription({
        type: 'answer',
        sdp: message.sdp
      });

      await this.peerConnection!.setRemoteDescription(answer);
      this.updateCallState({ callStatus: VideoCallStatus.ACTIVE });
    } catch (error) {
      console.error('Error handling answer:', error);
      this.handleError(error as Error);
    }
  }

  // Handle ICE candidate
  private handleIceCandidate(message: any): void {
    try {
      const candidate = new RTCIceCandidate({
        candidate: message.candidate,
        sdpMid: message.sdpMid,
        sdpMLineIndex: message.sdpMLineIndex
      });

      this.peerConnection!.addIceCandidate(candidate);
    } catch (error) {
      console.error('Error handling ICE candidate:', error);
    }
  }

  // Handle incoming call request
  private handleCallRequest(message: any): void {
    this.updateCallState({
      callStatus: VideoCallStatus.INCOMING,
      callerId: message.from,
      callStartTime: Date.now()
    });

    // Emit event for UI to handle
    this.emit('incoming-call', { callerId: message.from });
  }

  // Handle call accepted
  private handleCallAccepted(message: any): void {
    this.updateCallState({ callStatus: VideoCallStatus.CONNECTING });
    // Emit event for UI updates
    this.emit('call-accepted', { calleeId: message.from });
  }

  // Handle call rejected
  private handleCallRejected(message: any): void {
    this.updateCallState({
      callStatus: VideoCallStatus.ENDED,
      errorMessage: 'Call was rejected'
    });
    
    this.emit('call-rejected', { callerId: message.from });
  }

  // Handle call ended
  private handleCallEnded(message: any): void {
    this.endVideoCall();
    this.emit('call-ended', { remoteId: message.from });
  }

  // Handle control messages
  private handleControlMessage(message: any): void {
    switch (message.controlType) {
      case 'camera':
        this.updateCallState({ isRemoteVideoEnabled: message.enabled });
        break;
      case 'microphone':
        // Handle remote microphone toggle
        break;
    }
  }

  // Set up peer connection events
  private setupPeerConnectionEvents(): void {
    if (!this.peerConnection) return;

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendSignalingMessage({
          type: 'ice-candidate',
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex
        });
      }
    };

    this.peerConnection.ontrack = (event) => {
      this.remoteStream = event.streams[0];
      this.updateCallState({ isRemoteVideoEnabled: true });
      this.emit('remote-stream', this.remoteStream);
    };

    this.peerConnection.onconnectionstatechange = () => {
      const state = this.peerConnection?.connectionState;
      console.log('Connection state changed:', state);
      
      switch (state) {
        case 'connected':
          this.updateCallState({ callStatus: VideoCallStatus.ACTIVE });
          this.startDurationTimer();
          break;
        case 'disconnected':
        case 'failed':
          this.handleError(new Error('Connection failed'));
          break;
        case 'closed':
          this.endVideoCall();
          break;
      }
    };
  }

  // Start duration timer
  private startDurationTimer(): void {
    if (this.callState.callStartTime) {
      const interval = setInterval(() => {
        if (this.callState.callStatus === VideoCallStatus.ACTIVE) {
          const duration = Math.floor((Date.now() - this.callState.callStartTime!) / 1000);
          this.updateCallState({ duration });
        } else {
          clearInterval(interval);
        }
      }, 1000);
    }
  }

  // Send signaling message
  private sendSignalingMessage(message: any): void {
    if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(message));
    }
  }

  // Update call state and emit change event
  private updateCallState(updates: Partial<VideoCallState>): void {
    this.callState = { ...this.callState, ...updates };
    this.emit('state-change', { ...this.callState });
  }

  // Handle errors
  private handleError(error: Error): void {
    console.error('Video call error:', error);
    this.updateCallState({
      callStatus: VideoCallStatus.ERROR,
      errorMessage: error.message
    });
    this.emit('error', error);
  }

  // Cleanup resources
  public destroy(): void {
    this.endVideoCall();
    this.removeAllListeners();
  }
}