// 用户相关类型
export interface User {
  id: string;
  username: string;
  email: string;
  phoneNumber?: string;
  avatar?: string;
  status: 'online' | 'offline' | 'away' | 'busy';
  lastSeen?: Date;
}

// 数字ID用户类型（用于API响应）
export interface ApiUser {
  userId: number;
  username: string;
  email: string;
  phoneNumber: string;
}

// 字符串ID用户类型（用于本地状态）
export interface LocalUser {
  userId: string;
  username: string;
  email?: string;
  phoneNumber?: string;
  avatar?: string;
  status?: 'online' | 'offline' | 'away';
  token?: string;
  refreshToken?: string;
}

// 联系人专用用户类型（简化版User）
export interface ContactUser {
  userId: number;
  username: string;
  email: string;
  phoneNumber: string;
}

// 组织架构节点类型
export interface OrganizationNode {
  id: string;
  name: string;
  type: 'DEPARTMENT' | 'USER';
  description?: string;
  userInfo?: UserInfo;
  children?: OrganizationNode[];
}

// 用户信息类型（与User略有不同，用于组织架构）
export interface UserInfo {
  userId: number;
  username: string;
  email: string;
  avatar?: string;
  department?: string;
  position?: string;
}

// 认证相关类型
export interface LoginCredentials {
  loginAccount: string;
  password: string;
}

export interface AuthData {
  token: string;
  user: User;
  refreshToken?: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  user: LocalUser | null;
}

// 登录响应类型
export interface LoginResponse {
  userId: number;
  username: string;
  email: string;
  token: string;
  phoneNumber: string;
  refreshToken: string;
}

export interface LoginResult {
  success: boolean;
  data?: LoginResponse;
  error?: string;
}

// 文件操作相关类型
export interface FileFilter {
  name: string;
  extensions: string[];
}

export interface SelectFileOptions {
  title?: string;
  defaultPath?: string;
  filters?: FileFilter[];
  properties?: Array<'openFile' | 'openDirectory' | 'multiSelections' | 'showHiddenFiles'>;
}

export interface SelectFileResult {
  canceled: boolean;
  filePaths: string[];
  bookmarks?: string[];
}

// API响应类型
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

// 搜索相关类型
export interface SearchResults {
  users: User[];
  total: number;
}

export interface QueryUsersResponse {
  content?: ApiUser[];
  [key: string]: unknown; // Allow flexible response structure
}

// 聊天相关类型
export interface Message {
  id: string;
  senderId: string;
  receiverId: string;
  content: string;
  timestamp: Date;
  type: 'text' | 'image' | 'file' | 'system';
  status: 'sent' | 'delivered' | 'read';
}

export interface ChatSession {
  id: string;
  participants: User[];
  lastMessage?: Message;
  unreadCount: number;
  createdAt: Date;
  updatedAt: Date;
}

// 联系人相关类型
export interface Contact {
  user: User;
  addedAt: Date;
  isFavorite: boolean;
  notes?: string;
}

// 群组相关类型
export interface Group {
  id: string;
  name: string;
  description?: string;
  avatar?: string;
  members: User[];
  admins: User[];
  createdAt: Date;
  createdBy: User;
}

// 通知相关类型
export interface Notification {
  id: string;
  title: string;
  body: string;
  timestamp: Date;
  read: boolean;
  type: 'message' | 'friend_request' | 'system' | 'mention';
  data?: any;
}

// 应用状态类型
export interface AppState {
  auth: AuthState;
  chats: {
    sessions: ChatSession[];
    activeSessionId: string | null;
    loading: boolean;
  };
  contacts: {
    list: Contact[];
    groups: Group[];
    loading: boolean;
  };
  notifications: {
    list: Notification[];
    unreadCount: number;
  };
  ui: {
    activeTab: string;
    sidebarOpen: boolean;
    theme: 'light' | 'dark';
  };
}