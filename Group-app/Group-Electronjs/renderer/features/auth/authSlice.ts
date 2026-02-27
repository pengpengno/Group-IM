import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { LocalUser as User, AuthData, AuthState } from '../../types';

const getInitialState = (): AuthState => {
  const token = localStorage.getItem('token');
  const userJson = localStorage.getItem('user');

  if (token && userJson) {
    try {
      const user = JSON.parse(userJson);
      return {
        isAuthenticated: true,
        loading: false,
        error: null,
        user: {
          ...user,
          token,
          refreshToken: localStorage.getItem('refreshToken') || ''
        }
      };
    } catch (e) {
      console.error('Failed to parse user from localStorage');
    }
  }

  return {
    isAuthenticated: false,
    loading: false,
    error: null,
    user: null
  };
};

const initialState: AuthState = getInitialState();

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginStart(state) {
      state.loading = true;
      state.error = null;
    },
    loginSuccess(state, action: PayloadAction<{ user: User; token: string; refreshToken: string }>) {
      const { user, token, refreshToken } = action.payload;

      // 保存到本地存储
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));

      state.isAuthenticated = true;
      state.loading = false;
      state.user = {
        ...user,
        userId: user.userId,
        username: user.username,
        email: user.email,
        phoneNumber: user.phoneNumber,
        token: token,
        refreshToken: refreshToken
      };
      state.error = null;
    },
    loginFailure(state, action: PayloadAction<string>) {
      state.loading = false;
      state.error = action.payload;
    },
    logout(state) {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      state.isAuthenticated = false;
      state.user = null;
      state.error = null;
    },
    clearError(state) {
      state.error = null;
    },
    restoreSession(state) {
      const token = localStorage.getItem('token');
      const userJson = localStorage.getItem('user');
      if (token && userJson) {
        state.user = {
          ...JSON.parse(userJson),
          token,
          refreshToken: localStorage.getItem('refreshToken') || ''
        };
        state.isAuthenticated = true;
      }
    }
  }
});

export const { loginStart, loginSuccess, loginFailure, logout, clearError } = authSlice.actions;

export default authSlice.reducer;

// Export AuthState for use in other components
export type { AuthState };