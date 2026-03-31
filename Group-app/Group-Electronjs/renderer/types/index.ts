// 鐢ㄦ埛鐩稿叧绫诲瀷
export interface User {
  id: string;
  username: string;
  email: string;
  phoneNumber?: string;
  avatar?: string;
  status: 'online' | 'offline' | 'away' | 'busy';
  lastSeen?: Date;
}

// 鏁板瓧ID鐢ㄦ埛绫诲瀷锛堢敤浜嶢PI鍝嶅簲锛?
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

// 瀛楃涓睮D鐢ㄦ埛绫诲瀷锛堢敤浜庢湰鍦扮姸鎬侊級
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

// 缁勭粐鏋舵瀯鑺傜偣绫诲瀷
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
  members: ApiUser[] | null;
  children: DepartmentInfo[];
}

// 璁よ瘉鐩稿叧绫诲瀷
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

// 鏂囦欢鎿嶄綔鐩稿叧绫诲瀷
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

// API鍝嶅簲绫诲瀷
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

// 鑱婂ぉ浜や簰鐩稿叧绫诲瀷
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'VOICE' | 'VIDEO';

export interface MessageDTO {
  msgId: number;
  conversationId: number;
  content: string;
  fromAccountId: number;
  type: MessageType;
  timestamp: number;  // 姣鏃堕棿鎴?
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
  members: ApiUser[] | null;
  createAt: string;
  lastMessage?: MessageDTO;
}

export interface GroupConversationPayload {
  groupName: string;
  description?: string;
  members: ApiUser[];
}

export interface ConversationDisplayState {
  conversation: ConversationRes;
  lastMessage: string;
  displayDateTime: string;
  unreadCount: number;
}

// 搴旂敤鐘舵€佸竷灞€绫诲瀷
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
