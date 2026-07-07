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
import { translate } from '../i18n';

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

export interface SelectedCandidateDiagnostic {
  type?: string;
  protocol?: string;
  address?: string;
  port?: number;
  relayProtocol?: string;
}

export interface PeerConnectionDiagnostic {
  remoteUserId: string;
  remoteUserName?: string;
  connectionState?: RTCPeerConnectionState | 'idle';
  iceConnectionState?: RTCIceConnectionState;
  iceGatheringState?: RTCIceGatheringState;
  signalingState?: RTCSignalingState;
  hasLocalDescription: boolean;
  hasRemoteDescription: boolean;
  queuedRemoteCandidateCount: number;
  receivedRemoteCandidateCount: number;
  localCandidateCount: number;
  localRelayCandidateCount: number;
  remoteTrackCount: number;
  remoteStreamId?: string;
  selectedPairState?: string;
  currentRoundTripTime?: number;
  selectedLocalCandidate?: SelectedCandidateDiagnostic | null;
  selectedRemoteCandidate?: SelectedCandidateDiagnostic | null;
  candidateError?: string;
  lastTrackAt?: number;
  lastUpdatedAt: number;
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
  isCameraAvailable: boolean;
  isMicrophoneAvailable: boolean;
  isMeeting: boolean;
  errorMessage?: string;
  mediaNotice?: string;
  activityLog: CallActivityItem[];
  sessionSummary?: CallSessionSummary;
  isInitiator: boolean;
  signalingConnectionState?: 'disconnected' | 'connecting' | 'reconnecting' | 'connected';
  relayOnlyForced: boolean;
  diagnostics: PeerConnectionDiagnostic[];
}

interface SignalingConnectionConfig {
  host: string;
  port: number;
  userId: string;
  token: string;
  pageProtocol: string;
}

const DEFAULT_ICE_SERVERS: RTCIceServer[] = [{ urls: 'stun:stun.l.google.com:19302' }];
const CALL_SETUP_TIMEOUT_MS = 30_000;

const RELAY_ONLY_DEBUG_STORAGE_KEY = 'group.webrtc.forceRelayOnly';

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
  private setupTimeout: ReturnType<typeof setTimeout> | null = null;
  private signalingConfig: SignalingConnectionConfig | null = null;
  private signalingUnsubscribe: (() => void) | null = null;
  private signalingStateUnsubscribe: (() => void) | null = null;
  private peerDiagnostics = new Map<string, PeerConnectionDiagnostic>();

  private state: CallInternalState = {
    callStatus: VideoCallStatus.IDLE,
    participants: [],
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: false,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true,
    isCameraAvailable: true,
    isMicrophoneAvailable: true,
    isMeeting: false,
    activityLog: [],
    isInitiator: false,
    relayOnlyForced: false,
    diagnostics: []
  };

  constructor(iceServers: RTCIceServer[] = DEFAULT_ICE_SERVERS) {
    super();
    this.iceServers = iceServers;
  }

  /**
   * 公网排障时，我们经常需要强制 WebRTC 只走 TURN relay，
   * 这样可以快速区分“直连候选失败”还是“TURN 本身不可用”。
   * 这里通过 localStorage 开关控制，避免每次都改代码。
   */
  private isRelayOnlyDebugEnabled(): boolean {
    if (typeof window === 'undefined' || !window.localStorage) {
      return false;
    }

    return window.localStorage.getItem(RELAY_ONLY_DEBUG_STORAGE_KEY) === 'true';
  }

  /**
   * 统一格式化 ICE server 日志，避免浏览器把 urls/object 混在一起时难读。
   */
  private describeIceServers(): Array<Record<string, unknown>> {
    return this.iceServers.map((server) => ({
      urls: server.urls,
      username: server.username,
      hasCredential: Boolean(server.credential)
    }));
  }

  /**
   * 在连接成功或失败时抓一份候选对摘要。
   * 重点看是否真的出现 relay 候选，以及最终选中的 candidate pair 是哪一对。
   */
  private async logIceCandidateSummary(
    peerConnection: RTCPeerConnection,
    remoteUserId: string,
    scope: string
  ): Promise<void> {
    try {
      const stats = await peerConnection.getStats();
      const localCandidates = new Map<string, RTCStats>();
      const remoteCandidates = new Map<string, RTCStats>();
      const candidatePairs: RTCStats[] = [];

      stats.forEach((report) => {
        if (report.type === 'local-candidate') {
          localCandidates.set(report.id, report);
          return;
        }

        if (report.type === 'remote-candidate') {
          remoteCandidates.set(report.id, report);
          return;
        }

        if (report.type === 'candidate-pair') {
          candidatePairs.push(report);
        }
      });

      const selectedPair = candidatePairs.find((report: any) => report.selected) ||
        candidatePairs.find((report: any) => report.nominated);

      const summarizeCandidate = (candidate?: any) => candidate ? {
        id: candidate.id,
        type: candidate.candidateType,
        protocol: candidate.protocol,
        address: candidate.address || candidate.ip,
        port: candidate.port,
        relayProtocol: candidate.relayProtocol
      } : null;

      const relayLocalCandidates = [...localCandidates.values()]
        .filter((candidate: any) => candidate.candidateType === 'relay')
        .map((candidate: any) => summarizeCandidate(candidate));

      const relayRemoteCandidates = [...remoteCandidates.values()]
        .filter((candidate: any) => candidate.candidateType === 'relay')
        .map((candidate: any) => summarizeCandidate(candidate));

      const selectedLocal = selectedPair ? localCandidates.get((selectedPair as any).localCandidateId) : undefined;
      const selectedRemote = selectedPair ? remoteCandidates.get((selectedPair as any).remoteCandidateId) : undefined;
      const selectedLocalSummary = summarizeCandidate(selectedLocal as any);
      const selectedRemoteSummary = summarizeCandidate(selectedRemote as any);

      this.upsertPeerDiagnostic(remoteUserId, {
        selectedPairState: selectedPair ? String((selectedPair as any).state || '') : undefined,
        currentRoundTripTime: selectedPair ? Number((selectedPair as any).currentRoundTripTime || 0) : undefined,
        selectedLocalCandidate: selectedLocalSummary,
        selectedRemoteCandidate: selectedRemoteSummary
      });

      this.log(scope, {
        remoteUserId,
        relayOnlyDebug: this.isRelayOnlyDebugEnabled(),
        selectedPair: selectedPair ? {
          state: (selectedPair as any).state,
          nominated: (selectedPair as any).nominated,
          bytesSent: (selectedPair as any).bytesSent,
          bytesReceived: (selectedPair as any).bytesReceived,
          currentRoundTripTime: (selectedPair as any).currentRoundTripTime
        } : null,
        selectedLocalCandidate: selectedLocalSummary,
        selectedRemoteCandidate: selectedRemoteSummary,
        relayLocalCandidates,
        relayRemoteCandidates
      });
    } catch (error) {
      console.warn('[WebRTCService] failed to collect ICE summary', {
        scope,
        remoteUserId,
        error
      });
    }
  }

  /**
   * Keep WebRTC diagnostics consistent so browser console logs can be matched
   * against webrtc-internals timestamps during call setup debugging.
   */
  private log(scope: string, details?: Record<string, unknown>): void {
    console.log('[WebRTCService]', details ? { scope, ...details } : { scope });
  }

  private t(key: Parameters<typeof translate>[0], values?: Parameters<typeof translate>[1]): string {
    return translate(key, values);
  }

  public getState(): CallInternalState {
    return {
      ...this.state,
      participants: [...this.state.participants],
      diagnostics: [...this.state.diagnostics]
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
      this.updateState({
        signalingConnectionState: meetingSignalingService.getConnectionState(),
        relayOnlyForced: this.isRelayOnlyDebugEnabled()
      });
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
    this.signalingStateUnsubscribe = meetingSignalingService.onConnectionStateChange((connectionState) => {
      this.updateState({ signalingConnectionState: connectionState });
    });
    this.updateState({
      signalingConnectionState: meetingSignalingService.getConnectionState(),
      relayOnlyForced: this.isRelayOnlyDebugEnabled()
    });
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
    const cameraConstraints = { width: 1280, height: 720, frameRate: 30 };
    const attempts: Array<{
      label: string;
      constraints: MediaStreamConstraints;
      notice?: string;
      cameraAvailable: boolean;
      microphoneAvailable: boolean;
    }> = [
      {
        label: 'camera-and-microphone',
        constraints: { video: cameraConstraints, audio: true },
        cameraAvailable: true,
        microphoneAvailable: true
      },
      {
        label: 'microphone-only',
        constraints: { video: false, audio: true },
        notice: this.t('service.media.cameraUnavailableMicOnly'),
        cameraAvailable: false,
        microphoneAvailable: true
      },
      {
        label: 'camera-only',
        constraints: { video: cameraConstraints, audio: false },
        notice: this.t('service.media.microphoneUnavailableCameraOnly'),
        cameraAvailable: true,
        microphoneAvailable: false
      }
    ];

    const failures: string[] = [];
    for (const attempt of attempts) {
      try {
        this.releaseLocalStream();
        const stream = await this.requestUserMedia(attempt.constraints);
        this.applyLocalStream(stream, {
          isCameraAvailable: attempt.cameraAvailable,
          isMicrophoneAvailable: attempt.microphoneAvailable,
          mediaNotice: attempt.notice,
          reconnectTracks: true
        });
        if (attempt.notice) {
          this.pushActivity('warning', this.t('service.media.modeLimited'), attempt.notice);
        }
        this.log('acquire-local-media-success', {
          mode: attempt.label,
          streamId: stream.id,
          trackKinds: stream.getTracks().map((track) => track.kind)
        });
        return stream;
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        failures.push(`${attempt.label}: ${message}`);
        this.log('acquire-local-media-failed', {
          mode: attempt.label,
          message
        });
      }
    }

    const receiveOnlyStream = new MediaStream();
    const fallbackNotice = this.t('service.media.noneAvailableReceiveOnly');
    this.applyLocalStream(receiveOnlyStream, {
      isCameraAvailable: false,
      isMicrophoneAvailable: false,
      mediaNotice: fallbackNotice,
      reconnectTracks: true
    });
    this.pushActivity('warning', this.t('service.media.joinWithoutLocal'), fallbackNotice);
    this.log('acquire-local-media-receive-only', { failures });
    return receiveOnlyStream;
  }

  private syncLocalStreamToPeers(): void {
    this.peerConnections.forEach((peerConnection) => {
      const localTracks = this.localStream?.getTracks() || [];
      const sendersByKind = new Map<string, RTCRtpSender[]>();

      peerConnection.getSenders().forEach((sender) => {
        const kind = sender.track?.kind;
        if (!kind) {
          return;
        }
        const list = sendersByKind.get(kind) || [];
        list.push(sender);
        sendersByKind.set(kind, list);
      });

      localTracks.forEach((track) => {
        const matchingSender = (sendersByKind.get(track.kind) || []).find((sender) => sender.track !== track);
        if (matchingSender) {
          void matchingSender.replaceTrack(track);
          return;
        }

        peerConnection.addTrack(track, this.localStream!);
      });

      ['audio', 'video'].forEach((kind) => {
        const hasTrack = localTracks.some((track) => track.kind === kind);
        if (hasTrack) {
          return;
        }

        (sendersByKind.get(kind) || []).forEach((sender) => {
          void sender.replaceTrack(null);
        });
      });
    });
  }

  private releaseLocalStream(): void {
    if (!this.localStream) {
      return;
    }

    this.localStream.getTracks().forEach((track) => track.stop());
    this.localStream = null;
  }

  private applyLocalStream(
    stream: MediaStream,
    options?: {
      isCameraAvailable?: boolean;
      isMicrophoneAvailable?: boolean;
      mediaNotice?: string;
      reconnectTracks?: boolean;
    }
  ): void {
    this.localStream = stream;

    if (options?.reconnectTracks) {
      this.syncLocalStreamToPeers();
    }

    if (this.store) {
      this.store.dispatch(setLocalStreamId(stream.id || null));
    }

    const hasVideoTrack = stream.getVideoTracks().length > 0;
    const hasAudioTrack = stream.getAudioTracks().length > 0;

    this.upsertParticipant({
      userId: this.userId,
      userName: this.store?.getState().auth.user?.username,
      avatar: this.store?.getState().auth.user?.avatar,
      isLocal: true,
      streamId: stream.id || undefined,
      connectionState: 'idle'
    });

    this.updateState({
      isLocalVideoEnabled: hasVideoTrack && stream.getVideoTracks().some((track) => track.enabled),
      isMicrophoneEnabled: hasAudioTrack && stream.getAudioTracks().some((track) => track.enabled),
      isCameraAvailable: options?.isCameraAvailable ?? hasVideoTrack,
      isMicrophoneAvailable: options?.isMicrophoneAvailable ?? hasAudioTrack,
      mediaNotice: options?.mediaNotice
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
    this.stopSetupTimeout();
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
    this.pushActivity(
      'info',
      this.t('service.call.incoming'),
      this.t('service.call.incomingDetail', { name: message.fromUserName || message.fromUser || this.t('service.call.otherSide') })
    );

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
    this.stopSetupTimeout();
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
    this.pushActivity('info', this.t('service.call.inviteOpened'), this.t('service.call.joiningDetail', { roomId: invite.roomId }));
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
    this.startSetupTimeout(this.t('service.call.joining'));
    this.pushActivity('info', this.t('service.call.participantsSynced'), this.t('service.call.participantsSyncedDetail', { count: participants.length }));
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
    this.pushActivity(
      'info',
      this.t('service.call.participantJoined', { name: message.userName || message.fromUserName || participantId }),
      this.t('service.call.participantJoinedDetail')
    );

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
        title: this.t('service.call.endedByOther'),
        detail: this.t('service.call.endedByRemoteDetail', { name: participantName, duration: this.formatDuration(this.state.duration) })
      });
      return;
    }

    this.pushActivity(
      'warning',
      this.t('service.call.participantLeft', { name: participantName }),
      this.t('service.call.participantLeftDetail')
    );
  }

  private handleMeetingRejected(message: WebrtcMessage): void {
    const rejectedUserId = message.fromUser;
    if (rejectedUserId) {
      this.removeParticipantConnection(rejectedUserId);
    }

    if (this.getRemoteParticipantStreams().length === 0 && this.state.callStatus === VideoCallStatus.OUTGOING) {
      this.finishCall({
        endedBy: 'remote',
        title: this.t('service.call.declined'),
        detail: message.reason || this.t('service.call.incomingDetail', { name: this.getParticipantLabel(rejectedUserId) })
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
      this.upsertPeerDiagnostic(remoteUserId, {
        signalingState: peerConnection.signalingState,
        hasLocalDescription: Boolean(peerConnection.localDescription)
      });

      this.sendSignalingMessage({
        type: SIGNALING_MESSAGE_TYPES.OFFER,
        fromUser: this.userId,
        toUser: remoteUserId,
        roomId: this.state.roomId,
        sdp: offer.sdp || '',
        sdpType: SIGNALING_SDP_TYPES.OFFER
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
      this.pushActivity(
        'info',
        this.t('service.call.offerSent'),
        this.t('service.call.offerSentDetail', { name: this.getParticipantLabel(remoteUserId) })
      );
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
      this.upsertPeerDiagnostic(remoteUserId, {
        signalingState: peerConnection.signalingState,
        hasRemoteDescription: Boolean(peerConnection.remoteDescription),
        queuedRemoteCandidateCount: (this.pendingIceCandidates.get(remoteUserId) || []).length
      });

      const queuedCandidates = this.pendingIceCandidates.get(remoteUserId) || [];
      for (const candidate of queuedCandidates) {
        await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      }
      this.pendingIceCandidates.delete(remoteUserId);
      this.upsertPeerDiagnostic(remoteUserId, {
        queuedRemoteCandidateCount: 0
      });

      const answer = await peerConnection.createAnswer();
      await peerConnection.setLocalDescription(answer);
      this.upsertPeerDiagnostic(remoteUserId, {
        signalingState: peerConnection.signalingState,
        hasLocalDescription: Boolean(peerConnection.localDescription)
      });

      this.sendSignalingMessage({
        type: SIGNALING_MESSAGE_TYPES.ANSWER,
        fromUser: this.userId,
        toUser: remoteUserId,
        roomId: this.state.roomId || message.roomId,
        sdp: answer.sdp || '',
        sdpType: SIGNALING_SDP_TYPES.ANSWER
      });

      this.updateState({ callStatus: VideoCallStatus.CONNECTING });
      this.pushActivity(
        'info',
        this.t('service.call.answerSent'),
        this.t('service.call.answerSentDetail', { name: this.getParticipantLabel(remoteUserId) })
      );
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

      /**
       * answer 只能在本地已经发出 offer、并且 signalingState 仍处于
       * have-local-offer 时设置。
       * 公网弱网下可能会收到重复 answer，或者通话结束后的迟到 answer，
       * 这两类都不应该再触发 setRemoteDescription，否则会报
       * "Called in wrong state: stable"。
       */
      if (peerConnection.signalingState === 'stable' && peerConnection.remoteDescription) {
        this.log('handle-answer-ignored-stable', {
          remoteUserId,
          signalingState: peerConnection.signalingState,
          remoteDescriptionType: peerConnection.remoteDescription.type
        });
        return;
      }

      if (peerConnection.signalingState !== 'have-local-offer') {
        this.log('handle-answer-ignored-unexpected-state', {
          remoteUserId,
          signalingState: peerConnection.signalingState,
          hasRemoteDescription: Boolean(peerConnection.remoteDescription),
          hasLocalDescription: Boolean(peerConnection.localDescription)
        });
        return;
      }

      this.log('handle-answer-start', {
        remoteUserId,
        queuedCandidateCount: (this.pendingIceCandidates.get(remoteUserId) || []).length,
        signalingState: peerConnection.signalingState
      });

      await peerConnection.setRemoteDescription(new RTCSessionDescription({
        type: SIGNALING_SDP_TYPES.ANSWER,
        sdp: message.sdp
      }));
      this.upsertPeerDiagnostic(remoteUserId, {
        signalingState: peerConnection.signalingState,
        hasRemoteDescription: Boolean(peerConnection.remoteDescription),
        queuedRemoteCandidateCount: (this.pendingIceCandidates.get(remoteUserId) || []).length
      });

      const queuedCandidates = this.pendingIceCandidates.get(remoteUserId) || [];
      for (const candidate of queuedCandidates) {
        await peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
      }
      this.pendingIceCandidates.delete(remoteUserId);
      this.upsertPeerDiagnostic(remoteUserId, {
        queuedRemoteCandidateCount: 0,
        iceConnectionState: peerConnection.iceConnectionState
      });
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
    this.upsertPeerDiagnostic(remoteUserId, {
      receivedRemoteCandidateCount: (this.peerDiagnostics.get(remoteUserId)?.receivedRemoteCandidateCount || 0) + 1
    });
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
    this.upsertPeerDiagnostic(remoteUserId, {
      queuedRemoteCandidateCount: pending.length
    });
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
      iceServers: this.describeIceServers(),
      hasLocalStream: !!this.localStream,
      relayOnlyDebug: this.isRelayOnlyDebugEnabled()
    });
    const peerConnection = new RTCPeerConnection({
      iceServers: this.iceServers,
      iceTransportPolicy: this.isRelayOnlyDebugEnabled() ? 'relay' : 'all'
    });
    this.peerConnections.set(remoteUserId, peerConnection);
    this.upsertPeerDiagnostic(remoteUserId, {
      remoteUserName: this.participantDirectory.get(remoteUserId)?.userName,
      connectionState: peerConnection.connectionState,
      iceConnectionState: peerConnection.iceConnectionState,
      iceGatheringState: peerConnection.iceGatheringState,
      signalingState: peerConnection.signalingState,
      hasLocalDescription: Boolean(peerConnection.localDescription),
      hasRemoteDescription: Boolean(peerConnection.remoteDescription),
      queuedRemoteCandidateCount: this.pendingIceCandidates.get(remoteUserId)?.length || 0
    });

    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        peerConnection.addTrack(track, this.localStream!);
      });
    }

    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.upsertPeerDiagnostic(remoteUserId, {
          localCandidateCount: (this.peerDiagnostics.get(remoteUserId)?.localCandidateCount || 0) + 1,
          localRelayCandidateCount: (this.peerDiagnostics.get(remoteUserId)?.localRelayCandidateCount || 0)
            + (event.candidate.type === 'relay' ? 1 : 0)
        });
        this.log('peer-onicecandidate', {
          remoteUserId,
          candidateType: event.candidate.type,
          protocol: event.candidate.protocol,
          sdpMid: event.candidate.sdpMid,
          candidate: event.candidate.candidate
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

    peerConnection.onicecandidateerror = (event) => {
      this.upsertPeerDiagnostic(remoteUserId, {
        candidateError: `${event.errorCode || 'unknown'} ${event.errorText || ''}`.trim()
      });
      this.pushActivity(
        'warning',
        this.t('service.debug.iceCandidateError'),
        `${this.getParticipantLabel(remoteUserId)}: ${event.errorCode || 'unknown'} ${event.errorText || ''}`.trim()
      );
      this.log('peer-onicecandidateerror', {
        remoteUserId,
        url: event.url,
        address: event.address,
        port: event.port,
        errorCode: event.errorCode,
        errorText: event.errorText
      });
    };

    peerConnection.onicegatheringstatechange = () => {
      this.upsertPeerDiagnostic(remoteUserId, {
        iceGatheringState: peerConnection.iceGatheringState
      });
      this.log('peer-ice-gathering-state-change', {
        remoteUserId,
        iceGatheringState: peerConnection.iceGatheringState
      });
    };

    peerConnection.oniceconnectionstatechange = () => {
      this.upsertPeerDiagnostic(remoteUserId, {
        iceConnectionState: peerConnection.iceConnectionState,
        connectionState: peerConnection.connectionState
      });
      this.log('peer-ice-connection-state-change', {
        remoteUserId,
        iceConnectionState: peerConnection.iceConnectionState,
        connectionState: peerConnection.connectionState
      });
    };

    peerConnection.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        const stream = event.streams[0];
        this.log('peer-ontrack', {
          remoteUserId,
          streamId: stream.id,
          trackCount: stream.getTracks().length
        });
        this.upsertPeerDiagnostic(remoteUserId, {
          remoteStreamId: stream.id,
          remoteTrackCount: stream.getTracks().length,
          lastTrackAt: Date.now()
        });
        this.remoteStreams.set(remoteUserId, stream);
        this.upsertParticipant({
          userId: remoteUserId,
          streamId: stream.id,
          connectionState: peerConnection.connectionState
        });
        this.emit('remote-streams-change', this.getRemoteParticipantStreams());
        // 同时派发单路远端流事件，兼容仍然依赖旧单人通话流模型的界面层。
        this.emit('remote-stream', stream);

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
      this.upsertPeerDiagnostic(remoteUserId, {
        connectionState: peerConnection.connectionState,
        iceConnectionState: peerConnection.iceConnectionState,
        signalingState: peerConnection.signalingState,
        hasLocalDescription: Boolean(peerConnection.localDescription),
        hasRemoteDescription: Boolean(peerConnection.remoteDescription),
        queuedRemoteCandidateCount: this.pendingIceCandidates.get(remoteUserId)?.length || 0
      });
      this.upsertParticipant({
        userId: remoteUserId,
        connectionState: peerConnection.connectionState
      });

      if (peerConnection.connectionState === 'connected') {
        void this.logIceCandidateSummary(peerConnection, remoteUserId, 'peer-ice-summary-connected');
        this.stopSetupTimeout();
        if (!this.state.callStartTime) {
          this.updateState({
            callStatus: VideoCallStatus.ACTIVE,
            callStartTime: Date.now()
          });
          this.pushActivity(
            'success',
            this.t('service.call.connected'),
            this.t('service.call.connectedDetail', { name: this.getParticipantLabel(remoteUserId) })
          );
          this.startDurationTimer();
          if (this.store) {
            this.store.dispatch(callConnected());
          }
        } else {
          this.updateState({ callStatus: VideoCallStatus.ACTIVE });
        }
      } else if (peerConnection.connectionState === 'failed') {
        void this.logIceCandidateSummary(peerConnection, remoteUserId, 'peer-ice-summary-failed');
        this.handleError(new Error(this.t('service.call.connectionFailed', { name: this.getParticipantLabel(remoteUserId) })));
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
      this.pushActivity('info', this.t('service.call.joining'), this.t('service.call.joiningDetail', { roomId }));

      // Render the pre-join / connecting surface first, then start media setup.
      await nextUiFrame();
      await this.initialize();
      if (!this.localStream) {
        await this.acquireLocalMedia();
      }

      this.sendMeetingJoin(roomId);
      this.startSetupTimeout(this.t('service.call.joining'));
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
        throw new Error(this.t('service.call.noParticipants'));
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
        targets.length > 1 ? this.t('service.call.meetingInviteSent') : this.t('service.call.callingStarted'),
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
      this.startSetupTimeout(targets.length > 1 ? this.t('videoCall.status.outgoing.meeting') : this.t('videoCall.status.outgoing.call'));
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  private sendMeetingJoin(roomId: string): void {
    this.pushActivity('info', this.t('service.call.joinedRoom'), roomId);
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
      this.startSetupTimeout(this.t('videoCall.status.connecting.call'));
    } catch (error) {
      this.handleError(error as Error);
    }
  }

  public rejectCall(): void {
    if (this.state.callStatus !== VideoCallStatus.INCOMING && this.state.callStatus !== VideoCallStatus.PRE_JOIN) {
      return;
    }

    if (this.state.remoteUserId) {
      this.pushActivity('warning', this.t('service.call.declined'), this.t('service.call.declinedByYou'));
      this.sendSignalingMessage({
        type: SIGNALING_MESSAGE_TYPES.MEETING_REJECT,
        fromUser: this.userId,
        toUser: this.state.remoteUserId,
        roomId: this.state.roomId,
        reason: this.t('service.call.declined')
      });
    }
    this.finishCall({
      endedBy: 'local',
      title: this.t('service.call.declined'),
      detail: this.t('service.call.declinedByYou')
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
      title: connected ? this.t('service.call.ended') : this.t('service.call.cancelled'),
      detail: connected
        ? this.t('service.call.endedByYouDetail', { duration: this.formatDuration(this.state.duration) })
        : this.t('service.call.cancelledByYouDetail')
    });
  }

  private handleRemoteHangup(remoteUserId?: string): void {
    const connected = Boolean(this.state.callStartTime);
    const remoteName = this.getParticipantLabel(remoteUserId || this.state.remoteUserId);
    if (remoteUserId) {
      this.removeParticipantConnection(remoteUserId);
      if (this.getRemoteParticipantStreams().length > 0) {
        this.pushActivity(
          'warning',
          this.t('service.call.participantLeft', { name: remoteName }),
          connected
            ? this.t('service.call.durationOnly', { duration: this.formatDuration(this.state.duration) })
            : this.t('service.call.leftBeforeConnected')
        );
        return;
      }
    }

    this.finishCall({
      endedBy: 'remote',
      title: connected ? this.t('service.call.endedByOther') : this.t('service.call.notAnswered'),
      detail: connected
        ? this.t('service.call.endedByRemoteDetail', { name: remoteName, duration: this.formatDuration(this.state.duration) })
        : this.t('service.call.endedBeforeConnectedDetail', { name: remoteName })
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
    if (!participant.isLocal && !current.isLocal) {
      this.upsertPeerDiagnostic(participant.userId, {
        remoteUserName: participant.userName ?? current.userName,
        connectionState: participant.connectionState ?? current.connectionState
      });
    }
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
    this.removePeerDiagnostic(userId);

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
      this.peerDiagnostics.clear();
      this.updateState({
        activityLog: [],
        sessionSummary: undefined,
        errorMessage: undefined,
        duration: 0,
        callStartTime: undefined,
        isInitiator: false,
        diagnostics: []
      });
    }
  }

  private updateState(updates: Partial<CallInternalState>): void {
    this.state = {
      ...this.state,
      ...updates,
      participants: updates.participants ?? this.state.participants,
      diagnostics: updates.diagnostics ?? this.state.diagnostics
    };
    this.emit('state-change', this.getState());
  }

  private upsertPeerDiagnostic(
    remoteUserId: string,
    updates: Partial<PeerConnectionDiagnostic>
  ): void {
    const previous = this.peerDiagnostics.get(remoteUserId) || {
      remoteUserId,
      hasLocalDescription: false,
      hasRemoteDescription: false,
      queuedRemoteCandidateCount: 0,
      receivedRemoteCandidateCount: 0,
      localCandidateCount: 0,
      localRelayCandidateCount: 0,
      remoteTrackCount: 0,
      lastUpdatedAt: Date.now()
    };

    this.peerDiagnostics.set(remoteUserId, {
      ...previous,
      ...updates,
      remoteUserId,
      lastUpdatedAt: Date.now()
    });
    this.updateState({
      diagnostics: [...this.peerDiagnostics.values()]
    });
  }

  private removePeerDiagnostic(remoteUserId: string): void {
    if (!this.peerDiagnostics.delete(remoteUserId)) {
      return;
    }

    this.updateState({
      diagnostics: [...this.peerDiagnostics.values()]
    });
  }

  private handleError(error: Error): void {
    console.error('WebRTCService Error:', error);
    this.stopSetupTimeout();
    this.pushActivity('warning', this.t('service.call.error'), error.message);
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

  private startSetupTimeout(context: string): void {
    this.stopSetupTimeout();
    this.setupTimeout = setTimeout(() => {
      if (this.state.callStartTime || this.state.callStatus === VideoCallStatus.ACTIVE) {
        return;
      }

      const detail = this.state.mediaNotice
        ? this.t('service.call.timeoutDetailWithNotice', { context, notice: this.state.mediaNotice })
        : this.t('service.call.timeoutDetail', { context });

      this.finishCall({
        endedBy: 'system',
        title: this.t('service.call.timeoutTitle'),
        detail
      });
    }, CALL_SETUP_TIMEOUT_MS);
  }

  private stopSetupTimeout(): void {
    if (this.setupTimeout) {
      clearTimeout(this.setupTimeout);
      this.setupTimeout = null;
    }
  }

  private cleanupCallState(resetError: boolean): void {
    this.stopDurationTimer();
    this.stopSetupTimeout();

    this.peerConnections.forEach((peerConnection) => {
      peerConnection.onicecandidate = null;
      peerConnection.ontrack = null;
      peerConnection.onconnectionstatechange = null;
      peerConnection.close();
    });
    this.peerConnections.clear();
    this.pendingIceCandidates.clear();
    this.remoteStreams.clear();
    this.peerDiagnostics.clear();

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
      isLocalVideoEnabled: true,
      isMicrophoneEnabled: true,
      isRemoteVideoEnabled: false,
      isCameraAvailable: true,
      isMicrophoneAvailable: true,
      isMeeting: false,
      activityLog: resetError ? [] : this.state.activityLog,
      sessionSummary: resetError ? undefined : this.state.sessionSummary,
      errorMessage: resetError ? undefined : this.state.errorMessage,
      mediaNotice: resetError ? undefined : this.state.mediaNotice,
      isInitiator: resetError ? false : this.state.isInitiator,
      diagnostics: []
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
    if (this.signalingStateUnsubscribe) {
      this.signalingStateUnsubscribe();
      this.signalingStateUnsubscribe = null;
    }
    this.initialized = false;
    this.cleanupCallState(true);
    this.removeAllListeners();
  }

  public toggleCamera(enabled: boolean): void {
    if (!enabled) {
      this.localStream?.getVideoTracks().forEach((track) => {
        track.enabled = false;
      });
      this.updateState({ isLocalVideoEnabled: false });
      return;
    }

    const existingTracks = this.localStream?.getVideoTracks() || [];
    if (existingTracks.length > 0) {
      existingTracks.forEach((track) => {
        track.enabled = true;
      });
      this.updateState({
        isLocalVideoEnabled: true,
        isCameraAvailable: true,
        mediaNotice: this.state.isMicrophoneAvailable ? undefined : this.state.mediaNotice
      });
      return;
    }

    void this.enableMissingTrack('video');
  }

  public toggleMicrophone(enabled: boolean): void {
    if (!enabled) {
      this.localStream?.getAudioTracks().forEach((track) => {
        track.enabled = false;
      });
      this.updateState({ isMicrophoneEnabled: false });
      return;
    }

    const existingTracks = this.localStream?.getAudioTracks() || [];
    if (existingTracks.length > 0) {
      existingTracks.forEach((track) => {
        track.enabled = true;
      });
      this.updateState({
        isMicrophoneEnabled: true,
        isMicrophoneAvailable: true,
        mediaNotice: this.state.isCameraAvailable ? undefined : this.state.mediaNotice
      });
      return;
    }

    void this.enableMissingTrack('audio');
  }

  public toggleSpeaker(enabled: boolean): void {
    this.updateState({ isSpeakerEnabled: enabled });
  }

  public setRelayOnlyDebug(enabled: boolean): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.setItem(RELAY_ONLY_DEBUG_STORAGE_KEY, enabled ? 'true' : 'false');
    }
    this.updateState({ relayOnlyForced: enabled });
    this.pushActivity(
      'info',
      enabled ? this.t('service.debug.relayOnlyEnabled') : this.t('service.debug.relayOnlyDisabled'),
      enabled ? this.t('service.debug.relayOnlyEnabledDetail') : this.t('service.debug.relayOnlyDisabledDetail')
    );
  }

  public dismissCallSummary(): void {
    this.cleanupCallState(true);
  }

  private finishCall(summary: {
    endedBy: 'local' | 'remote' | 'system';
    title: string;
    detail: string;
  }): void {
    this.stopSetupTimeout();
    const durationSeconds = this.state.duration;
    this.pushActivity(
      summary.endedBy === 'system' ? 'warning' : 'success',
      summary.title,
      durationSeconds > 0
        ? this.t('service.call.detailWithDuration', { detail: summary.detail, duration: this.formatDuration(durationSeconds) })
        : summary.detail
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
    this.peerDiagnostics.clear();

    this.releaseLocalStream();

    this.participantDirectory.clear();
    this.emit('remote-streams-change', []);

    this.updateState({
      callStatus: VideoCallStatus.ENDED,
      roomId: undefined,
      conversationId: undefined,
      callKind: undefined,
      participants: [],
      callStartTime: undefined,
      isLocalVideoEnabled: true,
      isMicrophoneEnabled: true,
      isRemoteVideoEnabled: false,
      isCameraAvailable: true,
      isMicrophoneAvailable: true,
      mediaNotice: undefined,
      diagnostics: [],
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
      return this.t('service.call.otherSide');
    }
    return this.participantDirectory.get(userId)?.userName || this.state.remoteUserName || userId;
  }

  private formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  private async enableMissingTrack(kind: 'audio' | 'video'): Promise<void> {
    try {
      const stream = await this.requestUserMedia(
        kind === 'video'
          ? { video: { width: 1280, height: 720, frameRate: 30 }, audio: false }
          : { video: false, audio: true }
      );
      const track = kind === 'video' ? stream.getVideoTracks()[0] : stream.getAudioTracks()[0];
      if (!track) {
        throw new Error(this.t('service.media.trackMissing', { kind: this.t(kind === 'video' ? 'service.media.kind.camera' : 'service.media.kind.microphone') }));
      }

      const baseStream = this.localStream || new MediaStream();
      baseStream.addTrack(track);
      this.localStream = baseStream;
      this.syncLocalStreamToPeers();

      this.updateState({
        isLocalVideoEnabled: kind === 'video' ? true : this.state.isLocalVideoEnabled,
        isMicrophoneEnabled: kind === 'audio' ? true : this.state.isMicrophoneEnabled,
        isCameraAvailable: kind === 'video' ? true : this.state.isCameraAvailable,
        isMicrophoneAvailable: kind === 'audio' ? true : this.state.isMicrophoneAvailable,
        mediaNotice:
          kind === 'video'
            ? (this.state.isMicrophoneAvailable ? undefined : this.t('service.media.microphoneUnavailableCameraOnly'))
            : (this.state.isCameraAvailable ? undefined : this.t('service.media.cameraUnavailableMicOnly'))
      });

      if (this.store) {
        this.store.dispatch(setLocalStreamId(baseStream.id || null));
      }

      this.upsertParticipant({
        userId: this.userId,
        userName: this.store?.getState().auth.user?.username,
        avatar: this.store?.getState().auth.user?.avatar,
        isLocal: true,
        streamId: baseStream.id || undefined,
        connectionState: 'idle'
      });
      this.pushActivity(
        'success',
        this.t(kind === 'video' ? 'service.media.restoredCamera' : 'service.media.restoredMicrophone'),
        this.t('service.media.localAvailable', { kind: this.t(kind === 'video' ? 'service.media.kind.camera' : 'service.media.kind.microphone') })
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      const notice =
        kind === 'video'
          ? this.t('service.media.retryCameraFailed')
          : this.t('service.media.retryMicrophoneFailed');
      this.updateState({
        isCameraAvailable: kind === 'video' ? false : this.state.isCameraAvailable,
        isMicrophoneAvailable: kind === 'audio' ? false : this.state.isMicrophoneAvailable,
        mediaNotice: notice
      });
      this.pushActivity(
        'warning',
        this.t(kind === 'video' ? 'videoCall.media.noCamera' : 'videoCall.media.noMicrophone'),
        message
      );
    }
  }
}

export const webRTCService = new WebRTCService();
