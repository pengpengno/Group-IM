import axios, { AxiosResponse } from 'axios';
import { BASE_URL } from './apiClient';
import type {
  CreateTaskPayload,
  ServerApiResponse,
  TaskActivity,
  TaskComment,
  TaskDetail,
  TaskSummary,
  WorkbenchOverview,
  WorkbenchTaskAction,
} from '../../features/workbench/workbenchTypes';

const workbenchHttp = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
});

workbenchHttp.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function unwrapWorkbenchResponse<T>(response: AxiosResponse<ServerApiResponse<T>>): T {
  const envelope = response.data;
  if (!envelope || envelope.code < 200 || envelope.code >= 300) {
    throw new Error(envelope?.message || '请求失败');
  }
  return envelope.data;
}

export function workbenchErrorMessage(error: unknown): string {
  const candidate = error as {
    message?: string;
    response?: { data?: { message?: string; error?: string } };
  };
  return candidate?.response?.data?.message
    || candidate?.response?.data?.error
    || candidate?.message
    || '请求失败，请稍后重试';
}

export const workbenchAPI = {
  overview: () => workbenchHttp.get<ServerApiResponse<WorkbenchOverview>>('/api/workbench/overview'),
};

export const taskAPI = {
  list: (limit: number = 50) => workbenchHttp.get<ServerApiResponse<TaskSummary[]>>(
    '/api/workbench/tasks',
    { params: { limit } },
  ),

  detail: (taskId: number) => workbenchHttp.get<ServerApiResponse<TaskDetail>>(
    `/api/workbench/tasks/${taskId}`,
  ),

  create: (payload: CreateTaskPayload) => workbenchHttp.post<ServerApiResponse<TaskDetail>>(
    '/api/workbench/tasks',
    payload,
  ),

  action: (taskId: number, action: WorkbenchTaskAction, note?: string) => workbenchHttp.post<ServerApiResponse<TaskDetail>>(
    `/api/workbench/tasks/${taskId}/actions/${action}`,
    note?.trim() ? { note: note.trim() } : {},
  ),

  addComment: (taskId: number, content: string, replyToId?: number | null) => workbenchHttp.post<ServerApiResponse<TaskComment>>(
    `/api/workbench/tasks/${taskId}/comments`,
    { content, replyToId: replyToId ?? null },
  ),

  activities: (taskId: number) => workbenchHttp.get<ServerApiResponse<TaskActivity[]>>(
    `/api/workbench/tasks/${taskId}/activities`,
  ),
};
