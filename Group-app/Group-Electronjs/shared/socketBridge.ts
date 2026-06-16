// Keep Electron socket IPC names and payload contracts in one place so
// main/preload/renderer evolve together and do not drift over time.
export const SOCKET_IPC_CHANNELS = {
  CONNECT: 'socket:connect',
  SEND: 'socket:send',
  DISCONNECT: 'socket:disconnect',
  IS_ACTIVE: 'socket:is-active',
  SEND_MESSAGE: 'socket:send-message',
  MARK_READ: 'socket:mark-read',
  MESSAGE: 'socket:message',
  CONNECTED: 'socket:connected',
  DISCONNECTED: 'socket:disconnected',
  ERROR: 'socket:error',
  RECONNECTING: 'socket:reconnecting',
  RECONNECT_FAILED: 'socket:reconnect-failed'
} as const;

export interface SocketConnectConfig {
  host: string;
  port: number;
  userId: string;
  token: string;
  username: string;
}

export interface SocketInvokeResult {
  success: boolean;
  error?: string;
}

export interface SocketActiveResult {
  active: boolean;
}

export interface SocketMarkReadPayload {
  conversationId: number;
  lastMsgId: number;
  status?: number;
}

export interface SocketReconnectEvent {
  attempt: number;
}

export interface SocketErrorEvent {
  message: string;
}

export interface SocketConnectedEvent {
  host: string;
  port: number;
}

export interface SocketMessageEvent {
  type: 'message' | 'raw';
  payload?: any;
  data?: any;
}
