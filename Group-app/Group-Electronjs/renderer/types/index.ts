// API用户信息接口
export interface UserInfo {
  userId: number;
  username: string;
  email: string;
  phoneNumber: string;
  refreshToken?: string;
}

// 基础用户信息接口
export interface BaseUserInfo {
  userId: number;
  username: string;
  email: string;
  phoneNumber?: string;
}

// 扩展用户信息接口
export interface ExtendedUserInfo extends BaseUserInfo {
  avatar?: string;
  department?: string;
  position?: string;
}

// 完整用户信息接口（包含状态等）
export interface FullUserInfo extends ExtendedUserInfo {
  status?: 'online' | 'offline' | 'away' | 'busy';
  lastSeen?: Date;
}

// 本地用户信息接口（字符串ID）
export interface LocalUserInfo {
  userId: string;
  username: string;
  email?: string;
  phoneNumber?: string;
  avatar?: string;
  status?: 'online' | 'offline' | 'away';
  token?: string;
  refreshToken?: string;
}

// 组织架构节点类型
export interface OrganizationNode {
  id: string;
  name: string;
  type: 'DEPARTMENT' | 'USER';
  description?: string;
  userInfo?: ExtendedUserInfo;
  children?: OrganizationNode[];
}

// 认证相关类型
export interface LoginCredentials {
  loginAccount: string;
  password: string;
}

export interface AuthData {
  token: string;
  user: FullUserInfo;
  refreshToken?: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  user: LocalUserInfo | null;
}

// API用户响应接口
export interface ApiUserResponse extends BaseUserInfo {
  refreshToken?: string;
}

// 登录响应类型
export interface LoginResponse extends ApiUserResponse {
  token: string;
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
  users: FullUserInfo[];
  total: number;
}

export interface QueryUsersResponse {
  content?: ApiUserResponse[];
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
  participants: FullUserInfo[];
  lastMessage?: Message;
  unreadCount: number;
  createdAt: Date;
  updatedAt: Date;
}

// 联系人用户接口
export interface ContactUser {
  userId: number;
  username: string;
  email: string;
  phoneNumber?: string;
  department?: string;
  position?: string;
  avatarColor?: string;
}

// 联系人相关类型
export interface Contact {
  user: FullUserInfo;
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
  members: FullUserInfo[];
  admins: FullUserInfo[];
  createdAt: Date;
  createdBy: FullUserInfo;
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