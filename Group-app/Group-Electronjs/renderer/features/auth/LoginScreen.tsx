import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { loginStart, loginSuccess, loginFailure } from './authSlice';
import type { LoginCredentials, LocalUser } from '../../types/index';
import { getElectronAPI } from '../../services/api/electronAPI';
import Loading from '../../components/common/Loading';
import './LoginScreen.css';

interface LoginForm {
  loginAccount: string;
  password: string;
}

interface LoginScreenProps {
  onNavigateToSettings?: () => void;
  onNavigateToRegister?: () => void;
}

const LoginScreen: React.FC<LoginScreenProps> = ({ onNavigateToSettings, onNavigateToRegister }) => {
  const dispatch = useDispatch();
  const { loading } = useSelector(
    (state: { auth: ReturnType<typeof import('./authSlice').default> }) => state.auth
  );

  const [loginForm, setLoginForm] = useState<LoginForm>({
    loginAccount: 'peng.wang',
    password: '12345'
  });
  const [passwordVisible, setPasswordVisible] = useState(false);

  const electronAPI = getElectronAPI();

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setLoginForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!electronAPI) {
      dispatch(loginFailure('应用环境不支持'));
      return;
    }

    dispatch(loginStart());

    try {
      const credentials: LoginCredentials = {
        loginAccount: loginForm.loginAccount,
        password: loginForm.password
      };
      const result = await electronAPI.login(credentials);

      if (result.success && result.data) {
        const data = result.data as any;
        const userData: LocalUser = {
          userId: data.userId?.toString(),
          username: data.username,
          email: data.email,
          phoneNumber: data.phoneNumber,
        };

        dispatch(loginSuccess({
          user: userData,
          token: result.data.token,
          refreshToken: result.data.refreshToken || ''
        }));
      } else {
        dispatch(loginFailure(result.error || '用户名或密码错误'));
      }
    } catch (error) {
      dispatch(loginFailure('网络通讯异常，请确认后端服务已启动'));
    }
  };

  return (
    <div className="login-screen">
      {/* Premium Background with animated gradient */}
      <div className="login-background">
        <div className="gradient-sphere sphere-1"></div>
        <div className="gradient-sphere sphere-2"></div>
        <div className="gradient-sphere sphere-3"></div>
      </div>

      {/* Settings Button */}
      {onNavigateToSettings && (
        <button
          className="settings-fab"
          onClick={onNavigateToSettings}
          disabled={loading}
          aria-label="Settings"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.82,11.69,4.82,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z" />
          </svg>
        </button>
      )}

      <div className="login-content-wrapper">
        {/* Logo Section */}
        <div className="brand-section">
          <div className="brand-logo">
            <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="white">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 8h10v2H7zm0 4h10v2H7z" />
            </svg>
          </div>
          <h1 className="brand-name">Group IM</h1>
          <p className="brand-tagline">连接每一个值得交流的时刻</p>
        </div>

        {/* Login Form Card */}
        <div className="login-card">
          {loading && <Loading fullScreen text="正在验证身份..." />}

          <div className="login-header">
            <h2>欢迎回来</h2>
            <p>请输入您的账号信息开始畅聊</p>
          </div>

          <form onSubmit={handleLogin} className="login-form">
            <div className="form-group">
              <label htmlFor="loginAccount">账号</label>
              <div className="input-field-container">
                <span className="input-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </span>
                <input
                  id="loginAccount"
                  type="text"
                  name="loginAccount"
                  value={loginForm.loginAccount}
                  onChange={handleInputChange}
                  placeholder="用户名 / 手机号 / 邮箱"
                  required
                  autoComplete="username"
                  disabled={loading}
                />
              </div>
            </div>

            <div className="form-group">
              <div className="label-with-action">
                <label htmlFor="password">密码</label>
                <button type="button" className="text-action-btn">忘记密码？</button>
              </div>
              <div className="input-field-container">
                <span className="input-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                  </svg>
                </span>
                <input
                  id="password"
                  type={passwordVisible ? "text" : "password"}
                  name="password"
                  value={loginForm.password}
                  onChange={handleInputChange}
                  placeholder="请输入您的密码"
                  required
                  autoComplete="current-password"
                  disabled={loading}
                />
                <button
                  type="button"
                  className="password-eye-toggle"
                  onClick={() => setPasswordVisible(!passwordVisible)}
                  aria-label={passwordVisible ? "隐藏密码" : "显示密码"}
                >
                  {passwordVisible ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
                  )}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className={`submit-btn ${loading ? 'btn-loading' : ''}`}
              disabled={loading}
            >
              {loading ? "验证中..." : "立刻登录"}
            </button>
          </form>

          <div className="login-footer">
            <span>还没有账号？</span>
            {onNavigateToRegister && (
              <button
                className="register-link"
                onClick={onNavigateToRegister}
                disabled={loading}
              >
                免费注册
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginScreen;