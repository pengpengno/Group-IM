import { ipcMain } from 'electron';
import axios from 'axios';

// Define the base URL for the API
const BASE_URL = 'http://127.0.0.1:8080'; // This would come from environment/config

// Handle login request
ipcMain.handle('login', async (_, credentials) => {
  try {
    console.log('Main Process: Login attempt for:', credentials.loginAccount);
    const response = await axios.post(`${BASE_URL}/api/users/login`, credentials, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    console.log('Main Process: Login success', response.data);

    // Return the token and user info
    return {
      success: true,
      data: response.data
    };
  } catch (error: any) {
    console.error('Main Process: Login error detailed:', {
      message: error.message,
      code: error.code,
      response: error.response?.data,
      status: error.response?.status
    });
    return {
      success: false,
      error: error.response?.data?.message || error.message || 'Login failed'
    };
  }
});

// Handle user query request
ipcMain.handle('query-users', async (_, query, token) => {
  try {
    const params = new URLSearchParams();
    params.append('query', query);

    const headers: any = {
      'Content-Type': 'application/x-www-form-urlencoded'
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await axios.post(`${BASE_URL}/api/users/query`, params, { headers });

    return {
      success: true,
      data: response.data
    };
  } catch (error: any) {
    console.error('Query users error:', error);
    return {
      success: false,
      error: error.response?.data?.message || error.message || 'Query failed'
    };
  }
});

// Handle getting user companies
ipcMain.handle('get-user-companies', async (_, token) => {
  try {
    const response = await axios.get(`${BASE_URL}/api/users/company/list`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    return {
      success: true,
      data: response.data
    };
  } catch (error: any) {
    console.error('Get user companies error:', error);
    return {
      success: false,
      error: error.response?.data?.message || error.message || 'Get companies failed'
    };
  }
});