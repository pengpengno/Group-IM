import { store } from '../../store';
import { VideoCallManager, VideoCallState as ManagerState } from './VideoCallManager';
import {
    incomingCall,
    callConnected,
    callEnded,
    setLocalStreamId,
    setRemoteStreamId,
    callError,
    VideoCallStatus as ManagerStatus
} from './videoCallSlice';
import { streamMap } from '../../services/webrtc';

class VideoCallService {
    private manager: VideoCallManager;
    private initialized = false;

    constructor() {
        this.manager = new VideoCallManager();
        this.setupListeners();
    }

    private setupListeners() {
        this.manager.on('incoming-call', ({ callerId }) => {
            // In a real app, we'd fetch user info by ID. 
            // For now, we use a placeholder or the data from the message if we extend the protocol.
            store.dispatch(incomingCall({
                callId: `call-${Date.now()}`,
                remoteUser: {
                    userId: callerId,
                    username: `User ${callerId}`,
                    email: '',
                    status: 'online'
                }
            }));
        });

        this.manager.on('state-change', (state: ManagerState) => {
            if (state.callStatus === ManagerStatus.ACTIVE) {
                store.dispatch(callConnected());
            } else if (state.callStatus === ManagerStatus.ENDED) {
                store.dispatch(callEnded());
            } else if (state.callStatus === ManagerStatus.ERROR) {
                store.dispatch(callError(state.errorMessage || 'Unknown error'));
            }
        });

        this.manager.on('remote-stream', (stream: MediaStream) => {
            streamMap.set(stream.id, stream);
            store.dispatch(setRemoteStreamId(stream.id));
        });

        this.manager.on('error', (error: Error) => {
            store.dispatch(callError(error.message));
        });
    }

    public async startCall(remoteUserId: string) {
        await this.ensureInitialized();
        const localStream = await this.manager.createLocalStream();
        streamMap.set(localStream.id, localStream);
        store.dispatch(setLocalStreamId(localStream.id));
        this.manager.initiateCall(remoteUserId);
    }

    public async acceptCall() {
        await this.ensureInitialized();
        const localStream = await this.manager.createLocalStream();
        streamMap.set(localStream.id, localStream);
        store.dispatch(setLocalStreamId(localStream.id));
        this.manager.acceptCall();
    }

    public rejectCall() {
        this.manager.rejectCall();
    }

    public endCall() {
        this.manager.endCall();
    }

    public toggleCamera(enabled: boolean) {
        this.manager.toggleCamera(enabled);
    }

    public toggleMicrophone(enabled: boolean) {
        this.manager.toggleMicrophone(enabled);
    }

    private async ensureInitialized() {
        if (!this.initialized) {
            await this.manager.initialize();
            this.initialized = true;
        }
    }

    public connectSignaling(host: string, port: number, userId: string, token: string) {
        this.manager.connectSignaling(host, port, userId, token);
    }
}

export const videoCallService = new VideoCallService();
