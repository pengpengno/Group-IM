import { ipcMain, Notification } from 'electron';

// Show desktop notification
ipcMain.handle('show-notification', async (_, options) => {
  try {
    const { title, body, silent = false, ...otherOptions } = options;
    
    if (Notification.isSupported()) {
      const notification = new Notification({
        title,
        body,
        silent,
        ...otherOptions
      });
      
      notification.show();
      
      return {
        success: true,
        message: 'Notification displayed successfully'
      };
    } else {
      console.warn('Notifications not supported on this platform');
      return {
        success: false,
        message: 'Notifications not supported on this platform'
      };
    }
  } catch (error: any) {
    console.error('Notification error:', error);
    return {
      success: false,
      error: error.message || 'Failed to show notification'
    };
  }
});

// Handle requesting notification permission (only needed on some platforms)
ipcMain.handle('request-notification-permission', async () => {
  try {
    // On most platforms, notifications work without explicit permission
    // This is mainly for platforms that require permission
    return {
      success: true,
      granted: true
    };
  } catch (error: any) {
    console.error('Notification permission error:', error);
    return {
      success: false,
      granted: false,
      error: error.message || 'Failed to request notification permission'
    };
  }
});