export interface ServerApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export type WorkbenchTaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED' | 'CANCELLED';
export type WorkbenchTaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type WorkbenchTaskAction = 'start' | 'block' | 'resume' | 'complete' | 'reopen' | 'cancel';

export interface WorkbenchCompanySummary {
  companyId: number;
  name: string;
}

export interface WorkbenchTodoSummary {
  assignedTaskCount: number;
  overdueTaskCount: number;
  pendingApprovalCount: number;
  unreadAnnouncementCount: number;
}

export interface WorkbenchRecentTask {
  taskId: number;
  title: string;
  status: WorkbenchTaskStatus;
  dueAt?: string | null;
}

export interface WorkbenchScheduleSummary {
  type: string;
  resourceId: number;
  title: string;
  status: string;
  startsAt?: string | null;
  endsAt?: string | null;
}

export interface WorkbenchQuickApp {
  key: string;
  title: string;
}

export interface WorkbenchOverview {
  currentCompany: WorkbenchCompanySummary;
  todoSummary: WorkbenchTodoSummary;
  recentTasks: WorkbenchRecentTask[];
  pendingApprovals: unknown[];
  todaySchedules: WorkbenchScheduleSummary[];
  announcements: unknown[];
  quickApps: WorkbenchQuickApp[];
}

export interface TaskSummary {
  taskId: number;
  title: string;
  status: WorkbenchTaskStatus;
  priority: WorkbenchTaskPriority;
  ownerId?: number | null;
  dueAt?: string | null;
  progress: number;
  updatedAt?: string | null;
}

export interface TaskAssignee {
  userId: number;
  role: 'OWNER' | 'COLLABORATOR' | 'WATCHER' | string;
  createdAt?: string | null;
}

export interface TaskComment {
  commentId: number;
  authorId: number;
  content: string;
  replyToId?: number | null;
  createdAt?: string | null;
}

export interface TaskActivity {
  activityId: number;
  actorId: number;
  action: string;
  beforeState?: string | null;
  afterState?: string | null;
  detail?: string | null;
  createdAt?: string | null;
}

export interface TaskDetail {
  taskId: number;
  title: string;
  description?: string | null;
  status: WorkbenchTaskStatus;
  priority: WorkbenchTaskPriority;
  creatorId: number;
  ownerId?: number | null;
  departmentId?: number | null;
  conversationId?: number | null;
  startAt?: string | null;
  dueAt?: string | null;
  completedAt?: string | null;
  progress: number;
  version: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  assignees: TaskAssignee[];
  comments: TaskComment[];
}

export interface CreateTaskPayload {
  title: string;
  description?: string | null;
  priority?: WorkbenchTaskPriority;
  ownerId?: number | null;
  departmentId?: number | null;
  conversationId?: number | null;
  startAt?: string | null;
  dueAt?: string | null;
}
