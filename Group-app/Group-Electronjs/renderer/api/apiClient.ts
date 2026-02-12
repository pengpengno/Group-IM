// apiClient.ts - Web 端真实接口实现（基于 Postman 文档）
import axios from 'axios';

export type LoginPayload = {
  loginAccount: string;
  password: string;
};

type QueryUsersPayload = string;

const REST_HOST = 'http://localhost:8080';

const http = axios.create({
  baseURL: REST_HOST,
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
  // POST {{rest-host}}/api/users/login
  login: async (payload: LoginPayload) => {
    return http.post('/api/users/login', payload);
  },

  // POST {{rest-host}}/api/users/query (form-data)
  queryUsers: async (query: QueryUsersPayload) => {
    const form = new FormData();
    form.append('query', query);
    return http.post('/api/users/query', form);
  },

  // GET {{rest-host}}/api/users/company/list
  getUserCompanies: async () => {
    return http.get('/api/users/company/list');
  }
};
