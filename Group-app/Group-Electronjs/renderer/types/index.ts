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

export interface CompanyDTO {
  companyId: number;
  name: string;
  code: string;
  description?: string;
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
  currentCompany?: CompanyDTO;
  companies?: CompanyDTO[];
}

// 组织架构节点类型
export interface OrgTreeNode {
  id: number;
  name: string;
  type: 'DEPARTMENT' | 'USER';
  parentId: number;
  children: OrgTreeNode[];
  userInfo?: ApiUser;
  departmentInfo?: DepartmentInfo;
}

export interface DepartmentInfo {
  departmentId: number;
  name: string;
  parentId: number;
  description?: string;
  members: ApiUser[];
  children: DepartmentInfo[];
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
  companies?: CompanyDTO[];
  currentCompany?: CompanyDTO;
}

export interface AuthState {
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  user: LocalUser | null;
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

// 聊天交互相关类型
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'VOICE' | 'VIDEO';

export interface MessageDTO {
  msgId: number;
  conversationId: number;
  content: string;
  fromAccountId: number;
  type: MessageType;
  timestamp: number;  // 毫秒时间戳
  sequenceId?: number;
  fromAccount?: ApiUser;
  clientMsgId?: string;
  payload?: any;
}

export interface Message {
  id: string;
  senderId: string;
  receiverId: string;
  content: string;
  timestamp: Date;
  type: 'text' | 'image' | 'file' | string;
  status: 'sent' | 'delivered' | 'read' | string;
}

export enum ConversationType {
  GROUP = 'GROUP',
  PRIVATE_CHAT = 'PRIVATE_CHAT'
}

export interface ConversationRes {
  conversationId: number;
  type: ConversationType;
  groupName?: string;
  description?: string;
  members: ApiUser[];
  createAt: string;
  lastMessage?: MessageDTO;
}

export interface ConversationDisplayState {
  conversation: ConversationRes;
  lastMessage: string;
  displayDateTime: string;
  unreadCount: number;
}

// 应用状态布局类型
export type ActiveTab = 'home' | 'chats' | 'contacts' | 'settings';

export interface RootState {
  auth: AuthState;
  videoCall: any; // Simplified for now
  chat: {
    conversations: ConversationDisplayState[];
    activeConversationId: number | null;
    messages: Record<number, MessageDTO[]>;
    loading: boolean;
  };
  contacts: {
    orgTree: OrgTreeNode[];
    loading: boolean;
  };
}
