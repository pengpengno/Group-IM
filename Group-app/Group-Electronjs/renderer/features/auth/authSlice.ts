import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { LocalUser as User, AuthData, AuthState, CompanyDTO } from '../../types';

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
    loginSuccess(state, action: PayloadAction<{
      user: User;
      token: string;
      refreshToken: string;
      companies?: CompanyDTO[];
      currentCompany?: CompanyDTO;
    }>) {
      const { user, token, refreshToken, companies, currentCompany } = action.payload;

      // 保存到本地存储
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify({
        ...user,
        companies,
        currentCompany
      }));

      state.isAuthenticated = true;
      state.loading = false;
      state.user = {
        ...user,
        userId: user.userId,
        username: user.username,
        email: user.email,
        phoneNumber: user.phoneNumber,
        token: token,
        refreshToken: refreshToken,
        companies: companies,
        currentCompany: currentCompany
      };
      state.error = null;
    },
    setCompanies(state, action: PayloadAction<CompanyDTO[]>) {
      if (state.user) {
        state.user.companies = action.payload;
        localStorage.setItem('user', JSON.stringify(state.user));
      }
    },
    setCurrentCompany(state, action: PayloadAction<CompanyDTO>) {
      if (state.user) {
        state.user.currentCompany = action.payload;
        localStorage.setItem('user', JSON.stringify(state.user));
      }
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

export const { loginStart, loginSuccess, loginFailure, logout, clearError, setCompanies, setCurrentCompany } = authSlice.actions;

export default authSlice.reducer;

// Export AuthState for use in other components
export type { AuthState };