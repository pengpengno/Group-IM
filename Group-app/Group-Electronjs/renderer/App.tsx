import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { clearError } from './features/auth/authSlice';
import LoginScreen from './features/auth/LoginScreen';
import Dashboard from './features/dashboard/Dashboard';
import Notification from './components/common/Notification';
import { webRTCService } from './services/WebRTCService';
import { socketService } from './services/socketService';
import { meetingSignalingService } from './services/meetingSignalingService';
import { syncCurrentPushEndpoint, disableCurrentPushEndpoint } from './services/notificationEndpointService';
import { notificationRuntimeService } from './services/notificationRuntimeService';
import { store } from './store';

const App: React.FC = () => {
  const dispatch = useDispatch();
  const { isAuthenticated, user, error } = useSelector(
    (state: { auth: ReturnType<typeof import('./features/auth/authSlice').default> }) => state.auth
  );
  const sessionUserId = user?.userId || '';
  const sessionUsername = user?.username || '';
  const sessionToken = user?.token || localStorage.getItem('token') || '';

  useEffect(() => {
    if (isAuthenticated && sessionUserId) {
      console.log('[App] initialize-realtime-session', {
        sessionUserId,
        sessionUsername,
        hasToken: !!sessionToken
      });

      // App boot owns lifecycle wiring:
      // transport -> signaling -> call session.
      // Keeping signaling alive immediately after login ensures incoming invites
      // can surface even before the user manually opens any call UI.
      meetingSignalingService.initialize();

      // Initialize unified WebRTC service
      webRTCService.initialize(store, sessionUserId);

      // Desktop uses Electron IPC + TCP for chat realtime sync.
      // Web uses the same socketService, but it degrades to browser WebSocket on /ws.
      socketService.initialize(store, sessionUserId, __TCP_HOST__, Number(__TCP_PORT__), sessionToken, sessionUsername);
      notificationRuntimeService.bindElectronNotificationClicks();
      syncCurrentPushEndpoint().catch((error) => {
        console.warn('Failed to sync browser push endpoint:', error);
      });

      return () => {
        console.log('[App] cleanup-realtime-session', {
          sessionUserId,
          sessionUsername
        });
        webRTCService.destroy();
        meetingSignalingService.destroy();
        socketService.disconnect();
        disableCurrentPushEndpoint().catch((error) => {
          console.warn('Failed to disable browser push endpoint:', error);
        });
      };
    }
  }, [isAuthenticated, sessionUserId, sessionUsername, sessionToken]);

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
