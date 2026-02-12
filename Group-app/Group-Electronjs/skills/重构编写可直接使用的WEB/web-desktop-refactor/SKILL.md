# Skill: Group IM Electron 项目重构为 Web / Desktop 可共用结构

## 描述

用于将 Group IM Electron 桌面端项目重构为现代化的跨平台架构，实现：

- **业务逻辑统一**：基于 Kotlin Multiplatform 共享核心业务逻辑
- **UI/UX一致性**：保持移动端设计语言，适配Web/Desktop交互特点
- **平台能力隔离**：通过 platform 层封装所有平台差异
- **高效开发流程**：一套代码多端运行，降低维护成本

适用于希望实现三端（移动端/Web端/Desktop端）体验一致性的 Group IM 项目。

---

## 触发示例

当用户输入以下或相近语义时触发本技能：

- 重构 Group IM Electron 项目，实现三端统一
- 将移动端UI设计适配到Web和Desktop平台
- 实现Group IM的跨平台统一架构
- 保持移动端设计风格的同时优化Web/Desktop交互
- 重构为支持移动端/Web/Desktop一致体验的架构

---

## 执行规范（必须按顺序执行）

### 1. 项目现状分析

识别并列出 Group IM 项目的关键特征：

**移动端设计特色**：
- Material Design 3 设计语言
- 蓝色渐变主题 (#1976d2 → #64b5f6)
- 卡片式布局和圆角设计
- 底部导航栏交互模式
- 侧滑抽屉菜单
- 下拉刷新手势操作

**核心业务模块**：
- 用户认证与状态管理
- 实时消息通信
- 联系人管理系统
- 音视频通话功能
- 文件传输服务

**技术架构现状**：
- Kotlin Multiplatform 共享业务逻辑
- Compose Multiplatform UI 框架
- Electron 桌面应用容器
- React + TypeScript 前端技术栈

### 2. 代码扫描与分类

识别现有代码的依赖关系和平台特性：

**A. KMP 共享业务代码**（位于 `composeApp/src/commonMain/kotlin/`）：
- 用户认证服务 (`UserRepository`, `UserViewModel`)
- 消息通信服务 (`ChatViewModel`, `ChatRoomViewModel`)
- 联系人管理 (`ContactsViewModel`, `FriendShipRepository`)
- 音视频通话 (`WebRTCManager`, `VideoCallViewModel`)
- 状态管理 (`LoginStateManager`, `GlobalErrorHandler`)
- 领域模型 (`UserInfo`, `Conversation`, `Message`)

**B. UI 组件代码**（React 组件）：
- 登录界面 (`LoginScreen`)
- 主界面 (`MainScreen`)
- 聊天界面 (`ChatList`, `ChatRoomScreen`)
- 联系人界面 (`ContactsScreen`)
- 个人中心 (`ProfileScreen`)
- 视频通话界面 (`VideoCallScreen`)

**C. Electron 桌面能力代码**：
- 主进程管理 (`main/index.ts`)
- IPC 通信处理器 (`main/ipc-handlers/`)
- 预加载脚本 (`main/preload.ts`)
- 窗口管理和系统托盘
- 文件系统操作
- 原生对话框集成

**D. 平台桥接代码**：
- Electron API 封装 (`renderer/api/electronAPI.ts`)
- HTTP 客户端适配 (`services/api/apiClient.ts`)
- 类型定义统一 (`types/index.ts`)

### 3. 目标架构设计

生成符合 Group IM 项目实际目录结构的目标架构：

```
Group-app/
├── Group/                                  # 移动端项目（KMP）
│   ├── composeApp/                        # KMP 共享业务逻辑
│   │   ├── src/
│   │   │   ├── commonMain/kotlin/         # 跨平台核心业务
│   │   │   │   ├── com/github/im/group/
│   │   │   │   │   ├── viewmodel/         # 状态管理
│   │   │   │   │   ├── repository/        # 数据访问
│   │   │   │   │   ├── manager/           # 业务管理器
│   │   │   │   │   ├── sdk/               # 平台服务
│   │   │   │   │   └── model/             # 领域模型
│   │   │   │   └── proto/                 # 协议定义
│   │   │   ├── androidMain/kotlin/        # Android 特有实现
│   │   │   ├── iosMain/kotlin/            # iOS 特有实现
│   │   │   └── jsMain/kotlin/             # Web/Desktop 共享实现
│   │   └── build.gradle.kts
│   └── README.md
│
├── Group-Electronjs/                      # Web/Desktop 项目
│   ├── main/                              # Electron 主进程
│   │   ├── index.ts                       # 主进程入口
│   │   ├── preload.ts                     # 预加载脚本
│   │   └── ipc-handlers/                  # IPC 处理器
│   │       ├── auth-handler.ts
│   │       ├── file-handler.ts
│   │       └── notification-handler.ts
│   ├── renderer/                          # 渲染进程（前端代码）
│   │   ├── api/                           # API 客户端
│   │   │   ├── apiClient.ts
│   │   │   └── electronAPI.ts
│   │   ├── features/                      # 功能模块
│   │   │   ├── auth/                      # 认证模块
│   │   │   │   ├── LoginScreen.css
│   │   │   │   ├── LoginScreen.tsx
│   │   │   │   └── authSlice.ts
│   │   │   ├── chat/                      # 聊天模块
│   │   │   │   ├── ChatList.css
│   │   │   │   └── ChatList.tsx
│   │   │   ├── contacts/                  # 联系人模块
│   │   │   │   ├── ContactsScreen.css
│   │   │   │   └── ContactsScreen.tsx
│   │   │   ├── profile/                   # 个人中心
│   │   │   │   ├── ProfileScreen.css
│   │   │   │   └── ProfileScreen.tsx
│   │   │   └── video-call/                # 视频通话
│   │   │       ├── VideoCallManager.ts
│   │   │       ├── VideoCallScreen.css
│   │   │       ├── VideoCallScreen.tsx
│   │   │       └── useVideoCall.ts
│   │   ├── services/                      # 服务层
│   │   │   └── api/
│   │   │       ├── apiClient.ts
│   │   │       └── electronAPI.ts
│   │   ├── types/                         # 类型定义
│   │   │   └── index.ts
│   │   ├── App.tsx                        # 主应用组件
│   │   ├── index.html                     # HTML 模板
│   │   ├── index.tsx                      # 渲染进程入口
│   │   └── store.ts                       # Redux 状态管理
│   ├── skills/                            # 技能文档
│   │   └── 重构编写可直接使用的WEB/
│   │       └── web-desktop-refactor/
│   │           └── SKILL.md
│   ├── package.json
│   ├── tsconfig.json
│   ├── web.webpack.config.js             # Web 开发配置
│   ├── webpack.config.js                 # 主进程构建配置
│   └── webpack.renderer.config.js        # 渲染进程构建配置
│
└── doc/                                   # 文档目录
    ├── 三端统一功能架构大纲.md
    ├── Web_Desktop_UI_UX改造实施计划.md
    ├── 三端开发协作指南.md
    └── 功能文档/
        └── 登录流程详细文档.md
```

### 4. 统一平台接口设计

设计符合 Group IM 业务需求的平台抽象接口：

``typescript
// Group-Electronjs/renderer/types/index.ts
export interface PlatformApi {
  // 认证相关
  login(credentials: LoginCredentials): Promise<AuthResult>;
  logout(): Promise<void>;
  getCurrentUser(): Promise<UserInfo | null>;
  
  // 文件操作
  selectFile(options?: SelectFileOptions): Promise<FileSelectionResult>;
  selectMultipleFiles(options?: SelectFileOptions): Promise<FileSelectionResult[]>;
  saveFile(data: Uint8Array, filename: string): Promise<boolean>;
  
  // 用户搜索
  searchUsers(query: string): Promise<SearchResults>;
  
  // 系统通知
  showNotification(title: string, options?: NotificationOptions): Promise<void>;
  requestNotificationPermission(): Promise<NotificationPermission>;
  
  // 网络状态
  getNetworkStatus(): NetworkStatus;
  onNetworkStatusChange(callback: (status: NetworkStatus) => void): void;
  
  // 音视频设备
  getMediaDevices(): Promise<MediaDeviceInfo[]>;
  getUserMedia(constraints: MediaStreamConstraints): Promise<MediaStream>;
  
  // 本地存储
  localStorage: {
    getItem(key: string): Promise<string | null>;
    setItem(key: string, value: string): Promise<void>;
    removeItem(key: string): Promise<void>;
  };
  
  // 桌面特有能力（可选）
  minimizeWindow?(): Promise<void>;
  maximizeWindow?(): Promise<void>;
  setTrayIcon?(iconPath: string): Promise<void>;
  createTrayMenu?(menuItems: TrayMenuItem[]): Promise<void>;
}

// 数据类型定义
export interface UserInfo {
  userId: number;
  username: string;
  email: string;
  avatar?: string;
  status: 'online' | 'offline' | 'away';
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface AuthResult {
  success: boolean;
  user?: UserInfo;
  token?: string;
  refreshToken?: string;
  error?: string;
}

export interface SelectFileOptions {
  title?: string;
  filters?: FileFilter[];
  multiSelection?: boolean;
}

export interface FileFilter {
  name: string;
  extensions: string[];
}

export interface FileSelectionResult {
  canceled: boolean;
  filePaths: string[];
  bookmarks?: string[];
}

export interface SearchResults {
  users: UserInfo[];
  total: number;
}

export interface NetworkStatus {
  online: boolean;
  type: 'wifi' | 'cellular' | 'ethernet' | 'unknown';
}

export interface TrayMenuItem {
  label: string;
  click: () => void;
  enabled?: boolean;
}
```

### 5. Desktop 平台实现

基于 Electron 实现桌面端平台能力：

``typescript
// Group-Electronjs/renderer/services/api/electronAPI.ts
import { PlatformApi, UserInfo, LoginCredentials, AuthResult } from '../types';
import { ipcRenderer } from 'electron';

export class DesktopPlatformApi implements PlatformApi {
  // 认证相关
  async login(credentials: LoginCredentials): Promise<AuthResult> {
    try {
      const result = await ipcRenderer.invoke('login', credentials);
      return {
        success: result.success,
        user: result.user,
        token: result.token,
        refreshToken: result.refreshToken,
        error: result.error
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Login failed'
      };
    }
  }

  async logout(): Promise<void> {
    await ipcRenderer.invoke('logout');
  }

  async getCurrentUser(): Promise<UserInfo | null> {
    return await ipcRenderer.invoke('get-current-user');
  }

  // 文件操作
  async selectFile(options?: SelectFileOptions): Promise<FileSelectionResult> {
    try {
      const result = await ipcRenderer.invoke('select-file', options);
      return {
        canceled: result.canceled,
        filePaths: result.filePaths || [],
        bookmarks: result.bookmarks
      };
    } catch (error) {
      return {
        canceled: true,
        filePaths: []
      };
    }
  }

  async selectMultipleFiles(options?: SelectFileOptions): Promise<FileSelectionResult[]> {
    try {
      const results = await ipcRenderer.invoke('select-multiple-files', options);
      return results.map((result: any) => ({
        canceled: result.canceled,
        filePaths: result.filePaths || [],
        bookmarks: result.bookmarks
      }));
    } catch (error) {
      return [{
        canceled: true,
        filePaths: []
      }];
    }
  }

  async saveFile(data: Uint8Array, filename: string): Promise<boolean> {
    try {
      const result = await ipcRenderer.invoke('save-file', { data, filename });
      return !result.canceled;
    } catch (error) {
      return false;
    }
  }

  // 用户搜索
  async searchUsers(query: string): Promise<SearchResults> {
    try {
      const result = await ipcRenderer.invoke('search-users', query);
      return {
        users: result.users || [],
        total: result.total || 0
      };
    } catch (error) {
      return {
        users: [],
        total: 0
      };
    }
  }

  // 系统通知
  async showNotification(title: string, options?: NotificationOptions): Promise<void> {
    await ipcRenderer.invoke('show-notification', { title, ...options });
  }

  async requestNotificationPermission(): Promise<NotificationPermission> {
    return await ipcRenderer.invoke('request-notification-permission');
  }

  // 网络状态
  getNetworkStatus(): NetworkStatus {
    return {
      online: navigator.onLine,
      type: 'unknown' // Electron 可以提供更多网络信息
    };
  }

  onNetworkStatusChange(callback: (status: NetworkStatus) => void): void {
    const handleOnline = () => callback(this.getNetworkStatus());
    const handleOffline = () => callback(this.getNetworkStatus());
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    // 返回清理函数
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }

  // 音视频设备
  async getMediaDevices(): Promise<MediaDeviceInfo[]> {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      return devices;
    } catch (error) {
      return [];
    }
  }

  async getUserMedia(constraints: MediaStreamConstraints): Promise<MediaStream> {
    return await navigator.mediaDevices.getUserMedia(constraints);
  }

  // 本地存储
  localStorage = {
    async getItem(key: string): Promise<string | null> {
      return localStorage.getItem(key);
    },
    
    async setItem(key: string, value: string): Promise<void> {
      localStorage.setItem(key, value);
    },
    
    async removeItem(key: string): Promise<void> {
      localStorage.removeItem(key);
    }
  };

  // 桌面特有能力
  async minimizeWindow(): Promise<void> {
    await ipcRenderer.invoke('minimize-window');
  }

  async maximizeWindow(): Promise<void> {
    await ipcRenderer.invoke('maximize-window');
  }

  async setTrayIcon(iconPath: string): Promise<void> {
    await ipcRenderer.invoke('set-tray-icon', iconPath);
  }

  async createTrayMenu(menuItems: TrayMenuItem[]): Promise<void> {
    await ipcRenderer.invoke('create-tray-menu', menuItems);
  }
}
```

### 6. Web 平台实现

实现浏览器环境的平台能力：

``typescript
// Group-Electronjs/renderer/services/api/webAPI.ts
import { PlatformApi, UserInfo, LoginCredentials, AuthResult, FileSelectionResult } from '../types';

export class WebPlatformApi implements PlatformApi {
  // 认证相关
  async login(credentials: LoginCredentials): Promise<AuthResult> {
    try {
      // 使用 HTTP API 进行认证
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
      });
      
      const result = await response.json();
      
      if (response.ok) {
        // 保存认证信息
        localStorage.setItem('token', result.token);
        localStorage.setItem('refreshToken', result.refreshToken);
        localStorage.setItem('user', JSON.stringify(result.user));
        
        return {
          success: true,
          user: result.user,
          token: result.token,
          refreshToken: result.refreshToken
        };
      } else {
        return {
          success: false,
          error: result.message || 'Login failed'
        };
      }
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Network error'
      };
    }
  }

  async logout(): Promise<void> {
    try {
      // 调用登出API
      await fetch('/api/logout', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
    } catch (error) {
      console.error('Logout API call failed:', error);
    } finally {
      // 清理本地存储
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  }

  async getCurrentUser(): Promise<UserInfo | null> {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  }

  // 文件操作 - 使用浏览器原生API
  async selectFile(options?: SelectFileOptions): Promise<FileSelectionResult> {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = false;
      
      if (options?.filters) {
        const acceptTypes = options.filters
          .map(filter => filter.extensions.map(ext => `.${ext}`).join(','))
          .join(',');
        input.accept = acceptTypes;
      }
      
      input.onchange = (event) => {
        const file = (event.target as HTMLInputElement)?.files?.[0];
        if (file) {
          resolve({
            canceled: false,
            filePaths: [file.name],
            fileObjects: [file] // Web端额外提供File对象
          });
        } else {
          resolve({
            canceled: true,
            filePaths: []
          });
        }
      };
      
      input.click();
    });
  }

  async selectMultipleFiles(options?: SelectFileOptions): Promise<FileSelectionResult[]> {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      
      if (options?.filters) {
        const acceptTypes = options.filters
          .map(filter => filter.extensions.map(ext => `.${ext}`).join(','))
          .join(',');
        input.accept = acceptTypes;
      }
      
      input.onchange = (event) => {
        const files = (event.target as HTMLInputElement)?.files;
        if (files && files.length > 0) {
          resolve([{
            canceled: false,
            filePaths: Array.from(files).map(f => f.name),
            fileObjects: Array.from(files)
          }]);
        } else {
          resolve([{
            canceled: true,
            filePaths: []
          }]);
        }
      };
      
      input.click();
    });
  }

  async saveFile(data: Uint8Array, filename: string): Promise<boolean> {
    try {
      const blob = new Blob([data], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      
      URL.revokeObjectURL(url);
      return true;
    } catch (error) {
      console.error('File save failed:', error);
      return false;
    }
  }

  // 用户搜索 - 使用HTTP API
  async searchUsers(query: string): Promise<SearchResults> {
    try {
      const response = await fetch(`/api/users/search?q=${encodeURIComponent(query)}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        return {
          users: result.users || [],
          total: result.total || 0
        };
      } else {
        return {
          users: [],
          total: 0
        };
      }
    } catch (error) {
      console.error('User search failed:', error);
      return {
        users: [],
        total: 0
      };
    }
  }

  // 系统通知
  async showNotification(title: string, options?: NotificationOptions): Promise<void> {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body: options?.body,
        icon: options?.icon,
        ...options
      });
    }
  }

  async requestNotificationPermission(): Promise<NotificationPermission> {
    if ('Notification' in window) {
      return await Notification.requestPermission();
    }
    return 'denied';
  }

  // 网络状态
  getNetworkStatus(): NetworkStatus {
    return {
      online: navigator.onLine,
      type: this.getNetworkType()
    };
  }

  private getNetworkType(): 'wifi' | 'cellular' | 'ethernet' | 'unknown' {
    // 简化实现，实际项目中可以使用更精确的检测
    return 'unknown';
  }

  onNetworkStatusChange(callback: (status: NetworkStatus) => void): () => void {
    const handleOnline = () => callback(this.getNetworkStatus());
    const handleOffline = () => callback(this.getNetworkStatus());
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }

  // 音视频设备
  async getMediaDevices(): Promise<MediaDeviceInfo[]> {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      return devices;
    } catch (error) {
      return [];
    }
  }

  async getUserMedia(constraints: MediaStreamConstraints): Promise<MediaStream> {
    return await navigator.mediaDevices.getUserMedia(constraints);
  }

  // 本地存储
  localStorage = {
    async getItem(key: string): Promise<string | null> {
      return localStorage.getItem(key);
    },
    
    async setItem(key: string, value: string): Promise<void> {
      localStorage.setItem(key, value);
    },
    
    async removeItem(key: string): Promise<void> {
      localStorage.removeItem(key);
    }
  };

  // Web端不支持的桌面特有能力
  async minimizeWindow(): Promise<void> {
    throw new Error('Window management not supported in web browser');
  }

  async maximizeWindow(): Promise<void> {
    throw new Error('Window management not supported in web browser');
  }

  async setTrayIcon(): Promise<void> {
    throw new Error('System tray not supported in web browser');
  }

  async createTrayMenu(): Promise<void> {
    throw new Error('System tray not supported in web browser');
  }
}
```

### 7. 平台注入统一入口

创建平台抽象层的统一入口点：

``typescript
// Group-Electronjs/renderer/api/platform.ts
import { PlatformApi } from '../types';
import { DesktopPlatformApi } from './electronAPI';
import { WebPlatformApi } from './webAPI';

// 检测运行环境
const isElectron = (): boolean => {
  return typeof window !== 'undefined' && 
         typeof (window as any).electron !== 'undefined';
};

// 检测是否为桌面环境
const isDesktop = (): boolean => {
  return isElectron() || 
         (typeof navigator !== 'undefined' && 
          /electron/i.test(navigator.userAgent));
};

// 根据环境选择合适的平台实现
export const platformApi: PlatformApi = isDesktop() 
  ? new DesktopPlatformApi() 
  : new WebPlatformApi();

// 环境检测工具函数
export const platformDetection = {
  isElectron,
  isDesktop,
  isWeb: () => !isDesktop(),
  
  // 获取平台信息
  getInfo: () => ({
    isElectron: isElectron(),
    isDesktop: isDesktop(),
    isWeb: !isDesktop(),
    userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : '',
    platform: typeof navigator !== 'undefined' ? navigator.platform : ''
  })
};

// 导出类型定义
export type { 
  PlatformApi, 
  UserInfo, 
  LoginCredentials, 
  AuthResult,
  SelectFileOptions,
  FileSelectionResult,
  SearchResults,
  NetworkStatus,
  TrayMenuItem
} from '../types';
```

### 8. 业务代码重构示例

以登录功能为例，展示如何重构业务代码：

**改造前**（直接使用 Electron）：
``typescript
// Group-Electronjs/renderer/features/auth/LoginScreen.tsx
import { ipcRenderer } from 'electron';

const handleLogin = async (username: string, password: string) => {
  try {
    setLoading(true);
    const result = await ipcRenderer.invoke('login', { username, password });
    
    if (result.success) {
      dispatch(loginSuccess({
        user: result.user,
        token: result.token,
        refreshToken: result.refreshToken
      }));
      navigate('/main');
    } else {
      setError(result.error || 'Login failed');
    }
  } catch (error) {
    setError('Network error');
  } finally {
    setLoading(false);
  }
};
```

**改造后**（使用平台抽象）：
``typescript
// Group-Electronjs/renderer/features/auth/LoginScreen.tsx
import { platformApi } from '../../api/platform';
import { useAppDispatch } from '../../store';
import { loginSuccess } from './authSlice';

const LoginScreen: React.FC = () => {
  const dispatch = useAppDispatch();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const handleLogin = async (username: string, password: string) => {
    try {
      setLoading(true);
      setError(null);
      
      const result = await platformApi.login({ username, password });
      
      if (result.success && result.user && result.token) {
        dispatch(loginSuccess({
          user: result.user,
          token: result.token,
          refreshToken: result.refreshToken || ''
        }));
        
        // 导航到主界面
        window.location.hash = '#/main';
      } else {
        setError(result.error || 'Login failed');
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      {/* 登录表单UI */}
      <LoginForm 
        onSubmit={handleLogin}
        loading={loading}
        error={error}
      />
    </div>
  );
};
```

### 9. Electron Preload 脚本重构

简化 preload 脚本，只暴露必要的 IPC 接口：

``typescript
// Group-Electronjs/main/preload.ts
import { contextBridge, ipcRenderer } from 'electron';
import { 
  LoginCredentials, 
  UserInfo,
  SelectFileOptions,
  NotificationOptions,
  TrayMenuItem
} from '../renderer/types';

// 定义安全的 Electron API
interface SafeElectronAPI {
  // 认证相关
  login: (credentials: LoginCredentials) => Promise<any>;
  logout: () => Promise<void>;
  getCurrentUser: () => Promise<UserInfo | null>;
  
  // 文件操作
  selectFile: (options?: SelectFileOptions) => Promise<any>;
  selectMultipleFiles: (options?: SelectFileOptions) => Promise<any>;
  saveFile: (params: { data: Uint8Array; filename: string }) => Promise<any>;
  
  // 用户搜索
  searchUsers: (query: string) => Promise<any>;
  
  // 通知相关
  showNotification: (params: { title: string; options?: NotificationOptions }) => Promise<void>;
  requestNotificationPermission: () => Promise<NotificationPermission>;
  
  // 窗口管理
  minimizeWindow: () => Promise<void>;
  maximizeWindow: () => Promise<void>;
  
  // 系统托盘
  setTrayIcon: (iconPath: string) => Promise<void>;
  createTrayMenu: (menuItems: TrayMenuItem[]) => Promise<void>;
}

// 安全地暴露 Electron API
contextBridge.exposeInMainWorld('electron', {
  // 认证相关
  login: (credentials: LoginCredentials) => ipcRenderer.invoke('login', credentials),
  logout: () => ipcRenderer.invoke('logout'),
  getCurrentUser: () => ipcRenderer.invoke('get-current-user'),
  
  // 文件操作
  selectFile: (options?: SelectFileOptions) => ipcRenderer.invoke('select-file', options),
  selectMultipleFiles: (options?: SelectFileOptions) => ipcRenderer.invoke('select-multiple-files', options),
  saveFile: (params: { data: Uint8Array; filename: string }) => ipcRenderer.invoke('save-file', params),
  
  // 用户搜索
  searchUsers: (query: string) => ipcRenderer.invoke('search-users', query),
  
  // 通知相关
  showNotification: (params: { title: string; options?: NotificationOptions }) => 
    ipcRenderer.invoke('show-notification', params),
  requestNotificationPermission: () => ipcRenderer.invoke('request-notification-permission'),
  
  // 窗口管理
  minimizeWindow: () => ipcRenderer.invoke('minimize-window'),
  maximizeWindow: () => ipcRenderer.invoke('maximize-window'),
  
  // 系统托盘
  setTrayIcon: (iconPath: string) => ipcRenderer.invoke('set-tray-icon', iconPath),
  createTrayMenu: (menuItems: TrayMenuItem[]) => ipcRenderer.invoke('create-tray-menu', menuItems)
} satisfies SafeElectronAPI);
```

### 10. KMP 共享模块集成

确保前端能够使用 KMP 共享的业务逻辑：

``typescript
// Group-Electronjs/renderer/services/UserService.ts
import { platformApi } from '../api/platform';

export class UserService {
  private static instance: UserService;
  
  private constructor() {}
  
  static getInstance(): UserService {
    if (!UserService.instance) {
      UserService.instance = new UserService();
    }
    return UserService.instance;
  }
  
  async login(username: string, password: string) {
    const result = await platformApi.login({ username, password });
    
    if (result.success) {
      // 可以在这里调用 KMP 共享的业务逻辑
      // 例如：用户状态同步、数据初始化等
      await this.initializeUserSession(result.user!);
    }
    
    return result;
  }
  
  async logout() {
    await platformApi.logout();
    // 清理本地状态
    this.cleanupUserSession();
  }
  
  private async initializeUserSession(user: any) {
    // 初始化用户会话
    // 可以调用 KMP 共享的状态管理逻辑
    console.log('Initializing user session for:', user);
  }
  
  private cleanupUserSession() {
    // 清理会话数据
    console.log('Cleaning up user session');
  }
}
```

### 11. 重构实施计划

**第一阶段：基础设施搭建**（1-2周）
- [ ] 在 `Group-Electronjs/renderer/api/` 目录下创建平台抽象层
- [ ] 实现 `platform.ts` 统一入口文件
- [ ] 完成 Desktop 和 Web 平台实现
- [ ] 配置 Webpack 开发环境

**第二阶段：核心功能迁移**（2-3周）
- [ ] 迁移认证模块到新架构
- [ ] 重构聊天功能组件
- [ ] 实现联系人管理功能
- [ ] 集成音视频通话能力

**第三阶段：UI/UX 优化**（1-2周）
- [ ] 实现响应式设计
- [ ] 优化交互体验
- [ ] 统一主题样式
- [ ] 完善动画效果

**第四阶段：测试与优化**（1周）
- [ ] 完整功能测试
- [ ] 性能优化
- [ ] 跨平台兼容性验证
- [ ] 用户体验调优

### 12. 重构结果输出

**新增文件列表**：
```
Group-Electronjs/
├── renderer/
│   ├── api/
│   │   ├── platform.ts              # 平台抽象统一入口
│   │   ├── webAPI.ts                # Web平台实现
│   │   └── electronAPI.ts           # Desktop平台实现（已存在）
│   └── services/
│       └── UserService.ts            # 用户服务封装
└── main/
    └── preload.ts                    # 更新的预加载脚本
```

**修改文件列表**：
```
Group-Electronjs/renderer/features/auth/LoginScreen.tsx  # 使用平台抽象
Group-Electronjs/renderer/features/chat/ChatList.tsx     # 使用平台抽象
Group-Electronjs/renderer/features/contacts/ContactsScreen.tsx  # 使用平台抽象
Group-Electronjs/renderer/store.ts                       # 可能需要调整状态结构
```

**删除或废弃文件列表**：
```
Group-Electronjs/renderer/features/auth/AuthService.ts   # 迁移到 services/UserService.ts
Group-Electronjs/renderer/features/file/FileService.ts   # 迁移到平台抽象层
```

**当前仍无法跨平台的能力说明**：
1. **系统托盘功能** - 仅 Desktop 支持
2. **窗口管理** - 仅 Desktop 支持  
3. **原生文件对话框** - Web 端使用 HTML5 替代
4. **后台运行** - Web 端受浏览器限制
5. **系统级通知** - Web 端需要用户授权

## 强制约束

- `renderer/services/` 层禁止出现 Electron、fs、path、ipcRenderer 等引用
- 所有平台差异必须集中在 `renderer/api/` 层
- UI 层禁止直接访问 Electron API
- 必须通过平台抽象层访问所有原生能力
- 保持与 KMP 共享模块的类型一致性

## 输出格式要求

最终输出必须包含：

1. **新目录结构树**
2. **平台接口定义代码**
3. **Desktop 实现代码**
4. **Web 实现代码**
5. **一个业务模块改造前 / 改造后的代码对比示例**

## 项目特定注意事项

- 项目使用 React 18 + TypeScript + Redux Toolkit
- 构建工具为 Webpack
- 包含音视频通话功能（WebRTC）
- 需要考虑移动端 UI 设计的响应式适配
- 存在多个 types 文件需要合并统一
- 保持 Material Design 3 设计语言
- 维护蓝色主题色彩体系
- 严格遵循当前项目的三层结构：Group（移动端）/ Group-Electronjs（Web/Desktop）/ doc（文档）