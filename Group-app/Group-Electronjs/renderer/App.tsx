import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { clearError } from './features/auth/authSlice';
import LoginScreen from './features/auth/LoginScreen';
import Dashboard from './features/dashboard/Dashboard';
import Notification from './components/common/Notification';
import { webRTCService } from './services/WebRTCService';
import { socketService } from './services/socketService';
import { store } from './store';

const App: React.FC = () => {
  const dispatch = useDispatch();
  const { isAuthenticated, user, error } = useSelector(
    (state: { auth: ReturnType<typeof import('./features/auth/authSlice').default> }) => state.auth
  );

  useEffect(() => {
    if (isAuthenticated && user && user.userId) {
      // Initialize unified WebRTC service
      webRTCService.initialize(store, user.userId);

      // Initialize Socket connection
      const token = localStorage.getItem('token') || '';
      socketService.initialize(store, user.userId, 'localhost', 8088, token, user.username);

      return () => {
        webRTCService.destroy();
        socketService.disconnect();
      };
    }
  }, [isAuthenticated, user]);

  if (!isAuthenticated) {
    return (
      <>
        <LoginScreen />
        {error && (
          <Notification
            message={error}
            type="error"
            onClose={() => dispatch(clearError())}
          />
        )}
      </>
    );
  }

  return (
    <div className="app">
      {error && (
        <Notification
          message={error}
          type="error"
          onClose={() => dispatch(clearError())}
        />
      )}

      <Dashboard user={user} />
    </div>
  );
};

export default App;