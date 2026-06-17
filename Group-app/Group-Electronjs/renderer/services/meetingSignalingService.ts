import { EventEmitter } from 'events';
import { notificationRuntimeService } from './notificationRuntimeService';
import { socketService } from './socketService';
import { SIGNALING_MESSAGE_TYPES, type WebrtcMessage } from '../types/webrtc';

type ConnectionState = 'disconnected' | 'connecting' | 'reconnecting' | 'connected';

class MeetingSignalingService extends EventEmitter {
  private initialized = false;

  private log(scope: string, details?: Record<string, unknown>): void {
    console.log('[MeetingSignalingService]', details ? { scope, ...details } : { scope });
  }

  private readonly forwardSocketMessage = (message: WebrtcMessage) => {
    this.log('forward-socket-message', {
      type: message.type,
      roomId: message.roomId,
      fromUser: message.fromUser,
      toUser: message.toUser
    });

    // Notification side effects belong to the meeting signaling domain,
    // not to the generic socket transport layer.
    if (message.type === SIGNALING_MESSAGE_TYPES.MEETING_REQUEST) {
      notificationRuntimeService.handleMeetingInvite(message);
    }

    this.emit('message', message);
  };

  public initialize(): void {
    if (this.initialized) {
      this.log('initialize-skipped');
      return;
    }

    // socketService owns transport/reconnect/queueing. This service only
    // re-emits room signaling so WebRTC/UI code can depend on a narrower API.
    socketService.on('signaling-message', this.forwardSocketMessage);
    this.initialized = true;
    this.log('initialized');
  }

  public destroy(): void {
    if (!this.initialized) {
      return;
    }

    socketService.off('signaling-message', this.forwardSocketMessage);
    this.initialized = false;
    this.removeAllListeners();
    this.log('destroyed');
  }

  public sendMessage(message: WebrtcMessage): { accepted: boolean; queued: boolean } {
    return socketService.sendSignalingMessage(message);
  }

  public getConnectionState(): ConnectionState {
    return socketService.getConnectionState();
  }

  public onConnectionStateChange(handler: (state: ConnectionState) => void): () => void {
    return socketService.onConnectionStateChange(handler);
  }

  public onMessage(handler: (message: WebrtcMessage) => void): () => void {
    this.on('message', handler);
    return () => this.off('message', handler);
  }
}

export const meetingSignalingService = new MeetingSignalingService();
