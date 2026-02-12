import { ipcMain, dialog } from 'electron';
import * as fs from 'fs';
import * as path from 'path';
import axios from 'axios';

// Define the base URL for the API
const BASE_URL = 'http://localhost:8080'; // This would come from environment/config

// Handle file upload request
ipcMain.handle('upload-file', async (_, filePath, clientId) => {
  try {node --version
npm --version
    const fileBuffer = fs.readFileSync(filePath);
    const fileName = path.basename(filePath);
    
    const formData = new FormData();
    formData.append('file', new Blob([fileBuffer]), fileName);
    formData.append('clientId', clientId || Math.random().toString(36).substring(2, 15));
    
    // For Node.js we need to use a different approach since FormData is browser-specific
    // Using axios with form-data library instead
    const FormDataNode = require('form-data');
    const form = new FormDataNode();
    form.append('file', fs.createReadStream(filePath));
    form.append('clientId', clientId || Math.random().toString(36).substring(2, 15));
    
    const response = await axios.post(`${BASE_URL}/api/files/upload`, form, {
      headers: form.getHeaders(),
      onUploadProgress: (progressEvent) => {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        // Send progress to renderer process
        if (progressEvent.total) {
          // mainWindow.webContents.send('upload-progress', { clientId, progress });
        }
      }
    });

    return {
      success: true,
      data: response.data
    };
  } catch (error: any) {
    console.error('File upload error:', error);
    return {
      success: false,
      error: error.response?.data?.message || error.message || 'File upload failed'
    };
  }
});

// Handle file selection dialog
ipcMain.handle('select-file', async (_, options = {}) => {
  try {
    const result = await dialog.showOpenDialog({
      properties: ['openFile'],
      ...options
    });

    if (result.canceled) {
      return { canceled: true };
    }

    const filePath = result.filePaths[0];
    const stats = fs.statSync(filePath);
    
    return {
      canceled: false,
      filePath,
      fileName: path.basename(filePath),
      fileSize: stats.size,
      mimeType: getMimeType(filePath)
    };
  } catch (error: any) {
    console.error('File selection error:', error);
    return {
      success: false,
      error: error.message || 'File selection failed'
    };
  }
});

// Simple mime type detection based on file extension
function getMimeType(filePath: string): string {
  const ext = path.extname(filePath).toLowerCase();
  switch (ext) {
    case '.jpg':
    case '.jpeg':
      return 'image/jpeg';
    case '.png':
      return 'image/png';
    case '.gif':
      return 'image/gif';
    case '.pdf':
      return 'application/pdf';
    case '.txt':
      return 'text/plain';
    case '.mp4':
      return 'video/mp4';
    case '.mov':
      return 'video/quicktime';
    default:
      return 'application/octet-stream';
  }
}