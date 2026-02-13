import { configureStore } from '@reduxjs/toolkit';
import authReducer from './features/auth/authSlice';
import videoCallReducer from './features/video-call/videoCallSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    videoCall: videoCallReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;