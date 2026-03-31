import { useState, useEffect, useCallback } from 'react';
import { webRTCService, CallInternalState } from '../../services/WebRTCService';
import { VideoCallState, VideoCallStatus } from './videoCallSlice';

interface UseVideoCallReturn {
  // State
  state: CallInternalState;
  localStream: MediaStream | null;
  remoteStream: MediaStream | null;

  // Actions
  initialize: () => Promise<void>;
  startCall: (calleeId: string, calleeName?: string) => void;
  acceptCall: (callerId: string) => void;
  rejectCall: (callerId: string) => void;
  endCall: () => void;
  toggleCamera: (enabled: boolean) => void;
  toggleMicrophone: (enabled: boolean) => void;
  toggleSpeaker: (enabled: boolean) => void;
  connectSignaling: (host: string, port: number, userId: string, token: string, pageProtocol?: string) => void;

  // Events
  onIncomingCall: (callback: (callerId: string) => void) => void;
  onCallAccepted: (callback: (calleeId: string) => void) => void;
  onCallRejected: (callback: (callerId: string) => void) => void;
  onCallEnded: (callback: (remoteId: string) => void) => void;
  onError: (callback: (error: Error) => void) => void;
}

export const useVideoCall = (): UseVideoCallReturn => {
  const [state, setState] = useState<CallInternalState>(webRTCService.getState());
  const [localStream, setLocalStream] = useState<MediaStream | null>(webRTCService.getLocalStream());
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(webRTCService.getRemoteStream());

  useEffect(() => {
    const handleStateChange = (newState: CallInternalState) => {
      setState(newState);
      setLocalStream(webRTCService.getLocalStream());
    };

    const handleRemoteStream = (stream: MediaStream) => {
      setRemoteStream(stream);
    };

    webRTCService.on('state-change', handleStateChange);
    webRTCService.on('remote-stream', handleRemoteStream);

    return () => {
      webRTCService.off('state-change', handleStateChange);
      webRTCService.off('remote-stream', handleRemoteStream);
    };
  }, []);

  const initialize = useCallback(async () => {
    await webRTCService.initialize();
    await webRTCService.acquireLocalMedia();
    setLocalStream(webRTCService.getLocalStream());
  }, []);

  const startCall = useCallback((calleeId: string, calleeName?: string) => {
    webRTCService.initiateCall(calleeId, calleeName);
  }, []);

  const acceptCall = useCallback((callerId: string) => {
    webRTCService.acceptCall();
  }, []);

  const rejectCall = useCallback((callerId: string) => {
    webRTCService.endCall();
  }, []);

  const endCall = useCallback(() => {
    webRTCService.endCall();
  }, []);

  const toggleCamera = useCallback((enabled: boolean) => {
    webRTCService.toggleCamera(enabled);
  }, []);

  const toggleMicrophone = useCallback((enabled: boolean) => {
    webRTCService.toggleMicrophone(enabled);
  }, []);

  const toggleSpeaker = useCallback((enabled: boolean) => {
    // Basic implementation toggle internal state only
    // Real speaker toggle would use audio output device selection
  }, []);

  const connectSignaling = useCallback((host: string, port: number, userId: string, token: string, pageProtocol?: string) => {
    webRTCService.connectSignaling(host, port, userId, token, pageProtocol);
  }, []);

  // Event attachment helpers
  const onIncomingCall = useCallback((callback: (callerId: string) => void) => {
    webRTCService.on('incoming-call', ({ callerId }) => callback(callerId));
  }, []);

  const onCallAccepted = useCallback((callback: (calleeId: string) => void) => {
    webRTCService.on('call-accepted', ({ calleeId }) => callback(calleeId));
  }, []);

  const onCallRejected = useCallback((callback: (callerId: string) => void) => {
    webRTCService.on('call-rejected', ({ callerId }) => callback(callerId));
  }, []);

  const onCallEnded = useCallback((callback: (remoteId: string) => void) => {
    webRTCService.on('call-ended', ({ remoteId }) => callback(remoteId));
  }, []);

  const onError = useCallback((callback: (error: Error) => void) => {
    webRTCService.on('error', callback);
  }, []);

  return {
    state,
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
