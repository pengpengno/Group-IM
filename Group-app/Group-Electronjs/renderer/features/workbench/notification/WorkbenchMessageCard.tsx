import React, { useMemo } from 'react';
import {
  parseWorkbenchCard,
  WORKBENCH_NAVIGATION_EVENT,
  type WorkbenchCardCategory,
} from './workbenchCard';
import './WorkbenchMessageCard.css';

const CATEGORY_LABEL: Record<WorkbenchCardCategory, string> = {
  TASK: '任务',
  APPROVAL: '审批',
  ANNOUNCEMENT: '公告',
  SCHEDULE: '日程',
  REPORT: '报告',
};

const CATEGORY_ICON: Record<WorkbenchCardCategory, string> = {
  TASK: '✓',
  APPROVAL: '◇',
  ANNOUNCEMENT: '!',
  SCHEDULE: '◷',
  REPORT: '▤',
};

const formatOccurredAt = (value: string): string => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString();
};

const WorkbenchMessageCard: React.FC<{ content: string }> = ({ content }) => {
  const result = useMemo(() => parseWorkbenchCard(content), [content]);

  if (result.kind === 'fallback') {
    return (
      <div className="workbench-message-card is-fallback" aria-label="工作台消息">
        <div className="workbench-message-card__badge">WORKBENCH</div>
        <div className="workbench-message-card__title">工作台消息</div>
        <div className="workbench-message-card__summary">{result.fallbackText}</div>
        <div className="workbench-message-card__hint">此客户端不会猜测未知协议内容，也不会执行卡片中的业务操作。</div>
      </div>
    );
  }

  const { card } = result;
  const openWorkbench = () => {
    window.dispatchEvent(new CustomEvent(WORKBENCH_NAVIGATION_EVENT, {
      detail: { deepLink: card.deepLink },
    }));
  };

  return (
    <div className="workbench-message-card" aria-label={`${CATEGORY_LABEL[card.category]}工作台消息`}>
      <div className="workbench-message-card__topline">
        <span className="workbench-message-card__icon" aria-hidden="true">{CATEGORY_ICON[card.category]}</span>
        <span className="workbench-message-card__badge">{CATEGORY_LABEL[card.category]}</span>
        {card.status && <span className="workbench-message-card__status">{card.status}</span>}
      </div>
      <div className="workbench-message-card__title">{card.title}</div>
      {card.summary && <div className="workbench-message-card__summary">{card.summary}</div>}
      <div className="workbench-message-card__meta">
        <span>{card.action}</span>
        <time>{formatOccurredAt(card.occurredAt)}</time>
      </div>
      <button type="button" className="workbench-message-card__open" onClick={openWorkbench}>
        打开工作台
        <span aria-hidden="true">→</span>
      </button>
    </div>
  );
};

export default WorkbenchMessageCard;
