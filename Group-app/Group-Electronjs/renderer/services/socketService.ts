import { Store } from '@reduxjs/toolkit';
import { RootState } from '../store';
import { getElectronAPI } from './api/electronAPI';
import { addMessage } from '../features/chat/chatSlice';

class SocketService {
  private store: Store<RootState> | null = null;
  private userId: string | null = null;
  private host: string = 'localhost';
  private port: number = 8088;
  private token: string = '';
  private username: string = '';
  private intentionToClose = false;
  private isInitializing = false;
  private isConnected = false;
  private socket: WebSocket | null = null;
  private receiveBuffer: Uint8Array = new Uint8Array(0);
  private connectionCheckInterval: NodeJS.Timeout | null = null;

  initialize(store: Store<RootState>, userId: string, host: string = 'localhost', port: number = 8088, token: string = '', username: string = '') {
    if (this.isConnected || this.isInitializing) {
      console.log('Socket already connected or initializing, skipping...');
      return;
    }

    this.store = store;
    this.userId = userId;
    this.host = host;
    this.port = port;
    this.token = token;
    this.username = username;
    this.intentionToClose = false;
    this.isInitializing = true;
    this.connect();
  }

  private async connect() {
    if (!this.userId) {
      console.error('Cannot connect: userId is not set');
      return;
    }

    const electronAPI = getElectronAPI();
    if (!electronAPI) {
      console.warn('Electron API not available, trying native WebSocket fallback...');
      this.connectNativeWS();
      return;
    }

    try {
      console.log(`Socket connecting via Electron IPC to ${this.host}:${this.port}`);
      const result = await electronAPI.socketConnect({
        host: this.host,
        port: this.port,
        userId: this.userId,
        token: this.token,
        username: this.username
      });

      if (result.success) {
        console.log('Socket connected successfully via IPC');
        this.isConnected = true;
        this.isInitializing = false;
        this.setupListeners();
      } else {
        console.error('Socket connection failed (IPC):', result.error);
        this.isInitializing = false;
      }
    } catch (error) {
      console.error('Socket connection error (IPC):', error);
      this.isInitializing = false;
    }
  }

  /**
   * 浏览器环境下的原声 WebSocket 连接实现
   */
  private connectNativeWS() {
    try {
      // 注意：如果服务器不支持直接 WS，这里可能需要后端配合或使用代理
      // 这里的 8088 通常是 TCP 端口，如果是 Web 环境，后端可能开启了 WS 协议适配
      const wsUrl = `ws://${this.host}:${this.port}`;
      console.log(`Native WebSocket connecting to ${wsUrl}...`);
      
      this.socket = new WebSocket(wsUrl);
      this.socket.binaryType = 'arraybuffer';

      this.socket.onopen = () => {
        console.log('Native WebSocket connected');
        this.isConnected = true;
        this.isInitializing = false;
        this.registerToRemoteWS();
      };

      this.socket.onmessage = (event) => {
        this.handleWSData(new Uint8Array(event.data));
      };

      this.socket.onclose = () => {
        console.log('Native WebSocket closed');
        this.isConnected = false;
        this.isInitializing = false;
        // 自动重连逻辑可以在这里实现
      };

      this.socket.onerror = (error) => {
        console.error('Native WebSocket error:', error);
        this.isInitializing = false;
      };
    } catch (err) {
      console.error('Failed to initiate native WebSocket:', err);
      this.isInitializing = false;
    }
  }

  private async registerToRemoteWS() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    // 这里需要构造 UserInfo 注册包，由于是 Web 端，目前可能依赖后端能够处理 WS 协议
    console.log('Sending registration via WebSocket...');
    // TODO: 实现 Web 端 Protobuf 编码并发送。由于 Renderer 目前主要依赖 IPC 解码，
    // 这里如果要做完整功能，需要在 Renderer 也引入 protobufjs 逻辑。
    // 如果后端支持，也可以发送 JSON 或特定的握手。
  }

  private handleWSData(data: Uint8Array) {
      // 同理，这里需要处理 Varint32 分包和 Protobuf 解码
      console.log('Received binary data via WebSocket, length:', data.length);
  }

  /**
   * 设置IPC事件监听
   */
  private setupListeners() {
    const electronAPI = getElectronAPI();
    if (!electronAPI) return;

    // 监听来自主进程的消息
    if (electronAPI.onSocketMessage) {
      electronAPI.onSocketMessage((data: any) => {
        console.log('Received socket message:', data);
        this.handleMessage(data);
      });
    }

    // 监听连接事件
    if (electronAPI.onSocketConnected) {
      electronAPI.onSocketConnected(() => {
        console.log('Socket connected');
      });
    }

    // 监听断开事件
    if (electronAPI.onSocketDisconnected) {
      electronAPI.onSocketDisconnected(() => {
        console.log('Socket disconnected');
      });
    }

    // 监听错误事件
    if (electronAPI.onSocketError) {
      electronAPI.onSocketError((error: any) => {
        console.error('Socket error:', error);
      });
    }

    // 监听重连事件
    if (electronAPI.onSocketReconnecting) {
      electronAPI.onSocketReconnecting((data: any) => {
        console.log('Socket reconnecting:', data);
      });
    }
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: any) {
    try {
      if (message.type === 'message') {
        const payload = message.payload;
        console.log('Processed socket message:', payload);

        // 如果是聊天消息，分发到 Redux
        if (payload.message && this.store) {
          const chatMsg = payload.message;
          // 将 protobuf 格式转回 MessageDTO 格式
          const messageDto: any = {
            msgId: chatMsg.msgId,
            fromAccountId: chatMsg.fromUser?.userId,
            content: chatMsg.content,
            timestamp: chatMsg.serverTimeStamp || chatMsg.clientTimeStamp || Date.now(),
            conversationId: chatMsg.conversationId,
            type: chatMsg.type === 'TEXT' ? 'TEXT' : chatMsg.type,
            clientMsgId: chatMsg.clientMsgId
          };
          this.store.dispatch(addMessage(messageDto));
        }
      } else if (message.type === 'raw') {
        console.log('Received raw socket data:', message.data);
      }
    } catch (error) {
      console.error('Error handling socket message:', error);
    }
  }

  /**
   * 发送结构化负载
   */
  async sendPayload(payload: any): Promise<boolean> {
    const electronAPI = getElectronAPI();
    if (!electronAPI) {
      console.error('Electron API not available');
      return false;
    }

    try {
      const result = await electronAPI.socketSendMessage(payload);
      return result.success;
    } catch (error) {
      console.error('Error sending payload:', error);
      return false;
    }
  }

  /**
   * 发送消息
   */
  async sendMessage(messageData: Buffer): Promise<boolean> {
    const electronAPI = getElectronAPI();
    if (!electronAPI) {
      console.error('Electron API not available');
      return false;
    }

    try {
      // 将Buffer转换为base64字符串通过IPC传输
      const dataBase64 = messageData.toString('base64');
      const result = await electronAPI.socketSend(dataBase64);

      if (result.success) {
        console.log('Message sent successfully');
        return true;
      } else {
        console.error('Failed to send message:', result.error);
        return false;
      }
    } catch (error) {
      console.error('Error sending message:', error);
      return false;
    }
  }

  /**
   * 检查连接状态
   */
  async isActive(): Promise<boolean> {
    const electronAPI = getElectronAPI();
    if (!electronAPI) {
      return false;
    }

    try {
      const result = await electronAPI.socketIsActive();
      return result.active || false;
    } catch (error) {
      console.error('Error checking socket status:', error);
      return false;
    }
  }

  /**
   * 断开连接
   */
  async disconnect() {
    this.intentionToClose = true;
    this.isConnected = false;
    this.isInitializing = false;

    const electronAPI = getElectronAPI();
    if (electronAPI) {
      try {
        await electronAPI.socketDisconnect();
      } catch (error) {
        console.error('Error disconnecting socket:', error);
      }
    }
  }
}

export const socketService = new SocketService();
