import { contextBridge, ipcRenderer } from 'electron';

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  // Authentication related
  login: (credentials: any) => ipcRenderer.invoke('login', credentials),
  queryUsers: (query: string) => ipcRenderer.invoke('query-users', query),
  getUserCompanies: (token: string) => ipcRenderer.invoke('get-user-companies', token),
  
  // File related
  uploadFile: (filePath: string, clientId?: string) => ipcRenderer.invoke('upload-file', filePath, clientId),
  selectFile: (options?: any) => ipcRenderer.invoke('select-file', options),
  
  // Notification related
  showNotification: (options: any) => ipcRenderer.invoke('show-notification', options),
  requestNotificationPermission: () => ipcRenderer.invoke('request-notification-permission'),
  
  // Other APIs could go here...
});