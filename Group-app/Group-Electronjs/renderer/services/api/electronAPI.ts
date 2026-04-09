import { UUID } from 'crypto';
import type {
  AuthData,
  LoginCredentials,
  SelectFileOptions,
  SelectFileResult,
  ApiResponse,
  User,
  ApiUser
} from '../../types/index';
import { authAPI, fileAPI } from './apiClient';

// 定义搜索结果接口
export interface SearchResults {
  users: User[];
  total: number;
}

export interface ApiSearchResults extends ApiResponse<SearchResults> {
  users: User[];
  total: number;
}

export enum FileStatus {
  UPLOADING = 'UPLOADING',
  NORMAL = 'NORMAL',
  DELETED = 'DELETED'
}

export interface FileUploadResponse {
  id: UUID;
  fileMeta: FileMeta,
  fileStatus: FileStatus
}

export interface FileMeta {
  fileId: UUID;
  fileSize: number;
  contentType: string;
  filename: string;
  hash: string;
  duration?: number;
  thumbnailId?: string;
  fileStatus: FileStatus;
}

export interface UploadFileRequest {
  size: number;
  fileName: string;
  duration?: number;
}

// Electron API 接口定义
export interface ElectronAPI {
  // 认证相关
  login: (credentials: LoginCredentials) => Promise<ApiResponse<AuthData>>;
  logout: () => Promise<void>;

  // 文件操作相关
  selectFile: (options: SelectFileOptions) => Promise<SelectFileResult & { file?: File }>;
  selectFiles: (options: SelectFileOptions) => Promise<Array<SelectFileResult & { file?: File }>>;
  uploadFile: (fileOrPath: string | File, fileId: string, duration?: number) => Promise<FileUploadResponse>;
  getUploadId: (request: UploadFileRequest) => Promise<FileUploadResponse>;

  // 用户搜索相关
  searchUsers: (query: string, token?: string) => Promise<ApiSearchResults>;
  queryUsers: (query: string, token?: string) => Promise<ApiSearchResults>;

  // 通知相关
  showNotification: (title: string, body: string) => void;

  // Socket相关
  socketConnect: (config: { host: string; port: number; userId: string; token: string; username: string }) => Promise<any>;
  socketSend: (dataBase64: string) => Promise<any>;
  socketDisconnect: () => Promise<any>;
  socketIsActive: () => Promise<any>;
  socketSendMessage: (payload: any) => Promise<any>;
  socketMarkRead: (data: { conversationId: number; lastMsgId: number; status?: number }) => Promise<any>;

  // Socket事件监听
  onSocketMessage?: (handler: (data: any) => void) => void;
  onSocketConnected?: (handler: () => void) => void;
  onSocketDisconnected?: (handler: () => void) => void;
  onSocketError?: (handler: (error: any) => void) => void;
  onSocketReconnecting?: (handler: (data: any) => void) => void;

  // 系统相关
  getVersion: () => Promise<string>;
  getPath: (name: string) => Promise<string>;
}

// Web环境下的API实现
const webAPI: ElectronAPI = {
  // 认证相关
  login: async (credentials: LoginCredentials): Promise<ApiResponse<AuthData>> => {
    try {
      const response = await authAPI.login({
        loginAccount: credentials.loginAccount,
        password: credentials.password
      });

      return {
        success: true,
        data: response.data
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Login failed'
      };
    }
  },

  logout: async (): Promise<void> => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  // 文件操作相关
  selectFile: async (options: SelectFileOptions): Promise<SelectFileResult & { file?: File }> => {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      if (options.filters && options.filters.length > 0) {
        input.accept = options.filters.map(f => f.extensions.map(ext => `.${ext}`).join(',')).join(',');
      }
      input.onchange = (e) => {
        const files = (e.target as HTMLInputElement).files;
        if (files && files.length > 0) {
          const file = files[0];
          resolve({ canceled: false, filePaths: [file.name], file });
        } else {
          resolve({ canceled: true, filePaths: [] });
        }
      };
      input.click();
    });
  },

  selectFiles: async (options: SelectFileOptions): Promise<Array<SelectFileResult & { file?: File }>> => {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      if (options.filters && options.filters.length > 0) {
        input.accept = options.filters.map(f => f.extensions.map(ext => `.${ext}`).join(',')).join(',');
      }
      input.onchange = (e) => {
        const files = (e.target as HTMLInputElement).files;
        if (files && files.length > 0) {
          const results = Array.from(files).map(file => ({
            canceled: false,
            filePaths: [file.name],
            file
          }));
          resolve(results);
        } else {
          resolve([{ canceled: true, filePaths: [] }]);
        }
      };
      input.click();
    });
  },

  uploadFile: async (fileOrPath: string | File, fileId: string, duration?: number): Promise<FileUploadResponse> => {
    if (typeof fileOrPath === 'string') {
      throw new Error('Web environment only supports uploading File objects, not paths.');
    }

    try {
      const res = await fileAPI.upload(fileOrPath, fileId, duration);
      return res.data;
    } catch (error: any) {
      console.error('Web file upload error:', error);
      throw error;
    }
  },

  getUploadId: async (request: UploadFileRequest): Promise<FileUploadResponse> => {
    try {
      const res = await fileAPI.getUploadId(request);
      return res.data;
    } catch (error: any) {
      console.error('Web get upload ID error:', error);
      throw error;
    }
  },

  // 用户搜索相关
  searchUsers: async (query: string, token?: string): Promise<ApiSearchResults> => {
    try {
      // If token is provided, it's already handled by apiClient's interceptor if we use authAPI.
      // But we pass it here for clarity or if we want to override.
      const response = await authAPI.queryUsers(query);
      const content = response.data?.content || [];
      const users: User[] = content.map((u: ApiUser) => ({
        id: u.userId.toString(),
        username: u.username,
        email: u.email,
        phoneNumber: u.phoneNumber,
        status: 'online'
      }));
      return {
        success: true,
        data: { users, total: users.length },
        users: users,
        total: users.length
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.message,
        users: [],
        total: 0
      };
    }
  },

  queryUsers: async (query: string, token?: string) => webAPI.searchUsers(query, token),

  // 通知相关
  showNotification: (title: string, body: string) => {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, { body });
    }
  },

  // Socket相关 - Web环境下的实现为空或返回错误
  socketConnect: async (config: { host: string; port: number; userId: string; token: string; username: string }) => {
    throw new Error('Socket not supported in web environment');
  },

  socketSend: async (dataBase64: string) => {
    throw new Error('Socket not supported in web environment');
  },

  socketDisconnect: async () => {
    throw new Error('Socket not supported in web environment');
  },

  socketIsActive: async () => {
    return { active: false };
  },

  socketSendMessage: async (payload: any) => {
    throw new Error('Socket not supported in web environment');
  },

  socketMarkRead: async (data: any) => {
    throw new Error('Socket not supported in web environment');
  },

  getVersion: async () => '1.0.0-web',
  getPath: async (name: string) => `web-${name}`
};

// 获取Electron API实例
export function getElectronAPI(): ElectronAPI {
  const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

  if (isElectron) {
    const electron = (window as any).electronAPI;

    // Wrap methods to automatically include token from localStorage
    return {
      ...electron,
      socketMarkRead: async (data: any) => {
        if (electron.socketMarkRead) {
          return electron.socketMarkRead(data);
        }
        // Fallback if not injected (compatibility)
        console.warn('socket:mark-read not supported in bridge');
        return { success: false, error: 'Not supported' };
      },
      queryUsers: async (query: string, token?: string) => {
        const authToken = token || localStorage.getItem('token') || '';
        return electron.queryUsers(query, authToken);
      },
      searchUsers: async (query: string, token?: string) => {
        const authToken = token || localStorage.getItem('token') || '';
        return electron.queryUsers(query, authToken); // searchUsers usually calls queryUsers in this app
      },
      getUploadId: async (request: UploadFileRequest): Promise<FileUploadResponse> => {
        if (electron.getUploadId) {
          const res = await electron.getUploadId(request, localStorage.getItem('token') || '');
          // If IPC handler still returns {success, data}, extract it. 
          // But I will update IPC handler to return raw data later.
          return res.success !== undefined ? res.data : res;
        }
        return webAPI.getUploadId(request);
      },
      uploadFile: async (filePath: string | File, fileId: string, duration?: number): Promise<FileUploadResponse> => {
        if (electron.uploadFile) {
          const authToken = localStorage.getItem('token') || '';
          const res = await electron.uploadFile(filePath, fileId, authToken, duration);
          return res.success !== undefined ? res.data : res;
        }
        return webAPI.uploadFile(filePath, fileId, duration);
      }
    };
  }

  return webAPI;
}

export function isElectronEnvironment(): boolean {
  return typeof window !== 'undefined' && !!(window as any).electronAPI;
}