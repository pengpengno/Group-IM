import React, { useEffect } from 'react';
import { Provider, useSelector, useDispatch } from 'react-redux';
import { store } from './store';
import LoginScreen from './features/auth/LoginScreen';
import { logout, clearError } from './features/auth/authSlice';
import MainScreen from './MainScreen';
import './App.css';
import type { AuthState } from './types';

// AppContent component to access Redux store
const AppContent: React.FC = () => {
  const dispatch = useDispatch();
  const { isAuthenticated, user, loading, error } = useSelector((state: { auth: AuthState }) => state.auth);

  // Check authentication status on app start
  useEffect(() => {
    // In a real app, you would check for stored tokens and validate them
    console.log('App started, auth state:', { isAuthenticated, user, loading, error });
  }, [isAuthenticated, user, loading, error]);

  const handleLogout = () => {
    // Dispatch logout action
    dispatch(logout());
  };

  const handleNavigateToSettings = () => {
    console.log('Navigate to settings');
    // Implement settings navigation
  };

  const handleClearError = () => {
    dispatch(clearError());
  };

  // 如果正在加载，显示加载界面
  if (loading) {
    return (
      <div className="app-loading">
        <div className="loading-spinner"></div>
        <p>加载中...</p>
      </div>
    );
  }

  // 如果有错误且未认证，显示错误界面
  if (error && !isAuthenticated) {
    return (
      <div className="app">
        <div className="error-message">
          <h2>错误</h2>
          <p>{error}</p>
          <div className="error-actions">
            <button onClick={handleClearError} className="clear-error-btn">
              清除错误
            </button>
            <button onClick={() => window.location.reload()} className="reload-btn">
              重新加载
            </button>
          </div>
        </div>
      </div>
    );
  }

  // 如果未认证，显示登录界面
  if (!isAuthenticated) {
    return (
      <LoginScreen 
        onNavigateToSettings={handleNavigateToSettings}
        onNavigateToRegister={() => console.log('Navigate to register')}
      />
    );
  }

  // 如果已认证，显示主界面
  return (
    <MainScreen 
      onLogout={handleLogout}
      onNavigateToSettings={handleNavigateToSettings}
    />
  );
};

// Main App component with Redux provider
const App: React.FC = () => {
  return (
    <Provider store={store}>
      <div className="app">
        <AppContent />
      </div>
    </Provider>
  );
};

export default App;