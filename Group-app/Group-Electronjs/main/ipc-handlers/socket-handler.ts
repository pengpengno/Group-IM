import { ipcMain, BrowserWindow } from 'electron';
import * as net from 'net';
import { protobufService, IBaseMessagePkg, proto } from '../services/protobuf-service';
import Long from 'long';

// Socket Client 配置
interface SocketConfig {
  host: string;
  port: number;
  userId: string;
  token: string;
  username: string;
}

// 对应 UserInfoProto.proto 中的 PlatformType
enum PlatformType {
  WEB = 0,
  ANDROID = 1,
  IOS = 2,
  WINDOWS = 3,
  MAC = 4,
  LINUX = 5
}

interface SocketMessage {
  type: 'heartbeat' | 'message' | 'connect' | 'disconnect';
  data?: any;
}

class ElectronSocketClient {
  private socket: net.Socket | null = null;
  private config: SocketConfig | null = null;
  private receiveBuffer = Buffer.alloc(0);
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 2000;
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private heartbeatTimeout: NodeJS.Timeout | null = null;
  private lastHeartbeatTime = 0;
  private readonly HEARTBEAT_INTERVAL = 25000; // 25秒发送一次心跳
  private readonly HEARTBEAT_TIMEOUT = 35000; // 35秒超时
  private mainWindow: BrowserWindow | null = null;
  private isReconnecting = false;
  private connectionPromise: Promise<void> | null = null;

  constructor(mainWindow: BrowserWindow) {
    this.mainWindow = mainWindow;
  }

  /**
   * 编码 Varint32
   */
  private encodeVarint32(value: number): Buffer {
    const buffer: number[] = [];
    let v = value >>> 0; // 转换为无符号32位整数
    while (true) {
      if ((v & ~0x7f) === 0) {
        buffer.push(v);
        break;
      } else {
        buffer.push((v & 0x7f) | 0x80);
        v >>>= 7;
      }
    }
    return Buffer.from(buffer);
  }

  /**
   * 解码 Varint32
   */
  private decodeVarint32(buffer: Buffer, offset: number = 0): { value: number; bytesRead: number } {
    let result = 0;
    let shift = 0;
    let bytesRead = 0;

    while (shift < 32) {
      if (offset + bytesRead >= buffer.length) {
        throw new Error('Not enough bytes to decode varint32');
      }

      const b = buffer[offset + bytesRead];
      bytesRead++;

      result |= (b & 0x7f) << shift;

      if ((b & 0x80) === 0) {
        return { value: result >>> 0, bytesRead };
      }

      shift += 7;
    }

    throw new Error('Malformed varint32');
  }

  /**
   * 获取当前运行平台
   */
  private getPlatformType(): PlatformType {
    const platform = process.platform;
    if (platform === 'win32') return PlatformType.WINDOWS;
    if (platform === 'darwin') return PlatformType.MAC;
    return PlatformType.LINUX;
  }

  /**
   * 注册到远程服务器（绑定用户信息）
   */
  private async registerToRemote(): Promise<void> {
    if (!this.config || !this.socket) return;

    console.log(`Registering user ${this.config.username} (ID: ${this.config.userId}) to remote server...`);

    const userInfo: proto.IUserInfo = {
      userId: Long.fromString(this.config.userId),
      username: this.config.username,
      accessToken: this.config.token,
      platformType: this.getPlatformType()
    };

    try {
      const payload: IBaseMessagePkg = { userInfo };
      const data = protobufService.encode(payload);
      await this.send(data);
      console.log('Registration message sent successfully');
    } catch (error) {
      console.error('Failed to send registration message:', error);
      throw error;
    }
  }

  /**
   * 连接到服务器
   */
  async connect(host: string, port: number, userId: string, token: string, username: string): Promise<void> {
    // 竞态保护：如果已经连接且可用，直接返回
    if (this.socket && this.socket.writable) {
      console.log('Socket already connected, skipping new connection request');
      return;
    }

    // 竞态保护：防止重复发起连接
    if (this.connectionPromise) {
      // 只有在非重连状态下才打印此日志，减少刷屏
      if (!this.isReconnecting) {
        console.log('Socket connection already in progress...');
      }
      return this.connectionPromise;
    }

    this.config = { host, port, userId, token, username };

    this.connectionPromise = new Promise((resolve, reject) => {
      console.log(`Establishing TCP connection to ${host}:${port}...`);

      const socket = net.createConnection(port, host);
      this.socket = socket;

      socket.on('connect', async () => {
        console.log(`Socket connected to ${host}:${port}`);
        // 连接建立后，设置闲置超时为心跳超时的时长
        socket.setTimeout(this.HEARTBEAT_TIMEOUT);
        this.reconnectAttempts = 0;
        this.lastHeartbeatTime = Date.now();

        try {
          // 在连接成功后，立即按照 Android/KMP 逻辑注册身份
          await this.registerToRemote();

          this.startHeartbeat();
          this.startReceiving();
          this.notifyRenderer('socket:connected', { host, port });
          this.connectionPromise = null;
          resolve();
        } catch (error) {
          // 注册失败通常是逻辑或鉴权问题（如 Token 失效），这种情况下不应重连
          this.connectionPromise = null;
          this.isReconnecting = false; // 明确停止重连状态
          this.reconnectAttempts = this.maxReconnectAttempts; // 设为最大，防止后续自动重连
          this.socket?.destroy();
          this.socket = null;
          reject(error);
        }
      });

      socket.on('error', (error) => {
        console.error('Socket error:', error);
        this.notifyRenderer('socket:error', { message: error.message });
        if (this.connectionPromise) {
          this.connectionPromise = null;
          reject(error);
        }
      });

      socket.on('close', (hadError) => {
        // 如果是正在重连中触发的 Close，不打印重复日志
        if (!this.isReconnecting) {
          console.log(`Socket closed (hadError: ${hadError})`);
        }
        this.clearHeartbeat();
        this.notifyRenderer('socket:disconnected', {});
        this.connectionPromise = null;
        this.socket = null;

        // 如果配置存在且不是主动关闭，也不是刚注册失败，则尝试重放
        if (this.config && !this.isReconnecting && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.startAutoReconnect();
        }
      });

      // 设置连接超时
      socket.setTimeout(8000); // 增加到8秒
      socket.on('timeout', () => {
        console.warn('Socket connection timeout');
        socket.destroy();
      });
    });

    return this.connectionPromise;
  }

  /**
   * 发送数据
   */
  async send(data: Buffer): Promise<void> {
    if (!this.socket || !this.socket.writable) {
      throw new Error('Socket is not connected or not writable');
    }

    return new Promise((resolve, reject) => {
      const lengthPrefix = this.encodeVarint32(data.length);
      const fullData = Buffer.concat([lengthPrefix, data]);

      this.socket!.write(fullData, (error) => {
        if (error) {
          console.error('Error writing to socket:', error);
          reject(error);
        } else {
          resolve();
        }
      });
    });
  }

  /**
   * 启动心跳包
   */
  private startHeartbeat(): void {
    this.clearHeartbeat();

    this.heartbeatInterval = setInterval(async () => {
      try {
        if (this.socket && this.socket.writable) {
          // 使用 protobufService 创建正确的心跳包
          const heartbeatData = protobufService.createHeartbeat(true);
          await this.send(heartbeatData);
          this.lastHeartbeatTime = Date.now();
          console.log('Heartbeat sent');
        }
      } catch (error) {
        console.error('Error sending heartbeat:', error);
        this.startAutoReconnect();
      }
    }, this.HEARTBEAT_INTERVAL);

    // 监控心跳超时
    this.heartbeatTimeout = setInterval(() => {
      const timeSinceLastHeartbeat = Date.now() - this.lastHeartbeatTime;
      if (timeSinceLastHeartbeat > this.HEARTBEAT_TIMEOUT) {
        console.warn('Heartbeat timeout, reconnecting...');
        this.socket?.destroy();
      }
    }, 5000);
  }

  /**
   * 清除心跳
   */
  private clearHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
    if (this.heartbeatTimeout) {
      clearInterval(this.heartbeatTimeout);
      this.heartbeatTimeout = null;
    }
  }

  /**
   * 启动接收数据
   */
  private startReceiving(): void {
    if (!this.socket) return;

    this.socket.on('data', (chunk: Buffer) => {
      this.receiveBuffer = Buffer.concat([this.receiveBuffer, chunk]);

      // 尝试读取完整的消息
      while (this.receiveBuffer.length > 0) {
        try {
          const { value: length, bytesRead } = this.decodeVarint32(this.receiveBuffer);

          if (this.receiveBuffer.length < bytesRead + length) {
            // 数据未完整，等待更多数据
            break;
          }

          const messageData = this.receiveBuffer.slice(bytesRead, bytesRead + length);
          this.receiveBuffer = this.receiveBuffer.slice(bytesRead + length);

          // 处理接收到的消息
          this.handleMessage(messageData);
        } catch (error) {
          console.error('Error parsing message:', error);
          break;
        }
      }
    });
  }

  /**
   * 处理收到的消息
   */
  private handleMessage(data: Buffer): void {
    try {
      // 使用 protobufService 解码消息
      const decodedMessage = protobufService.decode(data);

      // 只要收到任何有效消息，就更新心跳时间
      this.lastHeartbeatTime = Date.now();

      // 处理心跳回应
      if (decodedMessage.heartbeat) {
        console.log('Received heartbeat:', decodedMessage.heartbeat.ping ? 'PING' : 'PONG');
        if (decodedMessage.heartbeat.ping) {
          // 收到 PING，回复 PONG
          const pongData = protobufService.createHeartbeat(false);
          this.send(pongData).catch(err => console.error('Failed to send PONG:', err));
        }
        return;
      }

      // 将解码后的消息转换为普通的 POJO 对象，方便渲染进程处理
      const messageObj = protobufService.toObject(decodedMessage);

      // 通知渲染进程有新消息
      this.notifyRenderer('socket:message', {
        type: 'message',
        payload: messageObj
      });
    } catch (error) {
      console.error('Error handling message:', error);
      // 如果解码失败，至少通知渲染进程原始数据
      this.notifyRenderer('socket:message', {
        type: 'raw',
        data: data.toString('base64')
      });
    }
  }

  /**
   * 自动重连
   */
  private startAutoReconnect(): void {
    // 已经正在重连，或者重连次数超限，或者没有配置 (说明已登出)，则退出
    if (this.isReconnecting || this.reconnectAttempts >= this.maxReconnectAttempts || !this.config) {
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        console.error('Max reconnect attempts reached, stopping.');
        this.notifyRenderer('socket:reconnect-failed', {});
      }
      return;
    }

    this.isReconnecting = true;
    this.reconnectAttempts++;

    // 指数退避延迟，基础2秒，最大30秒
    const delay = Math.min(30000, 2000 * Math.pow(1.5, this.reconnectAttempts - 1));
    console.log(`Reconnecting (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${Math.round(delay)}ms...`);
    this.notifyRenderer('socket:reconnecting', { attempt: this.reconnectAttempts });

    setTimeout(async () => {
      // 检查执行时配置是否还在 (防止 setTimeout 期间用户登出)
      if (!this.config) {
        this.isReconnecting = false;
        return;
      }

      try {
        await this.connect(this.config.host, this.config.port, this.config.userId, this.config.token, this.config.username);
        console.log('Reconnection successful');
        this.isReconnecting = false;
        this.reconnectAttempts = 0;
      } catch (error) {
        console.error('Reconnection attempt failed:', error);
        this.isReconnecting = false;
        // 递归触发下一次重连
        this.startAutoReconnect();
      }
    }, delay);
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.clearHeartbeat();
    this.connectionPromise = null;
    if (this.socket) {
      this.socket.destroy();
      this.socket = null;
    }
    this.receiveBuffer = Buffer.alloc(0);
    this.config = null;
    this.reconnectAttempts = 0;
    this.isReconnecting = false;
  }

  /**
   * 检查连接是否活跃
   */
  isActive(): boolean {
    const socketActive = this.socket !== null && this.socket.writable && !this.socket.destroyed;
    if (!socketActive) return false;

    const timeSinceLastHeartbeat = Date.now() - this.lastHeartbeatTime;
    return timeSinceLastHeartbeat <= this.HEARTBEAT_TIMEOUT;
  }

  /**
   * 通知渲染进程
   */
  private notifyRenderer(channel: string, data: any): void {
    if (this.mainWindow && !this.mainWindow.isDestroyed()) {
      this.mainWindow.webContents.send(channel, data);
    }
  }
}

// 单例实例
let socketClient: ElectronSocketClient | null = null;

export async function initializeSocketHandler(mainWindow: BrowserWindow): Promise<void> {
  socketClient = new ElectronSocketClient(mainWindow);

  // 应用启动时初始化 Protobuf 服务 (单例模式，内部有哨兵变量)
  await protobufService.initialize();

  // Socket 连接
  ipcMain.handle('socket:connect', async (_, { host, port, userId, token, username }) => {
    try {
      if (!socketClient) {
        throw new Error('Socket client not initialized');
      }

      await socketClient.connect(host, port, userId, token, username);
      return { success: true };
    } catch (error: any) {
      console.error('Socket connection error:', error);
      return {
        success: false,
        error: error.message || 'Connection failed'
      };
    }
  });

  // Socket 发送数据
  ipcMain.handle('socket:send', async (_, dataBase64: string) => {
    try {
      if (!socketClient) {
        throw new Error('Socket client not initialized');
      }
      const data = Buffer.from(dataBase64, 'base64');
      await socketClient.send(data);
      return { success: true };
    } catch (error: any) {
      console.error('Socket send error:', error);
      return {
        success: false,
        error: error.message || 'Send failed'
      };
    }
  });

  // Socket 断开连接
  ipcMain.handle('socket:disconnect', async () => {
    try {
      if (socketClient) {
        socketClient.disconnect();
      }
      return { success: true };
    } catch (error: any) {
      console.error('Socket disconnect error:', error);
      return {
        success: false,
        error: error.message || 'Disconnect failed'
      };
    }
  });

  // Socket 检查连接状态
  ipcMain.handle('socket:is-active', async () => {
    try {
      if (!socketClient) {
        return { active: false };
      }
      return { active: socketClient.isActive() };
    } catch (error: any) {
      return { active: false };
    }
  });

  // 发送结构化消息
  ipcMain.handle('socket:send-message', async (_, payload: any) => {
    try {
      if (!socketClient) {
        throw new Error('Socket client not initialized');
      }
      // 使用 protobufService 编码
      const data = protobufService.encode(payload);
      await socketClient.send(data);
      return { success: true };
    } catch (error: any) {
      console.error('Socket send-message error:', error);
      return {
        success: false,
        error: error.message || 'Send failed'
      };
    }
  });
}
