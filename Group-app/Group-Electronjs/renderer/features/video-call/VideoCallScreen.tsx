import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { formatClockTime, useI18n } from '../../i18n';
import { useVideoCall } from './useVideoCall';
import { VideoCallStatus } from './videoCallSlice';
import { RootState } from '../../store';
import './VideoCallScreen.css';

interface VideoCallScreenProps {
  onCallEnd: () => void;
  remoteUserId?: string;
  remoteUserName?: string;
  remoteAvatar?: string;
}

const getStatusLabel = (status: VideoCallStatus, isMeeting: boolean, t: ReturnType<typeof useI18n>['t']) => {
  switch (status) {
    case VideoCallStatus.OUTGOING:
      return isMeeting ? t('videoCall.status.outgoing.meeting') : t('videoCall.status.outgoing.call');
    case VideoCallStatus.PRE_JOIN:
      return t('videoCall.status.preJoin');
    case VideoCallStatus.CONNECTING:
      return isMeeting ? t('videoCall.status.connecting.meeting') : t('videoCall.status.connecting.call');
    case VideoCallStatus.INCOMING:
      return isMeeting ? t('videoCall.status.incoming.meeting') : t('videoCall.status.incoming.call');
    case VideoCallStatus.ACTIVE:
      return isMeeting ? t('videoCall.status.active.meeting') : t('videoCall.status.active.call');
    default:
      return isMeeting ? t('videoCall.status.preparing.meeting') : t('videoCall.status.preparing.call');
  }
};

const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

const formatDiagnosticNumber = (value?: number, digits = 2) =>
  typeof value === 'number' && Number.isFinite(value) ? value.toFixed(digits) : '--';

const getDiagnosticStatusLabel = (
  callState: ReturnType<typeof useVideoCall>['state'],
  hasRemoteVideo: boolean,
  t: ReturnType<typeof useI18n>['t']
) => {
  if (callState.errorMessage) {
    return t('videoCall.debug.error');
  }
  if (hasRemoteVideo) {
    return t('videoCall.debug.remoteReady');
  }
  if (callState.diagnostics.some((item) => item.connectionState === 'failed' || item.iceConnectionState === 'failed')) {
    return t('videoCall.debug.iceFailed');
  }
  if (callState.diagnostics.some((item) => item.remoteTrackCount > 0)) {
    return t('videoCall.debug.trackDetected');
  }
  if (callState.diagnostics.some((item) => item.hasRemoteDescription)) {
    return t('videoCall.debug.waitingForTrack');
  }
  return t('videoCall.debug.gathering');
};

const getParticipantStateLabel = (state?: string) => {
  switch (state) {
    case 'connected':
      return '已连接';
    case 'connecting':
      return '连接中';
    case 'disconnected':
      return '已断开';
    case 'failed':
      return '连接失败';
    case 'closed':
      return '已关闭';
    case 'new':
      return '准备中';
    case 'idle':
    default:
      return '等待中';
  }
};

const getLocalizedParticipantStateLabel = (
  state: string | undefined,
  t: ReturnType<typeof useI18n>['t']
) => {
  switch (state) {
    case 'connected':
      return t('videoCall.participant.connected');
    case 'connecting':
      return t('videoCall.participant.connecting');
    case 'disconnected':
      return t('videoCall.participant.disconnected');
    case 'failed':
      return t('videoCall.participant.failed');
    case 'closed':
      return t('videoCall.participant.closed');
    case 'new':
      return t('videoCall.participant.new');
    case 'idle':
    default:
      return t('videoCall.participant.idle');
  }
};

const MicIcon: React.FC<{ muted?: boolean }> = ({ muted }) => (
  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    {muted ? (
      <>
        <path d="M9 9v3a3 3 0 0 0 5.12 2.12" />
        <path d="M15 9V4a3 3 0 0 0-5.77-1.16" />
        <path d="M17 13a5 5 0 0 1-8.43 3.64" />
        <path d="M12 19v4" />
        <path d="M8 23h8" />
        <path d="M1 1l22 22" />
      </>
    ) : (
      <>
        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
        <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
        <path d="M12 19v4" />
        <path d="M8 23h8" />
      </>
    )}
  </svg>
);

const CameraIcon: React.FC<{ off?: boolean; size?: number }> = ({ off, size = 24 }) => (
  <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    {off ? (
      <>
        <path d="M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2" />
        <path d="M10 5h4a2 2 0 0 1 2 2v3" />
        <path d="M23 7l-7 5 7 5V7z" />
        <path d="M1 1l22 22" />
      </>
    ) : (
      <>
        <path d="M23 7l-7 5 7 5V7z" />
        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
      </>
    )}
  </svg>
);

/**
 * 统一处理 video.srcObject 绑定。
 * WebRTC 场景里流对象会比 DOM 元素更早或更晚到达，这里集中做兜底，
 * 避免某个 useEffect 漏掉后出现“已经 connected 但画面没挂上”的情况。
 */
const bindMediaStreamToVideo = (
  element: HTMLVideoElement | null,
  stream: MediaStream | null,
  scope: string
) => {
  if (!element) {
    return;
  }

  if (!stream) {
    if (element.srcObject) {
      console.log('[VideoCallScreen]', { scope, action: 'clear-srcObject' });
    }
    element.srcObject = null;
    return;
  }

  if (element.srcObject !== stream) {
    console.log('[VideoCallScreen]', {
      scope,
      action: 'bind-srcObject',
      streamId: stream.id,
      trackCount: stream.getTracks().length
    });
    element.srcObject = stream;
  }

  element.play().catch((error) => console.warn(`${scope} video play failed:`, error));
};

const VideoCallScreen: React.FC<VideoCallScreenProps> = ({
  onCallEnd,
  remoteUserId,
  remoteUserName,
  remoteAvatar
}) => {
  const { t } = useI18n();
  const {
    state: callState,
    localStream,
    remoteStream,
    remoteParticipants,
    acceptCall,
    rejectCall,
    endCall,
    dismissCallSummary,
    toggleCamera,
    toggleMicrophone,
    toggleSpeaker,
    setRelayOnlyDebug,
    setVideoQualityPreset,
    onCallEnded,
    onError
  } = useVideoCall();

  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRefs = useRef<Record<string, HTMLVideoElement | null>>({});
  const floatingWindowRef = useRef<HTMLDivElement>(null);
  const dragStartRef = useRef({ x: 0, y: 0, pointerX: 0, pointerY: 0 });
  const dragMovedRef = useRef(false);
  const [floatingPos, setFloatingPos] = useState({ x: 20, y: 20 });
  const [isDragging, setIsDragging] = useState(false);
  const [snapSide, setSnapSide] = useState<'left' | 'right'>('left');
  const [debugExpanded, setDebugExpanded] = useState(false);
  const [qualityUpdating, setQualityUpdating] = useState(false);
  const [debugMode, setDebugMode] = useState<'compact' | 'verbose'>(() => {
    if (typeof window === 'undefined' || !window.localStorage) {
      return 'compact';
    }
    return window.localStorage.getItem('group.webrtc.debugPanelMode') === 'verbose' ? 'verbose' : 'compact';
  });

  const attachPrimaryRemoteVideoRef = (element: HTMLVideoElement | null) => {
    // React 在不同阶段会多次回调 ref，这里集中补绑远端流，避免时序问题。
    (remoteVideoRef as React.MutableRefObject<HTMLVideoElement | null>).current = element;
    bindMediaStreamToVideo(element, remoteStream, 'primary-remote-ref');
  };

  const dispatch = useDispatch();
  const reduxState = useSelector((state: RootState) => state.videoCall);
  const isMinimized = reduxState?.isMinimized || false;

  useEffect(() => {
    bindMediaStreamToVideo(localVideoRef.current, localStream, 'local-preview');
  }, [localStream, isMinimized, callState.callStatus]);

  useEffect(() => {
    bindMediaStreamToVideo(remoteVideoRef.current, remoteStream, 'primary-remote');
  }, [remoteStream, isMinimized, callState.callStatus]);

  useEffect(() => {
    remoteParticipants.forEach((participant) => {
      const element = remoteVideoRefs.current[participant.userId];
      bindMediaStreamToVideo(element, participant.stream, `participant-${participant.userId}`);
    });
  }, [remoteParticipants, isMinimized]);

  useEffect(() => {
    const offEnded = onCallEnded(() => {
      // keep summary panel on screen; no immediate close
    });
    const offError = onError((error) => {
      console.error('Video call error:', error);
    });
    return () => {
      offEnded();
      offError();
    };
  }, [onCallEnded, onError, onCallEnd]);

  useEffect(() => {
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.setItem('group.webrtc.debugPanelMode', debugMode);
    }
  }, [debugMode]);

  const displayName = callState.remoteUserName || remoteUserName || callState.remoteUserId || remoteUserId || 'Unknown User';
  const displayAvatar = callState.remoteAvatar || remoteAvatar;
  const activeRemoteParticipants = remoteParticipants.filter((participant) => participant.stream);
  const isMeetingMode = callState.isMeeting || activeRemoteParticipants.length > 1 || callState.participants.filter((participant) => !participant.isLocal).length > 1;
  const statusLabel = getStatusLabel(callState.callStatus, isMeetingMode, t);
  const participantCount = Math.max(callState.participants.length, activeRemoteParticipants.length + (localStream ? 1 : 0));
  const sessionSummary = callState.sessionSummary;
  const endedOrFailed = callState.callStatus === VideoCallStatus.ENDED || callState.callStatus === VideoCallStatus.ERROR;
  const hasRemoteVideo = activeRemoteParticipants.length > 0 || Boolean(remoteStream);
  const shouldShowDebugPanel = !endedOrFailed && (
    callState.callStatus === VideoCallStatus.CONNECTING
    || callState.callStatus === VideoCallStatus.OUTGOING
    || callState.callStatus === VideoCallStatus.PRE_JOIN
    || Boolean(callState.errorMessage)
    || !hasRemoteVideo
  );
  const debugStatusLabel = getDiagnosticStatusLabel(callState, hasRemoteVideo, t);

  const rosterParticipants = useMemo(() => {
    const participants = [...callState.participants];
    return participants.sort((left, right) => {
      if (left.isLocal && !right.isLocal) return -1;
      if (!left.isLocal && right.isLocal) return 1;
      return (left.userName || left.userId).localeCompare(right.userName || right.userId);
    });
  }, [callState.participants]);

  const handleAccept = () => {
    acceptCall();
  };

  const handleDismissSummary = () => {
    dismissCallSummary();
    onCallEnd();
  };

  const mediaStatusChips = useMemo(() => {
    const chips: string[] = [];
    if (!callState.isCameraAvailable) {
      chips.push(t('videoCall.media.noCamera'));
    } else if (!callState.isLocalVideoEnabled) {
      chips.push(t('videoCall.media.cameraOff'));
    }

    if (!callState.isMicrophoneAvailable) {
      chips.push(t('videoCall.media.noMicrophone'));
    } else if (!callState.isMicrophoneEnabled) {
      chips.push(t('videoCall.media.microphoneMuted'));
    }
    return chips;
  }, [
    t,
    callState.isCameraAvailable,
    callState.isLocalVideoEnabled,
    callState.isMicrophoneAvailable,
    callState.isMicrophoneEnabled
  ]);

  const debugPeers = useMemo(
    () => [...callState.diagnostics].sort((left, right) => (right.lastUpdatedAt || 0) - (left.lastUpdatedAt || 0)),
    [callState.diagnostics]
  );

  const qualityOptions = useMemo(
    () => ([
      { value: 'fast', label: t('videoCall.quality.fast'), description: t('videoCall.quality.desc.fast') },
      { value: 'balanced', label: t('videoCall.quality.balanced'), description: t('videoCall.quality.desc.balanced') },
      { value: 'hd', label: t('videoCall.quality.hd'), description: t('videoCall.quality.desc.hd') }
    ] as const),
    [t]
  );

  const handleVideoQualityChange = async (preset: 'fast' | 'balanced' | 'hd') => {
    if (preset === callState.videoQualityPreset || qualityUpdating) {
      return;
    }

    setQualityUpdating(true);
    try {
      await setVideoQualityPreset(preset);
    } finally {
      setQualityUpdating(false);
    }
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!isMinimized) return;
    setIsDragging(true);
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    dragStartRef.current = {
      x: rect.left,
      y: rect.top,
      pointerX: event.clientX,
      pointerY: event.clientY
    };
    dragMovedRef.current = false;
    event.currentTarget.setPointerCapture?.(event.pointerId);
  };

  const clampFloatingPosition = (x: number, y: number) => {
    const element = floatingWindowRef.current;
    const width = element?.offsetWidth ?? 320;
    const height = element?.offsetHeight ?? 88;
    const maxX = Math.max(12, window.innerWidth - width - 12);
    const maxY = Math.max(12, window.innerHeight - height - 12);
    return {
      x: Math.min(Math.max(12, x), maxX),
      y: Math.min(Math.max(12, y), maxY)
    };
  };

  const getSnappedFloatingPosition = (x: number, y: number) => {
    const clamped = clampFloatingPosition(x, y);
    const element = floatingWindowRef.current;
    const width = element?.offsetWidth ?? 320;
    const maxX = Math.max(12, window.innerWidth - width - 12);
    const snapThreshold = 56;
    const distanceToLeft = clamped.x - 12;
    const distanceToRight = maxX - clamped.x;

    if (distanceToLeft <= snapThreshold || distanceToLeft <= distanceToRight) {
      return { x: 12, y: clamped.y, side: 'left' as const };
    }

    return { x: maxX, y: clamped.y, side: 'right' as const };
  };

  useEffect(() => {
    const handlePointerMove = (event: PointerEvent) => {
      if (!isDragging) return;
      const deltaX = event.clientX - dragStartRef.current.pointerX;
      const deltaY = event.clientY - dragStartRef.current.pointerY;
      if (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4) {
        dragMovedRef.current = true;
      }
      setFloatingPos(
        clampFloatingPosition(
          dragStartRef.current.x + deltaX,
          dragStartRef.current.y + deltaY
        )
      );
    };
    const handlePointerUp = () => {
      const snapped = getSnappedFloatingPosition(floatingPos.x, floatingPos.y);
      setFloatingPos({ x: snapped.x, y: snapped.y });
      setSnapSide(snapped.side);
      window.setTimeout(() => {
        setIsDragging(false);
      }, 0);
    };

    if (isDragging) {
      window.addEventListener('pointermove', handlePointerMove);
      window.addEventListener('pointerup', handlePointerUp);
    }

    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
    };
  }, [floatingPos.x, floatingPos.y, isDragging]);

  useEffect(() => {
    if (!isMinimized) {
      return;
    }

    const handleResize = () => {
      const snapped = getSnappedFloatingPosition(floatingPos.x, floatingPos.y);
      setFloatingPos({ x: snapped.x, y: snapped.y });
      setSnapSide(snapped.side);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [floatingPos.x, floatingPos.y, isMinimized]);

  const handleMinimize = () => {
    dispatch({ type: 'videoCall/minimizeCall' });
  };

  const handleRestore = () => {
    dispatch({ type: 'videoCall/restoreCall' });
  };

  if (isMinimized) {
    return (
      <div
        ref={floatingWindowRef}
        className={`video-call-floating-window ${isDragging ? 'dragging' : ''} snap-${snapSide}`}
        style={{
          left: `${floatingPos.x}px`,
          top: `${floatingPos.y}px`
        }}
        onPointerDown={handlePointerDown}
        onClick={() => {
          if (dragMovedRef.current) return;
          handleRestore();
        }}
      >
        <div className="floating-video-container">
          {remoteStream && callState.callStatus === VideoCallStatus.ACTIVE ? (
            <video
              ref={remoteVideoRef}
              autoPlay
              playsInline
              onLoadedMetadata={(event) => event.currentTarget.play()}
            />
          ) : (
            <div className="floating-avatar">
              {displayAvatar ? <img src={displayAvatar} alt="" /> : displayName.charAt(0).toUpperCase()}
            </div>
          )}
          <div className="floating-info">
            <span className={`dot ${callState.callStatus === VideoCallStatus.ACTIVE ? 'active' : 'idle'}`}></span>
            <span className="timer">
              {callState.callStatus === VideoCallStatus.ACTIVE ? formatDuration(callState.duration) : statusLabel}
            </span>
          </div>
        </div>
        <div className="floating-controls-overlay">
          <button className="mini-action-btn end" onClick={(event) => { event.stopPropagation(); endCall(); }}>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" /></svg>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`video-call-workspace status-${callState.callStatus.toLowerCase()} ${isMeetingMode ? 'meeting-mode' : 'call-mode'}`}>
      <div className="main-video-area">
        {isMeetingMode ? (
          activeRemoteParticipants.length > 0 && callState.callStatus === VideoCallStatus.ACTIVE ? (
            <div className={`meeting-grid participants-${Math.min(activeRemoteParticipants.length, 4)}`}>
              {activeRemoteParticipants.map((participant, index) => (
                <div key={participant.userId} className={`meeting-tile ${index === 0 ? 'meeting-tile-primary' : ''}`}>
                  <video
                    ref={(element) => {
                      remoteVideoRefs.current[participant.userId] = element;
                      bindMediaStreamToVideo(element, participant.stream, `participant-ref-${participant.userId}`);
                    }}
                    autoPlay
                    playsInline
                    className="remote-video-full"
                    onLoadedMetadata={(event) => event.currentTarget.play()}
                  />
                  <div className="meeting-tile-label">{participant.userName || participant.userId}</div>
                </div>
              ))}
            </div>
          ) : (
            <div className="call-gradient-bg meeting-gradient-bg">
              <div className="blurry-circle circle-1"></div>
              <div className="blurry-circle circle-2"></div>
              <div className="blurry-circle circle-3"></div>
            </div>
          )
        ) : remoteStream && callState.callStatus === VideoCallStatus.ACTIVE ? (
          <video
            ref={attachPrimaryRemoteVideoRef}
            autoPlay
            playsInline
            className="remote-video-full"
            onLoadedMetadata={(event) => event.currentTarget.play()}
          />
        ) : (
          <div className="call-gradient-bg">
            <div className="blurry-circle circle-1"></div>
            <div className="blurry-circle circle-2"></div>
            <div className="blurry-circle circle-3"></div>
          </div>
        )}
      </div>

      <div className="call-ui-overlay">
        {endedOrFailed && (
          <div className="call-summary-panel">
            <div className="call-summary-card">
              <div className="call-summary-header">
                <div>
                  <div className="call-summary-eyebrow">{t('videoCall.summary.eyebrow')}</div>
                  <h2>{sessionSummary?.title || t('videoCall.summary.defaultTitle')}</h2>
                  <p>{sessionSummary?.detail || callState.errorMessage || t('videoCall.summary.defaultBody')}</p>
                </div>
                <button className="summary-close-btn" onClick={handleDismissSummary}>{t('common.close')}</button>
              </div>

              <div className="call-summary-stats">
                <div className="summary-stat">
                  <span className="summary-stat-label">{t('videoCall.summary.duration')}</span>
                  <strong>{formatDuration(sessionSummary?.durationSeconds || callState.duration)}</strong>
                </div>
                <div className="summary-stat">
                  <span className="summary-stat-label">{t('videoCall.summary.result')}</span>
                  <strong>{sessionSummary?.connected ? t('videoCall.summary.connected') : t('videoCall.summary.notConnected')}</strong>
                </div>
                <div className="summary-stat">
                  <span className="summary-stat-label">{t('videoCall.summary.endedBy')}</span>
                  <strong>{sessionSummary?.endedBy || t('common.system')}</strong>
                </div>
              </div>

              <div className="activity-timeline">
                {callState.activityLog.map((item) => (
                  <div key={item.id} className={`activity-row tone-${item.tone}`}>
                    <div className="activity-marker"></div>
                    <div className="activity-copy">
                      <div className="activity-main">
                        <span>{item.label}</span>
                        <time>{formatClockTime(item.timestamp)}</time>
                      </div>
                      {item.detail ? <div className="activity-detail">{item.detail}</div> : null}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {!endedOrFailed && callState.mediaNotice && (
          <div className="call-media-notice" role="status">
            <div className="call-media-notice-icon">
              <CameraIcon off={!callState.isCameraAvailable} size={18} />
            </div>
            <div>
              <strong>{t('videoCall.media.fallbackTitle')}</strong>
              <span>{callState.mediaNotice}</span>
            </div>
          </div>
        )}

        {shouldShowDebugPanel && (
          <div className={`call-debug-panel ${debugExpanded ? 'expanded' : ''}`}>
            <div className="call-debug-header">
              <div>
                <div className="call-debug-title">{t('videoCall.debug.title')}</div>
                <div className="call-debug-subtitle">{debugStatusLabel}</div>
              </div>
              <div className="call-debug-actions">
                <button
                  type="button"
                  className={`call-debug-chip ${debugMode === 'compact' ? 'active' : ''}`}
                  onClick={() => setDebugMode('compact')}
                >
                  {t('videoCall.debug.compact')}
                </button>
                <button
                  type="button"
                  className={`call-debug-chip ${debugMode === 'verbose' ? 'active' : ''}`}
                  onClick={() => setDebugMode('verbose')}
                >
                  {t('videoCall.debug.verbose')}
                </button>
                <button
                  type="button"
                  className={`call-debug-chip ${callState.relayOnlyForced ? 'active warning' : ''}`}
                  onClick={() => setRelayOnlyDebug(!callState.relayOnlyForced)}
                >
                  {t('videoCall.debug.forceRelay')}
                </button>
                <button
                  type="button"
                  className="call-debug-toggle"
                  onClick={() => setDebugExpanded((value) => !value)}
                >
                  {debugExpanded ? t('videoCall.debug.hide') : t('videoCall.debug.show')}
                </button>
              </div>
            </div>

            <div className="call-debug-summary">
              <div className="call-debug-pill">
                <span>{t('videoCall.debug.signaling')}</span>
                <strong>{callState.signalingConnectionState || '--'}</strong>
              </div>
              <div className="call-debug-pill">
                <span>{t('videoCall.debug.remoteVideo')}</span>
                <strong>{hasRemoteVideo ? t('videoCall.debug.available') : t('videoCall.debug.missing')}</strong>
              </div>
              <div className="call-debug-pill">
                <span>{t('videoCall.debug.remoteTrack')}</span>
                <strong>{debugPeers.reduce((sum, item) => sum + item.remoteTrackCount, 0)}</strong>
              </div>
              <div className="call-debug-pill">
                <span>{t('videoCall.debug.peerCount')}</span>
                <strong>{debugPeers.length}</strong>
              </div>
            </div>

            {debugExpanded && (
              <div className="call-debug-details">
                {debugPeers.length === 0 ? (
                  <div className="call-debug-empty">{t('videoCall.debug.noPeer')}</div>
                ) : (
                  debugPeers.map((peer) => (
                    <div key={peer.remoteUserId} className="call-debug-card">
                      <div className="call-debug-card-head">
                        <strong>{peer.remoteUserName || peer.remoteUserId}</strong>
                        <span>{peer.remoteUserId}</span>
                      </div>
                      <div className="call-debug-grid">
                        <div><span>{t('videoCall.debug.connection')}</span><strong>{peer.connectionState || '--'}</strong></div>
                        <div><span>{t('videoCall.debug.iceConnection')}</span><strong>{peer.iceConnectionState || '--'}</strong></div>
                        <div><span>{t('videoCall.debug.iceGathering')}</span><strong>{peer.iceGatheringState || '--'}</strong></div>
                        <div><span>{t('videoCall.debug.signalingState')}</span><strong>{peer.signalingState || '--'}</strong></div>
                        <div><span>{t('videoCall.debug.remoteDesc')}</span><strong>{peer.hasRemoteDescription ? t('videoCall.debug.available') : t('videoCall.debug.missing')}</strong></div>
                        <div><span>{t('videoCall.debug.localDesc')}</span><strong>{peer.hasLocalDescription ? t('videoCall.debug.available') : t('videoCall.debug.missing')}</strong></div>
                        <div><span>{t('videoCall.debug.queuedCandidates')}</span><strong>{peer.queuedRemoteCandidateCount}</strong></div>
                        <div><span>{t('videoCall.debug.receivedCandidates')}</span><strong>{peer.receivedRemoteCandidateCount}</strong></div>
                        <div><span>{t('videoCall.debug.localCandidates')}</span><strong>{peer.localCandidateCount}</strong></div>
                        <div><span>{t('videoCall.debug.relayCandidates')}</span><strong>{peer.localRelayCandidateCount}</strong></div>
                        <div><span>{t('videoCall.debug.remoteTrack')}</span><strong>{peer.remoteTrackCount}</strong></div>
                        <div><span>{t('videoCall.debug.rtt')}</span><strong>{formatDiagnosticNumber(peer.currentRoundTripTime)}</strong></div>
                      </div>
                      {debugMode === 'verbose' && (
                        <div className="call-debug-meta">
                          <div className="call-debug-meta-row">
                            <span>{t('videoCall.debug.selectedLocal')}</span>
                            <strong>{peer.selectedLocalCandidate?.type || '--'} / {peer.selectedLocalCandidate?.protocol || '--'}</strong>
                          </div>
                          <div className="call-debug-meta-row">
                            <span>{t('videoCall.debug.selectedRemote')}</span>
                            <strong>{peer.selectedRemoteCandidate?.type || '--'} / {peer.selectedRemoteCandidate?.protocol || '--'}</strong>
                          </div>
                          <div className="call-debug-meta-row">
                            <span>{t('videoCall.debug.selectedPair')}</span>
                            <strong>{peer.selectedPairState || '--'}</strong>
                          </div>
                          <div className="call-debug-meta-row">
                            <span>{t('videoCall.debug.remoteStreamId')}</span>
                            <strong>{peer.remoteStreamId || '--'}</strong>
                          </div>
                          {peer.candidateError ? (
                            <div className="call-debug-warning">{peer.candidateError}</div>
                          ) : null}
                        </div>
                      )}
                    </div>
                  ))
                )}

                <div className="call-debug-activity">
                  <div className="call-debug-activity-title">{t('videoCall.debug.recentTimeline')}</div>
                  {callState.activityLog.slice(-4).map((item) => (
                    <div key={item.id} className="call-debug-activity-row">
                      <span>{item.label}</span>
                      <time>{formatClockTime(item.timestamp)}</time>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {isMeetingMode ? (
          <div className="meeting-session-shell">
            <div className="meeting-session-main">
              <div className="call-header">
                <div className="meeting-session-meta">
                  <span className="meeting-room-pill">{t('videoCall.meeting.roomPill')}</span>
                    <div>
                      <div className="meeting-session-title">{callState.roomId || t('videoCall.meeting.defaultRoom')}</div>
                      <div className="meeting-session-copy">
                      {callState.callStatus === VideoCallStatus.ACTIVE ? (
                        <span className="duration-timer">
                          <span className="live-dot"></span>
                          {formatDuration(callState.duration)}
                        </span>
                      ) : (
                        <span className="status-label">{statusLabel}</span>
                      )}
                      <span className="meeting-session-divider">/</span>
                      <span>{t('videoCall.meeting.participants', { count: Math.max(participantCount, 1) })}</span>
                      {mediaStatusChips.length > 0 && (
                        <>
                          <span className="meeting-session-divider">/</span>
                          <span>{mediaStatusChips.join(' / ')}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="header-right">
                  <div className="quality-selector" aria-label={t('videoCall.quality.title')}>
                    <span className="quality-selector-label">{t('videoCall.quality.title')}</span>
                    <div className="quality-selector-options">
                      {qualityOptions.map((option) => (
                        <button
                          key={option.value}
                          type="button"
                          className={`quality-option-btn ${callState.videoQualityPreset === option.value ? 'active' : ''}`}
                          onClick={() => void handleVideoQualityChange(option.value)}
                          disabled={qualityUpdating}
                          title={option.description}
                        >
                          {option.label}
                        </button>
                      ))}
                    </div>
                  </div>
                  <button className="minimize-btn" title={t('videoCall.controls.minimize')} onClick={handleMinimize}>
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <path d="M4 14h6m0 0v6m0-6L3 21m17-11h-6m0 0V4m0 6l7-7"></path>
                    </svg>
                  </button>
                </div>
              </div>

              {callState.callStatus !== VideoCallStatus.ACTIVE && (
                <div className="meeting-center-stage">
                  <div className="meeting-stage-card">
                    <div className="avatar-pulse-container">
                      <div className="pulse-ring ring-1"></div>
                      <div className="pulse-ring ring-2"></div>
                      <div className="avatar-giant">
                        {displayAvatar ? <img src={displayAvatar} alt="" /> : 'M'}
                      </div>
                    </div>
                    <h2 className="display-name-large">{t('videoCall.meeting.preparingRoom')}</h2>
                    <div className="premium-status-badge">
                      <div className="status-dot"></div>
                      <p className="status-message">{statusLabel}</p>
                    </div>
                  </div>
                </div>
              )}

              <div className={`local-preview-card ${callState.callStatus === VideoCallStatus.ACTIVE ? 'pip' : 'init'} ${endedOrFailed ? 'hidden-preview' : ''}`}>
                <video
                  ref={localVideoRef}
                  autoPlay
                  playsInline
                  muted
                  onLoadedMetadata={(event) => event.currentTarget.play()}
                  className={`local-video-element ${!callState.isLocalVideoEnabled ? 'hidden' : ''}`}
                />
                {!callState.isLocalVideoEnabled && (
                  <div className="camera-off-msg">
                    <CameraIcon off size={32} />
                    <span>{callState.isCameraAvailable ? t('videoCall.media.cameraTurnedOff') : t('videoCall.media.cameraUnavailable')}</span>
                  </div>
                )}
              </div>

              {!endedOrFailed && <div className="call-action-bar">
                <div className="actions-wrapper">
                  <button
                    className={`action-fab ${!callState.isMicrophoneEnabled ? 'off' : ''}`}
                    onClick={() => toggleMicrophone(!callState.isMicrophoneEnabled)}
                    title={callState.isMicrophoneEnabled ? t('videoCall.controls.mute') : t('videoCall.controls.unmute')}
                  >
                    <MicIcon muted={!callState.isMicrophoneEnabled} />
                  </button>

                  <button
                    className={`action-fab ${!callState.isLocalVideoEnabled ? 'off' : ''}`}
                    onClick={() => toggleCamera(!callState.isLocalVideoEnabled)}
                    title={callState.isLocalVideoEnabled ? t('videoCall.controls.cameraOff') : t('videoCall.controls.cameraOn')}
                  >
                    <CameraIcon off={!callState.isLocalVideoEnabled} />
                  </button>

                  <button className="action-fab end-call" onClick={() => endCall()} title={t('videoCall.meeting.leave')}>
                    <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                      <path d="M22.21 17.3l-5.11-2.12c-.52-.22-1.12-.1-1.51.3l-2.01 2.01c-2.43-1.25-4.42-3.24-5.67-5.67l2.01-2.01c.39-.39.52-.99.3-1.51L8.1 3.23a1.5 1.5 0 0 0-1.74-.88L2.43 3.55a1.5 1.5 0 0 0-1.1 1.45c0 9.17 7.46 16.64 16.64 16.64a1.5 1.5 0 0 0 1.45-1.1l1.2-3.93a1.5 1.5 0 0 0-.88-1.74z" transform="rotate(135 12 12)"></path>
                    </svg>
                  </button>

                  <button className={`action-fab ${!callState.isSpeakerEnabled ? 'off' : ''}`} onClick={() => toggleSpeaker(!callState.isSpeakerEnabled)} title={t('videoCall.controls.speaker')}>
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                    </svg>
                  </button>
                </div>
              </div>}
            </div>

            <aside className="meeting-side-panel">
              <div className="meeting-side-title">{t('videoCall.meeting.participantsTitle')}</div>
              <div className="meeting-side-subtitle">{t('videoCall.meeting.participantsSubtitle')}</div>
              <div className="meeting-roster">
                {rosterParticipants.map((participant) => (
                  <div key={participant.userId} className={`meeting-roster-item ${participant.isLocal ? 'local' : ''}`}>
                    <div className="meeting-roster-avatar">
                      {participant.avatar ? <img src={participant.avatar} alt="" /> : (participant.userName || participant.userId).charAt(0).toUpperCase()}
                    </div>
                    <div className="meeting-roster-copy">
                      <div className="meeting-roster-name">
                        {participant.userName || participant.userId}
                        {participant.isLocal ? <span className="meeting-you-badge">{t('videoCall.meeting.you')}</span> : null}
                      </div>
                      <div className="meeting-roster-state">{getLocalizedParticipantStateLabel(participant.connectionState, t)}</div>
                    </div>
                  </div>
                ))}
              </div>
            </aside>
          </div>
        ) : (
          <>
            <div className="call-header">
              <div className="remote-user-badge">
                <div className="avatar-small">
                  {displayAvatar ? <img src={displayAvatar} alt="" /> : displayName.charAt(0).toUpperCase()}
                </div>
                <div className="user-text">
                  <div className="username">{displayName}</div>
                  <div className="call-status-tag">
                    {callState.callStatus === VideoCallStatus.ACTIVE ? (
                      <span className="duration-timer">
                        <span className="live-dot"></span>
                        {formatDuration(callState.duration)}
                      </span>
                    ) : (
                      <span className="status-label">{statusLabel}</span>
                    )}
                  </div>
                  {mediaStatusChips.length > 0 ? (
                    <div className="call-media-chip-row">
                      {mediaStatusChips.map((chip) => (
                        <span key={chip} className="call-media-chip">{chip}</span>
                      ))}
                    </div>
                  ) : null}
                </div>
              </div>

              <div className="header-right">
                <div className="quality-selector" aria-label={t('videoCall.quality.title')}>
                  <span className="quality-selector-label">{t('videoCall.quality.title')}</span>
                  <div className="quality-selector-options">
                    {qualityOptions.map((option) => (
                      <button
                        key={option.value}
                        type="button"
                        className={`quality-option-btn ${callState.videoQualityPreset === option.value ? 'active' : ''}`}
                        onClick={() => void handleVideoQualityChange(option.value)}
                        disabled={qualityUpdating}
                        title={option.description}
                      >
                        {option.label}
                      </button>
                    ))}
                  </div>
                </div>
                <button className="minimize-btn" title={t('videoCall.controls.minimize')} onClick={handleMinimize}>
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <path d="M4 14h6m0 0v6m0-6L3 21m17-11h-6m0 0V4m0 6l7-7"></path>
                  </svg>
                </button>
              </div>
            </div>

            {callState.callStatus !== VideoCallStatus.ACTIVE && (
              <div className="call-center-stage">
                <div className="avatar-pulse-container">
                  <div className="pulse-ring ring-1"></div>
                  <div className="pulse-ring ring-2"></div>
                  <div className="avatar-giant">
                    {displayAvatar ? <img src={displayAvatar} alt="" /> : displayName.charAt(0).toUpperCase()}
                  </div>
                </div>
                <h2 className="display-name-large">{displayName}</h2>
                <div className="premium-status-badge">
                  <div className="status-dot"></div>
                  <p className="status-message">{statusLabel}</p>
                </div>
              </div>
            )}

            <div className={`local-preview-card ${callState.callStatus === VideoCallStatus.ACTIVE ? 'pip' : 'init'} ${endedOrFailed ? 'hidden-preview' : ''}`}>
              <video
                ref={localVideoRef}
                autoPlay
                playsInline
                muted
                onLoadedMetadata={(event) => event.currentTarget.play()}
                className={`local-video-element ${!callState.isLocalVideoEnabled ? 'hidden' : ''}`}
              />
              {!callState.isLocalVideoEnabled && (
                <div className="camera-off-msg">
                  <CameraIcon off size={32} />
                  <span>{callState.isCameraAvailable ? t('videoCall.media.cameraTurnedOff') : t('videoCall.media.cameraUnavailable')}</span>
                </div>
              )}
            </div>

            {!endedOrFailed && <div className="call-action-bar compact">
              <div className="actions-wrapper compact">
                <button
                  className={`action-fab ${!callState.isMicrophoneEnabled ? 'off' : ''}`}
                  onClick={() => toggleMicrophone(!callState.isMicrophoneEnabled)}
                  title={callState.isMicrophoneEnabled ? t('videoCall.controls.mute') : t('videoCall.controls.unmute')}
                >
                  <MicIcon muted={!callState.isMicrophoneEnabled} />
                </button>

                <button
                  className={`action-fab ${!callState.isLocalVideoEnabled ? 'off' : ''}`}
                  onClick={() => toggleCamera(!callState.isLocalVideoEnabled)}
                  title={callState.isLocalVideoEnabled ? t('videoCall.controls.cameraOff') : t('videoCall.controls.cameraOn')}
                >
                  <CameraIcon off={!callState.isLocalVideoEnabled} />
                </button>

                <button className="action-fab end-call" onClick={() => endCall()} title={t('videoCall.controls.endCall')}>
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                    <path d="M22.21 17.3l-5.11-2.12c-.52-.22-1.12-.1-1.51.3l-2.01 2.01c-2.43-1.25-4.42-3.24-5.67-5.67l2.01-2.01c.39-.39.52-.99.3-1.51L8.1 3.23a1.5 1.5 0 0 0-1.74-.88L2.43 3.55a1.5 1.5 0 0 0-1.1 1.45c0 9.17 7.46 16.64 16.64 16.64a1.5 1.5 0 0 0 1.45-1.1l1.2-3.93a1.5 1.5 0 0 0-.88-1.74z" transform="rotate(135 12 12)"></path>
                  </svg>
                </button>
              </div>
            </div>}
          </>
        )}
      </div>

      {(callState.callStatus === VideoCallStatus.INCOMING || callState.callStatus === VideoCallStatus.PRE_JOIN) && (
        <div className="incoming-modal-backdrop">
          <div className="incoming-card">
            <div className="caller-profile">
              <div className="avatar-med">
                {displayAvatar ? <img src={displayAvatar} alt="" /> : displayName.charAt(0).toUpperCase()}
              </div>
              <h3>{displayName}</h3>
              <p>
                {callState.callStatus === VideoCallStatus.PRE_JOIN
                  ? t('videoCall.incoming.preJoin')
                  : isMeetingMode ? t('videoCall.incoming.meeting') : t('videoCall.incoming.call')}
              </p>
            </div>
            <div className="modal-actions">
              <button className="modal-btn accept" onClick={handleAccept}>
                <div className="icon-circle">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                    <path d="M20 15.5c-1.2 0-2.4-.2-3.6-.6-.3-.1-.7 0-1 .3l-2.2 2.2c-2.8-1.4-5.1-3.8-6.6-6.6l2.2-2.2c.3-.3.4-.7.2-1-.3-1.1-.5-2.3-.5-3.5 0-.5-.4-.9-.9-.9H4c-.5 0-1 .4-1 .9 0 9.4 7.6 17 17 17 .5 0 .9-.4.9-.9v-3.5c0-.5-.4-.9-.9-.9z"></path>
                  </svg>
                </div>
                <span>{callState.callStatus === VideoCallStatus.PRE_JOIN || isMeetingMode ? t('videoCall.incoming.join') : t('videoCall.incoming.accept')}</span>
              </button>
              <button className="modal-btn reject" onClick={() => rejectCall()}>
                <div className="icon-circle">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                    <path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.58.9-.98.45-1.87 1.05-2.65 1.76-.17.16-.34.22-.52.22-.17 0-.35-.07-.48-.2l-3.37-3.37c-.13-.13-.2-.3-.2-.48s.07-.35.2-.48C3.36 8.35 7.42 6 12 6s8.64 2.35 12.19 5.39c.13.13.2.3.2.48s-.07.35-.2.48l-3.37 3.37c-.13.13-.3.2-.48.2s-.35-.07-.48-.2c-.78-.71-1.67-1.31-2.65-1.76-.35-.16-.58-.51-.58-.9v-3.1c-1.45-.47-3-.72-4.6-.72z"></path>
                  </svg>
                </div>
                <span>{callState.callStatus === VideoCallStatus.PRE_JOIN ? t('videoCall.incoming.dismiss') : t('videoCall.incoming.decline')}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VideoCallScreen;
