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
import { socketService } from './socketService';

export interface IceCandidateData {
  candidate: string;
  sdpMid: string;
  sdpMLineIndex: number;
}

export interface MeetingParticipantState {
  userId: string;
  userName?: string;
  avatar?: string;
  streamId?: string;
  isLocal?: boolean;
  connectionState?: RTCPeerConnectionState | 'idle';
}

export interface WebrtcMessage {
  type: string;
  fromUser?: string;
  fromUserName?: string;
  fromAvatar?: string;
  toUser?: string;
  roomId?: string;
  sdp?: string;
  sdpType?: string;
  candidate?: IceCandidateData;
  reason?: string;
  participants?: Array<Record<string, any>>;
  userId?: string;
  userName?: string;
  avatar?: string;
}

export interface RemoteParticipantStream {
  userId: string;
  userName?: string;
  avatar?: string;
  stream: MediaStream | null;
}

export interface CallInternalState {
  callStatus: VideoCallStatus;
  roomId?: string;
  remoteUserId?: string;
  remoteUserName?: string;
  remoteAvatar?: string;
  participants: MeetingParticipantState[];
  callStartTime?: number;
  duration: number;
  isLocalVideoEnabled: boolean;
  isRemoteVideoEnabled: boolean;
  isMicrophoneEnabled: boolean;
  isSpeakerEnabled: boolean;
  isMeeting: boolean;
  errorMessage?: string;
}

const DEFAULT_ICE_SERVERS: RTCIceServer[] = [{ urls: 'stun:stun.l.google.com:19302' }];

export class WebRTCService extends EventEmitter {
  private localStream: MediaStream | null = null;
  private websocket: WebSocket | null = null;
  private store: Store<RootState> | null = null;
  private userId = '';
  private iceServers: RTCIceServer[];
  private initialized = false;
  private participantDirectory = new Map<string, MeetingParticipantState>();
  private peerConnections = new Map<string, RTCPeerConnection>();
  private remoteStreams = new Map<string, MediaStream>();
  private pendingIceCandidates = new Map<string, RTCIceCandidateInit[]>();
  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_ATTEMPTS = 5;
  private durationInterval: ReturnType<typeof setInterval> | null = null;
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private isConnecting = false;

  private state: CallInternalState = {
    callStatus: VideoCallStatus.IDLE,
    participants: [],
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: false,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true,
    isMeeting: false
  };

  constructor(iceServers: RTCIceServer[] = DEFAULT_ICE_SERVERS) {
    super();
    this.iceServers = iceServers;
  }

  public getState(): CallInternalState {
    return {
      ...this.state,
      participants: [...this.state.participants]
    };
  }

  public getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  public getRemoteStream(): MediaStream | null {
    return this.getRemoteParticipantStreams()[0]?.stream || null;
  }

  public getRemoteParticipantStreams(): RemoteParticipantStream[] {
    return [...this.participantDirectory.values()]
      .filter((participant) => !participant.isLocal)
      .map((participant) => ({
        userId: participant.userId,
        userName: participant.userName,
        avatar: participant.avatar,
        stream: this.remoteStreams.get(participant.userId) || null
      }));
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

  public async initialize(store?: Store<RootState>, userId?: string): Promise<void> {
    if (store) this.store = store;
    if (userId) this.userId = userId;

    if (this.initialized) {
      return;
    }

    socketService.on('webrtc-message', (message) => this.handleSignalingMessage(message));

    console.log('Fetching ICE servers from backend...');
    try {
      const response = await webrtcAPI.getIceServers();
      if (response.data && Array.isArray(response.data)) {
        this.iceServers = response.data.map((server: any) => ({
          urls: server.url,
          username: server.username,
          credential: server.credential
        }));
        console.log('Fetched ICE servers successfully:', this.iceServers);
      }
    } catch (error) {
      console.warn('Failed to fetch ICE servers, using defaults:', error);
    }

    this.initialized = true;
  }

  public async acquireLocalMedia(): Promise<MediaStream> {
    try {
      if (this.localStream) {
        this.localStream.getTracks().forEach((track) => track.stop());
      }

      this.localStream = await this.requestUserMedia({
        video: { width: 1280, height: 720, frameRate: 30 },
        audio: true
      });

      this.syncLocalStreamToPeers();

      if (this.store) {
        this.store.dispatch(setLocalStreamId(this.localStream.id));
      }

      this.upsertParticipant({
        userId: this.userId,
        userName: this.store?.getState().auth.user?.username,
        avatar: this.store?.getState().auth.user?.avatar,
        isLocal: true,
        streamId: this.localStream.id,
        connectionState: 'idle'
      });

      return this.localStream;
    } catch (error) {
      console.error('Failed to acquire local media:', error);
      throw error;
    }
  }

  private syncLocalStreamToPeers(): void {
    if (!this.localStream) {
      return;
    }

    this.peerConnections.forEach((peerConnection) => {
      const existingTrackIds = new Set(
        peerConnection.getSenders().map((sender) => sender.track?.id).filter(Boolean)
      );

      this.localStream!.getTracks().forEach((track) => {
        if (!existingTrackIds.has(track.id)) {
          peerConnection.addTrack(track, this.localStream!);
        }
      });
    });
  }

  public connectSignaling(host: string, port: number, userId: string, token: string, pageProtocol: string = 'http:'): void {
    const protocol = pageProtocol === 'https:' || pageProtocol === 'wss:' ? 'wss' : 'ws';
    const cleanHost = host.replace(/^https?:\/\//, '');
    const url = `${protocol}://${cleanHost}:${port}/ws?userId=${userId}&token=${token}`;

    const sharedWS = socketService.getWebSocket();
    if (sharedWS && sharedWS.readyState === WebSocket.OPEN) {
      this.websocket = sharedWS;
      console.log('Using shared WebSocket for signaling');
      return;
    }

    if (this.websocket && (this.websocket.readyState === WebSocket.OPEN || this.websocket.readyState === WebSocket.CONNECTING) && this.websocket.url === url) {
      console.log('Already connected or connecting to signaling server:', url);
      return;
    }

    if (this.isConnecting) {
      console.log('Signaling connection already in progress...');
      return;
    }

    this.userId = userId;
    console.log('Connecting to signaling server:', url);

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

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

      if (this.state.roomId && this.state.callStatus !== VideoCallStatus.IDLE) {
        this.sendMeetingJoin(this.state.roomId);
      }
    };

    this.websocket.onmessage = (event) => {
      try {
        const message: WebrtcMessage = JSON.parse(event.data);
        this.handleSignalingMessage(message);
      } catch (error) {
        console.error('Failed to parse signaling message:', error);
      }
    };

    this.websocket.onclose = (event) => {
      console.log('Signaling WebSocket closed:', event.code, 'Reason:', event.reason);
      this.isConnecting = false;
      this.stopHeartbeat();

      if (this.state.callStatus !== VideoCallStatus.IDLE && this.state.callStatus !== VideoCallStatus.ENDED && this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
        this.reconnectAttempts++;
        this.reconnectTimer = setTimeout(() => {
          this.reconnectTimer = null;
          this.connectSignaling(host, port, userId, token, pageProtocol);
        }, 5000);
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
        this.websocket.send(JSON.stringify({ type: 'ping', fromUser: this.userId }));
      }
    }, 30000);
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
      case 'meeting/request':
      case 'call/request':
        this.handleMeetingRequest(message);
        break;
      case 'meeting/participants':
        this.handleMeetingParticipants(message);
        break;
      case 'meeting/participant-joined':
        this.handleParticipantJoined(message);
        break;
      case 'meeting/participant-left':
        this.handleParticipantLeft(message);
        break;
      case 'meeting/reject':
      case 'call/failed':
        this.handleMeetingRejected(message);
        break;
      case 'offer':
        void this.handleOffer(message);
        break;
      case 'answer':
        void this.handleAnswer(message);
        break;
      case 'candidate':
        this.handleIceCandidate(message);
        break;
      case 'meeting/leave':
      case 'call/end':
        this.handleRemoteHangup(message.fromUser);
        break;
      default:
        break;
    }
  }

  private handleMeetingRequest(message: WebrtcMessage): void {
    const roomId = message.roomId || this.createRoomId();
    this.upsertParticipant({
      userId: message.fromUser || '',
      userName: message.fromUserName,
      avatar: message.fromAvatar
    });

    this.updateState({
      callStatus: VideoCallStatus.INCOMING,
      roomId,
      remoteUserId: message.fromUser,
      remoteUserName: message.fromUserName,
      remoteAvatar: message.fromAvatar,
      isMeeting: (message.participants?.length || 0) > 1
    });

    if (this.store && message.fromUser) {
      this.store.dispatch(incomingCall({
        callId: roomId,
        remoteUser: {
          userId: message.fromUser,
          username: message.fromUserName || `User ${message.fromUser}`,
          avatar: message.fromAvatar,
          email: '',
          status: 'online'
        }
      }));
    }

    this.emit('incoming-call', { callerId: message.fromUser, roomId });
  }

  private handleMeetingParticipants(message: WebrtcMessage): void {
    const participants = message.participants || [];
    participants.forEach((participant) => {
      const participantId = String(participant.userId || participant.fromUser || '');
      if (!participantId || participantId === this.userId) {
        return;
      }

      this.upsertParticipant({
        userId: participantId,
        userName: participant.userName || participant.fromUserName,
        avatar: participant.avatar
      });
    });

    this.updateState({
      roomId: message.roomId || this.state.roomId,
      callStatus: VideoCallStatus.CONNECTING,
      isMeeting: participants.length > 1
    });
  }

  private handleParticipantJoined(message: WebrtcMessage): void {
    const participantId = String(message.fromUser || message.userId || '');
    if (!participantId || participantId === this.userId) {
      return;
    }

    this.upsertParticipant({
      userId: participantId,
      userName: message.userName || message.fromUserName,
      avatar: message.avatar || message.fromAvatar
    });

    if (this.state.callStatus !== VideoCallStatus.INCOMING) {
      void this.createOfferForParticipant(participantId);
    }
  }

  private handleParticipantLeft(message: WebrtcMessage): void {
    const participantId = String(message.fromUser || message.userId || '');
    if (!participantId) {
      return;
    }

    this.removeParticipantConnection(participantId);

    if (this.getRemoteParticipantStreams().length === 0 && this.state.callStatus === VideoCallStatus.ACTIVE) {
      this.cleanupCallState(false);
    }
  }

  private handleMeetingRejected(message: WebrtcMessage): void {
    const rejectedUserId = message.fromUser;
    if (rejectedUserId) {
      this.removeParticipantConnection(rejectedUserId);
    }

    if (this.getRemoteParticipantStreams().length === 0 && this.state.callStatus === VideoCallStatus.OUTGOING) {
      this.handleError(new Error(message.reason || 'Call was rejected'));
    }
  }

  private async createOfferForParticipant(remoteUserId: string): Promise<void> {
    try {
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      const peerConnection = await this.ensurePeerConnection(remoteUserId);
      const offer = await peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true
      });
      await peerConnection.setLocalDescription(offer);

      this.sendSignalingMessage({
        type: 'offer',
        fromUser: this.userId,
        toUser: remoteUserId,
        roomId: this.state.roomId,
        sdp: offer.sdp || '',
        sdpType: 'offer'
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private async handleOffer(message: WebrtcMessage): Promise<void> {
    try {
      const remoteUserId = String(message.fromUser || '');
      if (!remoteUserId) {
        return;
      }

      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      const peerConnection = await this.ensurePeerConnection(remoteUserId);
      await peerConnection.setRemoteDescription(new RTCSessionDescription({
        type: 'offer',
        sdp: message.sdp
      }));

      const queuedCandidates = this.pendingIceCandidates.get(remoteUserId) || [];
      for (const candidate of queuedCandidates) {
        await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      }
      this.pendingIceCandidates.delete(remoteUserId);

      const answer = await peerConnection.createAnswer();
      await peerConnection.setLocalDescription(answer);

      this.sendSignalingMessage({
        type: 'answer',
        fromUser: this.userId,
        toUser: remoteUserId,
        roomId: this.state.roomId || message.roomId,
        sdp: answer.sdp || '',
        sdpType: 'answer'
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private async handleAnswer(message: WebrtcMessage): Promise<void> {
    try {
      const remoteUserId = String(message.fromUser || '');
      const peerConnection = this.peerConnections.get(remoteUserId);
      if (!peerConnection || !message.sdp) {
        return;
      }

      await peerConnection.setRemoteDescription(new RTCSessionDescription({
        type: 'answer',
        sdp: message.sdp
      }));

      const queuedCandidates = this.pendingIceCandidates.get(remoteUserId) || [];
      for (const candidate of queuedCandidates) {
        await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      }
      this.pendingIceCandidates.delete(remoteUserId);
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private handleIceCandidate(message: WebrtcMessage): void {
    const remoteUserId = String(message.fromUser || '');
    if (!remoteUserId || !message.candidate) {
      return;
    }

    const candidate: RTCIceCandidateInit = {
      candidate: message.candidate.candidate,
      sdpMid: message.candidate.sdpMid,
      sdpMLineIndex: message.candidate.sdpMLineIndex
    };

    const peerConnection = this.peerConnections.get(remoteUserId);
    if (peerConnection && peerConnection.remoteDescription) {
      peerConnection.addIceCandidate(new RTCIceCandidate(candidate)).catch((error) => {
        console.error('Error adding ICE candidate:', error);
      });
      return;
    }

    const pending = this.pendingIceCandidates.get(remoteUserId) || [];
    pending.push(candidate);
    this.pendingIceCandidates.set(remoteUserId, pending);
  }

  private async ensurePeerConnection(remoteUserId: string): Promise<RTCPeerConnection> {
    await this.initialize();

    const existing = this.peerConnections.get(remoteUserId);
    if (existing) {
      return existing;
    }

    const peerConnection = new RTCPeerConnection({ iceServers: this.iceServers });
    this.peerConnections.set(remoteUserId, peerConnection);

    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        peerConnection.addTrack(track, this.localStream!);
      });
    }

    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendSignalingMessage({
          type: 'candidate',
          fromUser: this.userId,
          toUser: remoteUserId,
          roomId: this.state.roomId,
          candidate: {
            candidate: event.candidate.candidate,
            sdpMid: event.candidate.sdpMid || '',
            sdpMLineIndex: event.candidate.sdpMLineIndex || 0
          }
        });
      }
    };

    peerConnection.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        const stream = event.streams[0];
        this.remoteStreams.set(remoteUserId, stream);
        this.upsertParticipant({
          userId: remoteUserId,
          streamId: stream.id,
          connectionState: peerConnection.connectionState
        });
        this.emit('remote-streams-change', this.getRemoteParticipantStreams());

        const firstRemoteStream = this.getRemoteStream();
        if (this.store) {
          this.store.dispatch(setRemoteStreamId(firstRemoteStream?.id || null));
        }
      }
    };

    peerConnection.onconnectionstatechange = () => {
      this.upsertParticipant({
        userId: remoteUserId,
        connectionState: peerConnection.connectionState
      });

      if (peerConnection.connectionState === 'connected') {
        if (!this.state.callStartTime) {
          this.updateState({
            callStatus: VideoCallStatus.ACTIVE,
            callStartTime: Date.now()
          });
          this.startDurationTimer();
          if (this.store) {
            this.store.dispatch(callConnected());
          }
        } else {
          this.updateState({ callStatus: VideoCallStatus.ACTIVE });
        }
      } else if (peerConnection.connectionState === 'failed') {
        this.handleError(new Error(`WebRTC Connection failed for ${remoteUserId}`));
      } else if (peerConnection.connectionState === 'disconnected' || peerConnection.connectionState === 'closed') {
        this.removeParticipantConnection(remoteUserId);
      }
    };

    return peerConnection;
  }

  public initiateCall(remoteUserId: string, remoteUserName?: string): void {
    this.initiateMeeting([{ userId: remoteUserId, userName: remoteUserName }]);
  }

  public initiateMeeting(targets: Array<{ userId: string; userName?: string; avatar?: string }>, roomId?: string): void {
    void this.startMeetingFlow(targets, roomId);
  }

  private async startMeetingFlow(targets: Array<{ userId: string; userName?: string; avatar?: string }>, roomId?: string): Promise<void> {
    try {
      if (!targets.length) {
        throw new Error('No participants provided for meeting');
      }

      await this.initialize();
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      const finalRoomId = roomId || this.createRoomId();
      targets.forEach((target) => {
        this.upsertParticipant({
          userId: target.userId,
          userName: target.userName,
          avatar: target.avatar
        });
      });

      const firstTarget = targets[0];
      this.updateState({
        callStatus: VideoCallStatus.OUTGOING,
        roomId: finalRoomId,
        remoteUserId: firstTarget.userId,
        remoteUserName: firstTarget.userName,
        remoteAvatar: firstTarget.avatar,
        isMeeting: targets.length > 1
      });

      this.sendMeetingJoin(finalRoomId);

      for (const target of targets) {
        this.sendSignalingMessage({
          type: 'meeting/request',
          fromUser: this.userId,
          fromUserName: this.store?.getState().auth.user?.username,
          fromAvatar: this.store?.getState().auth.user?.avatar,
          toUser: target.userId,
          roomId: finalRoomId,
          participants: targets.map((participant) => ({
            userId: participant.userId,
            userName: participant.userName,
            avatar: participant.avatar
          }))
        });
      }
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private sendMeetingJoin(roomId: string): void {
    this.sendSignalingMessage({
      type: 'meeting/join',
      fromUser: this.userId,
      fromUserName: this.store?.getState().auth.user?.username,
      fromAvatar: this.store?.getState().auth.user?.avatar,
      roomId
    });
  }

  public acceptCall(): void {
    void this.acceptPendingMeeting();
  }

  private async acceptPendingMeeting(): Promise<void> {
    try {
      if (!this.state.roomId) {
        return;
      }

      await this.initialize();
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
      this.sendMeetingJoin(this.state.roomId);
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  public rejectCall(): void {
    if (this.state.remoteUserId) {
      this.sendSignalingMessage({
        type: 'meeting/reject',
        fromUser: this.userId,
        toUser: this.state.remoteUserId,
        roomId: this.state.roomId,
        reason: 'Call rejected'
      });
    }
    this.cleanupCallState(false);
  }

  public endCall(): void {
    if (this.state.roomId) {
      this.sendSignalingMessage({
        type: 'meeting/leave',
        fromUser: this.userId,
        roomId: this.state.roomId
      });
    }
    this.cleanupCallState(false);
  }

  private handleRemoteHangup(remoteUserId?: string): void {
    if (remoteUserId) {
      this.removeParticipantConnection(remoteUserId);
    } else {
      this.cleanupCallState(false);
    }

    this.emit('call-ended', { remoteId: remoteUserId || this.state.remoteUserId });
  }

  private sendSignalingMessage(message: WebrtcMessage): void {
    if (this.websocket?.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(message));
    } else {
      console.warn('Cannot send signaling message: WebSocket not open');
    }
  }

  private createRoomId(): string {
    return `meeting-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  }

  private upsertParticipant(participant: Partial<MeetingParticipantState> & { userId: string }): void {
    const current = this.participantDirectory.get(participant.userId) || {
      userId: participant.userId,
      connectionState: 'idle'
    };

    this.participantDirectory.set(participant.userId, {
      ...current,
      ...participant
    });
    this.syncStateParticipants();
  }

  private removeParticipantConnection(userId: string): void {
    const peerConnection = this.peerConnections.get(userId);
    if (peerConnection) {
      peerConnection.onicecandidate = null;
      peerConnection.ontrack = null;
      peerConnection.onconnectionstatechange = null;
      peerConnection.close();
      this.peerConnections.delete(userId);
    }

    this.pendingIceCandidates.delete(userId);
    this.remoteStreams.delete(userId);
    this.participantDirectory.delete(userId);

    this.emit('remote-streams-change', this.getRemoteParticipantStreams());
    const firstRemoteStream = this.getRemoteStream();
    if (this.store) {
      this.store.dispatch(setRemoteStreamId(firstRemoteStream?.id || null));
    }
    this.syncStateParticipants();
  }

  private syncStateParticipants(): void {
    const participants = [...this.participantDirectory.values()];
    const firstRemote = participants.find((participant) => !participant.isLocal);

    this.updateState({
      participants,
      remoteUserId: firstRemote?.userId,
      remoteUserName: firstRemote?.userName,
      remoteAvatar: firstRemote?.avatar,
      isRemoteVideoEnabled: this.getRemoteParticipantStreams().length > 0,
      isMeeting: participants.filter((participant) => !participant.isLocal).length > 1 || this.state.isMeeting
    });
  }

  private updateState(updates: Partial<CallInternalState>): void {
    this.state = {
      ...this.state,
      ...updates,
      participants: updates.participants ?? this.state.participants
    };
    this.emit('state-change', this.getState());
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

  private cleanupCallState(resetError: boolean): void {
    this.stopDurationTimer();

    this.peerConnections.forEach((peerConnection) => {
      peerConnection.onicecandidate = null;
      peerConnection.ontrack = null;
      peerConnection.onconnectionstatechange = null;
      peerConnection.close();
    });
    this.peerConnections.clear();
    this.pendingIceCandidates.clear();
    this.remoteStreams.clear();

    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => track.stop());
      this.localStream = null;
    }

    this.participantDirectory.clear();
    this.emit('remote-streams-change', []);

    this.updateState({
      callStatus: VideoCallStatus.IDLE,
      roomId: undefined,
      remoteUserId: undefined,
      remoteUserName: undefined,
      remoteAvatar: undefined,
      participants: [],
      callStartTime: undefined,
      duration: 0,
      isRemoteVideoEnabled: false,
      isMeeting: false,
      errorMessage: resetError ? undefined : this.state.errorMessage
    });

    if (this.store) {
      this.store.dispatch(setLocalStreamId(null));
      this.store.dispatch(setRemoteStreamId(null));
      this.store.dispatch(callEnded());
    }
  }

  public destroy(): void {
    this.cleanupCallState(true);
    this.stopHeartbeat();

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
    }

    this.removeAllListeners();
  }

  public toggleCamera(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach((track) => {
        track.enabled = enabled;
      });
    }

    this.updateState({ isLocalVideoEnabled: enabled });
  }

  public toggleMicrophone(enabled: boolean): void {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach((track) => {
        track.enabled = enabled;
      });
    }

    this.updateState({ isMicrophoneEnabled: enabled });
  }

  public toggleSpeaker(enabled: boolean): void {
    this.updateState({ isSpeakerEnabled: enabled });
  }
}

export const webRTCService = new WebRTCService();
