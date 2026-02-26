import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { User, UserInfo } from '../../types';

export enum VideoCallStatus {
    IDLE = 'IDLE',
    OUTGOING = 'OUTGOING',
    INCOMING = 'INCOMING',
    CONNECTING = 'CONNECTING',
    ACTIVE = 'ACTIVE',
    MINIMIZED = 'MINIMIZED',
    ENDING = 'ENDING',
    ENDED = 'ENDED',
    ERROR = 'ERROR'
}

export interface VideoCallState {
    callStatus: VideoCallStatus;
    callId: string | null;
    caller: UserInfo | null;
    participants: UserInfo[];
    startTime: number | null;
    isMuted: boolean;
    isVideoEnabled: boolean;
    isMinimized: boolean;
    errorMessage: string | null;
    localStreamId: string | null;
    remoteStreamId: string | null;
}

const initialState: VideoCallState = {
    callStatus: VideoCallStatus.IDLE,
    callId: null,
    caller: null,
    participants: [],
    startTime: null,
    isMuted: false,
    isVideoEnabled: true,
    isMinimized: false,
    errorMessage: null,
    localStreamId: null,
    remoteStreamId: null
};

const videoCallSlice = createSlice({
    name: 'videoCall',
    initialState,
    reducers: {
        initiateCall(state, action: PayloadAction<{ callId: string; participants: UserInfo[] }>) {
            state.callStatus = VideoCallStatus.OUTGOING;
            state.callId = action.payload.callId;
            state.participants = action.payload.participants;
            state.errorMessage = null;
        },
        incomingCall(state, action: PayloadAction<{ callId: string; caller: UserInfo }>) {
            if (state.callStatus === VideoCallStatus.IDLE) {
                state.callStatus = VideoCallStatus.INCOMING;
                state.callId = action.payload.callId;
                state.caller = action.payload.caller;
                state.participants = [action.payload.caller];
                state.errorMessage = null;
            }
        },
        acceptCall(state) {
            state.callStatus = VideoCallStatus.CONNECTING;
        },
        callConnected(state) {
            state.callStatus = VideoCallStatus.ACTIVE;
            state.startTime = Date.now();
        },
        endCall(state) {
            state.callStatus = VideoCallStatus.ENDING;
        },
        callEnded(state) {
            return initialState;
        },
        callError(state, action: PayloadAction<string>) {
            state.callStatus = VideoCallStatus.ERROR;
            state.errorMessage = action.payload;
        },
        toggleMute(state) {
            state.isMuted = !state.isMuted;
        },
        toggleVideo(state) {
            state.isVideoEnabled = !state.isVideoEnabled;
        },
        minimizeCall(state) {
            state.isMinimized = true;
        },
        restoreCall(state) {
            state.isMinimized = false;
        },
        setLocalStreamId(state, action: PayloadAction<string | null>) {
            state.localStreamId = action.payload;
        },
        setRemoteStreamId(state, action: PayloadAction<string | null>) {
            state.remoteStreamId = action.payload;
        }
    }
});

export const {
    initiateCall,
    incomingCall,
    acceptCall,
    callConnected,
    endCall,
    callEnded,
    callError,
    toggleMute,
    toggleVideo,
    minimizeCall,
    restoreCall,
    setLocalStreamId,
    setRemoteStreamId
} = videoCallSlice.actions;

export default videoCallSlice.reducer;
