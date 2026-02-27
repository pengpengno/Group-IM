import { configureStore } from '@reduxjs/toolkit';
import authReducer from './features/auth/authSlice';
import videoCallReducer from './features/video-call/videoCallSlice';
import chatReducer from './features/chat/chatSlice';
import contactsReducer from './features/contacts/contactsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    videoCall: videoCallReducer,
    chat: chatReducer,
    contacts: contactsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
