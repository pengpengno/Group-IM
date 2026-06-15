import { Store } from '@reduxjs/toolkit';
import { RootState } from '../store';
import { getElectronAPI } from './api/electronAPI';
import { addMessage, fetchConversations, fetchMessages } from '../features/chat/chatSlice';
import { BaseMessagePkg, UserInfo, Heartbeat, AckMessage } from './protoDefinitions';
import { notificationRuntimeService } from './notificationRuntimeService';
import Long from 'long';
import { EventEmitter } from 'events';

class SocketService extends EventEmitter {
  private static readonly WEB_LEADER_LEASE_MS = 10000;
  private static readonly WEB_LEADER_HEARTBEAT_MS = 4000;
  private static readonly MAX_PENDING_PAYLOADS = 200;
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
  private signalingWS: WebSocket | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private connectionCheckInterval: NodeJS.Timeout | null = null;
  private readonly browserTabId = `tab-${Math.random().toString(36).slice(2)}-${Date.now()}`;
  private browserLeaderHeartbeatTimer: NodeJS.Timeout | null = null;
  private browserLeaderElectionTimer: NodeJS.Timeout | null = null;
  private browserLeader = false;
  private browserCoordinationReady = false;
  private connectionState: 'disconnected' | 'connecting' | 'reconnecting' | 'connected' = 'disconnected';
  private pendingPayloads: any[] = [];
  private readonly onStorage = (event: StorageEvent) => {
    if (!this.isBrowserEnvironment() || event.key !== this.getLeaderStorageKey()) {
      return;
    }

    const leader = this.readLeaderRecord();
    if (!leader || leader.tabId === this.browserTabId) {
      this.tryAcquireBrowserLeadership();
      return;
    }

    if (this.browserLeader) {
      this.demoteBrowserLeader();
    }
  };
  private readonly onBeforeUnload = () => {
    this.releaseBrowserLeadership();
  };

  private log(scope: string, details?: Record<string, unknown>) {
    const base = {
      scope,
      userId: this.userId,
      state: this.connectionState,
      isElectron: this.isElectron,
      isConnected: this.isConnected,
      isInitializing: this.isInitializing,
      browserLeader: this.browserLeader
    };
    console.log('[SocketService]', details ? { ...base, ...details } : base);
  }

  public getWebSocket(): WebSocket | null {
    return this.signalingWS;
  }

  public getConnectionState(): 'disconnected' | 'connecting' | 'reconnecting' | 'connected' {
    return this.connectionState;
  }

  public onConnectionStateChange(handler: (state: 'disconnected' | 'connecting' | 'reconnecting' | 'connected') => void): () => void {
    this.on('connection-state', handler);
    return () => this.off('connection-state', handler);
  }

  private setConnectionState(state: 'disconnected' | 'connecting' | 'reconnecting' | 'connected') {
    if (this.connectionState === state) {
      return;
    }

    this.connectionState = state;
    this.emit('connection-state', state);
  }

  private handleWebRTCMessage(message: any) {
    if (message?.type === 'meeting/request') {
      notificationRuntimeService.handleMeetingInvite(message);
    }
    this.emit('webrtc-message', message);
  }

  initialize(store: Store<RootState>, userId: string, host: string = 'localhost', port: number = 8088, token: string = '', username: string = '') {
    if (this.isConnected || this.isInitializing) {
      this.log('initialize-skipped', { host, port });
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
    this.setConnectionState('connecting');
    this.log('initialize', { host, port, username });
    this.connect();
  }

  private async connect() {
    if (!this.userId) {
      console.error('Cannot connect: userId is not set');
      this.setConnectionState('disconnected');
      return;
    }

    const electronAPI = getElectronAPI();
    if (!electronAPI || !((window as any).electronAPI)) {
      this.log('connect-browser-branch', { host: this.host, port: this.port });
      this.connectBrowserRealtime();
      return;
    }

    try {
      this.log('connect-electron-ipc', { host: this.host, port: this.port });
      const result = await electronAPI.socketConnect({
        host: this.host,
        port: this.port,
        userId: this.userId,
        token: this.token,
        username: this.username
      });

      if (result.success) {
        this.log('connect-electron-ipc-success');
        this.isConnected = true;
        this.isInitializing = false;
        this.setConnectionState('connected');
        this.setupListeners();
        this.flushPendingPayloads();
        this.connectSignalingWS();
      } else {
        console.error('[SocketService] connect-electron-ipc-failed', result.error);
        this.isInitializing = false;
        this.setConnectionState('reconnecting');
        // Even if IPC fails, try polling as a fail-safe
        this.startPollingFallback();
      }
    } catch (error) {
      console.error('[SocketService] connect-electron-ipc-error', error);
      this.isInitializing = false;
      this.setConnectionState('reconnecting');
      this.startPollingFallback();
    }
  }

  private isBrowserEnvironment(): boolean {
    return typeof window !== 'undefined' && !(window as any).electronAPI;
  }

  private getLeaderStorageKey(): string {
    return `groupim:websocket:leader:${this.userId || 'anonymous'}`;
  }

  private readLeaderRecord(): { tabId: string; expiresAt: number } | null {
    if (typeof window === 'undefined' || !this.userId) {
      return null;
    }

    try {
      const raw = window.localStorage.getItem(this.getLeaderStorageKey());
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw);
      if (!parsed?.tabId || typeof parsed.expiresAt !== 'number') {
        return null;
      }

      return parsed;
    } catch (error) {
      console.warn('Failed to parse browser leader record:', error);
      return null;
    }
  }

  private writeLeaderRecord() {
    if (typeof window === 'undefined' || !this.userId) {
      return;
    }

    window.localStorage.setItem(
      this.getLeaderStorageKey(),
      JSON.stringify({
        tabId: this.browserTabId,
        expiresAt: Date.now() + SocketService.WEB_LEADER_LEASE_MS
      })
    );
  }

  private connectBrowserRealtime() {
    this.setupBrowserCoordination();
    this.tryAcquireBrowserLeadership();

    if (this.browserLeader) {
      this.log('browser-leader-ready');
      this.isInitializing = true;
      this.connectNativeWS();
      return;
    }

    this.log('browser-follower-polling');
    this.isInitializing = false;
    this.setConnectionState('reconnecting');
    this.startPollingFallback();
  }

  private setupBrowserCoordination() {
    if (!this.isBrowserEnvironment() || this.browserCoordinationReady) {
      return;
    }

    window.addEventListener('storage', this.onStorage);
    window.addEventListener('beforeunload', this.onBeforeUnload);
    this.browserCoordinationReady = true;
  }

  private connectSignalingWS() {
    const protocol = this.host.startsWith('https') ? 'wss' : 'ws';
    const cleanHost = this.host.replace(/^https?:\/\//, '');
    const url = `${protocol}://${cleanHost}:${this.port}/ws?userId=${this.userId}&token=${this.token}`;
    this.log('connect-signaling-ws', { url });

    this.signalingWS = new WebSocket(url);
    this.signalingWS.onopen = () => {
      this.log('signaling-ws-open');
    };
    this.signalingWS.onmessage = (event) => {
      if (typeof event.data === 'string') {
        try {
          const message = JSON.parse(event.data);
          this.handleWebRTCMessage(message);
        } catch (e) {
          console.error('Failed to parse signaling message:', e);
        }
      }
    };
    this.signalingWS.onclose = (event) => {
      this.log('signaling-ws-close', { code: event.code, reason: event.reason });
    };
    this.signalingWS.onerror = (error) => {
      console.error('[SocketService] signaling-ws-error', error);
    };
  }

  private tryAcquireBrowserLeadership() {
    if (!this.isBrowserEnvironment() || !this.userId) {
      return;
    }

    const currentLeader = this.readLeaderRecord();
    const now = Date.now();
    const hasValidLeader = !!currentLeader && currentLeader.expiresAt > now;

    if (!hasValidLeader || currentLeader?.tabId === this.browserTabId) {
      this.browserLeader = true;
      this.writeLeaderRecord();
      this.startBrowserLeaderHeartbeat();
      this.log('browser-leader-acquired', {
        previousLeaderTabId: currentLeader?.tabId,
        leaderExpired: currentLeader ? currentLeader.expiresAt <= now : true
      });

      return;
    }

    this.browserLeader = false;
    this.log('browser-leader-exists', { leaderTabId: currentLeader?.tabId, leaderExpiresAt: currentLeader?.expiresAt });
    this.stopBrowserLeaderHeartbeat();
    this.startBrowserElectionWatch();
  }

  private startBrowserLeaderHeartbeat() {
    this.stopBrowserLeaderHeartbeat();
    this.browserLeaderHeartbeatTimer = setInterval(() => {
      if (this.browserLeader) {
        this.writeLeaderRecord();
      }
    }, SocketService.WEB_LEADER_HEARTBEAT_MS);
  }

  private stopBrowserLeaderHeartbeat() {
    if (this.browserLeaderHeartbeatTimer) {
      clearInterval(this.browserLeaderHeartbeatTimer);
      this.browserLeaderHeartbeatTimer = null;
    }
  }

  private startBrowserElectionWatch() {
    if (this.browserLeaderElectionTimer) {
      return;
    }

    this.browserLeaderElectionTimer = setInterval(() => {
      const leader = this.readLeaderRecord();
      if (!leader || leader.expiresAt <= Date.now()) {
        this.tryAcquireBrowserLeadership();
      }
    }, SocketService.WEB_LEADER_HEARTBEAT_MS);
  }

  private stopBrowserElectionWatch() {
    if (this.browserLeaderElectionTimer) {
      clearInterval(this.browserLeaderElectionTimer);
      this.browserLeaderElectionTimer = null;
    }
  }

  private demoteBrowserLeader() {
    this.log('browser-leader-demoted');
    this.browserLeader = false;
    this.stopBrowserLeaderHeartbeat();
    this.startBrowserElectionWatch();
    this.isConnected = false;
    this.isInitializing = false;
    this.setConnectionState('reconnecting');
    this.stopHeartbeat();

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.socket) {
      const socket = this.socket;
      this.socket = null;
      socket.onopen = null;
      socket.onmessage = null;
      socket.onclose = null;
      socket.onerror = null;
      socket.close();
    }

    this.startPollingFallback();
  }

  private releaseBrowserLeadership() {
    if (!this.isBrowserEnvironment() || !this.browserLeader) {
      return;
    }

    this.log('browser-leader-release');
    const leader = this.readLeaderRecord();
    if (leader?.tabId === this.browserTabId) {
      window.localStorage.removeItem(this.getLeaderStorageKey());
    }

    this.browserLeader = false;
    this.stopBrowserLeaderHeartbeat();
    this.stopBrowserElectionWatch();
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

    this.log('polling-fallback-start');

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
    if (this.isBrowserEnvironment() && !this.browserLeader) {
      this.isInitializing = false;
      this.setConnectionState('reconnecting');
      this.log('connect-native-skipped-not-leader');
      this.startPollingFallback();
      return;
    }

    if (this.socket) {
      this.socket.close();
    }

    try {
      const isSecurePage = typeof window !== 'undefined' && window.location.protocol === 'https:';
      const wsProtocol = isSecurePage ? 'wss' : 'ws';
      const currentHost = typeof window !== 'undefined' ? window.location.host : `${this.host}:${this.port}`;
      // Web 端统一走当前页面同源 /ws，由 devServer/nginx 负责转发
      const wsUrl = `${wsProtocol}://${currentHost}/ws?userId=${this.userId}&token=${this.token}`;
      this.log('connect-native-ws', { wsUrl });

      this.socket = new WebSocket(wsUrl);
      this.socket.binaryType = 'arraybuffer';
      this.signalingWS = this.socket;

      this.socket.onopen = () => {
        this.log('native-ws-open');
        this.isConnected = true;
        this.isInitializing = false;
        this.setConnectionState('connected');
        this.stopBrowserElectionWatch();

        // 停止掉轮询降级
        if (this.pollingTimer) {
          clearInterval(this.pollingTimer);
          this.pollingTimer = null;
        }

        this.registerToRemoteWS();
        this.startHeartbeat();
        this.flushPendingPayloads();
      };

      this.socket.onmessage = (event) => {
        if (typeof event.data === 'string') {
          try {
            const message = JSON.parse(event.data);
            if (message.type && ['offer', 'answer', 'candidate', 'meeting/request', 'meeting/participants', 'meeting/participant-joined', 'meeting/participant-left', 'meeting/reject', 'meeting/join', 'meeting/leave', 'meeting/end', 'heartbeat'].includes(message.type)) {
              this.handleWebRTCMessage(message);
            } else {
              console.log('Unknown JSON message:', message);
            }
          } catch (e) {
            console.error('Failed to parse JSON message:', e);
          }
        } else {
          this.handleWSData(new Uint8Array(event.data));
        }
      };

      this.socket.onclose = (event) => {
        this.log('native-ws-close', { code: event.code, reason: event.reason });
        this.isConnected = false;
        this.isInitializing = false;
        this.setConnectionState(this.intentionToClose ? 'disconnected' : 'reconnecting');
        this.stopHeartbeat();

        if (!this.intentionToClose) {
          if (this.isBrowserEnvironment()) {
            this.tryAcquireBrowserLeadership();
          }
          // 3秒后尝试重连
          if ((!this.isBrowserEnvironment() || this.browserLeader) && !this.reconnectTimer) {
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
        console.error('[SocketService] native-ws-error', error);
        this.isInitializing = false;
        this.setConnectionState('reconnecting');
        this.startPollingFallback();
      };
    } catch (err) {
      console.error('[SocketService] native-ws-init-failed', err);
      this.isInitializing = false;
      this.setConnectionState('reconnecting');
      this.startPollingFallback();
    }
  }

  private async registerToRemoteWS() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    this.log('register-ws-start');

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
      this.log('register-ws-success');
    } catch (err) {
      console.error('[SocketService] register-ws-failed', err);
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
        this.log('heartbeat-sent');
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
      this.log('ws-binary-received', {
        hasMessage: !!pkg.message,
        hasAck: !!pkg.ack,
        hasNotification: !!pkg.notification
      });

      this.handleMessage({
        type: 'message',
        payload: pkg
      });
    } catch (err) {
      console.error('[SocketService] ws-binary-decode-failed', err);
    }
  }

  private enqueuePayload(payload: any): boolean {
    if (this.pendingPayloads.length >= SocketService.MAX_PENDING_PAYLOADS) {
      console.error('Pending realtime payload queue is full');
      return false;
    }

    this.pendingPayloads.push(payload);
    return true;
  }

  private async flushPendingPayloads() {
    if (this.pendingPayloads.length === 0) {
      return;
    }

    this.log('flush-pending-payloads', { count: this.pendingPayloads.length });
    const queuedPayloads = [...this.pendingPayloads];
    this.pendingPayloads = [];

    for (let index = 0; index < queuedPayloads.length; index += 1) {
      const payload = queuedPayloads[index];
      const result = await this.sendPayload(payload, true);

      if (!result.accepted) {
        this.pendingPayloads = queuedPayloads.slice(index);
        break;
      }
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
        this.isConnected = true;
        this.isInitializing = false;
        this.setConnectionState('connected');
        this.flushPendingPayloads();
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
        this.isConnected = false;
        this.setConnectionState('reconnecting');
      });
    }

    // 监听错误事件
    if (electronAPI.onSocketError) {
      electronAPI.onSocketError((error: any) => {
        console.error('Socket error:', error);
        this.setConnectionState('reconnecting');
      });
    }

    // 监听重连事件
    if (electronAPI.onSocketReconnecting) {
      electronAPI.onSocketReconnecting((data: any) => {
        console.log('Socket reconnecting:', data);
        this.setConnectionState('reconnecting');
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

        if (payload.notification && this.store && this.userId) {
          console.log('Received realtime notification package:', payload.notification);
          notificationRuntimeService.handleRealtimeNotification(payload.notification, this.store);
          // 当前 web 端还没有完整的通知状态树，先刷新会话列表兜底，避免实时变更丢失。
          (this.store.dispatch as any)(fetchConversations(this.userId));
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
  async sendPayload(payload: any, skipQueue: boolean = false): Promise<{ accepted: boolean; queued: boolean }> {
    const messageSummary = {
      messageType: payload?.message?.type,
      conversationId: payload?.message?.conversationId,
      clientMsgId: payload?.message?.clientMsgId,
      skipQueue
    };
    if (this.isElectron) {
      try {
        const result = await (window as any).electronAPI.socketSendMessage(payload);
        if (result.success) {
          this.log('send-payload-electron-success', messageSummary);
          return { accepted: true, queued: false };
        }
      } catch (error) {
        console.error('[SocketService] send-payload-electron-failed', error, messageSummary);
      }

      if (!skipQueue && !this.intentionToClose && this.enqueuePayload(payload)) {
        this.setConnectionState('reconnecting');
        this.log('send-payload-electron-queued', messageSummary);
        return { accepted: true, queued: true };
      }

      return { accepted: false, queued: false };
    } else if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      try {
        // 将普通对象转换为 Protobuf 格式发送
        const pkg = BaseMessagePkg.create(payload);
        const buffer = BaseMessagePkg.encode(pkg).finish();
        this.socket.send(buffer);
        this.log('send-payload-browser-success', messageSummary);
        return { accepted: true, queued: false };
      } catch (err) {
        console.error('[SocketService] send-payload-browser-failed', err, messageSummary);
        if (!skipQueue && !this.intentionToClose && this.enqueuePayload(payload)) {
          this.setConnectionState('reconnecting');
          this.log('send-payload-browser-queued-after-error', messageSummary);
          return { accepted: true, queued: true };
        }
        return { accepted: false, queued: false };
      }
    } else {
      if (!skipQueue && !this.intentionToClose && this.enqueuePayload(payload)) {
        this.setConnectionState('reconnecting');
        this.log('send-payload-queued-no-socket', messageSummary);
        return { accepted: true, queued: true };
      }
      console.error('[SocketService] send-payload-no-socket', messageSummary);
      return { accepted: false, queued: false };
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
    if (!electronAPI || !((window as any).electronAPI)) {
      return !!this.socket && this.socket.readyState === WebSocket.OPEN;
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
    this.setConnectionState('disconnected');
    this.pendingPayloads = [];

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

    if (this.signalingWS && this.signalingWS !== this.socket) {
      this.signalingWS.close();
      this.signalingWS = null;
    }

    if (this.isBrowserEnvironment()) {
      this.releaseBrowserLeadership();
      if (this.browserCoordinationReady) {
        window.removeEventListener('storage', this.onStorage);
        window.removeEventListener('beforeunload', this.onBeforeUnload);
        this.browserCoordinationReady = false;
      }
    }

    this.stopBrowserLeaderHeartbeat();
    this.stopBrowserElectionWatch();

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
