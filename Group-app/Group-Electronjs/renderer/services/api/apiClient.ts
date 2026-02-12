// apiClient.ts - Web 端真实接口实现（基于 Postman 文档）
import axios from 'axios';
import type { UserInfo } from '../../types';

export type LoginPayload = {
  loginAccount: string;
  password: string;
};

type QueryUsersPayload = string;

// 组织架构相关类型定义
export interface Department {
  departmentId: number;
  name: string;
  description?: string;
  companyId: number;
  parentId?: number;
  orderNum?: number;
  status: boolean;
  children?: Department[];
  members?: UserInfo[];
}

export interface OrganizationStructure {
  departmentId: number;
  name: string;
  description?: string;
  companyId: number;
  children?: Department[];
  members?: UserInfo[];
}

// Use proxy in development, direct URL in production
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

// 组织架构相关API
export const companyAPI = {
  // GET /api/company/structure
  getOrganizationStructure: async (): Promise<{ data: OrganizationStructure }> => {
    return http.get('/api/company/structure');
  },

  // POST /api/company/department
  createDepartment: async (payload: {
    name: string;
    description?: string;
    parentId?: number;
    orderNum?: number;
  }) => {
    return http.post('/api/company/department', payload);
  },

  // PUT /api/company/department/{departmentId}
  updateDepartment: async (departmentId: number, payload: {
    name: string;
    description?: string;
    orderNum?: number;
    status: boolean;
  }) => {
    return http.put(`/api/company/department/${departmentId}`, payload);
  },

  // DELETE /api/company/department/{departmentId}
  deleteDepartment: async (departmentId: number) => {
    return http.delete(`/api/company/department/${departmentId}`);
  },

  // PATCH /api/company/department/{departmentId}/move
  moveDepartment: async (departmentId: number, newParentId?: number) => {
    const params = newParentId ? { newParentId } : {};
    return http.patch(`/api/company/department/${departmentId}/move`, null, { params });
  },

  // POST /api/company/department/{departmentId}/user/{userId}
  assignUserToDepartment: async (departmentId: number, userId: number) => {
    return http.post(`/api/company/department/${departmentId}/user/${userId}`);
  },

  // DELETE /api/company/department/{departmentId}/user/{userId}
  removeUserFromDepartment: async (departmentId: number, userId: number) => {
    return http.delete(`/api/company/department/${departmentId}/user/${userId}`);
  }
};

// 用户相关API扩展
export const usersAPI = {
  // GET /api/users/{username}
  getUserByUsername: async (username: string) => {
    return http.get(`/api/users/${username}`);
  },

  // PUT /api/users/reset-password/{userId}
  resetPassword: async (userId: number, newPassword: string) => {
    return http.put(`/api/users/reset-password/${userId}`, newPassword, {
      headers: { 'Content-Type': 'text/plain' }
    });
  }
};