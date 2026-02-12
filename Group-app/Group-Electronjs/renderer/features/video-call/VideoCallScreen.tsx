import React, { useEffect, useRef } from 'react';
import { useVideoCall } from './useVideoCall';
import { VideoCallStatus } from './VideoCallManager';
import './VideoCallScreen.css';

interface VideoCallScreenProps {
  onCallEnd: () => void;
  remoteUserId?: string;
}

const VideoCallScreen: React.FC<VideoCallScreenProps> = ({ 
  onCallEnd, 
  remoteUserId 
}) => {
  const {
    callState,
    localStream,
    remoteStream,
    startCall,
    acceptCall,
    endCall,
    toggleCamera,
    toggleMicrophone,
    toggleSpeaker,
    onIncomingCall,
    onCallAccepted,
    onCallRejected,
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
    onIncomingCall((callerId) => {
      // In a real app, you'd show a notification or modal
      console.log(`Incoming call from ${callerId}`);
      // Auto-accept for demo purposes
      acceptCall(callerId);
    });

    onCallAccepted((calleeId) => {
      console.log(`Call accepted by ${calleeId}`);
    });

    onCallRejected((callerId) => {
      console.log(`Call rejected by ${callerId}`);
      onCallEnd();
    });

    onCallEnded(() => {
      console.log('Call ended');
      onCallEnd();
    });

    onError((error) => {
      console.error('Video call error:', error);
      alert(`Call error: ${error.message}`);
      onCallEnd();
    });
  }, []);

  // Start call when remote user is provided
  useEffect(() => {
    if (remoteUserId) {
      startCall(remoteUserId);
    }
  }, [remoteUserId]);

  const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const getStatusText = (): string => {
    switch (callState.callStatus) {
      case VideoCallStatus.OUTGOING:
        return 'Calling...';
      case VideoCallStatus.INCOMING:
        return 'Incoming call...';
      case VideoCallStatus.CONNECTING:
        return 'Connecting...';
      case VideoCallStatus.ACTIVE:
        return formatDuration(callState.duration);
      case VideoCallStatus.ENDED:
        return 'Call ended';
      case VideoCallStatus.ERROR:
        return callState.errorMessage || 'Error occurred';
      default:
        return '';
    }
  };

  const getNetworkQualityColor = (): string => {
    // Simplified network quality indicator
    if (callState.callStatus === VideoCallStatus.ACTIVE) {
      return '#4CAF50'; // Green - good
    } else if (callState.callStatus === VideoCallStatus.CONNECTING) {
      return '#FFC107'; // Yellow - connecting
    } else {
      return '#9E9E9E'; // Gray - inactive
    }
  };

  return (
    <div className="video-call-screen">
      {/* Remote Video Area */}
      <div className="remote-video-container">
        <video
          ref={remoteVideoRef}
          autoPlay
          playsInline
          className={`remote-video ${!remoteStream ? 'hidden' : ''}`}
        />
        
        {/* Remote video placeholder */}
        {!remoteStream && (
          <div className="remote-placeholder">
            <div className="user-avatar-large">
              {remoteUserId?.charAt(0).toUpperCase() || 'U'}
            </div>
            <p className="remote-user-name">
              {remoteUserId || 'User'}
            </p>
          </div>
        )}
      </div>

      {/* Local Video Preview */}
      <div className="local-video-container">
        <video
          ref={localVideoRef}
          autoPlay
          playsInline
          muted
          className={`local-video ${!callState.isLocalVideoEnabled ? 'hidden' : ''}`}
        />
        
        {/* Camera off indicator */}
        {!callState.isLocalVideoEnabled && (
          <div className="camera-off-indicator">
            <span className="camera-icon">📷</span>
            <p>Camera Off</p>
          </div>
        )}
      </div>

      {/* Call Controls */}
      <div className="call-controls">
        {/* Status Bar */}
        <div className="status-bar">
          <div className="status-info">
            <div 
              className="network-indicator"
              style={{ backgroundColor: getNetworkQualityColor() }}
            />
            <span className="status-text">{getStatusText()}</span>
          </div>
        </div>

        {/* Control Buttons */}
        <div className="control-buttons">
          {/* Microphone Toggle */}
          <button
            className={`control-button mic-button ${
              !callState.isMicrophoneEnabled ? 'muted' : ''
            }`}
            onClick={() => toggleMicrophone(!callState.isMicrophoneEnabled)}
            title={callState.isMicrophoneEnabled ? 'Mute' : 'Unmute'}
          >
            <span className="button-icon">
              {callState.isMicrophoneEnabled ? '🎤' : '🔇'}
            </span>
          </button>

          {/* Camera Toggle */}
          <button
            className={`control-button camera-button ${
              !callState.isLocalVideoEnabled ? 'disabled' : ''
            }`}
            onClick={() => toggleCamera(!callState.isLocalVideoEnabled)}
            title={callState.isLocalVideoEnabled ? 'Turn off camera' : 'Turn on camera'}
          >
            <span className="button-icon">
              {callState.isLocalVideoEnabled ? '📹' : '📷'}
            </span>
          </button>

          {/* End Call Button */}
          <button
            className="control-button end-button"
            onClick={() => {
              endCall();
              onCallEnd();
            }}
            title="End call"
          >
            <span className="button-icon">📞</span>
          </button>

          {/* Speaker Toggle */}
          <button
            className={`control-button speaker-button ${
              callState.isSpeakerEnabled ? 'active' : ''
            }`}
            onClick={() => toggleSpeaker(!callState.isSpeakerEnabled)}
            title={callState.isSpeakerEnabled ? 'Speaker off' : 'Speaker on'}
          >
            <span className="button-icon">
              {callState.isSpeakerEnabled ? '🔊' : '🔈'}
            </span>
          </button>
        </div>
      </div>

      {/* Incoming Call Overlay */}
      {callState.callStatus === VideoCallStatus.INCOMING && (
        <div className="incoming-call-overlay">
          <div className="incoming-call-content">
            <div className="caller-avatar">
              {callState.callerId?.charAt(0).toUpperCase() || 'U'}
            </div>
            <h2>Incoming Video Call</h2>
            <p>From: {callState.callerId || 'Unknown'}</p>
            
            <div className="incoming-call-actions">
              <button
                className="accept-button"
                onClick={() => acceptCall(callState.callerId!)}
              >
                Accept
              </button>
              <button
                className="reject-button"
                onClick={() => {
                  // rejectCall(callState.callerId!);
                  endCall();
                  onCallEnd();
                }}
              >
                Reject
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VideoCallScreen;