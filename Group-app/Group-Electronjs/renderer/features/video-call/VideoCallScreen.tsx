import React, { useEffect, useRef } from 'react';
import { useVideoCall } from './useVideoCall';
import { VideoCallStatus } from './videoCallSlice';
import './VideoCallScreen.css';

interface VideoCallScreenProps {
  onCallEnd: () => void;
  remoteUserId?: string;
  remoteUserName?: string;
}

const VideoCallScreen: React.FC<VideoCallScreenProps> = ({
  onCallEnd,
  remoteUserId,
  remoteUserName
}) => {
  const {
    state: callState,
    localStream,
    remoteStream,
    startCall,
    acceptCall,
    endCall,
    toggleCamera,
    toggleMicrophone,
    toggleSpeaker,
    onCallEnded,
    onError
  } = useVideoCall();

  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);

  // Handle video stream attachment
  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
    }
  }, [localStream]);

  useEffect(() => {
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
    }
  }, [remoteStream]);

  // Set up event listeners
  useEffect(() => {
    onCallEnded(() => {
      onCallEnd();
    });

    onError((error) => {
      console.error('Video call error:', error);
      onCallEnd();
    });
  }, [onCallEnded, onError, onCallEnd]);

  // Start call when remote user is provided
  useEffect(() => {
    if (remoteUserId && callState.callStatus === VideoCallStatus.IDLE) {
      startCall(remoteUserId);
    }
  }, [remoteUserId, callState.callStatus, startCall]);

  const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const displayName = remoteUserName || remoteUserId || 'Unknown User';

  return (
    <div className={`video-call-workspace status-${callState.callStatus.toLowerCase()}`}>
      {/* Background Layer: Remote Video or Gradient */}
      <div className="main-video-area">
        {remoteStream && callState.callStatus === VideoCallStatus.ACTIVE ? (
          <video
            ref={remoteVideoRef}
            autoPlay
            playsInline
            className="remote-video-full"
          />
        ) : (
          <div className="call-gradient-bg">
            <div className="blurry-circle circle-1"></div>
            <div className="blurry-circle circle-2"></div>
          </div>
        )}
      </div>

      {/* Foreground Content */}
      <div className="call-ui-overlay">
        {/* Top Header: User Info & Duration */}
        <div className="call-header">
           <div className="remote-user-badge">
              <div className="avatar-small">
                {displayName.charAt(0).toUpperCase()}
              </div>
              <div className="user-text">
                <div className="username">{displayName}</div>
                <div className="call-status-tag">
                    {callState.callStatus === VideoCallStatus.ACTIVE && (
                        <span className="duration-timer">{formatDuration(callState.duration)}</span>
                    )}
                    {callState.callStatus === VideoCallStatus.OUTGOING && 'Calling...'}
                    {callState.callStatus === VideoCallStatus.CONNECTING && 'Connecting...'}
                </div>
              </div>
           </div>
           
           <button className="minimize-btn" onClick={() => onCallEnd()}>
             <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.5">
               <path d="M4 14h6m0 0v6m0-6L3 21m17-11h-6m0 0V4m0 6l7-7"></path>
             </svg>
           </button>
        </div>

        {/* Center: Avatars for non-active states */}
        {callState.callStatus !== VideoCallStatus.ACTIVE && (
          <div className="call-center-stage">
            <div className="avatar-pulse-container">
               <div className="pulse-ring ring-1"></div>
               <div className="pulse-ring ring-2"></div>
               <div className="avatar-giant">
                 {displayName.charAt(0).toUpperCase()}
               </div>
            </div>
            <h2 className="display-name-large">{displayName}</h2>
            <p className="status-message">
                {callState.callStatus === VideoCallStatus.OUTGOING && 'Waiting for answer...'}
                {callState.callStatus === VideoCallStatus.INCOMING && 'Incoming video call...'}
                {callState.callStatus === VideoCallStatus.CONNECTING && 'Establishing secure connection...'}
            </p>
          </div>
        )}

        {/* Local Preview: Floating Picture-in-Picture */}
        <div className={`local-preview-card ${callState.callStatus === VideoCallStatus.ACTIVE ? 'pip' : 'init'}`}>
           <video
             ref={localVideoRef}
             autoPlay
             playsInline
             muted
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

        {/* Bottom Bar: Action Controls */}
        <div className="call-action-bar">
           <div className="actions-wrapper">
              <button 
                className={`action-fab ${!callState.isMicrophoneEnabled ? 'off' : ''}`} 
                onClick={() => toggleMicrophone(!callState.isMicrophoneEnabled)}
                title={callState.isMicrophoneEnabled ? 'Mute' : 'Unmute'}
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

              <button className="action-fab end-call" onClick={() => endCall()} title="End Call">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="white">
                   <path d="M22.21 17.3l-5.11-2.12c-.52-.22-1.12-.1-1.51.3l-2.01 2.01c-2.43-1.25-4.42-3.24-5.67-5.67l2.01-2.01c.39-.39.52-.99.3-1.51L8.1 3.23a1.5 1.5 0 0 0-1.74-.88L2.43 3.55a1.5 1.5 0 0 0-1.1 1.45c0 9.17 7.46 16.64 16.64 16.64a1.5 1.5 0 0 0 1.45-1.1l1.2-3.93a1.5 1.5 0 0 0-.88-1.74z" transform="rotate(135 12 12)"></path>
                </svg>
              </button>

              <button className="action-fab" onClick={() => toggleSpeaker(!callState.isSpeakerEnabled)} title="Speaker">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                   <path d="M11 5L6 9H2v6h4l5 4V5zM19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                </svg>
              </button>

              <button className="action-fab" title="More Options">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                   <circle cx="12" cy="12" r="1"></circle>
                   <circle cx="19" cy="12" r="1"></circle>
                   <circle cx="5" cy="12" r="1"></circle>
                </svg>
              </button>
           </div>
        </div>
      </div>

      {/* Incoming Call Dialog (Glass Overlay) */}
      {callState.callStatus === VideoCallStatus.INCOMING && (
        <div className="incoming-modal-backdrop">
           <div className="incoming-card">
              <div className="caller-profile">
                <div className="avatar-med">
                  {displayName.charAt(0).toUpperCase()}
                </div>
                <h3>{displayName}</h3>
                <p>Incoming video call...</p>
              </div>
              <div className="modal-actions">
                 <button className="modal-btn accept" onClick={() => acceptCall(remoteUserId || '')}>
                   <svg viewBox="0 0 24 24" width="24" height="24" fill="white">
                     <path d="M20 15.5c-1.2 0-2.4-.2-3.6-.6-.3-.1-.7 0-1 .3l-2.2 2.2c-2.8-1.4-5.1-3.8-6.6-6.6l2.2-2.2c.3-.3.4-.7.2-1-.3-1.1-.5-2.3-.5-3.5 0-.5-.4-.9-.9-.9H4c-.5 0-1 .4-1 .9 0 9.4 7.6 17 17 17 .5 0 .9-.4.9-.9v-3.5c0-.5-.4-.9-.9-.9z"></path>
                   </svg>
                   <span>Accept</span>
                 </button>
                 <button className="modal-btn reject" onClick={() => endCall()}>
                   <svg viewBox="0 0 24 24" width="24" height="24" fill="white">
                     <path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.58.9-.98.45-1.87 1.05-2.65 1.76-.17.16-.34.22-.52.22-.17 0-.35-.07-.48-.2l-3.37-3.37c-.13-.13-.2-.3-.2-.48s.07-.35.2-.48C3.36 8.35 7.42 6 12 6s8.64 2.35 12.19 5.39c.13.13.2.3.2.48s-.07.35-.2.48l-3.37 3.37c-.13.13-.3.2-.48.2s-.35-.07-.48-.2c-.78-.71-1.67-1.31-2.65-1.76-.35-.16-.58-.51-.58-.9v-3.1c-1.45-.47-3-.72-4.6-.72z"></path>
                   </svg>
                   <span>Reject</span>
                 </button>
              </div>
           </div>
        </div>
      )}
    </div>
  );
};

export default VideoCallScreen;