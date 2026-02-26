import { Store } from '@reduxjs/toolkit';
import {
    incomingCall,
    callConnected,
    endCall,
    callError,
    VideoCallStatus,
    callEnded
} from '../features/video-call/videoCallSlice';
import { RootState } from '../store';
import { WebrtcMessage } from '../types/webrtc';


class SignalingService {
    private socket: WebSocket | null = null;
    private store: Store<RootState> | null = null;
    private userId: string | null = null;
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;
    private reconnectDelay = 5000;
    private intentionToClose = false;

    initialize(store: Store<RootState>, userId: string) {
        this.store = store;
        this.userId = userId;
        this.intentionToClose = false;
        this.connect();
    }

    private connect() {
        if (this.socket || !this.userId) return;

        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        // Use localhost:8080 as default for dev if not proxied, or infer from location if served
        // For Electron dev with webpack proxy, it might be different. 
        // Let's assume the backend is at localhost:8080 for now as per apiClient.ts
        const host = 'localhost:8080';
        const url = `${protocol}//${host}/ws?userId=${this.userId}`;

        console.log('Connecting to WebSocket:', url);

        this.socket = new WebSocket(url);

        this.socket.onopen = () => {
            console.log('WebSocket Connected');
            this.reconnectAttempts = 0;
        };

        this.socket.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data) as WebrtcMessage;
                this.handleMessage(message);
            } catch (error) {
                console.error('Error parsing WebSocket message:', error);
            }
        };

        this.socket.onclose = (event) => {
            console.log('WebSocket Closed:', event.code, event.reason);
            this.socket = null;
            if (!this.intentionToClose) {
                this.attemptReconnect();
            }
        };

        this.socket.onerror = (error) => {
            console.error('WebSocket Error:', error);
        };
    }

    private attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            setTimeout(() => this.connect(), this.reconnectDelay);
        } else {
            console.error('Max reconnect attempts reached');
            this.store?.dispatch(callError('Connection to signaling server lost'));
        }
    }

    sendMessage(message: WebrtcMessage) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(message));
        } else {
            console.error('WebSocket not connected, cannot send message');
            // Queueing logic could go here
        }
    }

    private handleMessage(message: WebrtcMessage) {
        if (!this.store) return;

        console.log('Received signaling message:', message.type);

        switch (message.type) {
            case 'call/request':
                // Dispatch incoming call
                this.store.dispatch(incomingCall({
                    callId: `call-${Date.now()}`, // Generate a temporary call ID if not provided
                    caller: {
                        userId: parseInt(message.fromUser || '0'),
                        username: `User ${message.fromUser}`,
                        email: ''
                    }
                }));
                break;

            case 'call/accept':
                this.store.dispatch(callConnected());
                // WebRTC manager will handle the offer creation/sending
                break;

            case 'call/end':
                this.store.dispatch(endCall());
                setTimeout(() => this.store?.dispatch(callEnded()), 1000); // Allow cleanup time
                break;

            case 'offer':
            case 'answer':
            case 'candidate':
                // Pass these to WebRTC manager
                // We'll dispatch a custom event or call WebRTC manager directly
                // For now, let's assume we have a global event bus or we inject WebRTC manager
                window.dispatchEvent(new CustomEvent('webrtc-signaling', { detail: message }));
                break;

            default:
                console.log('Unhandled message type:', message.type);
        }
    }

    disconnect() {
        this.intentionToClose = true;
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
    }
}

export const signalingService = new SignalingService();
