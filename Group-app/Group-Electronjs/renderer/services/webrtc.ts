import { signalingService } from './signaling';
import { WebrtcMessage } from '../types/webrtc';
import { Store } from '@reduxjs/toolkit';
import { RootState } from '../store';
import {
    setLocalStreamId,
    setRemoteStreamId,
    callError,
    callEnded,
    callConnected
} from '../features/video-call/videoCallSlice';

class WebRTCManager {
    private peerConnection: RTCPeerConnection | null = null;
    private localStream: MediaStream | null = null;
    private remoteStream: MediaStream | null = null;
    private store: Store<RootState> | null = null;
    private userId: string = '';
    private remoteUserId: string = '';

    private iceServers = {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' }
        ]
    };

    initialize(store: Store<RootState>, userId: string) {
        this.store = store;
        this.userId = userId;

        // Listen for signaling messages dispatched via custom event
        window.addEventListener('webrtc-signaling', this.handleSignalingMessage.bind(this) as EventListener);
    }

    private handleSignalingMessage(event: CustomEvent<WebrtcMessage>) {
        const message = event.detail;
        switch (message.type) {
            case 'offer':
                this.handleOffer(message);
                break;
            case 'answer':
                this.handleAnswer(message);
                break;
            case 'candidate':
                this.handleCandidate(message);
                break;
        }
    }

    async startLocalStream() {
        try {
            this.localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
            this.store?.dispatch(setLocalStreamId(this.localStream.id));
            return this.localStream;
        } catch (error) {
            console.error('Error accessing media devices:', error);
            this.store?.dispatch(callError('Failed to access camera/microphone'));
            throw error;
        }
    }

    createPeerConnection() {
        if (this.peerConnection) return;

        this.peerConnection = new RTCPeerConnection(this.iceServers);

        this.peerConnection.onicecandidate = (event) => {
            if (event.candidate) {
                signalingService.sendMessage({
                    type: 'candidate',
                    fromUser: this.userId,
                    toUser: this.remoteUserId,
                    candidate: {
                        candidate: event.candidate.candidate,
                        sdpMid: event.candidate.sdpMid!,
                        sdpMLineIndex: event.candidate.sdpMLineIndex!
                    }
                });
            }
        };

        this.peerConnection.ontrack = (event) => {
            console.log('Received remote track', event.streams);
            if (event.streams && event.streams[0]) {
                this.remoteStream = event.streams[0];
                this.store?.dispatch(setRemoteStreamId(this.remoteStream.id));

                // Directly attach to video element if needed, but we prefer using stream ID in React
                // However, React needs the actual object. We might need to store the stream in a global map
                // or just attach it here if we pass a ref. 
                // For Redux, we can't store non-serializable data.
                // So we will use a global map or singleton to access streams by ID.
                streamMap.set(this.remoteStream.id, this.remoteStream);
            }
        };

        this.peerConnection.onconnectionstatechange = () => {
            console.log('Connection state changed:', this.peerConnection?.connectionState);
            if (this.peerConnection?.connectionState === 'connected') {
                this.store?.dispatch(callConnected());
            } else if (this.peerConnection?.connectionState === 'disconnected' ||
                this.peerConnection?.connectionState === 'failed') {
                this.endCall();
            }
        };

        if (this.localStream) {
            this.localStream.getTracks().forEach(track => {
                this.peerConnection?.addTrack(track, this.localStream!);
            });
            streamMap.set(this.localStream.id, this.localStream);
        }
    }

    async initiateCall(remoteUserId: string) {
        this.remoteUserId = remoteUserId;
        await this.startLocalStream();
        this.createPeerConnection();

        // The actual "call/request" is sent by the UI or Thunk before calling this, 
        // or we can send it here. The Android logic sends it first.
        // Here we assume "call/request" is already sent/accepted, and we are establishing P2P.
        // Actually, according to docs: 
        // Android: initiateCall -> createPeerConnection -> send call/request.

        // We should follow:
        signalingService.sendMessage({
            type: 'call/request',
            fromUser: this.userId,
            toUser: remoteUserId
        });
    }

    async acceptCall(callId: string, remoteUserId?: string) {
        if (remoteUserId) this.remoteUserId = remoteUserId;
        await this.startLocalStream();
        this.createPeerConnection();

        signalingService.sendMessage({
            type: 'call/accept',
            fromUser: this.userId,
            toUser: this.remoteUserId
        });
    }

    rejectCall(callId: string, remoteUserId?: string) {
        signalingService.sendMessage({
            type: 'call/end',
            fromUser: this.userId,
            toUser: remoteUserId || this.remoteUserId
        });
        this.endCall();
    }      // Android: handleCallAccept -> createAndSendOffer.
    // Wait, usually the Caller sends Offer.
    // Doc: "为避免冲突：由 join 较晚的一方创建 offer" (To avoid conflict: the one joining later creates offer).
    // Or simplified: Caller sends call/request. Callee sends call/accept.
    // Upon receiving call/accept, Caller creates Offer.
    // So here (Callee), we just send call/accept and wait for Offer.

    // Caller logic: receives call/accept, creates offer
    async handleCallAccepted(remoteUserId: string) {
        if (!this.peerConnection) this.createPeerConnection();
        const offer = await this.peerConnection!.createOffer({
            offerToReceiveAudio: true,
            offerToReceiveVideo: true
        });
        await this.peerConnection!.setLocalDescription(offer);

        signalingService.sendMessage({
            type: 'offer',
            fromUser: this.userId,
            toUser: remoteUserId,
            sdp: offer.sdp,
            sdpType: 'offer'
        });
    }

    async handleOffer(message: WebrtcMessage) {
        this.remoteUserId = message.fromUser;
        if (!this.peerConnection) {
            // If we are Callee, we might not have PC if we didn't call acceptCall yet?
            // But usually we accepted first. 
            // If we are receiving offer, we must be Callee.
            this.createPeerConnection();
        }

        await this.peerConnection!.setRemoteDescription(new RTCSessionDescription({
            type: 'offer',
            sdp: message.sdp!
        }));

        const answer = await this.peerConnection!.createAnswer();
        await this.peerConnection!.setLocalDescription(answer);

        signalingService.sendMessage({
            type: 'answer',
            fromUser: this.userId,
            toUser: this.remoteUserId,
            sdp: answer.sdp,
            sdpType: 'answer'
        });
    }

    async handleAnswer(message: WebrtcMessage) {
        await this.peerConnection!.setRemoteDescription(new RTCSessionDescription({
            type: 'answer',
            sdp: message.sdp!
        }));
    }

    async handleCandidate(message: WebrtcMessage) {
        if (this.peerConnection && message.candidate) {
            await this.peerConnection.addIceCandidate(new RTCIceCandidate({
                candidate: message.candidate.candidate,
                sdpMid: message.candidate.sdpMid,
                sdpMLineIndex: message.candidate.sdpMLineIndex
            }));
        }
    }

    endCall() {
        this.localStream?.getTracks().forEach(track => track.stop());
        this.peerConnection?.close();
        this.peerConnection = null;
        this.localStream = null;
        this.remoteStream = null;
        this.store?.dispatch(callEnded());

        // Also send end call signal if needed, but usually UI triggers that
        // signalingService.sendMessage({ type: 'call/end', ... });
    }
}

// Global stream map to access MediaStreams by ID (since Redux can't store them)
export const streamMap = new Map<string, MediaStream>();

export const webRTCManager = new WebRTCManager();
