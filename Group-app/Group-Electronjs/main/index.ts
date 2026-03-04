import { app, BrowserWindow, ipcMain, Tray, Menu, dialog } from 'electron';
import * as path from 'path';
import * as url from 'url';

let mainWindow: BrowserWindow | null = null;
let tray: Tray | null = null;

// IPC Handlers
import './ipc-handlers/auth-handler';
import './ipc-handlers/file-handler';
import './ipc-handlers/notification-handler';
import { initializeSocketHandler } from './ipc-handlers/socket-handler';

function createWindow() {
  mainWindow = new BrowserWindow({
    height: 800,
    width: 1200,
    minWidth: 800,
    minHeight: 600,
    frame: false, // Remove native frame
    titleBarStyle: 'hidden', // Hidden title bar
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    },
    resizable: true,
    icon: path.join(__dirname, '../assets/icon.png')
  });

  // Load index.html from the same directory (dist)
  // In dev mode (concurrently), it's built to dist/index.html by HtmlWebpackPlugin
  mainWindow.loadFile(path.join(__dirname, 'index.html'));

  // Initialize Socket Handler
  if (mainWindow) {
    initializeSocketHandler(mainWindow).catch(err => {
      console.error('Failed to initialize socket handler:', err);
    });
  }

  // Open dev tools in development
  if (process.env.NODE_ENV === 'development') {
    mainWindow.webContents.openDevTools();
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// Create system tray icon
function createTray() {
  tray = new Tray(path.join(__dirname, '../assets/tray-icon.png'));
  const contextMenu = Menu.buildFromTemplate([
    {
      label: 'Open',
      click: () => {
        if (mainWindow) {
          mainWindow.show();
        }
      }
    },
    {
      label: 'Settings',
      click: () => {
        // TODO: Implement settings window
        console.log('Settings clicked');
      }
    },
    {
      label: 'Exit',
      click: () => {
        app.quit();
      }
    }
  ]);

  tray.setContextMenu(contextMenu);
  tray.setIgnoreDoubleClickEvents(true);
  tray.on('click', () => {
    if (mainWindow) {
      mainWindow.show();
    }
  });
}

try {
  // Make sure only one instance is running
  const gotTheLock = app.requestSingleInstanceLock();

  if (!gotTheLock) {
    app.quit();
  } else {
    app.on('second-instance', (event, commandLine, workingDirectory) => {
      // Someone tried to run a second instance, we should focus our window.
      if (mainWindow) {
        if (mainWindow.isMinimized()) mainWindow.restore();
        mainWindow.focus();
      }
    });

    app.on('ready', () => {
      createWindow();
      createTray();
    });
  }

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
      app.quit();
    }
  });

  app.on('activate', () => {
    if (mainWindow === null) {
      createWindow();
    }
  });
} catch (e) {
  console.error(e);
}