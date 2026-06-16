import { getElectronAPI } from './api/electronAPI';
import type {
  SocketActiveResult,
  SocketConnectConfig,
  SocketErrorEvent,
  SocketInvokeResult,
  SocketMarkReadPayload,
  SocketMessageEvent,
  SocketReconnectEvent
} from '../../shared/socketBridge';

type SocketBridgeApi = {
  connect: (config: SocketConnectConfig) => Promise<SocketInvokeResult>;
  send: (dataBase64: string) => Promise<SocketInvokeResult>;
  disconnect: () => Promise<SocketInvokeResult>;
  isActive: () => Promise<SocketActiveResult>;
  sendMessage: (payload: any) => Promise<SocketInvokeResult>;
  markRead: (data: SocketMarkReadPayload) => Promise<SocketInvokeResult>;
  onMessage?: (handler: (data: SocketMessageEvent) => void) => void;
  onConnected?: (handler: () => void) => void;
  onDisconnected?: (handler: () => void) => void;
  onError?: (handler: (error: SocketErrorEvent) => void) => void;
  onReconnecting?: (handler: (data: SocketReconnectEvent) => void) => void;
};

function resolveSocketBridge(): SocketBridgeApi | null {
  const electron = getElectronAPI() as any;
  if (electron.socket) {
    // Preferred path: the new namespaced preload bridge keeps socket IPC
    // isolated behind a single entrypoint.
    return electron.socket as SocketBridgeApi;
  }

  if (electron.socketConnect) {
    // Compatibility path: older preload builds exposed flat methods.
    // Keeping this adapter lets renderer services migrate once without
    // forcing every caller to know both bridge shapes.
    return {
      connect: electron.socketConnect.bind(electron),
      send: electron.socketSend.bind(electron),
      disconnect: electron.socketDisconnect.bind(electron),
      isActive: electron.socketIsActive.bind(electron),
      sendMessage: electron.socketSendMessage.bind(electron),
      markRead: electron.socketMarkRead.bind(electron),
      onMessage: electron.onSocketMessage?.bind(electron),
      onConnected: electron.onSocketConnected?.bind(electron),
      onDisconnected: electron.onSocketDisconnected?.bind(electron),
      onError: electron.onSocketError?.bind(electron),
      onReconnecting: electron.onSocketReconnecting?.bind(electron)
    };
  }

  return null;
}

class ElectronSocketBridge {
  private getSocketApi(): SocketBridgeApi {
    const api = resolveSocketBridge();
    if (!api) {
      throw new Error('Electron socket bridge is unavailable in the current runtime');
    }
    return api;
  }

  public connect(config: SocketConnectConfig): Promise<SocketInvokeResult> {
    return this.getSocketApi().connect(config);
  }

  public send(dataBase64: string): Promise<SocketInvokeResult> {
    return this.getSocketApi().send(dataBase64);
  }

  public disconnect(): Promise<SocketInvokeResult> {
    return this.getSocketApi().disconnect();
  }

  public isActive(): Promise<SocketActiveResult> {
    return this.getSocketApi().isActive();
  }

  public sendMessage(payload: any): Promise<SocketInvokeResult> {
    return this.getSocketApi().sendMessage(payload);
  }

  public markRead(data: SocketMarkReadPayload): Promise<SocketInvokeResult> {
    return this.getSocketApi().markRead(data);
  }

  public onMessage(handler: (data: SocketMessageEvent) => void): void {
    this.getSocketApi().onMessage?.(handler);
  }

  public onConnected(handler: () => void): void {
    this.getSocketApi().onConnected?.(handler);
  }

  public onDisconnected(handler: () => void): void {
    this.getSocketApi().onDisconnected?.(handler);
  }

  public onError(handler: (error: SocketErrorEvent) => void): void {
    this.getSocketApi().onError?.(handler);
  }

  public onReconnecting(handler: (data: SocketReconnectEvent) => void): void {
    this.getSocketApi().onReconnecting?.(handler);
  }
}

// Renderer code should talk to Electron socket IPC only through this bridge.
// That keeps transport-specific details out of domain services such as
// socketService and meetingSignalingService.
export const electronSocketBridge = new ElectronSocketBridge();
