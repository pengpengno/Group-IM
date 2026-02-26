import React, { useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../../store';
import { VideoCallStatus, endCall, toggleMute, toggleVideo } from './videoCallSlice';
import { webRTCManager, streamMap } from '../../services/webrtc';

const VideoCallModal: React.FC = () => {
    const {
        callStatus,
        localStreamId,
        remoteStreamId,
        isMuted,
        isVideoEnabled
    } = useSelector((state: RootState) => state.videoCall);

    const dispatch = useDispatch();
    const localVideoRef = useRef<HTMLVideoElement>(null);
    const remoteVideoRef = useRef<HTMLVideoElement>(null);

    const isCallActive = [
        VideoCallStatus.CONNECTING,
        VideoCallStatus.ACTIVE,
        VideoCallStatus.OUTGOING
    ].includes(callStatus);

    useEffect(() => {
        if (localStreamId && localVideoRef.current) {
            const stream = streamMap.get(localStreamId);
            if (stream && localVideoRef.current.srcObject !== stream) {
                localVideoRef.current.srcObject = stream;
            }
        }
    }, [localStreamId, isCallActive]);

    useEffect(() => {
        if (remoteStreamId && remoteVideoRef.current) {
            const stream = streamMap.get(remoteStreamId);
            if (stream && remoteVideoRef.current.srcObject !== stream) {
                remoteVideoRef.current.srcObject = stream;
            }
        }
    }, [remoteStreamId, isCallActive]);

    // Handle mute/video toggle effects on stream
    useEffect(() => {
        if (localStreamId) {
            const stream = streamMap.get(localStreamId);
            if (stream) {
                stream.getAudioTracks().forEach(track => track.enabled = !isMuted);
                stream.getVideoTracks().forEach(track => track.enabled = isVideoEnabled);
            }
        }
    }, [isMuted, isVideoEnabled, localStreamId]);

    if (!isCallActive) return null;

    const handleEndCall = () => {
        webRTCManager.endCall();
        dispatch(endCall());
    };

    return (
        <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            width: '100vw',
            height: '100vh',
            backgroundColor: '#000',
            zIndex: 2000,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center'
        }}>
            {/* Remote Video (Full Screen) */}
            <video
                ref={remoteVideoRef}
                autoPlay
                playsInline
                style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover'
                }}
            />

            {/* Local Video (PiP) */}
            <div style={{
                position: 'absolute',
                bottom: '100px',
                right: '20px',
                width: '150px',
                height: '200px',
                backgroundColor: '#333',
                borderRadius: '8px',
                overflow: 'hidden',
                boxShadow: '0 4px 12px rgba(0,0,0,0.5)'
            }}>
                <video
                    ref={localVideoRef}
                    autoPlay
                    playsInline
                    muted // Always mute local video to prevent feedback
                    style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover'
                    }}
                />
            </div>

            {/* Controls */}
            <div style={{
                position: 'absolute',
                bottom: '20px',
                left: '50%',
                transform: 'translateX(-50%)',
                display: 'flex',
                gap: '20px'
            }}>
                <button
                    onClick={() => dispatch(toggleMute())}
                    style={{
                        width: '50px',
                        height: '50px',
                        borderRadius: '50%',
                        border: 'none',
                        backgroundColor: isMuted ? '#ff4d4f' : '#fff',
                        cursor: 'pointer',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center'
                    }}
                >
                    {isMuted ? 'Unmute' : 'Mute'}
                </button>
                <button
                    onClick={handleEndCall}
                    style={{
                        width: '60px',
                        height: '60px',
                        borderRadius: '50%',
                        border: 'none',
                        backgroundColor: '#ff4d4f',
                        color: 'white',
                        cursor: 'pointer',
                        fontWeight: 'bold'
                    }}
                >
                    End
                </button>
                <button
                    onClick={() => dispatch(toggleVideo())}
                    style={{
                        width: '50px',
                        height: '50px',
                        borderRadius: '50%',
                        border: 'none',
                        backgroundColor: !isVideoEnabled ? '#ff4d4f' : '#fff',
                        cursor: 'pointer',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center'
                    }}
                >
                    {isVideoEnabled ? 'Video' : 'No Video'}
                </button>
            </div>

            {callStatus === VideoCallStatus.OUTGOING && (
                <div style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    color: 'white',
                    fontSize: '24px',
                    fontWeight: 'bold',
                    textShadow: '0 2px 4px rgba(0,0,0,0.5)'
                }}>
                    Calling...
                </div>
            )}
        </div>
    );
};

export default VideoCallModal;
