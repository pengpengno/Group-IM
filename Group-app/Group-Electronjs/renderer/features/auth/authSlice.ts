import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { LocalUser as User, AuthData, AuthState } from '../../types';

const initialState: AuthState = {
  isAuthenticated: false,
  loading: false,
  error: null,
  user: null
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginStart(state) {
      state.loading = true;
      state.error = null;
    },
    loginSuccess(state, action: PayloadAction<{ user: User; token: string; refreshToken: string }>) {
      state.isAuthenticated = true;
      state.loading = false;
      state.user = {
        ...action.payload.user,
        token: action.payload.token,
        refreshToken: action.payload.refreshToken
      };
      state.error = null;
    },
    loginFailure(state, action: PayloadAction<string>) {
      state.loading = false;
      state.error = action.payload;
    },
    logout(state) {
      state.isAuthenticated = false;
      state.user = null;
      state.error = null;
    },
    clearError(state) {
      state.error = null;
    }
  }
});

export const { loginStart, loginSuccess, loginFailure, logout, clearError } = authSlice.actions;

export default authSlice.reducer;

// Export AuthState for use in other components
export type { AuthState };