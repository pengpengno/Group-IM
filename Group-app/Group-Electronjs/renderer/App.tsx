import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { clearError } from './features/auth/authSlice';
import LoginScreen from './features/auth/LoginScreen';
import Dashboard from './features/dashboard/Dashboard';
import Notification from './components/common/Notification';
import { webRTCService } from './services/WebRTCService';
import { socketService } from './services/socketService';
import { isElectronEnvironment } from './services/api/electronAPI';
import { syncCurrentPushEndpoint, disableCurrentPushEndpoint } from './services/notificationEndpointService';
import { notificationRuntimeService } from './services/notificationRuntimeService';
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

      const isElectron = isElectronEnvironment();
      const token = localStorage.getItem('token') || '';

      // Desktop uses Electron IPC + TCP for chat realtime sync.
      // Web uses the same socketService, but it degrades to browser WebSocket on /ws.
      socketService.initialize(store, user.userId, __TCP_HOST__, Number(__TCP_PORT__), token, user.username);
      notificationRuntimeService.bindElectronNotificationClicks();
      syncCurrentPushEndpoint().catch((error) => {
        console.warn('Failed to sync browser push endpoint:', error);
      });

      return () => {
        webRTCService.destroy();
        socketService.disconnect();
        disableCurrentPushEndpoint().catch((error) => {
          console.warn('Failed to disable browser push endpoint:', error);
        });
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

      <Dashboard />
    </div>
  );
};

export default App;
