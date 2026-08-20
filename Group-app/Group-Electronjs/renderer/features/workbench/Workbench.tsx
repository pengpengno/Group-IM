import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { ActiveTab } from '../../types';
import { unwrapWorkbenchResponse, workbenchAPI, workbenchErrorMessage } from '../../services/api/workbenchAPI';
import type { WorkbenchOverview, WorkbenchQuickApp } from './workbenchTypes';
import TaskCenter from './TaskCenter';
import './Workbench.css';

interface WorkbenchProps {
  userName?: string;
  onNavigate: (tab: ActiveTab) => void;
}

type WorkbenchView = 'overview' | 'tasks';

const APP_META: Record<string, { icon: string; description: string; tab?: ActiveTab }> = {
  TASK: { icon: '✓', description: '创建、跟进并完成团队任务' },
  MEETING: { icon: '◉', description: '查看并加入在线会议', tab: 'meetings' },
  CONTACTS: { icon: '◎', description: '浏览组织架构与同事', tab: 'contacts' },
  AUTOMATION: { icon: '⚡', description: '配置群自动化规则', tab: 'automation' },
  SETTINGS: { icon: '⚙', description: '管理客户端与通知设置', tab: 'settings' },
};

function formatDate(value?: string | null): string {
  if (!value) return '未设置';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

const Workbench: React.FC<WorkbenchProps> = ({ userName, onNavigate }) => {
  const [view, setView] = useState<WorkbenchView>('overview');
  const [overview, setOverview] = useState<WorkbenchOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadOverview = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await workbenchAPI.overview();
      setOverview(unwrapWorkbenchResponse(response));
    } catch (requestError) {
      setError(workbenchErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadOverview();
  }, [loadOverview]);

  const quickApps = useMemo(() => {
    const apps = overview?.quickApps || [];
    const hasTask = apps.some((app) => app.key === 'TASK');
    return hasTask ? apps : [{ key: 'TASK', title: '任务' }, ...apps];
  }, [overview]);

  const openQuickApp = (app: WorkbenchQuickApp) => {
    if (app.key === 'TASK') {
      setView('tasks');
      return;
    }
    const tab = APP_META[app.key]?.tab;
    if (tab) onNavigate(tab);
  };

  if (view === 'tasks') {
    return (
      <TaskCenter
        onBack={() => {
          setView('overview');
          void loadOverview();
        }}
      />
    );
  }

  return (
    <section className="workbench" aria-labelledby="workbench-title">
      <header className="workbench__header">
        <div>
          <p className="workbench__eyebrow">GROUP IM · WORKBENCH</p>
          <h2 id="workbench-title">工作台</h2>
          <p>
            {userName ? `${userName}，` : ''}
            {overview?.currentCompany?.name
              ? `当前在「${overview.currentCompany.name}」工作区。`
              : '从这里进入团队协作与办公能力。'}
          </p>
        </div>
        <button type="button" className="workbench__refresh" onClick={() => void loadOverview()} disabled={loading}>
          {loading ? '刷新中…' : '刷新'}
        </button>
      </header>

      {error && (
        <div className="workbench__error" role="alert">
          <div><strong>工作台数据加载失败</strong><span>{error}</span></div>
          <button type="button" onClick={() => void loadOverview()}>重试</button>
        </div>
      )}

      <section className="workbench__section" aria-labelledby="workbench-todo-title">
        <div className="workbench__section-heading">
          <div><p className="workbench__section-kicker">TODAY</p><h3 id="workbench-todo-title">待办概览</h3></div>
          <span>{loading ? '读取中' : '实时数据'}</span>
        </div>
        <div className="workbench__stats">
          <button type="button" onClick={() => setView('tasks')}>
            <strong>{overview?.todoSummary.assignedTaskCount ?? 0}</strong>
            <span>我的未完成任务</span>
          </button>
          <button type="button" onClick={() => setView('tasks')} className={(overview?.todoSummary.overdueTaskCount || 0) > 0 ? 'is-alert' : ''}>
            <strong>{overview?.todoSummary.overdueTaskCount ?? 0}</strong>
            <span>已逾期任务</span>
          </button>
          <div><strong>{overview?.todoSummary.pendingApprovalCount ?? 0}</strong><span>待审批</span></div>
          <div><strong>{overview?.todoSummary.unreadAnnouncementCount ?? 0}</strong><span>未读公告</span></div>
        </div>
      </section>

      <div className="workbench__columns">
        <section className="workbench__section">
          <div className="workbench__section-heading">
            <div><p className="workbench__section-kicker">TASK</p><h3>最近任务</h3></div>
            <button type="button" onClick={() => setView('tasks')}>查看全部 →</button>
          </div>
          <div className="workbench__list">
            {loading && !overview && <div className="workbench__empty">正在加载任务…</div>}
            {!loading && (overview?.recentTasks.length || 0) === 0 && <div className="workbench__empty">暂无最近任务</div>}
            {overview?.recentTasks.map((task) => (
              <button type="button" className="workbench__list-item" key={task.taskId} onClick={() => setView('tasks')}>
                <div><strong>{task.title}</strong><span>截止：{formatDate(task.dueAt)}</span></div>
                <span className={`workbench__status status-${task.status.toLowerCase()}`}>{task.status}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="workbench__section">
          <div className="workbench__section-heading">
            <div><p className="workbench__section-kicker">SCHEDULE</p><h3>今日安排</h3></div>
            <button type="button" onClick={() => onNavigate('meetings')}>会议 →</button>
          </div>
          <div className="workbench__list">
            {loading && !overview && <div className="workbench__empty">正在加载日程…</div>}
            {!loading && (overview?.todaySchedules.length || 0) === 0 && <div className="workbench__empty">今天暂无会议安排</div>}
            {overview?.todaySchedules.slice(0, 5).map((schedule) => (
              <button type="button" className="workbench__list-item" key={`${schedule.type}-${schedule.resourceId}`} onClick={() => onNavigate('meetings')}>
                <div><strong>{schedule.title}</strong><span>{formatDate(schedule.startsAt)}</span></div>
                <span className="workbench__status">{schedule.status}</span>
              </button>
            ))}
          </div>
        </section>
      </div>

      <section className="workbench__section">
        <div className="workbench__section-heading">
          <div><p className="workbench__section-kicker">APPS</p><h3>快捷应用</h3></div>
        </div>
        <div className="workbench__grid">
          <button className="workbench__card" onClick={() => onNavigate('chats')}>
            <span className="workbench__icon" aria-hidden="true">✦</span>
            <span className="workbench__content"><strong>会话协作</strong><span>查看消息、发起私聊和群聊协作</span></span>
            <span className="workbench__arrow" aria-hidden="true">→</span>
          </button>
          {quickApps.map((app) => {
            const meta = APP_META[app.key] || { icon: '□', description: '打开工作台应用' };
            return (
              <button key={app.key} className="workbench__card" onClick={() => openQuickApp(app)}>
                <span className="workbench__icon" aria-hidden="true">{meta.icon}</span>
                <span className="workbench__content"><strong>{app.title}</strong><span>{meta.description}</span></span>
                <span className="workbench__arrow" aria-hidden="true">→</span>
              </button>
            );
          })}
        </div>
      </section>
    </section>
  );
};

export default Workbench;
