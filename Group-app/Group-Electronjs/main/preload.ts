import { contextBridge, ipcRenderer } from 'electron';

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  // Authentication related
  login: (credentials: any) => ipcRenderer.invoke('login', credentials),
  queryUsers: (query: string, token?: string) => ipcRenderer.invoke('query-users', query, token),
  getUserCompanies: (token: string) => ipcRenderer.invoke('get-user-companies', token),

  // File related
  uploadFile: (filePath: string, clientId?: string, token?: string) => ipcRenderer.invoke('upload-file', filePath, clientId, token),
  downloadFile: (url: string, fileName: string, token?: string) => ipcRenderer.invoke('download-file', url, fileName, token),
  selectFile: (options?: any) => ipcRenderer.invoke('select-file', options),

  // Notification related
  showNotification: (options: any) => ipcRenderer.invoke('show-notification', options),
  requestNotificationPermission: () => ipcRenderer.invoke('request-notification-permission'),

  // Socket related
  socketConnect: (config: any) => ipcRenderer.invoke('socket:connect', config),
  socketSend: (dataBase64: string) => ipcRenderer.invoke('socket:send', dataBase64),
  socketDisconnect: () => ipcRenderer.invoke('socket:disconnect'),
  socketIsActive: () => ipcRenderer.invoke('socket:is-active'),
  socketSendMessage: (payload: any) => ipcRenderer.invoke('socket:send-message', payload),
  socketMarkRead: (data: any) => ipcRenderer.invoke('socket:mark-read', data),

  // Socket events
  onSocketMessage: (handler: (data: any) => void) => {
    ipcRenderer.on('socket:message', (_, data) => handler(data));
  },
  onSocketConnected: (handler: () => void) => {
    ipcRenderer.on('socket:connected', () => handler());
  },
  onSocketDisconnected: (handler: () => void) => {
    ipcRenderer.on('socket:disconnected', () => handler());
  },
  onSocketError: (handler: (error: any) => void) => {
    ipcRenderer.on('socket:error', (_, error) => handler(error));
  },
  onSocketReconnecting: (handler: (data: any) => void) => {
    ipcRenderer.on('socket:reconnecting', (_, data) => handler(data));
  },

  // Video/Desktop sharing related
  getDesktopSources: () => ipcRenderer.invoke('get-desktop-sources'),

  // Other APIs could go here...
});