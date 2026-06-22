import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
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

const getStatusLabel = (status: VideoCallStatus, isMeeting: boolean) => {
  switch (status) {
    case VideoCallStatus.OUTGOING:
      return isMeeting ? 'Inviting participants...' : 'Calling...';
    case VideoCallStatus.PRE_JOIN:
      return 'Ready to join meeting';
    case VideoCallStatus.CONNECTING:
      return isMeeting ? 'Joining meeting room...' : 'Connecting...';
    case VideoCallStatus.INCOMING:
      return isMeeting ? 'Incoming meeting invite' : 'Incoming video call';
    case VideoCallStatus.ACTIVE:
      return isMeeting ? 'Meeting in progress' : 'Video call in progress';
    default:
      return isMeeting ? 'Preparing meeting...' : 'Preparing call...';
  }
};

const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

const formatClockTime = (timestamp: number): string =>
  new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

const VideoCallScreen: React.FC<VideoCallScreenProps> = ({
  onCallEnd,
  remoteUserId,
  remoteUserName,
  remoteAvatar
}) => {
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
    onError
  } = useVideoCall();

  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRefs = useRef<Record<string, HTMLVideoElement | null>>({});
  const [floatingPos, setFloatingPos] = useState({ x: 20, y: 20 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });

  const dispatch = useDispatch();
  const reduxState = useSelector((state: RootState) => state.videoCall);
  const isMinimized = reduxState?.isMinimized || false;

  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
      localVideoRef.current.play().catch((error) => console.warn('Local video play failed:', error));
    }
  }, [localStream, isMinimized]);

  useEffect(() => {
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
      remoteVideoRef.current.play().catch((error) => console.warn('Remote video play failed:', error));
    }
  }, [remoteStream, isMinimized]);

  useEffect(() => {
    remoteParticipants.forEach((participant) => {
      const element = remoteVideoRefs.current[participant.userId];
      if (element && participant.stream) {
        element.srcObject = participant.stream;
        element.play().catch((error) => console.warn('Remote participant video play failed:', error));
      }
    });
  }, [remoteParticipants, isMinimized]);

  useEffect(() => {
    onError((error) => {
      console.error('Video call error:', error);
    });
  }, [onError, onCallEnd]);

  const displayName = callState.remoteUserName || remoteUserName || callState.remoteUserId || remoteUserId || 'Unknown User';
  const displayAvatar = callState.remoteAvatar || remoteAvatar;
  const activeRemoteParticipants = remoteParticipants.filter((participant) => participant.stream);
  const isMeetingMode = callState.isMeeting || activeRemoteParticipants.length > 1 || callState.participants.filter((participant) => !participant.isLocal).length > 1;
  const statusLabel = getStatusLabel(callState.callStatus, isMeetingMode);
  const participantCount = Math.max(callState.participants.length, activeRemoteParticipants.length + (localStream ? 1 : 0));
  const sessionSummary = callState.sessionSummary;
  const endedOrFailed = callState.callStatus === VideoCallStatus.ENDED || callState.callStatus === VideoCallStatus.ERROR;

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

  const handleMouseDown = (event: React.MouseEvent) => {
    if (!isMinimized) return;
    setIsDragging(true);
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    setDragOffset({
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    });
  };

  useEffect(() => {
    const handleMouseMove = (event: MouseEvent) => {
      if (!isDragging) return;
      setFloatingPos({
        x: window.innerWidth - event.clientX - (150 - dragOffset.x),
        y: event.clientY - dragOffset.y
      });
    };
    const handleMouseUp = () => setIsDragging(false);

    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging, dragOffset]);

  const handleMinimize = () => {
    dispatch({ type: 'videoCall/minimizeCall' });
  };

  const handleRestore = () => {
    dispatch({ type: 'videoCall/restoreCall' });
  };

  if (isMinimized) {
    return (
      <div
        className={`video-call-floating-window ${isDragging ? 'dragging' : ''}`}
        style={{
          right: `${floatingPos.x}px`,
          top: `${floatingPos.y}px`
        }}
        onMouseDown={handleMouseDown}
        onClick={() => {
          if (isDragging) return;
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
                      if (element && participant.stream) {
                        element.srcObject = participant.stream;
                      }
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
            ref={remoteVideoRef}
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
                  <div className="call-summary-eyebrow">Call recap</div>
                  <h2>{sessionSummary?.title || 'Call finished'}</h2>
                  <p>{sessionSummary?.detail || callState.errorMessage || 'The call has ended.'}</p>
                </div>
                <button className="summary-close-btn" onClick={handleDismissSummary}>Close</button>
              </div>

              <div className="call-summary-stats">
                <div className="summary-stat">
                  <span className="summary-stat-label">Duration</span>
                  <strong>{formatDuration(sessionSummary?.durationSeconds || callState.duration)}</strong>
                </div>
                <div className="summary-stat">
                  <span className="summary-stat-label">Result</span>
                  <strong>{sessionSummary?.connected ? 'Connected' : 'Not connected'}</strong>
                </div>
                <div className="summary-stat">
                  <span className="summary-stat-label">Ended by</span>
                  <strong>{sessionSummary?.endedBy || 'system'}</strong>
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

        {isMeetingMode ? (
          <div className="meeting-session-shell">
            <div className="meeting-session-main">
              <div className="call-header">
                <div className="meeting-session-meta">
                  <span className="meeting-room-pill">Meeting</span>
                  <div>
                    <div className="meeting-session-title">{callState.roomId || 'Meeting room'}</div>
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
                      <span>{Math.max(participantCount, 1)} participants</span>
                    </div>
                  </div>
                </div>

                <div className="header-right">
                  <button className="minimize-btn" title="Minimize" onClick={handleMinimize}>
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
                    <h2 className="display-name-large">Preparing meeting room</h2>
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
                    <svg viewBox="0 0 24 24" width="32" height="32" fill="white" opacity="0.6">
                      <path d="M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m5.66 0H14a2 2 0 0 1 2 2v3.34M23 7l-7 5 7 5V7z" stroke="currentColor" strokeWidth="2"></path>
                      <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" strokeWidth="2"></line>
                    </svg>
                  </div>
                )}
              </div>

              {!endedOrFailed && <div className="call-action-bar">
                <div className="actions-wrapper">
                  <button
                    className={`action-fab ${!callState.isMicrophoneEnabled ? 'off' : ''}`}
                    onClick={() => toggleMicrophone(!callState.isMicrophoneEnabled)}
                    title={callState.isMicrophoneEnabled ? 'Mute microphone' : 'Unmute microphone'}
                  >
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                      {callState.isMicrophoneEnabled ? (
                        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3zM19 10v2a7 7 0 0 1-14 0v-2M12 19v4m-4 0h8"></path>
                      ) : (
                        <path d="M18.89 12v2a6.89 6.89 0 0 1-2.06 4.93m-1.95 1.5A7 7 0 0 1 5 13v-3m10.22-3.14l-4.24-4.24a3 3 0 0 1 4.24 4.24zM12 1a3 3 0 0 0-3 3v.17M12 19v4m-4 0h8"></path>
                      )}
                    </svg>
                  </button>

                  <button
                    className={`action-fab ${!callState.isLocalVideoEnabled ? 'off' : ''}`}
                    onClick={() => toggleCamera(!callState.isLocalVideoEnabled)}
                    title={callState.isLocalVideoEnabled ? 'Turn off camera' : 'Turn on camera'}
                  >
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                      {callState.isLocalVideoEnabled ? (
                        <path d="M23 7l-7 5 7 5V7zM16 11.5a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v4.5z"></path>
                      ) : (
                        <path d="M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m5.66 0H14a2 2 0 0 1 2 2v3.34M23 7l-7 5 7 5V7zM1 1l22 22"></path>
                      )}
                    </svg>
                  </button>

                  <button className="action-fab end-call" onClick={() => endCall()} title="Leave meeting">
                    <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                      <path d="M22.21 17.3l-5.11-2.12c-.52-.22-1.12-.1-1.51.3l-2.01 2.01c-2.43-1.25-4.42-3.24-5.67-5.67l2.01-2.01c.39-.39.52-.99.3-1.51L8.1 3.23a1.5 1.5 0 0 0-1.74-.88L2.43 3.55a1.5 1.5 0 0 0-1.1 1.45c0 9.17 7.46 16.64 16.64 16.64a1.5 1.5 0 0 0 1.45-1.1l1.2-3.93a1.5 1.5 0 0 0-.88-1.74z" transform="rotate(135 12 12)"></path>
                    </svg>
                  </button>

                  <button className={`action-fab ${!callState.isSpeakerEnabled ? 'off' : ''}`} onClick={() => toggleSpeaker(!callState.isSpeakerEnabled)} title="Speaker">
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                    </svg>
                  </button>
                </div>
              </div>}
            </div>

            <aside className="meeting-side-panel">
              <div className="meeting-side-title">Participants</div>
              <div className="meeting-side-subtitle">Keep track of who is in the room and who is still connecting.</div>
              <div className="meeting-roster">
                {rosterParticipants.map((participant) => (
                  <div key={participant.userId} className={`meeting-roster-item ${participant.isLocal ? 'local' : ''}`}>
                    <div className="meeting-roster-avatar">
                      {participant.avatar ? <img src={participant.avatar} alt="" /> : (participant.userName || participant.userId).charAt(0).toUpperCase()}
                    </div>
                    <div className="meeting-roster-copy">
                      <div className="meeting-roster-name">
                        {participant.userName || participant.userId}
                        {participant.isLocal ? <span className="meeting-you-badge">You</span> : null}
                      </div>
                      <div className="meeting-roster-state">{participant.connectionState || 'idle'}</div>
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
                </div>
              </div>

              <div className="header-right">
                <button className="minimize-btn" title="Minimize" onClick={handleMinimize}>
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
                  <svg viewBox="0 0 24 24" width="32" height="32" fill="white" opacity="0.6">
                    <path d="M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m5.66 0H14a2 2 0 0 1 2 2v3.34M23 7l-7 5 7 5V7z" stroke="currentColor" strokeWidth="2"></path>
                    <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" strokeWidth="2"></line>
                  </svg>
                </div>
              )}
            </div>

            {!endedOrFailed && <div className="call-action-bar compact">
              <div className="actions-wrapper compact">
                <button
                  className={`action-fab ${!callState.isMicrophoneEnabled ? 'off' : ''}`}
                  onClick={() => toggleMicrophone(!callState.isMicrophoneEnabled)}
                  title={callState.isMicrophoneEnabled ? 'Mute microphone' : 'Unmute microphone'}
                >
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                    {callState.isMicrophoneEnabled ? (
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3zM19 10v2a7 7 0 0 1-14 0v-2M12 19v4m-4 0h8"></path>
                    ) : (
                      <path d="M18.89 12v2a6.89 6.89 0 0 1-2.06 4.93m-1.95 1.5A7 7 0 0 1 5 13v-3m10.22-3.14l-4.24-4.24a3 3 0 0 1 4.24 4.24zM12 1a3 3 0 0 0-3 3v.17M12 19v4m-4 0h8"></path>
                    )}
                  </svg>
                </button>

                <button
                  className={`action-fab ${!callState.isLocalVideoEnabled ? 'off' : ''}`}
                  onClick={() => toggleCamera(!callState.isLocalVideoEnabled)}
                  title={callState.isLocalVideoEnabled ? 'Turn off camera' : 'Turn on camera'}
                >
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                    {callState.isLocalVideoEnabled ? (
                      <path d="M23 7l-7 5 7 5V7zM16 11.5a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v4.5z"></path>
                    ) : (
                      <path d="M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m5.66 0H14a2 2 0 0 1 2 2v3.34M23 7l-7 5 7 5V7zM1 1l22 22"></path>
                    )}
                  </svg>
                </button>

                <button className="action-fab end-call" onClick={() => endCall()} title="End call">
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
                  ? 'Meeting invite opened from notification. Join when you are ready.'
                  : isMeetingMode ? 'Incoming meeting invitation...' : 'Incoming video call...'}
              </p>
            </div>
            <div className="modal-actions">
              <button className="modal-btn accept" onClick={handleAccept}>
                <div className="icon-circle">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                    <path d="M20 15.5c-1.2 0-2.4-.2-3.6-.6-.3-.1-.7 0-1 .3l-2.2 2.2c-2.8-1.4-5.1-3.8-6.6-6.6l2.2-2.2c.3-.3.4-.7.2-1-.3-1.1-.5-2.3-.5-3.5 0-.5-.4-.9-.9-.9H4c-.5 0-1 .4-1 .9 0 9.4 7.6 17 17 17 .5 0 .9-.4.9-.9v-3.5c0-.5-.4-.9-.9-.9z"></path>
                  </svg>
                </div>
                <span>{callState.callStatus === VideoCallStatus.PRE_JOIN || isMeetingMode ? 'Join' : 'Accept'}</span>
              </button>
              <button className="modal-btn reject" onClick={() => rejectCall()}>
                <div className="icon-circle">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                    <path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.58.9-.98.45-1.87 1.05-2.65 1.76-.17.16-.34.22-.52.22-.17 0-.35-.07-.48-.2l-3.37-3.37c-.13-.13-.2-.3-.2-.48s.07-.35.2-.48C3.36 8.35 7.42 6 12 6s8.64 2.35 12.19 5.39c.13.13.2.3.2.48s-.07.35-.2.48l-3.37 3.37c-.13.13-.3.2-.48.2s-.35-.07-.48-.2c-.78-.71-1.67-1.31-2.65-1.76-.35-.16-.58-.51-.58-.9v-3.1c-1.45-.47-3-.72-4.6-.72z"></path>
                  </svg>
                </div>
                <span>{callState.callStatus === VideoCallStatus.PRE_JOIN ? 'Dismiss' : 'Decline'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VideoCallScreen;
