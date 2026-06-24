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
import { meetingSignalingService } from './meetingSignalingService';
import {
  SIGNALING_MESSAGE_TYPES,
  SIGNALING_SDP_TYPES,
  type IceCandidateData,
  type WebrtcMessage
} from '../types/webrtc';

export interface MeetingParticipantState {
  userId: string;
  userName?: string;
  avatar?: string;
  streamId?: string;
  isLocal?: boolean;
  connectionState?: RTCPeerConnectionState | 'idle';
}

export interface RemoteParticipantStream {
  userId: string;
  userName?: string;
  avatar?: string;
  stream: MediaStream | null;
}

export interface CallActivityItem {
  id: string;
  tone: 'info' | 'success' | 'warning';
  label: string;
  detail?: string;
  timestamp: number;
}

export interface CallSessionSummary {
  title: string;
  detail: string;
  durationSeconds: number;
  connected: boolean;
  endedBy: 'local' | 'remote' | 'system';
  endedAt: number;
}

export interface CallInternalState {
  callStatus: VideoCallStatus;
  roomId?: string;
  conversationId?: number;
  callKind?: 'MEETING' | 'VIDEO_CALL' | 'VOICE_CALL';
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
  activityLog: CallActivityItem[];
  sessionSummary?: CallSessionSummary;
  isInitiator: boolean;
}

interface SignalingConnectionConfig {
  host: string;
  port: number;
  userId: string;
  token: string;
  pageProtocol: string;
}

const DEFAULT_ICE_SERVERS: RTCIceServer[] = [{ urls: 'stun:stun.l.google.com:19302' }];

function nextUiFrame(): Promise<void> {
  return new Promise((resolve) => {
    // Let React commit the call screen before camera/mic permission or WebRTC
    // initialization work starts. This keeps outbound/inbound transitions snappy.
    if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
      window.requestAnimationFrame(() => resolve());
      return;
    }
    setTimeout(resolve, 0);
  });
}

export class WebRTCService extends EventEmitter {
  private localStream: MediaStream | null = null;
  private store: Store<RootState> | null = null;
  private userId = '';
  private iceServers: RTCIceServer[];
  private initialized = false;
  private participantDirectory = new Map<string, MeetingParticipantState>();
  private peerConnections = new Map<string, RTCPeerConnection>();
  private remoteStreams = new Map<string, MediaStream>();
  private pendingIceCandidates = new Map<string, RTCIceCandidateInit[]>();
  private durationInterval: ReturnType<typeof setInterval> | null = null;
  private signalingConfig: SignalingConnectionConfig | null = null;
  private signalingUnsubscribe: (() => void) | null = null;

  private state: CallInternalState = {
    callStatus: VideoCallStatus.IDLE,
    participants: [],
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: false,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true,
    isMeeting: false,
    activityLog: [],
    isInitiator: false
  };

  constructor(iceServers: RTCIceServer[] = DEFAULT_ICE_SERVERS) {
    super();
    this.iceServers = iceServers;
  }

  /**
   * Keep WebRTC diagnostics consistent so browser console logs can be matched
   * against webrtc-internals timestamps during call setup debugging.
   */
  private log(scope: string, details?: Record<string, unknown>): void {
    console.log('[WebRTCService]', details ? { scope, ...details } : { scope });
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

    this.log('initialize-requested', {
      hasStore: !!store || !!this.store,
      userId: userId || this.userId,
      alreadyInitialized: this.initialized
    });

    if (this.initialized) {
      this.log('initialize-skipped', {
        userId: this.userId,
        connectionState: meetingSignalingService.getConnectionState()
      });
      return;
    }

    // Signaling lifecycle is owned by App boot so invites can be received even
    // before the user opens any call surface. WebRTCService only consumes that
    // signaling stream to manage call/session state.
    this.signalingUnsubscribe = meetingSignalingService.onMessage((message) => this.handleSignalingMessage(message));
    this.log('signaling-subscription-attached', {
      connectionState: meetingSignalingService.getConnectionState()
    });

    this.log('fetch-ice-servers-start');
    try {
      const response = await webrtcAPI.getIceServers();
      if (response.data && Array.isArray(response.data)) {
        this.iceServers = response.data.map((server: any) => ({
          urls: server.url,
          username: server.username,
          credential: server.credential
        }));
        this.log('fetch-ice-servers-success', {
          iceServers: this.iceServers
        });
      }
    } catch (error) {
      console.warn('[WebRTCService] fetch-ice-servers-failed', error);
    }

    this.initialized = true;
    this.log('initialized', {
      userId: this.userId,
      iceServerCount: this.iceServers.length
    });
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
    this.signalingConfig = { host, port, userId, token, pageProtocol };
    this.userId = userId;
    console.log('WebRTC signaling transport delegated to socketService', { host, port, pageProtocol });
  }

  private handleSignalingMessage(message: WebrtcMessage): void {
    this.log('signaling-received', {
      type: message.type,
      roomId: message.roomId,
      fromUser: message.fromUser,
      toUser: message.toUser,
      callStatus: this.state.callStatus,
      participantCount: this.state.participants.length
    });

    switch (message.type) {
      case SIGNALING_MESSAGE_TYPES.MEETING_REQUEST:
        this.handleMeetingRequest(message);
        break;
      case SIGNALING_MESSAGE_TYPES.MEETING_PARTICIPANTS:
        this.handleMeetingParticipants(message);
        break;
      case SIGNALING_MESSAGE_TYPES.MEETING_PARTICIPANT_JOINED:
        this.handleParticipantJoined(message);
        break;
      case SIGNALING_MESSAGE_TYPES.MEETING_PARTICIPANT_LEFT:
        this.handleParticipantLeft(message);
        break;
      case SIGNALING_MESSAGE_TYPES.MEETING_REJECT:
        this.handleMeetingRejected(message);
        break;
      case SIGNALING_MESSAGE_TYPES.OFFER:
        void this.handleOffer(message);
        break;
      case SIGNALING_MESSAGE_TYPES.ANSWER:
        void this.handleAnswer(message);
        break;
      case SIGNALING_MESSAGE_TYPES.CANDIDATE:
        this.handleIceCandidate(message);
        break;
      case SIGNALING_MESSAGE_TYPES.MEETING_LEAVE:
        this.handleRemoteHangup(message.fromUser);
        break;
      case SIGNALING_MESSAGE_TYPES.MEETING_END:
        this.cleanupCallState(true);
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
      conversationId: message.conversationId,
      callKind: message.callKind || ((message.participants?.length || 0) > 1 ? 'MEETING' : 'VIDEO_CALL'),
      remoteUserId: message.fromUser,
      remoteUserName: message.fromUserName,
      remoteAvatar: message.fromAvatar,
      isMeeting: (message.participants?.length || 0) > 1
    });
    this.pushActivity('info', 'Incoming call', `${message.fromUserName || message.fromUser || 'Unknown user'} is calling.`);

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

  public presentIncomingInvite(invite: {
    roomId: string;
    conversationId?: number;
    callKind?: 'MEETING' | 'VIDEO_CALL' | 'VOICE_CALL';
    remoteUserId?: string;
    remoteUserName?: string;
    remoteAvatar?: string;
  }): void {
    if (this.state.callStatus !== VideoCallStatus.IDLE && this.state.roomId !== invite.roomId) {
      return;
    }

    if (invite.remoteUserId) {
      this.upsertParticipant({
        userId: invite.remoteUserId,
        userName: invite.remoteUserName,
        avatar: invite.remoteAvatar
      });
    }

    this.updateState({
      callStatus: VideoCallStatus.PRE_JOIN,
      roomId: invite.roomId,
      conversationId: invite.conversationId,
      callKind: invite.callKind || 'MEETING',
      remoteUserId: invite.remoteUserId,
      remoteUserName: invite.remoteUserName,
      remoteAvatar: invite.remoteAvatar,
      isMeeting: true
    });
    this.pushActivity('info', 'Meeting invite opened', `Room ${invite.roomId}`);
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
    this.pushActivity('info', 'Participants synced', `${participants.length} participant(s) in room`);
  }

  private handleParticipantJoined(message: WebrtcMessage): void {
    const participantId = String(message.fromUser || message.userId || '');
    if (!participantId || participantId === this.userId) {
      this.log('participant-joined-ignored', {
        participantId,
        selfUserId: this.userId
      });
      return;
    }

    this.upsertParticipant({
      userId: participantId,
      userName: message.userName || message.fromUserName,
      avatar: message.avatar || message.fromAvatar
    });
    this.pushActivity('info', `${message.userName || message.fromUserName || participantId} joined`, 'Connecting media stream.');

    if (this.state.callStatus !== VideoCallStatus.INCOMING) {
      this.log('participant-joined-create-offer', {
        participantId,
        callStatus: this.state.callStatus,
        roomId: this.state.roomId
      });
      void this.createOfferForParticipant(participantId);
    } else {
      this.log('participant-joined-waiting-for-offer', {
        participantId,
        callStatus: this.state.callStatus
      });
    }
  }

  private handleParticipantLeft(message: WebrtcMessage): void {
    const participantId = String(message.fromUser || message.userId || '');
    if (!participantId) {
      return;
    }

    const participantName = this.getParticipantLabel(participantId);
    this.removeParticipantConnection(participantId);

    if (this.getRemoteParticipantStreams().length === 0 && this.state.callStatus === VideoCallStatus.ACTIVE) {
      this.finishCall({
        endedBy: 'remote',
        title: 'Call ended by other side',
        detail: `${participantName} left the call after ${this.formatDuration(this.state.duration)}.`
      });
      return;
    }

    this.pushActivity('warning', `${participantName} left`, 'The participant left the room.');
  }

  private handleMeetingRejected(message: WebrtcMessage): void {
    const rejectedUserId = message.fromUser;
    if (rejectedUserId) {
      this.removeParticipantConnection(rejectedUserId);
    }

    if (this.getRemoteParticipantStreams().length === 0 && this.state.callStatus === VideoCallStatus.OUTGOING) {
      this.finishCall({
        endedBy: 'remote',
        title: 'Call declined',
        detail: message.reason || `${this.getParticipantLabel(rejectedUserId)} declined the call.`
      });
    }
  }

  private async createOfferForParticipant(remoteUserId: string): Promise<void> {
    try {
      this.log('create-offer-start', {
        remoteUserId,
        hasLocalStream: !!this.localStream,
        roomId: this.state.roomId
      });
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
        type: SIGNALING_MESSAGE_TYPES.OFFER,
        fromUser: this.userId,
        toUser: remoteUserId,
        roomId: this.state.roomId,
        sdp: offer.sdp || '',
        sdpType: SIGNALING_SDP_TYPES.OFFER
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
      this.pushActivity('info', 'Offer sent', `Waiting for ${this.getParticipantLabel(remoteUserId)} to connect.`);
      this.log('create-offer-success', {
        remoteUserId,
        signalingState: peerConnection.signalingState,
        iceConnectionState: peerConnection.iceConnectionState
      });
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private async handleOffer(message: WebrtcMessage): Promise<void> {
    try {
      const remoteUserId = String(message.fromUser || '');
      if (!remoteUserId) {
        this.log('handle-offer-ignored', {
          reason: 'missing-remote-user',
          roomId: message.roomId
        });
        return;
      }

      this.log('handle-offer-start', {
        remoteUserId,
        roomId: message.roomId,
        hasLocalStream: !!this.localStream,
        hasSdp: !!message.sdp
      });

      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      const peerConnection = await this.ensurePeerConnection(remoteUserId);
      await peerConnection.setRemoteDescription(new RTCSessionDescription({
        type: SIGNALING_SDP_TYPES.OFFER,
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
        type: SIGNALING_MESSAGE_TYPES.ANSWER,
        fromUser: this.userId,
        toUser: remoteUserId,
        roomId: this.state.roomId || message.roomId,
        sdp: answer.sdp || '',
        sdpType: SIGNALING_SDP_TYPES.ANSWER
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
      this.pushActivity('info', 'Answer sent', `Accepted ${this.getParticipantLabel(remoteUserId)}.`);
      this.log('handle-offer-success', {
        remoteUserId,
        queuedCandidateCount: queuedCandidates.length,
        signalingState: peerConnection.signalingState
      });
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private async handleAnswer(message: WebrtcMessage): Promise<void> {
    try {
      const remoteUserId = String(message.fromUser || '');
      const peerConnection = this.peerConnections.get(remoteUserId);
      if (!peerConnection || !message.sdp) {
        this.log('handle-answer-ignored', {
          remoteUserId,
          hasPeerConnection: !!peerConnection,
          hasSdp: !!message.sdp
        });
        return;
      }

      this.log('handle-answer-start', {
        remoteUserId,
        queuedCandidateCount: (this.pendingIceCandidates.get(remoteUserId) || []).length
      });

      await peerConnection.setRemoteDescription(new RTCSessionDescription({
        type: SIGNALING_SDP_TYPES.ANSWER,
        sdp: message.sdp
      }));

      const queuedCandidates = this.pendingIceCandidates.get(remoteUserId) || [];
      for (const candidate of queuedCandidates) {
        await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      }
      this.pendingIceCandidates.delete(remoteUserId);
      this.log('handle-answer-success', {
        remoteUserId,
        queuedCandidateCount: queuedCandidates.length,
        signalingState: peerConnection.signalingState,
        iceConnectionState: peerConnection.iceConnectionState
      });
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private handleIceCandidate(message: WebrtcMessage): void {
    const remoteUserId = String(message.fromUser || '');
    if (!remoteUserId || !message.candidate) {
      this.log('handle-candidate-ignored', {
        remoteUserId,
        hasCandidate: !!message.candidate
      });
      return;
    }

    const candidate: RTCIceCandidateInit = {
      candidate: message.candidate.candidate,
      sdpMid: message.candidate.sdpMid,
      sdpMLineIndex: message.candidate.sdpMLineIndex
    };

    const peerConnection = this.peerConnections.get(remoteUserId);
    if (peerConnection && peerConnection.remoteDescription) {
      this.log('handle-candidate-apply-immediately', {
        remoteUserId,
        signalingState: peerConnection.signalingState
      });
      peerConnection.addIceCandidate(new RTCIceCandidate(candidate)).catch((error) => {
        console.error('Error adding ICE candidate:', error);
      });
      return;
    }

    const pending = this.pendingIceCandidates.get(remoteUserId) || [];
    pending.push(candidate);
    this.pendingIceCandidates.set(remoteUserId, pending);
    this.log('handle-candidate-queued', {
      remoteUserId,
      queuedCandidateCount: pending.length
    });
  }

  private async ensurePeerConnection(remoteUserId: string): Promise<RTCPeerConnection> {
    await this.initialize();

    const existing = this.peerConnections.get(remoteUserId);
    if (existing) {
      this.log('ensure-peer-connection-reuse', {
        remoteUserId,
        signalingState: existing.signalingState,
        connectionState: existing.connectionState
      });
      return existing;
    }

    this.log('ensure-peer-connection-create', {
      remoteUserId,
      iceServers: this.iceServers,
      hasLocalStream: !!this.localStream
    });
    const peerConnection = new RTCPeerConnection({ iceServers: this.iceServers });
    this.peerConnections.set(remoteUserId, peerConnection);

    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        peerConnection.addTrack(track, this.localStream!);
      });
    }

    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.log('peer-onicecandidate', {
          remoteUserId,
          candidateType: event.candidate.type,
          protocol: event.candidate.protocol,
          sdpMid: event.candidate.sdpMid
        });
        this.sendSignalingMessage({
          type: SIGNALING_MESSAGE_TYPES.CANDIDATE,
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
        this.log('peer-ontrack', {
          remoteUserId,
          streamId: stream.id,
          trackCount: stream.getTracks().length
        });
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
      this.log('peer-connection-state-change', {
        remoteUserId,
        connectionState: peerConnection.connectionState,
        iceConnectionState: peerConnection.iceConnectionState,
        signalingState: peerConnection.signalingState
      });
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
          this.pushActivity('success', 'Call connected', `Live with ${this.getParticipantLabel(remoteUserId)}.`);
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

  public initiateCall(
    remoteUserId: string,
    remoteUserName?: string,
    options?: { conversationId?: number; callKind?: 'VIDEO_CALL' | 'VOICE_CALL' }
  ): void {
    this.initiateMeeting(
      [{ userId: remoteUserId, userName: remoteUserName }],
      undefined,
      {
        conversationId: options?.conversationId,
        callKind: options?.callKind || 'VIDEO_CALL'
      }
    );
  }

  public initiateMeeting(
    targets: Array<{ userId: string; userName?: string; avatar?: string }>,
    roomId?: string,
    options?: { conversationId?: number; callKind?: 'MEETING' | 'VIDEO_CALL' | 'VOICE_CALL' }
  ): void {
    this.prepareFreshSession();
    void this.startMeetingFlow(targets, roomId, options);
  }

  public joinMeeting(roomId: string): void {
    this.prepareFreshSession();
    void this.joinMeetingFlow(roomId);
  }

  private async joinMeetingFlow(roomId: string): Promise<void> {
    try {
      this.updateState({
        callStatus: VideoCallStatus.CONNECTING,
        roomId,
        conversationId: this.state.conversationId,
        callKind: this.state.callKind,
        isMeeting: true,
        sessionSummary: undefined,
        errorMessage: undefined
      });
      this.pushActivity('info', 'Joining call', `Room ${roomId}`);

      // Render the pre-join / connecting surface first, then start media setup.
      await nextUiFrame();
      await this.initialize();
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      this.sendMeetingJoin(roomId);
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private async startMeetingFlow(
    targets: Array<{ userId: string; userName?: string; avatar?: string }>,
    roomId?: string,
    options?: { conversationId?: number; callKind?: 'MEETING' | 'VIDEO_CALL' | 'VOICE_CALL' }
  ): Promise<void> {
    try {
      if (!targets.length) {
        throw new Error('No participants provided for meeting');
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
        conversationId: options?.conversationId,
        callKind: options?.callKind || (targets.length > 1 ? 'MEETING' : 'VIDEO_CALL'),
        remoteUserId: firstTarget.userId,
        remoteUserName: firstTarget.userName,
        remoteAvatar: firstTarget.avatar,
        isMeeting: targets.length > 1,
        sessionSummary: undefined,
        errorMessage: undefined,
        isInitiator: true
      });
      this.pushActivity(
        'info',
        targets.length > 1 ? 'Meeting invite sent' : 'Calling started',
        targets.map((target) => target.userName || target.userId).join(', ')
      );

      // Show the outgoing call page before camera/mic work starts so the
      // transition feels immediate on web and Electron.
      await nextUiFrame();
      await this.initialize();
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      this.sendMeetingJoin(finalRoomId);

      for (const target of targets) {
        this.sendSignalingMessage({
          type: SIGNALING_MESSAGE_TYPES.MEETING_REQUEST,
          fromUser: this.userId,
          fromUserName: this.store?.getState().auth.user?.username,
          fromAvatar: this.store?.getState().auth.user?.avatar,
          toUser: target.userId,
          roomId: finalRoomId,
          conversationId: options?.conversationId,
          callKind: options?.callKind || (targets.length > 1 ? 'MEETING' : 'VIDEO_CALL'),
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
    this.pushActivity('info', 'Joined signaling room', roomId);
    this.sendSignalingMessage({
      type: SIGNALING_MESSAGE_TYPES.MEETING_JOIN,
      fromUser: this.userId,
      fromUserName: this.store?.getState().auth.user?.username,
      fromAvatar: this.store?.getState().auth.user?.avatar,
      roomId,
      conversationId: this.state.conversationId,
      callKind: this.state.callKind
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

      // Mirror mobile behavior: move into the visible connecting state first so
      // the callee sees a stable accept/join surface before media setup begins.
      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
      await nextUiFrame();
      await this.initialize();
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }
      this.sendMeetingJoin(this.state.roomId);
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  public rejectCall(): void {
    if (this.state.callStatus !== VideoCallStatus.INCOMING && this.state.callStatus !== VideoCallStatus.PRE_JOIN) {
      return;
    }

    if (this.state.remoteUserId) {
      this.pushActivity('warning', 'Call declined', 'You declined the incoming call.');
      this.sendSignalingMessage({
        type: SIGNALING_MESSAGE_TYPES.MEETING_REJECT,
        fromUser: this.userId,
        toUser: this.state.remoteUserId,
        roomId: this.state.roomId,
        reason: 'Call rejected'
      });
    }
    this.finishCall({
      endedBy: 'local',
      title: 'Call declined',
      detail: 'You declined the incoming call.'
    });
  }

  public endCall(): void {
    if (this.state.roomId) {
      this.sendSignalingMessage({
        type: SIGNALING_MESSAGE_TYPES.MEETING_LEAVE,
        fromUser: this.userId,
        roomId: this.state.roomId
      });
    }
    const connected = Boolean(this.state.callStartTime);
    this.finishCall({
      endedBy: 'local',
      title: connected ? 'Call ended' : 'Call cancelled',
      detail: connected ? `You ended the call after ${this.formatDuration(this.state.duration)}.` : 'You ended the call before it connected.'
    });
  }

  private handleRemoteHangup(remoteUserId?: string): void {
    const connected = Boolean(this.state.callStartTime);
    const remoteName = this.getParticipantLabel(remoteUserId || this.state.remoteUserId);
    if (remoteUserId) {
      this.removeParticipantConnection(remoteUserId);
      if (this.getRemoteParticipantStreams().length > 0) {
        this.pushActivity('warning', `${remoteName} left`, connected ? `Call duration ${this.formatDuration(this.state.duration)}` : 'Left before the call connected.');
        return;
      }
    }

    this.finishCall({
      endedBy: 'remote',
      title: connected ? 'Call ended by other side' : 'Call not answered',
      detail: connected
        ? `${remoteName} ended the call after ${this.formatDuration(this.state.duration)}.`
        : `${remoteName} ended the call before it connected.`
    });

    this.emit('call-ended', { remoteId: remoteUserId || this.state.remoteUserId });
  }

  private sendSignalingMessage(message: WebrtcMessage): void {
    this.log('signaling-send-attempt', {
      type: message.type,
      roomId: message.roomId,
      fromUser: message.fromUser,
      toUser: message.toUser
    });
    const result = meetingSignalingService.sendMessage(message);
    if (!result.accepted) {
      console.warn('Failed to hand signaling message to meetingSignalingService:', message.type);
    } else {
      this.log('signaling-send-dispatched', {
        type: message.type,
        roomId: message.roomId,
        queued: result.queued
      });
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

  private prepareFreshSession(): void {
    if (this.state.callStatus === VideoCallStatus.ENDED || this.state.callStatus === VideoCallStatus.ERROR) {
      this.updateState({
        activityLog: [],
        sessionSummary: undefined,
        errorMessage: undefined,
        duration: 0,
        callStartTime: undefined,
        isInitiator: false
      });
    }
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
    this.pushActivity('warning', 'Call error', error.message);
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
      conversationId: undefined,
      callKind: undefined,
      remoteUserId: undefined,
      remoteUserName: undefined,
      remoteAvatar: undefined,
      participants: [],
      callStartTime: undefined,
      duration: 0,
      isRemoteVideoEnabled: false,
      isMeeting: false,
      activityLog: resetError ? [] : this.state.activityLog,
      sessionSummary: resetError ? undefined : this.state.sessionSummary,
      errorMessage: resetError ? undefined : this.state.errorMessage,
      isInitiator: resetError ? false : this.state.isInitiator
    });

    if (this.store) {
      this.store.dispatch(setLocalStreamId(null));
      this.store.dispatch(setRemoteStreamId(null));
      this.store.dispatch(callEnded());
    }
  }

  public destroy(): void {
    if (this.signalingUnsubscribe) {
      this.signalingUnsubscribe();
      this.signalingUnsubscribe = null;
    }
    this.initialized = false;
    this.cleanupCallState(true);
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

  public dismissCallSummary(): void {
    this.cleanupCallState(true);
  }

  private finishCall(summary: {
    endedBy: 'local' | 'remote' | 'system';
    title: string;
    detail: string;
  }): void {
    const durationSeconds = this.state.duration;
    this.pushActivity(
      summary.endedBy === 'system' ? 'warning' : 'success',
      summary.title,
      durationSeconds > 0 ? `${summary.detail} Duration ${this.formatDuration(durationSeconds)}.` : summary.detail
    );

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
      callStatus: VideoCallStatus.ENDED,
      roomId: undefined,
      conversationId: undefined,
      callKind: undefined,
      participants: [],
      callStartTime: undefined,
      isRemoteVideoEnabled: false,
      sessionSummary: {
        title: summary.title,
        detail: summary.detail,
        durationSeconds,
        connected: durationSeconds > 0,
        endedBy: summary.endedBy,
        endedAt: Date.now()
      }
    });

    if (this.store) {
      this.store.dispatch(setLocalStreamId(null));
      this.store.dispatch(setRemoteStreamId(null));
      this.store.dispatch(callEnded());
    }
  }

  private pushActivity(tone: 'info' | 'success' | 'warning', label: string, detail?: string): void {
    const nextItem: CallActivityItem = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      tone,
      label,
      detail,
      timestamp: Date.now()
    };
    this.updateState({
      activityLog: [...this.state.activityLog, nextItem].slice(-8)
    });
  }

  private getParticipantLabel(userId?: string): string {
    if (!userId) {
      return 'The other side';
    }
    return this.participantDirectory.get(userId)?.userName || this.state.remoteUserName || userId;
  }

  private formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}

export const webRTCService = new WebRTCService();
