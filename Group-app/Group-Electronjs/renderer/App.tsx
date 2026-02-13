import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { clearError } from './features/auth/authSlice';
import IncomingCallAlert from './features/video-call/IncomingCallAlert';
import VideoCallModal from './features/video-call/VideoCallModal';
import LoginScreen from './features/auth/LoginScreen';
import Dashboard from './features/dashboard/Dashboard';
import { signalingService } from './services/signaling';
import { webRTCManager } from './services/webrtc';
import { store } from './store';

// Main App component
const App: React.FC = () => {
  const dispatch = useDispatch();
  const { isAuthenticated, user, loading, error } = useSelector(
    (state: { auth: ReturnType<typeof import('./features/auth/authSlice').default> }) => state.auth
  );

  // Initialize signaling and WebRTC services when user logs in
  useEffect(() => {
    if (isAuthenticated && user && user.userId) {
      signalingService.initialize(store, user.userId);
      webRTCManager.initialize(store, user.userId);

      return () => {
        signalingService.disconnect();
      };
    }
  }, [isAuthenticated, user]);

  if (loading) {
    return <div className="app">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <LoginScreen />;
  }

  // If there's a global error that isn't a login error (which LoginScreen handles),
  // we might want to show it. For now, we'll let Dashboard render and relying on
  // specific feature error handling, or simple overlay.
  // We can keep the error boundary concept if needed, but for "Premium" feel,
  // we shouldn't block the whole UI for a minor error if possible.

  return (
    <div className="app">
      <IncomingCallAlert />
      <VideoCallModal />

      {error && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          zIndex: 9999,
          background: '#fee2e2',
          color: '#b91c1c',
          padding: '12px 20px',
          borderRadius: '8px',
          boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <span>{error}</span>
          <button
            onClick={() => dispatch(clearError())}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#b91c1c',
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            ✕
          </button>
        </div>
      )}

      <Dashboard user={user} />
    </div>
  );
};

export default App;