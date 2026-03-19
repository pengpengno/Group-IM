// apiClient.ts - Web 端真实接口实现（基于 Postman 文档）
import axios from 'axios';

export type LoginPayload = {
  loginAccount: string;
  password: string;
};

type QueryUsersPayload = string;

// In web environment, use the same origin. In Electron, fallback to hardcoded backend or env.
const isDev = typeof window !== 'undefined' && window.location.hostname === 'localhost';
const BASE_URL = (typeof window !== 'undefined' && window.location.protocol.startsWith('http')) 
  ? '' 
  : 'http://localhost:8080';

const http = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers ?? {};
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

  // POST /api/organization/company/register
  registerCompany: async (name: string, code: string) => {
    return http.post('/api/organization/company/register', { name, code });
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
  }
};
