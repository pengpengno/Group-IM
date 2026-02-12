import { ipcMain } from 'electron';
import axios from 'axios';

// Define the base URL for the API
const BASE_URL = 'http://localhost:8080'; // This would come from environment/config

// Handle login request
ipcMain.handle('login', async (_, credentials) => {
  try {
    const response = await axios.post(`${BASE_URL}/api/users/login`, credentials, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    // Return the token and user info
    return {
      success: true,
      data: response.data
    };
  } catch (error: any) {
    console.error('Login error:', error);
    return {
      success: false,
      error: error.response?.data?.message || error.message || 'Login failed'
    };
  }
});

// Handle user query request
ipcMain.handle('query-users', async (_, query) => {
  try {
    const formData = new FormData();
    formData.append('query', query);

    // Note: FormData is not available in Node.js context, so we'll use form-urlencoded
    const params = new URLSearchParams();
    params.append('query', query);

    const response = await axios.post(`${BASE_URL}/api/users/query`, params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });

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