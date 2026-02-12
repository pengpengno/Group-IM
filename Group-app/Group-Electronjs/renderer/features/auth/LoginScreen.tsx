import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { loginStart, loginSuccess, loginFailure } from './authSlice';
import type { LoginCredentials, AuthData, LocalUser } from '../../types/index';
import { getElectronAPI } from '../../api/electronAPI';
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
  const [loginForm, setLoginForm] = useState<LoginForm>({
    loginAccount: '',
    password: ''
  });
  const [rememberMe, setRememberMe] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
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
      setErrorMessage('应用环境不支持');
      return;
    }

    setErrorMessage('');
    setIsLoading(true);
    dispatch(loginStart());

    try {
      const credentials: LoginCredentials = { 
        loginAccount: loginForm.loginAccount, 
        password: loginForm.password 
      };
      const result = await electronAPI.login(credentials);

      if (result.success && result.data) {
        // Create LocalUser object with correct structure
        const userData: LocalUser = {
          userId: result.data.user.id,
          username: result.data.user.username,
          email: result.data.user.email,
          phoneNumber: result.data.user.phoneNumber
        };
        
        dispatch(loginSuccess({ 
          user: userData, 
          token: result.data.token, 
          refreshToken: result.data.refreshToken ?? '' 
        }));
      } else {
        const errorMsg = result.error || '登录失败';
        setErrorMessage(errorMsg);
        dispatch(loginFailure(errorMsg));
      }
    } catch (error) {
      const errorMsg = '网络错误，请稍后重试';
      setErrorMessage(errorMsg);
      dispatch(loginFailure(errorMsg));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-screen">
      {/* Gradient Background */}
      <div className="login-background"></div>
      
      {/* Settings Button */}
      {onNavigateToSettings && (
        <button 
          className="settings-button"
          onClick={onNavigateToSettings}
          aria-label="Settings"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="white">
            <path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.82,11.69,4.82,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/>
          </svg>
        </button>
      )}

      <div className="login-content">
        {/* Logo Section */}
        <div className="logo-container">
          <div className="logo-card">
            <div className="logo-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="#1976d2">
                <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 8h10v2H7zm0 4h10v2H7z"/>
              </svg>
            </div>
          </div>
        </div>

        {/* Login Form Card */}
        <div className="login-form-card">
          <h2 className="welcome-text">欢迎回来</h2>
          
          <form onSubmit={handleLogin} className="login-form">
            <div className="input-group">
              <div className="input-wrapper">
                <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
                <input
                  type="text"
                  name="loginAccount"
                  value={loginForm.loginAccount}
                  onChange={handleInputChange}
                  placeholder="用户名/邮箱"
                  className="login-input"
                  required
                />
              </div>
            </div>

            <div className="input-group">
              <div className="input-wrapper">
                <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="#666">
                  <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
                </svg>
                <input
                  type={passwordVisible ? "text" : "password"}
                  name="password"
                  value={loginForm.password}
                  onChange={handleInputChange}
                  placeholder="密码"
                  className="login-input"
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setPasswordVisible(!passwordVisible)}
                  aria-label={passwordVisible ? "Hide password" : "Show password"}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="#666">
                    {passwordVisible ? (
                      <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                    ) : (
                      <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                    )}
                  </svg>
                </button>
              </div>
            </div>

            <button
              type="button"
              className="forgot-password-button"
            >
              忘记密码？
            </button>

            <button
              type="submit"
              disabled={isLoading}
              className="login-button"
            >
              {isLoading ? (
                <div className="loading-spinner"></div>
              ) : (
                "登 录"
              )}
            </button>

            {errorMessage && (
              <div className="error-message">
                {errorMessage}
              </div>
            )}
          </form>
        </div>

        {/* Registration Prompt */}
        <div className="registration-prompt">
          <span>还没有账号？</span>
          {onNavigateToRegister && (
            <button 
              className="register-button"
              onClick={onNavigateToRegister}
            >
              立即注册
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default LoginScreen;