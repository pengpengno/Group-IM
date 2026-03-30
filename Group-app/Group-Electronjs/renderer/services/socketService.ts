import { Store } from '@reduxjs/toolkit';
import { RootState } from '../store';
import { getElectronAPI } from './api/electronAPI';
import { addMessage, fetchConversations, fetchMessages } from '../features/chat/chatSlice';
import { BaseMessagePkg, UserInfo, Heartbeat, AckMessage } from './protoDefinitions';
import Long from 'long';

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
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;
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
    if (!electronAPI || !((window as any).electronAPI)) {
      console.warn('Electron API not available, trying native WebSocket...');
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
        // Even if IPC fails, try polling as a fail-safe
        this.startPollingFallback();
      }
    } catch (error) {
      console.error('Socket connection error (IPC):', error);
      this.isInitializing = false;
      this.startPollingFallback();
    }
  }

  /**
   * 浏览器环境下的轮询(Polling)降级实现
   * 由于现代浏览器无法直接开启 TCP 长连接，使用 HTTP 增量拉取作为实时更新的保底逻辑
   */
  private pollingTimer: NodeJS.Timeout | null = null;
  private startPollingFallback() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }

    console.log('SocketService: Starting HTTP Polling Fallback (5s interval)');
    
    // 立即执行一次
    this.executePollingSync();

    this.pollingTimer = setInterval(() => {
      this.executePollingSync();
    }, 5000); // 5秒轮询一次，兼顾实时性和服务器压力
  }

  private async executePollingSync() {
    if (!this.store || !this.userId) return;

    try {
      // 1. 同步会话列表 (更新未读数和会话列表排序)
      await (this.store.dispatch as any)(fetchConversations(this.userId));
      
      // 2. 如果存在当前活跃会话，执行增量消息拉取
      const activeId = this.store.getState().chat.activeConversationId;
      if (activeId) {
        await (this.store.dispatch as any)(fetchMessages(activeId));
      }
    } catch (err) {
      console.error('SocketService Polling Error:', err);
    }
  }

  /**
   * 浏览器环境下的原生 WebSocket 连接实现
   */
  private connectNativeWS() {
    if (this.socket) {
      this.socket.close();
    }

    try {
      // 统一使用 /ws 路径，后端 SignalWebSocketHandler 已支持二进制协议
      const wsUrl = `ws://${this.host}:${this.port}/ws?userId=${this.userId}`;
      console.log(`Native WebSocket connecting to ${wsUrl}...`);
      
      this.socket = new WebSocket(wsUrl);
      this.socket.binaryType = 'arraybuffer';

      this.socket.onopen = () => {
        console.log('Native WebSocket connected');
        this.isConnected = true;
        this.isInitializing = false;
        
        // 停止掉轮询降级
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
            this.pollingTimer = null;
        }

        this.registerToRemoteWS();
        this.startHeartbeat();
      };

      this.socket.onmessage = (event) => {
        this.handleWSData(new Uint8Array(event.data));
      };

      this.socket.onclose = () => {
        console.log('Native WebSocket closed');
        this.isConnected = false;
        this.isInitializing = false;
        this.stopHeartbeat();
        
        if (!this.intentionToClose) {
          // 3秒后尝试重连
          if (!this.reconnectTimer) {
            this.reconnectTimer = setTimeout(() => {
              this.reconnectTimer = null;
              this.connectNativeWS();
            }, 3000);
          }
          // 重连期间开启轮询保底
          this.startPollingFallback();
        }
      };

      this.socket.onerror = (error) => {
        console.error('Native WebSocket error:', error);
        this.isInitializing = false;
        this.startPollingFallback();
      };
    } catch (err) {
      console.error('Failed to initiate native WebSocket:', err);
      this.isInitializing = false;
      this.startPollingFallback();
    }
  }

  private async registerToRemoteWS() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    console.log('Sending registration via WebSocket...');
    
    try {
        const userInfoPayload = UserInfo.create({
            userId: Long.fromString(this.userId || '0'),
            username: this.username,
            accessToken: this.token,
            platformType: 0 // WEB
        });

        const pkg = BaseMessagePkg.create({
            userInfo: userInfoPayload
        });

        const buffer = BaseMessagePkg.encode(pkg).finish();
        this.socket.send(buffer);
        console.log('Registration sent successfully');
    } catch (err) {
        console.error('Failed to encode/send registration:', err);
    }
  }

  private startHeartbeat() {
      this.stopHeartbeat();
      this.heartbeatTimer = setInterval(() => {
          if (this.socket && this.socket.readyState === WebSocket.OPEN) {
              const pkg = BaseMessagePkg.create({
                  heartbeat: { ping: true }
              });
              const buffer = BaseMessagePkg.encode(pkg).finish();
              this.socket.send(buffer);
          }
      }, 30000); // 30s heartbeat
  }

  private stopHeartbeat() {
      if (this.heartbeatTimer) {
          clearInterval(this.heartbeatTimer);
          this.heartbeatTimer = null;
      }
  }

  private handleWSData(data: Uint8Array) {
      try {
          const pkg = BaseMessagePkg.decode(data) as any;
          console.log('Received WebSocket IM Package:', pkg.payload);
          
          this.handleMessage({
              type: 'message',
              payload: pkg
          });
      } catch (err) {
          console.error('Failed to decode binary WebSocket data:', err);
      }
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
        console.log('Socket connected/reconnected, triggering sync...');
        if (this.store && this.userId) {
          // 重新获取会话列表以更新未读数和最新的 MaxIndex
          (this.store.dispatch as any)(fetchConversations(this.userId));
        }
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
            timestamp: Number(chatMsg.serverTimeStamp || chatMsg.clientTimeStamp || Date.now()),
            conversationId: chatMsg.conversationId,
            type: chatMsg.type === 'TEXT' ? 'TEXT' : chatMsg.type,
            clientMsgId: chatMsg.clientMsgId,
            sequenceId: chatMsg.sequenceId
          };
          this.store.dispatch(addMessage(messageDto));
        }

        // 处理 ACK 消息 (例如：对方已读回执)
        if (payload.ack && this.store) {
          const ackMsg = payload.ack;
          if (ackMsg.status === 4 || ackMsg.status === 'READ') {
            console.log('Received READ receipt:', ackMsg);
            // 这里可以触发 Redux 状态更新，将该会话中小于某 ID 的消息标为已读
            // 由于目前 MessageDTO.status 还没反映到 UI 逻辑中，暂且打印
          }
        }
      } else if (message.type === 'raw') {
        console.log('Received raw socket data:', message.data);
      }
    } catch (error) {
      console.error('Error handling socket message:', error);
    }
  }

  private get isElectron(): boolean {
    return typeof window !== 'undefined' && !!(window as any).electronAPI;
  }

  /**
   * 发送结构化负载
   */
  async sendPayload(payload: any): Promise<boolean> {
    if (this.isElectron) {
      const result = await (window as any).electronAPI.socketSendMessage(payload);
      return result.success;
    } else if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        try {
            // 将普通对象转换为 Protobuf 格式发送
            const pkg = BaseMessagePkg.create(payload);
            const buffer = BaseMessagePkg.encode(pkg).finish();
            this.socket.send(buffer);
            return true;
        } catch (err) {
            console.error('Failed to send Protobuf over WebSocket:', err);
            return false;
        }
    } else {
      console.error('Socket not connected, cannot send payload');
      return false;
    }
  }

  /**
   * 标为已读
   */
  async markAsRead(conversationId: number, lastMsgId: number): Promise<boolean> {
    if (this.isElectron) {
      const result = await (window as any).electronAPI.socketMarkRead({
        conversationId,
        lastMsgId,
        status: 4 // READ
      });
      return result.success;
    } else if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        try {
            const pkg = BaseMessagePkg.create({
                ack: {
                    conversationId: conversationId,
                    msgId: lastMsgId,
                    status: 'READ'
                }
            });
            const buffer = BaseMessagePkg.encode(pkg).finish();
            this.socket.send(buffer);
            return true;
        } catch (err) {
            console.error('Failed to send ACK over WebSocket:', err);
            return false;
        }
    } else {
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

    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }

    this.stopHeartbeat();

    if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
    }

    if (this.socket) {
        this.socket.close();
        this.socket = null;
    }

    const electronAPI = getElectronAPI();
    if (electronAPI && (window as any).electronAPI) {
      try {
        await electronAPI.socketDisconnect();
      } catch (error) {
        console.error('Error disconnecting socket:', error);
      }
    }
  }
}

export const socketService = new SocketService();
