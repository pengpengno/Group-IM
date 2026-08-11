import React from 'react';
import type { ActiveTab } from '../../types';
import './Workbench.css';

interface WorkbenchProps {
  userName?: string;
  onNavigate: (tab: ActiveTab) => void;
}

/**
 * 工作台只负责聚合和跳转，不承担业务数据的二次存储。
 * 具体业务仍由会话、会议、联系人等既有模块维护，避免出现跨模块状态不一致。
 */
const Workbench: React.FC<WorkbenchProps> = ({ userName, onNavigate }) => {
  const modules: Array<{ title: string; description: string; tab: ActiveTab; icon: string }> = [
    { title: '会话协作', description: '查看消息、发起私聊和群聊协作', tab: 'chats', icon: '💬' },
    { title: '在线会议', description: '进入会议列表，发起或加入音视频会议', tab: 'meetings', icon: '🎥' },
    { title: '组织通讯录', description: '查找同事并快速开始沟通', tab: 'contacts', icon: '👥' },
    { title: '个人设置', description: '管理通知、账号与客户端偏好', tab: 'settings', icon: '⚙️' }
  ];

  return (
    <section className="workbench" aria-labelledby="workbench-title">
      <header className="workbench__header">
        <div>
          <p className="workbench__eyebrow">GROUP IM</p>
          <h2 id="workbench-title">工作台</h2>
          <p>{userName ? `${userName}，从这里快速进入常用协作能力。` : '从这里快速进入常用协作能力。'}</p>
        </div>
      </header>
      <div className="workbench__grid">
        {modules.map((module) => (
          <button key={module.tab} className="workbench__card" onClick={() => onNavigate(module.tab)}>
            <span className="workbench__icon" aria-hidden="true">{module.icon}</span>
            <span className="workbench__content">
              <strong>{module.title}</strong>
              <span>{module.description}</span>
            </span>
            <span className="workbench__arrow" aria-hidden="true">→</span>
          </button>
        ))}
      </div>
    </section>
  );
};

export default Workbench;
