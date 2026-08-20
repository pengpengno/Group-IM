import React, { FormEvent, useEffect, useMemo, useState } from 'react';
import { taskAPI, unwrapWorkbenchResponse, workbenchErrorMessage } from '../../services/api/workbenchAPI';
import type {
  CreateTaskPayload,
  TaskActivity,
  TaskDetail,
  TaskSummary,
  WorkbenchTaskAction,
  WorkbenchTaskPriority,
  WorkbenchTaskStatus,
} from './workbenchTypes';
import './TaskCenter.css';

interface TaskCenterProps {
  onBack: () => void;
}

interface CreateFormState {
  title: string;
  description: string;
  priority: WorkbenchTaskPriority;
  ownerId: string;
  dueAt: string;
}

const EMPTY_FORM: CreateFormState = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  ownerId: '',
  dueAt: '',
};

const STATUS_LABELS: Record<WorkbenchTaskStatus, string> = {
  TODO: '待开始',
  IN_PROGRESS: '进行中',
  BLOCKED: '已阻塞',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};

const PRIORITY_LABELS: Record<WorkbenchTaskPriority, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急',
};

const ACTIONS: Record<WorkbenchTaskStatus, Array<{ action: WorkbenchTaskAction; label: string; primary?: boolean }>> = {
  TODO: [
    { action: 'start', label: '开始任务', primary: true },
    { action: 'complete', label: '直接完成' },
    { action: 'cancel', label: '取消' },
  ],
  IN_PROGRESS: [
    { action: 'complete', label: '完成', primary: true },
    { action: 'block', label: '标记阻塞' },
    { action: 'cancel', label: '取消' },
  ],
  BLOCKED: [
    { action: 'resume', label: '恢复进行', primary: true },
    { action: 'cancel', label: '取消' },
  ],
  COMPLETED: [{ action: 'reopen', label: '重新打开', primary: true }],
  CANCELLED: [],
};

function formatDate(value?: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

const TaskCenter: React.FC<TaskCenterProps> = ({ onBack }) => {
  const [tasks, setTasks] = useState<TaskSummary[]>([]);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [detail, setDetail] = useState<TaskDetail | null>(null);
  const [activities, setActivities] = useState<TaskActivity[]>([]);
  const [listLoading, setListLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mutationBusy, setMutationBusy] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState<CreateFormState>(EMPTY_FORM);
  const [comment, setComment] = useState('');

  const loadTasks = async (preferredTaskId?: number) => {
    setListLoading(true);
    setError(null);
    try {
      const response = await taskAPI.list(100);
      const nextTasks = unwrapWorkbenchResponse(response);
      setTasks(nextTasks);
      const nextId = preferredTaskId
        ?? (selectedTaskId && nextTasks.some((task) => task.taskId === selectedTaskId) ? selectedTaskId : null)
        ?? nextTasks[0]?.taskId
        ?? null;
      setSelectedTaskId(nextId);
      if (!nextId) {
        setDetail(null);
        setActivities([]);
      }
    } catch (requestError) {
      setError(workbenchErrorMessage(requestError));
    } finally {
      setListLoading(false);
    }
  };

  const loadSelectedTask = async (taskId: number) => {
    setDetailLoading(true);
    setError(null);
    try {
      const [detailResponse, activityResponse] = await Promise.all([
        taskAPI.detail(taskId),
        taskAPI.activities(taskId),
      ]);
      setDetail(unwrapWorkbenchResponse(detailResponse));
      setActivities(unwrapWorkbenchResponse(activityResponse));
    } catch (requestError) {
      setDetail(null);
      setActivities([]);
      setError(workbenchErrorMessage(requestError));
    } finally {
      setDetailLoading(false);
    }
  };

  useEffect(() => {
    void loadTasks();
    // Initial tenant-scoped load only. Company switching reloads the whole renderer today.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selectedTaskId) {
      void loadSelectedTask(selectedTaskId);
    }
  }, [selectedTaskId]);

  const counts = useMemo(() => tasks.reduce(
    (acc, task) => {
      acc[task.status] = (acc[task.status] || 0) + 1;
      return acc;
    },
    {} as Partial<Record<WorkbenchTaskStatus, number>>,
  ), [tasks]);

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    if (!createForm.title.trim()) return;

    const ownerId = createForm.ownerId.trim() ? Number(createForm.ownerId) : null;
    if (ownerId !== null && (!Number.isFinite(ownerId) || ownerId <= 0)) {
      setError('负责人 ID 必须是有效的正整数');
      return;
    }

    const payload: CreateTaskPayload = {
      title: createForm.title.trim(),
      description: createForm.description.trim() || null,
      priority: createForm.priority,
      ownerId,
      dueAt: createForm.dueAt || null,
    };

    setMutationBusy(true);
    setError(null);
    try {
      const response = await taskAPI.create(payload);
      const created = unwrapWorkbenchResponse(response);
      setCreateForm(EMPTY_FORM);
      setShowCreate(false);
      await loadTasks(created.taskId);
      setSelectedTaskId(created.taskId);
    } catch (requestError) {
      setError(workbenchErrorMessage(requestError));
    } finally {
      setMutationBusy(false);
    }
  };

  const handleAction = async (action: WorkbenchTaskAction) => {
    if (!detail) return;
    setMutationBusy(true);
    setError(null);
    try {
      const response = await taskAPI.action(detail.taskId, action);
      const updated = unwrapWorkbenchResponse(response);
      setDetail(updated);
      await loadTasks(updated.taskId);
      const activityResponse = await taskAPI.activities(updated.taskId);
      setActivities(unwrapWorkbenchResponse(activityResponse));
    } catch (requestError) {
      setError(workbenchErrorMessage(requestError));
    } finally {
      setMutationBusy(false);
    }
  };

  const handleComment = async (event: FormEvent) => {
    event.preventDefault();
    if (!detail || !comment.trim()) return;
    setMutationBusy(true);
    setError(null);
    try {
      await taskAPI.addComment(detail.taskId, comment.trim());
      setComment('');
      await loadSelectedTask(detail.taskId);
      await loadTasks(detail.taskId);
    } catch (requestError) {
      setError(workbenchErrorMessage(requestError));
    } finally {
      setMutationBusy(false);
    }
  };

  return (
    <section className="task-center" aria-labelledby="task-center-title">
      <header className="task-center__topbar">
        <div>
          <button type="button" className="task-center__back" onClick={onBack}>← 返回工作台</button>
          <p className="task-center__eyebrow">WORKBENCH · TASK</p>
          <h2 id="task-center-title">任务中心</h2>
          <p>任务状态、评论和活动记录都直接来自当前公司的服务端数据。</p>
        </div>
        <div className="task-center__top-actions">
          <button type="button" className="task-center__secondary" onClick={() => void loadTasks()} disabled={listLoading || mutationBusy}>刷新</button>
          <button type="button" className="task-center__primary" onClick={() => setShowCreate(true)}>+ 新建任务</button>
        </div>
      </header>

      <div className="task-center__metrics" aria-label="任务状态概览">
        <div><strong>{tasks.length}</strong><span>可见任务</span></div>
        <div><strong>{counts.TODO || 0}</strong><span>待开始</span></div>
        <div><strong>{counts.IN_PROGRESS || 0}</strong><span>进行中</span></div>
        <div><strong>{counts.BLOCKED || 0}</strong><span>阻塞</span></div>
      </div>

      {error && (
        <div className="task-center__error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={() => setError(null)}>关闭</button>
        </div>
      )}

      <div className="task-center__layout">
        <aside className="task-center__list" aria-label="任务列表">
          <div className="task-center__section-title">
            <div><strong>我的任务</strong><span>{tasks.length} 项</span></div>
          </div>
          {listLoading ? (
            <div className="task-center__placeholder">正在加载任务…</div>
          ) : tasks.length === 0 ? (
            <div className="task-center__placeholder">
              <strong>还没有可见任务</strong>
              <span>创建第一个任务，或等待同事分配给你。</span>
            </div>
          ) : (
            <div className="task-center__task-list">
              {tasks.map((task) => (
                <button
                  type="button"
                  key={task.taskId}
                  className={`task-center__task ${selectedTaskId === task.taskId ? 'is-active' : ''}`}
                  onClick={() => setSelectedTaskId(task.taskId)}
                >
                  <div className="task-center__task-heading">
                    <strong>{task.title}</strong>
                    <span className={`task-center__status status-${task.status.toLowerCase()}`}>{STATUS_LABELS[task.status]}</span>
                  </div>
                  <div className="task-center__task-meta">
                    <span>{PRIORITY_LABELS[task.priority]}优先级</span>
                    <span>进度 {task.progress}%</span>
                  </div>
                  <div className="task-center__progress"><span style={{ width: `${task.progress}%` }} /></div>
                  <small>截止：{formatDate(task.dueAt)}</small>
                </button>
              ))}
            </div>
          )}
        </aside>

        <main className="task-center__detail">
          {detailLoading ? (
            <div className="task-center__placeholder">正在加载任务详情…</div>
          ) : !detail ? (
            <div className="task-center__placeholder">
              <strong>选择一个任务</strong>
              <span>这里会显示任务详情、操作、评论和活动记录。</span>
            </div>
          ) : (
            <>
              <div className="task-center__detail-header">
                <div>
                  <div className="task-center__detail-badges">
                    <span className={`task-center__status status-${detail.status.toLowerCase()}`}>{STATUS_LABELS[detail.status]}</span>
                    <span className={`task-center__priority priority-${detail.priority.toLowerCase()}`}>{PRIORITY_LABELS[detail.priority]}优先级</span>
                  </div>
                  <h3>{detail.title}</h3>
                  <p>{detail.description || '暂无任务描述。'}</p>
                </div>
                <div className="task-center__actions">
                  {ACTIONS[detail.status].map(({ action, label, primary }) => (
                    <button
                      type="button"
                      key={action}
                      className={primary ? 'task-center__primary' : 'task-center__secondary'}
                      disabled={mutationBusy}
                      onClick={() => void handleAction(action)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="task-center__detail-grid">
                <div><span>负责人</span><strong>{detail.ownerId ? `用户 #${detail.ownerId}` : '未指定'}</strong></div>
                <div><span>创建人</span><strong>用户 #{detail.creatorId}</strong></div>
                <div><span>截止时间</span><strong>{formatDate(detail.dueAt)}</strong></div>
                <div><span>当前进度</span><strong>{detail.progress}%</strong></div>
              </div>

              <section className="task-center__panel">
                <div className="task-center__section-title"><strong>参与人</strong></div>
                <div className="task-center__assignees">
                  {detail.assignees.length ? detail.assignees.map((assignee) => (
                    <span key={`${assignee.userId}-${assignee.role}`}>用户 #{assignee.userId} · {assignee.role}</span>
                  )) : <span className="task-center__muted">暂无参与人</span>}
                </div>
              </section>

              <section className="task-center__panel">
                <div className="task-center__section-title"><strong>评论</strong><span>{detail.comments.length}</span></div>
                <div className="task-center__comments">
                  {detail.comments.length === 0 && <div className="task-center__muted">暂无评论</div>}
                  {detail.comments.map((item) => (
                    <article key={item.commentId} className="task-center__comment">
                      <div><strong>用户 #{item.authorId}</strong><time>{formatDate(item.createdAt)}</time></div>
                      <p>{item.content}</p>
                    </article>
                  ))}
                </div>
                <form className="task-center__comment-form" onSubmit={handleComment}>
                  <textarea
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    placeholder="补充进展、阻塞原因或协作信息…"
                    maxLength={4000}
                  />
                  <button type="submit" className="task-center__primary" disabled={mutationBusy || !comment.trim()}>发表评论</button>
                </form>
              </section>

              <section className="task-center__panel">
                <div className="task-center__section-title"><strong>活动记录</strong><span>{activities.length}</span></div>
                <div className="task-center__timeline">
                  {activities.length === 0 && <div className="task-center__muted">暂无活动记录</div>}
                  {activities.map((activity) => (
                    <div className="task-center__activity" key={activity.activityId}>
                      <span className="task-center__activity-dot" />
                      <div>
                        <strong>{activity.action}</strong>
                        <p>
                          用户 #{activity.actorId}
                          {activity.beforeState || activity.afterState ? ` · ${activity.beforeState || '—'} → ${activity.afterState || '—'}` : ''}
                        </p>
                        {activity.detail && <p>{activity.detail}</p>}
                        <time>{formatDate(activity.createdAt)}</time>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </>
          )}
        </main>
      </div>

      {showCreate && (
        <div className="task-center__modal-backdrop" role="presentation" onMouseDown={() => !mutationBusy && setShowCreate(false)}>
          <form className="task-center__modal" onSubmit={handleCreate} onMouseDown={(event) => event.stopPropagation()}>
            <div className="task-center__modal-header">
              <div><p className="task-center__eyebrow">NEW TASK</p><h3>新建任务</h3></div>
              <button type="button" onClick={() => setShowCreate(false)} disabled={mutationBusy}>×</button>
            </div>
            <label>
              <span>任务标题 *</span>
              <input value={createForm.title} onChange={(event) => setCreateForm({ ...createForm, title: event.target.value })} maxLength={200} required autoFocus />
            </label>
            <label>
              <span>任务描述</span>
              <textarea value={createForm.description} onChange={(event) => setCreateForm({ ...createForm, description: event.target.value })} />
            </label>
            <div className="task-center__form-row">
              <label>
                <span>优先级</span>
                <select value={createForm.priority} onChange={(event) => setCreateForm({ ...createForm, priority: event.target.value as WorkbenchTaskPriority })}>
                  <option value="LOW">低</option>
                  <option value="MEDIUM">中</option>
                  <option value="HIGH">高</option>
                  <option value="URGENT">紧急</option>
                </select>
              </label>
              <label>
                <span>负责人用户 ID</span>
                <input type="number" min="1" value={createForm.ownerId} onChange={(event) => setCreateForm({ ...createForm, ownerId: event.target.value })} placeholder="可留空" />
              </label>
            </div>
            <label>
              <span>截止时间</span>
              <input type="datetime-local" value={createForm.dueAt} onChange={(event) => setCreateForm({ ...createForm, dueAt: event.target.value })} />
            </label>
            <div className="task-center__modal-actions">
              <button type="button" className="task-center__secondary" onClick={() => setShowCreate(false)} disabled={mutationBusy}>取消</button>
              <button type="submit" className="task-center__primary" disabled={mutationBusy || !createForm.title.trim()}>{mutationBusy ? '创建中…' : '创建任务'}</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
};

export default TaskCenter;
