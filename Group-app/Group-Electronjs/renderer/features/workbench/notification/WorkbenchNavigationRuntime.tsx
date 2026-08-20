import React, { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import Notification from '../../../components/common/Notification';
import { authAPI } from '../../../services/api/apiClient';
import { taskAPI, unwrapWorkbenchResponse, workbenchErrorMessage } from '../../../services/api/workbenchAPI';
import type { TaskDetail } from '../workbenchTypes';
import type { AppDispatch, RootState } from '../../../store';
import { loginSuccess } from '../../auth/authSlice';
import {
  parseWorkbenchDeepLink,
  PENDING_WORKBENCH_DEEP_LINK_KEY,
  WORKBENCH_NAVIGATION_EVENT,
  type WorkbenchDeepLinkTarget,
} from './workbenchCard';
import './WorkbenchNavigationRuntime.css';

const formatDate = (value?: string | null): string => {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const WorkbenchNavigationRuntime: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const user = useSelector((state: RootState) => state.auth.user);
  const [task, setTask] = useState<TaskDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const switchCompanyAndReload = useCallback(async (target: WorkbenchDeepLinkTarget) => {
    if (!user) throw new Error('当前登录会话不可用');
    const company = user.companies?.find((item) => Number(item.companyId) === target.companyId);
    if (!company) {
      throw new Error('你已不属于该工作区，无法打开此工作台资源');
    }

    sessionStorage.setItem(PENDING_WORKBENCH_DEEP_LINK_KEY, target.deepLink);
    const response = await authAPI.switchCompany(target.companyId);
    const envelope = response.data as any;
    const switched = envelope?.code === 200 || envelope?.success === true;
    if (!switched || !envelope?.data) {
      sessionStorage.removeItem(PENDING_WORKBENCH_DEEP_LINK_KEY);
      throw new Error(envelope?.message || '切换工作区失败');
    }

    const refreshedUser = envelope.data;
    dispatch(loginSuccess({
      user: refreshedUser,
      token: refreshedUser.token || user.token || localStorage.getItem('token') || '',
      refreshToken: refreshedUser.refreshToken || user.refreshToken || localStorage.getItem('refreshToken') || '',
      companies: user.companies,
      currentCompany: company,
    }));

    window.location.reload();
  }, [dispatch, user]);

  const openTarget = useCallback(async (deepLink: string) => {
    const target = parseWorkbenchDeepLink(deepLink);
    if (!target) {
      throw new Error('工作台链接无效或已损坏');
    }
    if (!user?.currentCompany?.companyId) {
      throw new Error('当前未选择工作区');
    }

    if (Number(user.currentCompany.companyId) !== target.companyId) {
      await switchCompanyAndReload(target);
      return;
    }

    sessionStorage.removeItem(PENDING_WORKBENCH_DEEP_LINK_KEY);

    if (target.category !== 'TASK') {
      throw new Error('当前客户端尚未提供此类工作台资源详情页');
    }

    const numericTaskId = Number(target.resourceId);
    if (!Number.isSafeInteger(numericTaskId) || numericTaskId <= 0) {
      throw new Error('任务链接中的资源标识无效');
    }

    // Card fields are immutable event snapshots. The only resource data rendered
    // below comes from this current-tenant detail fetch, which also enforces
    // membership and Task resource permission on the server.
    const response = await taskAPI.detail(numericTaskId);
    setTask(unwrapWorkbenchResponse(response));
  }, [switchCompanyAndReload, user?.currentCompany?.companyId]);

  const process = useCallback(async (deepLink: string) => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await openTarget(deepLink);
    } catch (requestError) {
      sessionStorage.removeItem(PENDING_WORKBENCH_DEEP_LINK_KEY);
      setError(workbenchErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }, [busy, openTarget]);

  useEffect(() => {
    const handler = (event: Event) => {
      const deepLink = (event as CustomEvent<{ deepLink?: string }>).detail?.deepLink;
      if (deepLink) void process(deepLink);
    };
    window.addEventListener(WORKBENCH_NAVIGATION_EVENT, handler as EventListener);
    return () => window.removeEventListener(WORKBENCH_NAVIGATION_EVENT, handler as EventListener);
  }, [process]);

  useEffect(() => {
    if (!user?.currentCompany?.companyId || busy || task) return;
    const pending = sessionStorage.getItem(PENDING_WORKBENCH_DEEP_LINK_KEY);
    if (pending) void process(pending);
  }, [busy, process, task, user?.currentCompany?.companyId]);

  return (
    <>
      {busy && (
        <div className="workbench-navigation-runtime__busy" role="status">
          正在安全打开工作台资源…
        </div>
      )}
      {error && (
        <Notification message={error} type="error" onClose={() => setError(null)} />
      )}
      {task && (
        <div className="workbench-navigation-runtime__overlay" role="dialog" aria-modal="true" aria-label="工作台任务详情">
          <article className="workbench-navigation-runtime__detail">
            <header>
              <div>
                <p>WORKBENCH · TASK</p>
                <h2>{task.title}</h2>
                <span>{task.status} · {task.priority}</span>
              </div>
              <button type="button" onClick={() => setTask(null)} aria-label="关闭任务详情">×</button>
            </header>
            <section className="workbench-navigation-runtime__summary">
              {task.description || '暂无任务描述。'}
            </section>
            <section className="workbench-navigation-runtime__facts">
              <div><span>负责人</span><strong>{task.ownerId ? `用户 #${task.ownerId}` : '未指定'}</strong></div>
              <div><span>创建人</span><strong>用户 #{task.creatorId}</strong></div>
              <div><span>进度</span><strong>{task.progress}%</strong></div>
              <div><span>截止时间</span><strong>{formatDate(task.dueAt)}</strong></div>
              <div><span>最近更新</span><strong>{formatDate(task.updatedAt)}</strong></div>
            </section>
            <footer>
              <span>此处内容已从当前工作区服务端重新读取；卡片快照不会授权任何写操作。</span>
              <button type="button" onClick={() => setTask(null)}>返回聊天</button>
            </footer>
          </article>
        </div>
      )}
    </>
  );
};

export default WorkbenchNavigationRuntime;
