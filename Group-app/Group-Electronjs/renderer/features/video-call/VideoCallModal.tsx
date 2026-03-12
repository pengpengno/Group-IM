import React, { useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../../store';
import { VideoCallStatus, toggleMute, toggleVideo } from './videoCallSlice';
import { videoCallService } from './VideoCallService';
import { streamMap } from '../../services/webrtc';
import {
    Mic, MicOff, Videocam, VideocamOff, CallEnd,
    VolumeUp, VolumeOff, Minimize, Fullscreen
} from '@mui/icons-material';

const VideoCallModal: React.FC = () => {
    const {
        callStatus,
        remoteUser, // Keep remoteUser as a whole object for now
        localStreamId,
        remoteStreamId,
        isMuted,
        isVideoEnabled,
        isMinimized
    } = useSelector((state: RootState) => state.videoCall);

    // Destructure remoteUser properties separately if needed, with defaults
    const callerId = remoteUser?.userId;
    const remoteUsername = remoteUser?.username || `User ${callerId || 'Unknown'}`;
    const remoteEmail = remoteUser?.email || '';
    const remoteStatus = remoteUser?.status || 'online';


    const dispatch = useDispatch();
    const localVideoRef = useRef<HTMLVideoElement>(null);
    const remoteVideoRef = useRef<HTMLVideoElement>(null);

    const isCallActive = [
        VideoCallStatus.IDLE, // Don't show modal when IDLE
    ].includes(callStatus) === false;

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
            if (stream) {
                console.log('Attaching remote stream to video element', remoteStreamId);
                remoteVideoRef.current.srcObject = stream;
            }
        }
    }, [remoteStreamId, isCallActive]);

    if (!isCallActive) return null;

    const handleEndCall = () => {
        videoCallService.endCall();
    };

    const handleAcceptCall = () => {
        videoCallService.acceptCall();
    };

    const handleToggleMute = () => {
        const nextState = !isMuted;
        videoCallService.toggleMicrophone(!nextState);
        dispatch(toggleMute());
    };

    const handleToggleVideo = () => {
        const nextState = !isVideoEnabled;
        videoCallService.toggleCamera(nextState);
        dispatch(toggleVideo());
    };

    return (
        <div className={`video-call-modal ${isMinimized ? 'minimized' : ''}`} style={{
            position: 'fixed',
            top: 0,
            left: 0,
            width: '100vw',
            height: '100vh',
            backgroundColor: 'rgba(0,0,0,0.95)',
            zIndex: 9999,
            display: 'flex',
            flexDirection: 'column',
            color: '#fff',
            fontFamily: 'Inter, sans-serif',
            overflow: 'hidden',
            backdropFilter: 'blur(20px)'
        }}>
            {/* Background Blur Overlay */}
            <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                backgroundImage: remoteUser?.avatar ? `url(${remoteUser.avatar})` : 'none',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                filter: 'blur(100px) brightness(0.3)',
                zIndex: -1
            }} />

            {/* Remote Video Container */}
            <div style={{
                flex: 1,
                position: 'relative',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
            }}>
                <video
                    ref={remoteVideoRef}
                    autoPlay
                    playsInline
                    style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                        opacity: remoteStreamId ? 1 : 0,
                        transition: 'opacity 0.5s ease-in-out'
                    }}
                />

                {!remoteStreamId && (callStatus !== VideoCallStatus.INCOMING || !remoteUser) && (
                    <div style={{
                        position: 'absolute',
                        textAlign: 'center'
                    }}>
                        <div style={{
                            width: '120px',
                            height: '120px',
                            borderRadius: '50%',
                            backgroundColor: 'rgba(255,255,255,0.1)',
                            margin: '0 auto 20px',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            fontSize: '48px',
                            border: '2px solid rgba(255,255,255,0.2)'
                        }}>
                            {remoteUser?.username?.[0]?.toUpperCase() || '?'}
                        </div>
                        <h2 style={{ fontSize: '24px', fontWeight: 500, margin: 0 }}>
                            {remoteUser?.username || 'Unknown User'}
                        </h2>
                        <p style={{ opacity: 0.6, marginTop: '8px' }}>
                            {callStatus === VideoCallStatus.OUTGOING ? 'Calling...' :
                                callStatus === VideoCallStatus.CONNECTING ? 'Connecting...' :
                                    callStatus === VideoCallStatus.INCOMING ? 'Incoming call' : ''}
                        </p>
                    </div>
                )}
            </div>

            {/* Local Video PiP */}
            <div style={{
                position: 'absolute',
                top: '40px',
                right: '40px',
                width: '180px',
                aspectRatio: '3/4',
                backgroundColor: 'rgba(0,0,0,0.5)',
                borderRadius: '16px',
                overflow: 'hidden',
                boxShadow: '0 20px 40px rgba(0,0,0,0.4)',
                border: '1px solid rgba(255,255,255,0.1)',
                zIndex: 10
            }}>
                <video
                    ref={localVideoRef}
                    autoPlay
                    playsInline
                    muted
                    style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                        display: isVideoEnabled ? 'block' : 'none'
                    }}
                />
                {!isVideoEnabled && (
                    <div style={{
                        width: '100%',
                        height: '100%',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        backgroundColor: '#222'
                    }}>
                        <VideocamOff sx={{ opacity: 0.3, fontSize: 40 }} />
                    </div>
                )}
            </div>

            {/* Control Bar */}
            <div style={{
                position: 'absolute',
                bottom: '40px',
                left: '50%',
                transform: 'translateX(-50%)',
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                padding: '16px 24px',
                backgroundColor: 'rgba(255,255,255,0.1)',
                backdropFilter: 'blur(20px)',
                borderRadius: '40px',
                border: '1px solid rgba(255,255,255,0.2)',
                boxShadow: '0 10px 30px rgba(0,0,0,0.3)',
                zIndex: 100
            }}>
                {callStatus === VideoCallStatus.INCOMING ? (
                    <>
                        <button
                            onClick={handleAcceptCall}
                            style={{
                                width: '60px',
                                height: '60px',
                                borderRadius: '50%',
                                backgroundColor: '#22c55e',
                                color: '#fff',
                                border: 'none',
                                cursor: 'pointer',
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }}>
                            <Videocam fontSize="large" />
                        </button>
                        <button
                            onClick={handleEndCall}
                            style={{
                                width: '60px',
                                height: '60px',
                                borderRadius: '50%',
                                backgroundColor: '#ef4444',
                                color: '#fff',
                                border: 'none',
                                cursor: 'pointer',
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }}>
                            <CallEnd fontSize="large" />
                        </button>
                    </>
                ) : (
                    <>
                        <button
                            onClick={handleToggleMute}
                            className="control-btn"
                            style={{
                                backgroundColor: isMuted ? '#ef4444' : 'rgba(255,255,255,0.1)',
                                border: 'none',
                                borderRadius: '50%',
                                width: '50px',
                                height: '50px',
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center',
                                cursor: 'pointer',
                                color: '#fff',
                                transition: 'all 0.2s'
                            }}
                        >
                            {isMuted ? <MicOff /> : <Mic />}
                        </button>

                        <button
                            onClick={handleToggleVideo}
                            className="control-btn"
                            style={{
                                backgroundColor: !isVideoEnabled ? '#ef4444' : 'rgba(255,255,255,0.1)',
                                border: 'none',
                                borderRadius: '50%',
                                width: '50px',
                                height: '50px',
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center',
                                cursor: 'pointer',
                                color: '#fff',
                                transition: 'all 0.2s'
                            }}
                        >
                            {isVideoEnabled ? <Videocam /> : <VideocamOff />}
                        </button>

                        <div style={{ width: '1px', height: '30px', backgroundColor: 'rgba(255,255,255,0.2)', margin: '0 8px' }} />

                        <button
                            onClick={handleEndCall}
                            style={{
                                backgroundColor: '#ef4444',
                                border: 'none',
                                borderRadius: '40px',
                                padding: '0 24px',
                                height: '50px',
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center',
                                cursor: 'pointer',
                                color: '#fff',
                                fontWeight: 'bold',
                                gap: '8px'
                            }}
                        >
                            <CallEnd />
                            <span>Hang Up</span>
                        </button>
                    </>
                )}
            </div>

            <style>{`
              .control-btn:hover {
                background-color: rgba(255,255,255,0.2) !important;
                transform: scale(1.05);
              }
              .video-call-modal video {
                transform: rotateY(180deg); /* Mirror effect */
              }
            `}</style>
        </div>
    );
};

export default VideoCallModal;
