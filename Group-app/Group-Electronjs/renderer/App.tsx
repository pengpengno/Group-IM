import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { loginStart, loginSuccess, loginFailure, logout, clearError } from './features/auth/authSlice';
import type { User, AuthData, LoginCredentials, FileFilter, SelectFileOptions, SelectFileResult, ApiResponse, SearchResults, ApiUser, LocalUser } from './types';
import { getElectronAPI } from './api/electronAPI';
import {AuthState} from './features/auth/authSlice'
// Main App component
const App: React.FC = () => {
  const [loginAccount, setLoginAccount] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  
  const dispatch = useDispatch();
  const { isAuthenticated, user, loading, error } = useSelector(
    (state: { auth: ReturnType<typeof import('./features/auth/authSlice').default> }) => state.auth
  );
  
  const electronAPI = getElectronAPI();

  // Handle login
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    dispatch(loginStart());
    
    try {
      if (!electronAPI) {
        throw new Error('Electron API not available');
      }
      
      const credentials: LoginCredentials = { loginAccount, password };
      const result = await electronAPI.login(credentials);
      
      if (result.success && result.data) {
        const userData: LocalUser = {
          userId: result.data.user?.id?.toString() || result.data.user.id?.toString() || '',
          username: result.data.user?.username || result.data.user.username || '',
          email: result.data.user?.email || result.data.user.email || '',
          phoneNumber: result.data.user?.phoneNumber || result.data.user.phoneNumber || ''
        };
        dispatch(loginSuccess({ 
          user: userData, 
          token: result.data.token, 
          refreshToken: result.data.refreshToken?.toString() || '',
        }));
      } else {
        dispatch(loginFailure(result.error || '登录失败'));
      }
    } catch (err) {
      dispatch(loginFailure('网络错误，请稍后重试'));
    }
  };

  // Handle search
  const handleSearch = async () => {
    if (!searchQuery.trim() || !electronAPI) return;
    
    try {
      const result = await electronAPI.searchUsers(searchQuery);
      let users: User[] = [];
      
      if (result.success && result.data) {
        const dataObj = result.data as any;
        // Handle different response formats
        if (dataObj.users && Array.isArray(dataObj.users)) {
          users = dataObj.users.map((u: any) => ({
            id: u.userId?.toString() || u.id || '',
            username: u.username || '',
            email: u.email || '',
            phoneNumber: u.phoneNumber || '',
            status: u.status || 'online'
          })) as User[];
        } else if (dataObj.content && Array.isArray(dataObj.content)) {
          // Convert ApiUser array to User array
          users = (dataObj.content as ApiUser[]).map(apiUser => ({
            id: apiUser.userId.toString(),
            username: apiUser.username,
            email: apiUser.email || '',
            phoneNumber: apiUser.phoneNumber || '',
            status: 'online'
          }));
        }
      }
      
      setSearchResults(users);
    } catch (err) {
      console.error('Search error:', err);
    }
  };

  // Handle file upload
  const handleSelectFile = async () => {
    try {
      const result = await electronAPI.selectFile({
        title: '选择文件',
        filters: [
          { name: 'Images', extensions: ['jpg', 'png', 'gif'] },
          { name: 'Documents', extensions: ['pdf', 'doc', 'docx'] }
        ]
      });
      
      if (result && !result.canceled && result.filePaths && result.filePaths.length > 0) {
        const uploadResult = await electronAPI.uploadFile(result.filePaths[0]);
        console.log('Upload result:', uploadResult);
      }
    } catch (err) {
      console.error('File upload error:', err);
    }
  };

  // Render user list
  const renderUserList = (users: User[]) => (
    <ul>
      {users.map(user => (
        <li key={'id' in user ? user.id : (user as any).userId}>
          {user.username} - {user.email}
        </li>
      ))}
    </ul>
  );

  // Handle file select for upload testing
  const handleFileSelect = async () => {
    try {
      const result = await getElectronAPI().selectFile({
        title: '选择文件',
        filters: [
          { name: 'Images', extensions: ['jpg', 'png', 'gif'] },
          { name: 'Documents', extensions: ['pdf', 'doc', 'docx'] }
        ]
      });
      
      if (result && !result.canceled) {
        console.log('Selected files:', result.filePaths);
      }
    } catch (error) {
      console.error('File selection failed:', error);
    }
  };

  // Handle search input changes
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
  };

  // Handle logout
  const handleLogout = () => {
    dispatch(logout());
  };

  // Effect for search with proper cleanup
  useEffect(() => {
    if (searchQuery.trim()) {
      const timer = setTimeout(handleSearch, 300); // Debounce search
      return () => clearTimeout(timer);
    } else {
      setSearchResults([]);
    }
  }, [searchQuery]);

  if (loading) {
    return <div className="app">Loading...</div>;
  }

  if (error) {
    return (
      <div className="app">
        <div className="error-message">
          <h2>错误</h2>
          <p>{error}</p>
          <button onClick={() => dispatch( clearError())}>
            清除错误
          </button>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="app">
        <div className="login-container">
          <h1>Group IM Client</h1>
          <form onSubmit={handleLogin} className="login-form">
            <div className="form-group">
              <label htmlFor="loginAccount">账号:</label>
              <input
                type="text"
                id="loginAccount"
                value={loginAccount}
                onChange={(e) => setLoginAccount(e.target.value)}
                required
              />
            </div>
            
            <div className="form-group">
              <label htmlFor="password">密码:</label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            
            <div className="form-group checkbox-group">
              <label>
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                记住我
              </label>
            </div>
            
            <button type="submit" disabled={loading}>
              {loading ? '登录中...' : '登录'}
            </button>
          </form>
          
          <div className="login-footer">
            <button onClick={handleFileSelect}>选择文件测试</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Group IM</h1>
        <div className="user-info">
          <span>欢迎, {user?.username || '用户'}</span>
          <button onClick={handleLogout}>退出登录</button>
        </div>
      </header>
      
      <main className="app-main">
        <div className="welcome-message">
          <h2>欢迎使用 Group IM</h2>
          <p>您已成功登录系统</p>
          <p>用户ID: {user?.userId || 'N/A'}</p>
          <p>邮箱: {user?.email || 'N/A'}</p>
        </div>
      </main>
    </div>
  );
};

export default App;