// apiClient.ts - Web 端真实接口实现（基于 Postman 文档）
import axios from 'axios';

export type LoginPayload = {
  loginAccount: string;
  password: string;
};

type QueryUsersPayload = string;

// In development, API calls will be proxied through webpack dev server
// In production, use direct backend URL
const BASE_URL = process.env.NODE_ENV === 'development' ? '' : 'http://localhost:8080';

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
  // POST /api/users/login
  login: async (payload: LoginPayload) => {
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
  }
};