import type { 
  User, 
  AuthData, 
  LoginCredentials, 
  FileFilter, 
  SelectFileOptions, 
  SelectFileResult, 
  ApiResponse, 
  SearchResults 
} from '../types/index';
import { authAPI } from '../services/api/apiClient';
import type { LoginPayload } from '../services/api/apiClient';

// 扩展SearchResults类型以包含API响应结构
interface ApiSearchResults extends SearchResults {
  success: boolean;
  data?: SearchResults;
  error?: string;
}

// Electron API 接口定义
export interface ElectronAPI {
  // 认证相关
  login: (credentials: LoginCredentials) => Promise<ApiResponse<AuthData>>;
  logout: () => Promise<void>;
  
  // 文件操作相关
  selectFile: (options: SelectFileOptions) => Promise<SelectFileResult>;
  selectFiles: (options: SelectFileOptions) => Promise<SelectFileResult[]>;
  uploadFile: (filePath: string, clientId?: string) => Promise<any>; // 添加uploadFile方法
  
  // 用户搜索相关
  searchUsers: (query: string) => Promise<ApiSearchResults>;
  queryUsers: (query: string) => Promise<ApiSearchResults>; // 添加queryUsers方法以保持兼容性
  
  // 通知相关
  showNotification: (title: string, body: string) => void;
  
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
    try {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    } catch (error) {
      console.error('Logout error:', error);
    }
  },
  
  // 文件操作相关 - Web环境下使用浏览器API
  selectFile: async (options: SelectFileOptions): Promise<SelectFileResult> => {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = false;
      
      if (options.filters) {
        const acceptTypes = options.filters.map(filter => 
          filter.extensions.map(ext => `.${ext}`).join(',')
        ).join(',');
        input.accept = acceptTypes;
      }
      
      input.onchange = (event) => {
        const file = (event.target as HTMLInputElement)?.files?.[0];
        if (file) {
          resolve({
            canceled: false,
            filePaths: [file.name],
            bookmarks: undefined
          });
        } else {
          resolve({
            canceled: true,
            filePaths: [],
            bookmarks: undefined
          });
        }
      };
      
      input.click();
    });
  },
  
  selectFiles: async (options: SelectFileOptions): Promise<SelectFileResult[]> => {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      
      if (options.filters) {
        const acceptTypes = options.filters.map(filter => 
          filter.extensions.map(ext => `.${ext}`).join(',')
        ).join(',');
        input.accept = acceptTypes;
      }
      
      input.onchange = (event) => {
        const files = (event.target as HTMLInputElement)?.files;
        if (files && files.length > 0) {
          const filePaths = Array.from(files).map(file => file.name);
          resolve([{
            canceled: false,
            filePaths: filePaths,
            bookmarks: undefined
          }]);
        } else {
          resolve([{
            canceled: true,
            filePaths: [],
            bookmarks: undefined
          }]);
        }
      };
      
      input.click();
    });
  },
  
  uploadFile: async (filePath: string, clientId?: string): Promise<any> => {
    // Web环境下文件上传需要通过表单或者其他方式处理
    throw new Error('File upload not implemented for web environment');
  },
  
  // 用户搜索相关
  searchUsers: async (query: string): Promise<ApiSearchResults> => {
    try {
      const response = await authAPI.queryUsers(query);
      return {
        success: true,
        data: response.data,
        users: response.data?.content || [],
        total: response.data?.content?.length || 0
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Search failed',
        users: [],
        total: 0
      };
    }
  },
  
  queryUsers: async (query: string): Promise<ApiSearchResults> => {
    return webAPI.searchUsers(query);
  },
  
  // 通知相关 - Web环境下使用浏览器通知API
  showNotification: (title: string, body: string): void => {
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        new Notification(title, { body });
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            new Notification(title, { body });
          }
        });
      }
    }
  },
  
  // 系统相关 - Web环境下返回默认值
  getVersion: async (): Promise<string> => {
    return '1.0.0-web';
  },
  
  getPath: async (name: string): Promise<string> => {
    // Web环境下返回localStorage或其他web存储路径
    return `web-${name}`;
  }
};

// 获取Electron API实例
export function getElectronAPI(): ElectronAPI {
  // 在Electron环境中
  if (typeof window !== 'undefined' && (window as any).electronAPI) {
    const api = (window as any).electronAPI;
    // 创建代理对象来处理方法名映射
    return new Proxy(api, {
      get(target, prop) {
        if (prop === 'queryUsers') {
          // 将queryUsers映射到searchUsers
          return target.searchUsers || target.queryUsers;
        }
        return target[prop];
      }
    });
  }
  
  // 在Web环境中返回web API实现
  return webAPI;
}

// 判断是否在Electron环境中
export function isElectronEnvironment(): boolean {
  return typeof window !== 'undefined' && !!(window as any).electronAPI;
}
