// apiClient.ts - Web 端真实接口实现（基于 Postman 文档）
import axios from 'axios';

export type LoginPayload = {
  loginAccount: string;
  password: string;
};

type QueryUsersPayload = string;

// Always use injected API_BASE. If empty, browser will use current origin (ideal for Web proxy/Nginx).
export const BASE_URL = __API_BASE__;

const http = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authAPI = {
  // POST /api/users/register
  register: async (payload: any) => {
    return http.post('/api/users/register', payload);
  },

  // POST /api/users/login
  login: async (payload: LoginPayload & { companyCode?: string }) => {
    return http.post('/api/users/login', payload);
  },

  // POST /api/users/query (form-data)
  queryUsers: async (query: QueryUsersPayload) => {
    const form = new FormData();
    form.append('query', query);
    return http.post('/api/users/query', form);
  },

  // GET /api/users/company/list
  getUserCompanies: async () => {
    return http.get('/api/users/company/list');
  },

  // GET /api/company/my
  getMyCompanies: async () => {
    return http.get('/api/company/my');
  },

  // POST /api/company/switch/{companyId}
  switchCompany: async (companyId: number) => {
    return http.post(`/api/company/switch/${companyId}`);
  },

  // POST /api/organization/company/register
  registerCompany: async (name: string, code: string) => {
    return http.post('/api/organization/company/register', { name, code });
  },

  // GET /api/users/online/{userId}
  isUserOnline: async (userId: string | number) => {
    return http.get(`/api/users/online/${userId}`);
  }
};

export const conversationAPI = {
  // GET /api/conversations/{userId}/active
  getActiveConversations: async (userId: string) => {
    return http.get(`/api/conversations/${userId}/active`);
  },

  // POST /api/conversations/private-chat?userId={userId}&friendId={friendId}
  createPrivateChat: async (userId: string, friendId: number) => {
    return http.post('/api/conversations/private-chat', null, {
      params: { userId, friendId }
    });
  },

  createGroup: async (payload: { groupName: string; description?: string; members: Array<{ userId: number; username?: string; email?: string; phoneNumber?: string }> }) => {
    return http.post('/api/conversations/group', payload);
  },

  addGroupMembers: async (conversationId: number, userIds: number[]) => {
    return http.post(`/api/groups/${conversationId}/members/bulk`, userIds);
  },

  // POST /api/messages/pull
  pullMessages: async (conversationId: number, fromSequenceId: number = 0) => {
    return http.post('/api/messages/pull', {
      conversationId,
      page: 0,
      size: 50,
      fromSequenceId
    });
  },

  // POST /api/messages/send
  sendMessage: async (conversationId: number, content: string, type: string = 'TEXT') => {
    return http.post('/api/messages/send', {
      conversationId,
      content,
      type
    });
  }
};

export const orgAPI = {
  // GET /api/company/structure
  getStructure: async () => {
    return http.get('/api/company/structure');
  },

  // GET /api/organization/company (Admin only)
  getAllCompanies: async () => {
    return http.get('/api/organization/company');
  },

  // POST /api/organization/users/import
  importUsers: async (companyId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post(`/api/organization/users/import`, formData, {
      params: { companyId },
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },

  // GET /api/organization/users/template
  getImportTemplate: async () => {
    return http.get('/api/organization/users/template', {
      responseType: 'blob'
    });
  },

  // POST /api/organization/company/sync-schema
  syncSchema: async (companyIds?: number[]) => {
    return http.post('/api/organization/company/sync-schema', companyIds);
  }
};

export const webrtcAPI = {
  // GET /api/webrtc/ice-servers
  getIceServers: async () => {
    return http.get('/api/webrtc/ice-servers');
  }
};

export const meetingAPI = {
  create: async (payload: { conversationId: number; roomId?: string; title?: string; participantIds?: number[]; recordMessage?: boolean }) => {
    return http.post('/api/meetings/create', payload);
  },
  join: async (roomId: string) => {
    return http.post('/api/meetings/join', { roomId });
  },
  leave: async (roomId: string) => {
    return http.post('/api/meetings/leave', { roomId });
  },
  end: async (roomId: string, recordMessage: boolean = true) => {
    return http.post('/api/meetings/end', { roomId, recordMessage });
  },
  getByRoom: async (roomId: string) => {
    return http.get(`/api/meetings/room/${roomId}`);
  }
};

export const fileAPI = {
  // POST /api/files/uploadId
  getUploadId: async (request: { size: number; fileName: string; duration?: number }) => {
    return http.post('/api/files/uploadId', request);
  },

  // POST /api/files/upload (form-data)
  upload: async (file: File | Blob, fileId: string, duration?: number) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('fileId', fileId);
    if (duration) {
      formData.append('duration', duration.toString());
    }
    return http.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },

  // GET /api/files/download/{fileId}
  getDownloadUrl: (fileId: string) => `${BASE_URL}/api/files/download/${fileId}`
};
