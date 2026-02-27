import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { clearError } from './features/auth/authSlice';
import IncomingCallAlert from './features/video-call/IncomingCallAlert';
import VideoCallModal from './features/video-call/VideoCallModal';
import LoginScreen from './features/auth/LoginScreen';
import Dashboard from './features/dashboard/Dashboard';
import Notification from './components/common/Notification';
import { signalingService } from './services/signaling';
import { webRTCManager } from './services/webrtc';
import { store } from './store';

const App: React.FC = () => {
  const dispatch = useDispatch();
  const { isAuthenticated, user, error } = useSelector(
    (state: { auth: ReturnType<typeof import('./features/auth/authSlice').default> }) => state.auth
  );

  useEffect(() => {
    if (isAuthenticated && user && user.userId) {
      signalingService.initialize(store, user.userId);
      webRTCManager.initialize(store, user.userId);

      return () => {
        signalingService.disconnect();
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
      <IncomingCallAlert />
      <VideoCallModal />

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