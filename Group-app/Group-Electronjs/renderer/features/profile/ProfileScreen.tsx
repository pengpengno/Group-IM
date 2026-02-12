import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { logout } from '../auth/authSlice';
import type { AuthState } from '../../types';
import './ProfileScreen.css';

interface ProfileScreenProps {
  onLogout?: () => void;
  onSettings?: () => void;
}

const ProfileScreen: React.FC<ProfileScreenProps> = ({ onLogout, onSettings }) => {
  const dispatch = useDispatch();
  const { user } = useSelector((state: { auth: AuthState }) => state.auth);
  const [darkMode, setDarkMode] = useState(false);

  const menuItems = [
    { 
      icon: '👤', 
      title: '个人资料', 
      subtitle: '查看和编辑个人信息',
      action: () => console.log('Open profile settings')
    },
    { 
      icon: '🛡️', 
      title: '隐私设置', 
      subtitle: '管理隐私和安全选项',
      action: () => console.log('Open privacy settings')
    },
    { 
      icon: '🔔', 
      title: '通知设置', 
      subtitle: '自定义通知偏好',
      action: () => console.log('Open notification settings')
    },
    { 
      icon: '🎨', 
      title: '主题设置', 
      subtitle: '更改应用外观',
      action: () => console.log('Open theme settings'),
      hasToggle: true
    },
    { 
      icon: '❓', 
      title: '帮助与反馈', 
      subtitle: '获取帮助和支持',
      action: () => console.log('Open help and feedback')
    },
    { 
      icon: 'ℹ️', 
      title: '关于应用', 
      subtitle: '版本信息和条款',
      action: () => console.log('Open about page')
    }
  ];

  const MenuItem: React.FC<{ item: typeof menuItems[0] }> = ({ item }) => {
    return (
      <div 
        className="menu-item"
        onClick={item.action}
      >
        <div className="menu-icon">{item.icon}</div>
        <div className="menu-content">
          <h3 className="menu-title">{item.title}</h3>
          <p className="menu-subtitle">{item.subtitle}</p>
        </div>
        {item.hasToggle ? (
          <label className="switch">
            <input 
              type="checkbox" 
              checked={darkMode}
              onChange={(e) => setDarkMode(e.target.checked)}
            />
            <span className="slider"></span>
          </label>
        ) : (
          <div className="menu-arrow">›</div>
        )}
      </div>
    );
  };

  const handleLogout = () => {
    if (onLogout) {
      onLogout();
    } else {
      dispatch(logout());
    }
  };

  const handleSettings = () => {
    if (onSettings) {
      onSettings();
    }
  };

  return (
    <div className="profile-screen">
      {/* User Profile Header - Enhanced */}
      <div className="profile-header">
        <div className="header-background"></div>
        <div className="user-profile">
          <div className="user-avatar-large">
            <span>{user?.username?.charAt(0)?.toUpperCase() || 'U'}</span>
          </div>
          <div className="user-info">
            <h2 className="username">{user?.username || '用户'}</h2>
            <p className="user-email">{user?.email || 'user@example.com'}</p>
            <div className="user-stats">
              <div className="stat-item">
                <span className="stat-number">24</span>
                <span className="stat-label">联系人</span>
              </div>
              <div className="stat-item">
                <span className="stat-number">12</span>
                <span className="stat-label">群组</span>
              </div>
              <div className="stat-item">
                <span className="stat-number">8</span>
                <span className="stat-label">文件</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="quick-actions">
        <div className="action-card" onClick={handleSettings}>
          <div className="action-icon">⚙️</div>
          <span>设置</span>
        </div>
        <div className="action-card">
          <div className="action-icon">📊</div>
          <span>统计</span>
        </div>
        <div className="action-card">
          <div className="action-icon">💾</div>
          <span>备份</span>
        </div>
      </div>

      {/* Profile Menu */}
      <div className="profile-menu">
        <div className="menu-section">
          <h3 className="section-title">账户设置</h3>
          {menuItems.slice(0, 3).map((item, index) => (
            <MenuItem key={index} item={item} />
          ))}
        </div>

        <div className="menu-section">
          <h3 className="section-title">应用设置</h3>
          {menuItems.slice(3, 5).map((item, index) => (
            <MenuItem key={index + 3} item={item} />
          ))}
        </div>

        <div className="menu-section">
          <h3 className="section-title">关于</h3>
          {menuItems.slice(5).map((item, index) => (
            <MenuItem key={index + 5} item={item} />
          ))}
        </div>
      </div>

      {/* Logout Button - Enhanced */}
      <div className="logout-section">
        <button 
          className="logout-button"
          onClick={handleLogout}
        >
          <span className="logout-icon">🚪</span>
          退出登录
        </button>
      </div>

      {/* App Version Info */}
      <div className="version-info">
        <p>Group IM v1.0.5</p>
        <p>© 2024 Group Inc. 保留所有权利</p>
      </div>
    </div>
  );
};

export default ProfileScreen;