// electronAPI.ts - API abstraction for both Electron and Web environments
import { authAPI, LoginPayload } from './apiClient';
import type { LoginResponse, LoginResult, ApiUser as User } from '../../types';

type ApiResponse<T> = {
  data: T;
};

// Define the shape of our API
interface ElectronAPI {
  login: (credentials: LoginPayload) => Promise<LoginResult>;
  queryUsers: (query: string) => Promise<{ success: boolean; data?: User[]; error?: string }>;
  getUserCompanies: () => Promise<{ success: boolean; data?: any; error?: string }>;
  uploadFile: (filePath: string, clientId?: string) => Promise<any>;
  selectFile: (options?: Record<string, unknown>) => Promise<any>;
  showNotification: (options: {
    title?: string;
    body?: string;
    icon?: string;
    [key: string]: unknown;
  }) => Promise<any>;
  requestNotificationPermission: () => Promise<any>;
}

// Type definition for the window object
declare global {
  interface Window {
    electronAPI?: ElectronAPI;
  }
}

// Default implementation for web environment
const webAPI: ElectronAPI = {
  login: async (credentials: LoginPayload): Promise<LoginResult> => {
    try {
      const response: ApiResponse<LoginResponse> = await authAPI.login(credentials);
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

  queryUsers: async (query) => {
    try {
      const response = await authAPI.queryUsers(query);
      return {
        success: true,
        data: response.data
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Query failed'
      };
    }
  },

  getUserCompanies: async () => {
    try {
      const response = await authAPI.getUserCompanies();
      return {
        success: true,
        data: response.data
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Get companies failed'
      };
    }
  },

  uploadFile: async (filePath, clientId?) => {
    // In web environment, this would be handled differently
    // For now, returning a mock response
    return {
      success: true,
      data: { filePath, clientId }
    };
  },

  selectFile: async (options?) => {
    // In web environment, we'd use an input element
    // For now, returning a mock response
    return {
      canceled: true,
      filePath: null
    };
  },

  showNotification: async (options) => {
    // Use the native browser notification API
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        new Notification(options.title || 'Notification', {
          body: options.body,
          icon: options.icon
        });
        return { success: true };
      } else if (Notification.permission !== 'denied') {
        const permission = await Notification.requestPermission();
        if (permission === 'granted') {
          new Notification(options.title || 'Notification', {
            body: options.body,
            icon: options.icon
          });
        }
        return { success: true };
      }
    }
    return { success: false, error: 'Notifications not supported or denied' };
  },

  requestNotificationPermission: async () => {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      return { success: true, permission };
    }
    return { success: false, error: 'Notifications not supported' };
  }
};

// Return the appropriate API based on environment
export const getElectronAPI = (): ElectronAPI | null => {
  if (typeof window !== 'undefined' && window.electronAPI) {
    // Running in Electron environment
    return window.electronAPI;
  } else {
    // Running in web environment
    return webAPI;
  }
};