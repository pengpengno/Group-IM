import { useState, useEffect, useRef } from 'react';
import { VideoCallManager } from './VideoCallManager';
import { VideoCallState, VideoCallStatus } from './videoCallSlice';

interface UseVideoCallReturn {
  // State
  state: VideoCallState;
  localStream: MediaStream | null;
  remoteStream: MediaStream | null;

  // Actions
  initialize: () => Promise<void>;
  startCall: (calleeId: string) => Promise<void>;
  acceptCall: (callerId: string) => Promise<void>;
  rejectCall: (callerId: string) => void;
  endCall: () => void;
  toggleCamera: (enabled: boolean) => void;
  toggleMicrophone: (enabled: boolean) => void;
  toggleSpeaker: (enabled: boolean) => void;
  connectSignaling: (host: string, port: number, userId: string, token: string) => void;

  // Events
  onIncomingCall: (callback: (callerId: string) => void) => void;
  onCallAccepted: (callback: (calleeId: string) => void) => void;
  onCallRejected: (callback: (callerId: string) => void) => void;
  onCallEnded: (callback: (remoteId: string) => void) => void;
  onError: (callback: (error: Error) => void) => void;
}

export const useVideoCall = (): UseVideoCallReturn => {
  const [callState, setCallState] = useState<VideoCallState>({
    callStatus: VideoCallStatus.IDLE,
    callId: null,
    remoteUser: null,
    participants: [],
    startTime: null,
    isMuted: false,
    isVideoEnabled: true,
    isMinimized: false,
    errorMessage: null,
    localStreamId: null,
    remoteStreamId: null,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: true,
    duration: 0
  });

  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);

  const videoCallManagerRef = useRef<VideoCallManager | null>(null);

  // Initialize video call manager
  const initialize = async (): Promise<void> => {
    if (!videoCallManagerRef.current) {
      videoCallManagerRef.current = new VideoCallManager();

      // Set up event listeners
      videoCallManagerRef.current.on('state-change', (state: VideoCallState) => {
        setCallState(state);
      });

      videoCallManagerRef.current.on('remote-stream', (stream: MediaStream) => {
        setRemoteStream(stream);
      });

      await videoCallManagerRef.current.initialize();
      setLocalStream(videoCallManagerRef.current.getLocalStream());
    }
  };

  // Start outgoing call
  const startCall = async (calleeId: string): Promise<void> => {
    if (!videoCallManagerRef.current) {
      await initialize();
    }
    videoCallManagerRef.current!.initiateCall(calleeId);
  };

  // Accept incoming call
  const acceptCall = async (callerId: string): Promise<void> => {
    if (!videoCallManagerRef.current) {
      await initialize();
    }
    videoCallManagerRef.current!.acceptCall();
  };

  // Reject incoming call
  const rejectCall = (callerId: string): void => {
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.rejectCall();
    }
  };

  // End current call
  const endCall = (): void => {
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.endCall();
    }
  };

  // Toggle camera
  const toggleCamera = (enabled: boolean): void => {
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.toggleCamera(enabled);
    }
  };

  // Toggle microphone
  const toggleMicrophone = (enabled: boolean): void => {
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.toggleMicrophone(enabled);
    }
  };

  // Toggle speaker
  const toggleSpeaker = (enabled: boolean): void => {
    // Speaker toggle logic would depend on the specific audio output device API
  };

  // Connect to signaling server
  const connectSignaling = (host: string, port: number, userId: string, token: string): void => {
    if (!videoCallManagerRef.current) {
      initialize().then(() => {
        videoCallManagerRef.current!.connectSignaling(host, port, userId, token);
      });
    } else {
      videoCallManagerRef.current.connectSignaling(host, port, userId, token);
    }
  };

  // Event handlers
  const onIncomingCall = (callback: (callerId: string) => void): void => {
    videoCallManagerRef.current?.on('incoming-call', ({ callerId }) => callback(callerId));
  };

  const onCallAccepted = (callback: (calleeId: string) => void): void => {
    videoCallManagerRef.current?.on('call-accepted', ({ calleeId }) => callback(calleeId));
  };

  const onCallRejected = (callback: (callerId: string) => void): void => {
    videoCallManagerRef.current?.on('call-rejected', ({ callerId }) => callback(callerId));
  };

  const onCallEnded = (callback: (remoteId: string) => void): void => {
    videoCallManagerRef.current?.on('call-ended', ({ remoteId }) => callback(remoteId));
  };

  const onError = (callback: (error: Error) => void): void => {
    videoCallManagerRef.current?.on('error', callback);
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (videoCallManagerRef.current) {
        videoCallManagerRef.current.destroy();
      }
    };
  }, []);

  return {
    state: callState,
    localStream,
    remoteStream,
    initialize,
    startCall,
    acceptCall,
    rejectCall,
    endCall,
    toggleCamera,
    toggleMicrophone,
    toggleSpeaker,
    connectSignaling,
    onIncomingCall,
    onCallAccepted,
    onCallRejected,
    onCallEnded,
    onError
  };
};