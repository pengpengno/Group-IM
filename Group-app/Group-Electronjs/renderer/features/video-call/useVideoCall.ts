import { useState, useEffect, useRef } from 'react';
import { VideoCallManager, VideoCallState, VideoCallStatus } from './VideoCallManager';

interface UseVideoCallReturn {
  // State
  callState: VideoCallState;
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
  connectSignaling: (url: string, userId: string) => void;
  
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
    duration: 0,
    isLocalVideoEnabled: true,
    isRemoteVideoEnabled: true,
    isMicrophoneEnabled: true,
    isSpeakerEnabled: true
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
    await videoCallManagerRef.current!.startVideoCall(calleeId);
  };
  
  // Accept incoming call
  const acceptCall = async (callerId: string): Promise<void> => {
    if (!videoCallManagerRef.current) {
      await initialize();
    }
    await videoCallManagerRef.current!.acceptVideoCall(callerId);
  };
  
  // Reject incoming call
  const rejectCall = (callerId: string): void => {
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.rejectVideoCall(callerId);
    }
  };
  
  // End current call
  const endCall = (): void => {
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.endVideoCall();
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
    if (videoCallManagerRef.current) {
      videoCallManagerRef.current.toggleSpeaker(enabled);
    }
  };
  
  // Connect to signaling server
  const connectSignaling = (url: string, userId: string): void => {
    if (!videoCallManagerRef.current) {
      initialize().then(() => {
        videoCallManagerRef.current!.connectToSignalingServer(url, userId);
      });
    } else {
      videoCallManagerRef.current.connectToSignalingServer(url, userId);
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
    // State
    callState,
    localStream,
    remoteStream,
    
    // Actions
    initialize,
    startCall,
    acceptCall,
    rejectCall,
    endCall,
    toggleCamera,
    toggleMicrophone,
    toggleSpeaker,
    connectSignaling,
    
    // Events
    onIncomingCall,
    onCallAccepted,
    onCallRejected,
    onCallEnded,
    onError
  };
};