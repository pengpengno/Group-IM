import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { logout } from '../auth/authSlice';
import type { AuthState } from '../../types';
import './ProfileScreen.css';

const ProfileScreen: React.FC = () => {
  const dispatch = useDispatch();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);

  const menuItems = [
    { icon: '👤', title: '个人资料', subtitle: '查看和编辑个人信息' },
    { icon: '🛡️', title: '隐私设置', subtitle: '管理隐私和安全选项' },
    { icon: '🔔', title: '通知设置', subtitle: '自定义通知偏好' },
    { icon: '🎨', title: '主题设置', subtitle: '更改应用外观' },
    { icon: '❓', title: '帮助与反馈', subtitle: '获取帮助和支持' },
    { icon: 'ℹ️', title: '关于应用', subtitle: '版本信息和条款' }
  ];

  const MenuItem: React.FC<{ item: typeof menuItems[0] }> = ({ item }) => {
    return (
      <div className="menu-item">
        <div className="menu-icon">{item.icon}</div>
        <div className="menu-content">
          <h3 className="menu-title">{item.title}</h3>
          <p className="menu-subtitle">{item.subtitle}</p>
        </div>
        <div className="menu-arrow">›</div>
      </div>
    );
  };

  return (
    <div className="profile-screen">
      {/* User Profile Header */}
      <div className="profile-header">
        <div className="user-avatar-large">
          <span>{user?.username?.charAt(0)?.toUpperCase() || 'U'}</span>
        </div>
        <div className="user-info">
          <h2 className="username">{user?.username || '用户'}</h2>
          <p className="user-email">{user?.email || 'user@example.com'}</p>
        </div>
      </div>

      {/* Profile Menu */}
      <div className="profile-menu">
        {menuItems.map((item, index) => (
          <MenuItem key={index} item={item} />
        ))}
      </div>

      {/* Logout Button */}
      <div className="logout-section">
        <button 
          className="logout-button"
          onClick={() => dispatch(logout())}
        >
          退出登录
        </button>
      </div>
    </div>
  );
};

export default ProfileScreen;