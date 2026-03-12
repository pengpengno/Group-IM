import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../../store';
import { acceptCall, VideoCallStatus, endCall } from './videoCallSlice';
import { webRTCManager } from '../../services/webrtc';

const IncomingCallAlert: React.FC = () => {
    const { callStatus, remoteUser } = useSelector((state: RootState) => state.videoCall);
    const dispatch = useDispatch();

    if (callStatus !== VideoCallStatus.INCOMING || !remoteUser) {
        return null;
    }

    const handleAccept = () => {
        dispatch(acceptCall());
        webRTCManager.acceptCall(remoteUser?.userId?.toString() ?? '');
    };

    const handleReject = () => {
        dispatch(endCall());
        // TODO: Send reject signal via signaling service
        // signalingService.sendMessage({ type: 'call/end', ... });
    };

    return (
        <div style={{
            position: 'fixed',
            top: '20px',
            right: '20px',
            backgroundColor: '#fff',
            padding: '20px',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            zIndex: 1000,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '10px'
        }}>
            <div style={{ fontWeight: 'bold' }}>Incoming Call</div>
            <div>{remoteUser?.username} is calling...</div>
            <div style={{ display: 'flex', gap: '10px' }}>
                <button
                    onClick={handleReject}
                    style={{
                        padding: '8px 16px',
                        backgroundColor: '#ff4d4f',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                    }}
                >
                    Decline
                </button>
                <button
                    onClick={handleAccept}
                    style={{
                        padding: '8px 16px',
                        backgroundColor: '#52c41a',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                    }}
                >
                    Accept
                </button>
            </div>
        </div>
    );
};

export default IncomingCallAlert;
