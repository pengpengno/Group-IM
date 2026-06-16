import { contextBridge, ipcRenderer } from 'electron';
import { SOCKET_IPC_CHANNELS } from '../shared/socketBridge';

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  // Authentication related
  login: (credentials: any) => ipcRenderer.invoke('login', credentials),
  queryUsers: (query: string, token?: string) => ipcRenderer.invoke('query-users', query, token),
  getUserCompanies: (token: string) => ipcRenderer.invoke('get-user-companies', token),

  // File related
  getUploadId: (request: any, token?: string) => ipcRenderer.invoke('get-upload-id', request, token),
  uploadFile: (filePath: string, fileId: string, token?: string, duration?: number) => ipcRenderer.invoke('upload-file', filePath, fileId, token, duration),
  downloadFile: (url: string, fileName: string, token?: string) => ipcRenderer.invoke('download-file', url, fileName, token),
  selectFile: (options?: any) => ipcRenderer.invoke('select-file', options),

  // Notification related
  showNotification: (options: any) => ipcRenderer.invoke('show-notification', options),
  requestNotificationPermission: () => ipcRenderer.invoke('request-notification-permission'),
  onNotificationClick: (handler: (data: any) => void) => {
    ipcRenderer.on('notification:click', (_, data) => handler(data));
  },

  // Socket related
  socket: {
    connect: (config: any) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.CONNECT, config),
    send: (dataBase64: string) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.SEND, dataBase64),
    disconnect: () => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.DISCONNECT),
    isActive: () => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.IS_ACTIVE),
    sendMessage: (payload: any) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.SEND_MESSAGE, payload),
    markRead: (data: any) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.MARK_READ, data),
    onMessage: (handler: (data: any) => void) => {
      ipcRenderer.on(SOCKET_IPC_CHANNELS.MESSAGE, (_, data) => handler(data));
    },
    onConnected: (handler: () => void) => {
      ipcRenderer.on(SOCKET_IPC_CHANNELS.CONNECTED, () => handler());
    },
    onDisconnected: (handler: () => void) => {
      ipcRenderer.on(SOCKET_IPC_CHANNELS.DISCONNECTED, () => handler());
    },
    onError: (handler: (error: any) => void) => {
      ipcRenderer.on(SOCKET_IPC_CHANNELS.ERROR, (_, error) => handler(error));
    },
    onReconnecting: (handler: (data: any) => void) => {
      ipcRenderer.on(SOCKET_IPC_CHANNELS.RECONNECTING, (_, data) => handler(data));
    }
  },
  socketConnect: (config: any) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.CONNECT, config),
  socketSend: (dataBase64: string) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.SEND, dataBase64),
  socketDisconnect: () => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.DISCONNECT),
  socketIsActive: () => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.IS_ACTIVE),
  socketSendMessage: (payload: any) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.SEND_MESSAGE, payload),
  socketMarkRead: (data: any) => ipcRenderer.invoke(SOCKET_IPC_CHANNELS.MARK_READ, data),

  // Socket events
  onSocketMessage: (handler: (data: any) => void) => {
    ipcRenderer.on(SOCKET_IPC_CHANNELS.MESSAGE, (_, data) => handler(data));
  },
  onSocketConnected: (handler: () => void) => {
    ipcRenderer.on(SOCKET_IPC_CHANNELS.CONNECTED, () => handler());
  },
  onSocketDisconnected: (handler: () => void) => {
    ipcRenderer.on(SOCKET_IPC_CHANNELS.DISCONNECTED, () => handler());
  },
  onSocketError: (handler: (error: any) => void) => {
    ipcRenderer.on(SOCKET_IPC_CHANNELS.ERROR, (_, error) => handler(error));
  },
  onSocketReconnecting: (handler: (data: any) => void) => {
    ipcRenderer.on(SOCKET_IPC_CHANNELS.RECONNECTING, (_, data) => handler(data));
  },

  // Video/Desktop sharing related
  getDesktopSources: () => ipcRenderer.invoke('get-desktop-sources'),

  // Other APIs could go here...
});
