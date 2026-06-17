import { Store } from '@reduxjs/toolkit';
import { RootState } from '../store';
import { getElectronAPI } from './api/electronAPI';
import { electronSocketBridge } from './electronSocketBridge';
import { addMessage, confirmMessageDelivery, fetchConversations, fetchMessages } from '../features/chat/chatSlice';
import { BaseMessagePkg, UserInfo, Heartbeat, AckMessage, MessageType, MessagesStatus } from './protoDefinitions';
import { notificationRuntimeService } from './notificationRuntimeService';
import Long from 'long';
import { EventEmitter } from 'events';
import { SIGNALING_HEARTBEAT_TYPE, SIGNALING_MESSAGE_TYPES, type WebrtcMessage } from '../types/webrtc';

class SocketService extends EventEmitter {
  private static readonly SIGNALING_RUNTIME_TYPES = new Set<string>([
    ...Object.values(SIGNALING_MESSAGE_TYPES),
    SIGNALING_HEARTBEAT_TYPE
  ]);
  private static readonly WEB_LEADER_LEASE_MS = 10000;
  private static readonly WEB_LEADER_HEARTBEAT_MS = 4000;
  private static readonly MAX_PENDING_PAYLOADS = 200;
  private static readonly CONNECTION_CHECK_MS = 15000;
  private static readonly BROWSER_REALTIME_CHANNEL_PREFIX = 'groupim:realtime';
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
  private activeBrowserWsUrl: string | null = null;
  private readonly browserTabId = `tab-${Math.random().toString(36).slice(2)}-${Date.now()}`;
  private browserLeaderHeartbeatTimer: NodeJS.Timeout | null = null;
  private browserLeaderElectionTimer: NodeJS.Timeout | null = null;
  private browserRealtimeChannel: BroadcastChannel | null = null;
  private browserLeader = false;
  private browserCoordinationReady = false;
  private connectionState: 'disconnected' | 'connecting' | 'reconnecting' | 'connected' = 'disconnected';
  private pendingPayloads: any[] = [];
  private pendingSignalingMessages: any[] = [];
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
  private readonly onBrowserWindowOnline = () => {
    this.activate('online');
  };
  private readonly onBrowserWindowFocus = () => {
    this.activate('focus');
  };
  private readonly onBrowserVisibilityChange = () => {
    if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
      this.activate('visibility');
    }
  };
  private readonly onBrowserRealtimeMessage = (event: MessageEvent<any>) => {
    if (!this.isBrowserEnvironment()) {
      return;
    }

    const payload = event.data;
    if (!payload || payload.tabId === this.browserTabId) {
      return;
    }

    if (payload.kind === 'socket-payload') {
      this.applyRealtimePayload(payload.payload, { source: 'broadcast' });
      return;
    }

    if (payload.kind === 'sync-conversations' && this.store && this.userId) {
      (this.store.dispatch as any)(fetchConversations(this.userId));
      return;
    }

    if (payload.kind === 'send-payload' && this.browserLeader) {
      void this.sendPayload(payload.payload);
      return;
    }

    if (payload.kind === 'send-signaling' && this.browserLeader) {
      this.sendSignalingMessage(payload.payload);
      return;
    }

    if (payload.kind === 'mark-read' && this.browserLeader) {
      void this.markAsRead(payload.conversationId, payload.lastMsgId);
      return;
    }

    if (payload.kind === 'connection-state' && typeof payload.state === 'string' && !this.browserLeader) {
      this.setConnectionState(payload.state);
    }
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

  private toNumber(value: any): number {
    if (typeof value === 'number') {
      return value;
    }
    if (typeof value === 'string') {
      const parsed = Number(value);
      return Number.isNaN(parsed) ? 0 : parsed;
    }
    if (value && typeof value.toNumber === 'function') {
      return value.toNumber();
    }
    return 0;
  }

  private normalizeMessageType(type: any): string {
    if (typeof type === 'string') {
      return type.toUpperCase();
    }

    if (typeof type === 'number' && (MessageType as any)[type] !== undefined) {
      return String((MessageType as any)[type]);
    }

    return 'TEXT';
  }

  private normalizeAckStatus(status: any): string {
    if (typeof status === 'string') {
      return status.toUpperCase();
    }

    if (typeof status === 'number' && (MessagesStatus as any)[status] !== undefined) {
      return String((MessagesStatus as any)[status]);
    }

    return '';
  }

  public getWebSocket(): WebSocket | null {
    return this.signalingWS;
  }

  public sendSignalingMessage(message: WebrtcMessage): { accepted: boolean; queued: boolean } {
    const socket = this.getActiveSignalingSocket();
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
      this.log('signaling-send-success', { type: message?.type, roomId: message?.roomId, toUser: message?.toUser });
      return { accepted: true, queued: false };
    }

    if (this.canProxyViaBrowserLeader()) {
      this.publishBrowserRealtimeEvent({
        kind: 'send-signaling',
        payload: message
      });
      this.log('signaling-send-proxied', { type: message?.type, roomId: message?.roomId, toUser: message?.toUser });
      return { accepted: true, queued: true };
    }

    if (!this.intentionToClose && this.enqueueSignalingMessage(message)) {
      this.setConnectionState('reconnecting');
      this.log('signaling-send-queued', { type: message?.type, roomId: message?.roomId, toUser: message?.toUser });
      return { accepted: true, queued: true };
    }

    console.error('[SocketService] signaling-send-failed', message);
    return { accepted: false, queued: false };
  }

  public getConnectionState(): 'disconnected' | 'connecting' | 'reconnecting' | 'connected' {
    return this.connectionState;
  }

  public onConnectionStateChange(handler: (state: 'disconnected' | 'connecting' | 'reconnecting' | 'connected') => void): () => void {
    this.on('connection-state', handler);
    return () => this.off('connection-state', handler);
  }

  public activate(reason: 'initialize' | 'focus' | 'visibility' | 'online' | 'health-check' | 'manual' = 'manual') {
    if (this.intentionToClose || !this.userId) {
      return;
    }

    this.log('activate', { reason });

    if (this.isElectron) {
      if (!this.isConnected && !this.isInitializing) {
        this.isInitializing = true;
        this.setConnectionState('connecting');
        void this.connect();
      }
      return;
    }

    this.setupBrowserCoordination();
    this.tryAcquireBrowserLeadership();

    if (this.browserLeader) {
      const socketReady = this.socket?.readyState === WebSocket.OPEN || this.socket?.readyState === WebSocket.CONNECTING;
      if (!socketReady && !this.isInitializing) {
        this.isInitializing = true;
        this.setConnectionState('connecting');
        this.connectNativeWS();
      }
      return;
    }

    if (this.browserRealtimeChannel) {
      this.setConnectionState('connected');
      return;
    }

    if (!this.pollingTimer) {
      this.setConnectionState('reconnecting');
      this.startPollingFallback();
    }
  }

  private setConnectionState(state: 'disconnected' | 'connecting' | 'reconnecting' | 'connected') {
    if (this.connectionState === state) {
      return;
    }

    this.connectionState = state;
    this.emit('connection-state', state);
    if (this.isBrowserEnvironment() && this.browserLeader) {
      this.publishBrowserRealtimeEvent({
        kind: 'connection-state',
        state
      });
    }
  }

  private handleSignalingRuntimeMessage(message: WebrtcMessage) {
    this.log('signaling-runtime-message', {
      type: message.type,
      roomId: message.roomId,
      fromUser: message.fromUser,
      toUser: message.toUser
    });
    this.emit('signaling-message', message);
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
    this.startConnectionMonitor();
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
      const result = await electronSocketBridge.connect({
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

    if (this.browserRealtimeChannel) {
      this.log('browser-follower-broadcast');
      this.isInitializing = false;
      this.setConnectionState('connected');
      return;
    }

    this.log('browser-follower-polling');
    this.isInitializing = false;
    this.setConnectionState('reconnecting');
    if (this.browserRealtimeChannel) {
      this.setConnectionState('connected');
      return;
    }

    this.startPollingFallback();
  }

  private setupBrowserCoordination() {
    if (!this.isBrowserEnvironment() || this.browserCoordinationReady) {
      return;
    }

    window.addEventListener('storage', this.onStorage);
    window.addEventListener('beforeunload', this.onBeforeUnload);
    window.addEventListener('online', this.onBrowserWindowOnline);
    window.addEventListener('focus', this.onBrowserWindowFocus);
    document.addEventListener('visibilitychange', this.onBrowserVisibilityChange);
    if (!this.browserRealtimeChannel && this.supportsBrowserBroadcast()) {
      this.browserRealtimeChannel = new BroadcastChannel(this.getBrowserRealtimeChannelName());
      this.browserRealtimeChannel.onmessage = this.onBrowserRealtimeMessage;
    }
    this.browserCoordinationReady = true;
  }

  private supportsBrowserBroadcast(): boolean {
    return typeof BroadcastChannel !== 'undefined';
  }

  private getBrowserRealtimeChannelName(): string {
    return `${SocketService.BROWSER_REALTIME_CHANNEL_PREFIX}:${this.userId || 'anonymous'}`;
  }

  private publishBrowserRealtimeEvent(payload: any) {
    if (!this.isBrowserEnvironment() || !this.browserRealtimeChannel) {
      return;
    }

    this.browserRealtimeChannel.postMessage({
      ...payload,
      tabId: this.browserTabId
    });
  }

  private canProxyViaBrowserLeader(): boolean {
    return this.isBrowserEnvironment() && !this.browserLeader && !!this.browserRealtimeChannel;
  }

  private startConnectionMonitor() {
    if (this.connectionCheckInterval) {
      clearInterval(this.connectionCheckInterval);
    }

    this.connectionCheckInterval = setInterval(() => {
      this.activate('health-check');
    }, SocketService.CONNECTION_CHECK_MS);
  }

  private connectSignalingWS() {
    const protocol = this.host.startsWith('https') ? 'wss' : 'ws';
    const cleanHost = this.host.replace(/^https?:\/\//, '');
    const url = `${protocol}://${cleanHost}:${this.port}/ws?userId=${this.userId}&token=${this.token}`;
    this.log('connect-signaling-ws', { url });

    this.signalingWS = new WebSocket(url);
    this.signalingWS.onopen = () => {
      this.log('signaling-ws-open');
      this.flushPendingSignalingMessages();
    };
    this.signalingWS.onmessage = (event) => {
      if (typeof event.data === 'string') {
        try {
          const message = JSON.parse(event.data) as WebrtcMessage;
          this.log('signaling-ws-message', {
            type: message.type,
            roomId: message.roomId,
            fromUser: message.fromUser,
            toUser: message.toUser
          });
          this.handleSignalingRuntimeMessage(message);
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

  private ensureConversationState(conversationId?: number) {
    if (!this.store || !this.userId || !conversationId) {
      return;
    }

    const exists = this.store
      .getState()
      .chat
      .conversations
      .some((item) => item.conversation.conversationId === conversationId);

    if (!exists) {
      (this.store.dispatch as any)(fetchConversations(this.userId));
    }
  }

  private applyRealtimePayload(payload: any, options?: { source?: 'socket' | 'broadcast' }) {
    console.log('Processed socket message:', payload, options);

    if (payload.message && this.store) {
      const chatMsg = payload.message;
      const messageDto: any = {
        msgId: this.toNumber(chatMsg.msgId),
        fromAccountId: this.toNumber(chatMsg.fromUser?.userId),
        content: chatMsg.content,
        timestamp: this.toNumber(chatMsg.serverTimeStamp || chatMsg.clientTimeStamp || Date.now()),
        conversationId: this.toNumber(chatMsg.conversationId),
        type: this.normalizeMessageType(chatMsg.type),
        clientMsgId: chatMsg.clientMsgId,
        sequenceId: this.toNumber(chatMsg.sequenceId)
      };
      this.store.dispatch(addMessage(messageDto));
      this.ensureConversationState(this.toNumber(chatMsg.conversationId));
    }

    if (payload.ack && this.store) {
      const ackMsg = payload.ack;
      const normalizedStatus = this.normalizeAckStatus(ackMsg.status);
      if (ackMsg.clientMsgId && this.toNumber(ackMsg.conversationId) > 0) {
        this.store.dispatch(confirmMessageDelivery({
          conversationId: this.toNumber(ackMsg.conversationId),
          clientMsgId: ackMsg.clientMsgId,
          msgId: this.toNumber(ackMsg.serverMsgId),
          timestamp: this.toNumber(ackMsg.ackTimestamp) || Date.now(),
          status: normalizedStatus === 'FAILED' ? 'failed' : 'success'
        }));
      }

      if (ackMsg.status === 4 || normalizedStatus === 'READ') {
        console.log('Received READ receipt:', ackMsg);
      }
    }

    if (payload.notification && this.store && this.userId) {
      console.log('Received realtime notification package:', payload.notification);
      notificationRuntimeService.handleRealtimeNotification(payload.notification, this.store);
      (this.store.dispatch as any)(fetchConversations(this.userId));
      if (options?.source === 'socket' && this.browserLeader) {
        this.publishBrowserRealtimeEvent({
          kind: 'sync-conversations'
        });
      }
    }

    if (options?.source === 'socket' && this.browserLeader) {
      this.publishBrowserRealtimeEvent({
        kind: 'socket-payload',
        payload
      });
    }
  }

  /**
   * 浏览器环境下的原生 WebSocket 连接实现
   */
  private connectNativeWS() {
    if (this.isBrowserEnvironment() && !this.browserLeader) {
      this.isInitializing = false;
      this.log('connect-native-skipped-not-leader');
      if (this.browserRealtimeChannel) {
        this.setConnectionState('connected');
        return;
      }
      this.setConnectionState('reconnecting');
      this.startPollingFallback();
      return;
    }

    try {
      const isSecurePage = typeof window !== 'undefined' && window.location.protocol === 'https:';
      const wsProtocol = isSecurePage ? 'wss' : 'ws';
      const currentHost = typeof window !== 'undefined' ? window.location.host : `${this.host}:${this.port}`;
      // Web 端统一走当前页面同源 /ws，由 devServer/nginx 负责转发
      const wsUrl = `${wsProtocol}://${currentHost}/ws?userId=${this.userId}&token=${this.token}`;
      if (
        this.socket &&
        this.activeBrowserWsUrl === wsUrl &&
        (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)
      ) {
        this.log('connect-native-ws-skipped-existing-socket', {
          wsUrl,
          readyState: this.socket.readyState
        });
        return;
      }

      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }

      if (this.socket && this.socket.readyState !== WebSocket.CLOSED) {
        this.log('connect-native-ws-replacing-stale-socket', {
          previousReadyState: this.socket.readyState
        });
        this.socket.close();
      }

      this.log('connect-native-ws', { wsUrl });

      const nextSocket = new WebSocket(wsUrl);
      this.socket = nextSocket;
      this.signalingWS = nextSocket;
      this.activeBrowserWsUrl = wsUrl;
      nextSocket.binaryType = 'arraybuffer';

      nextSocket.onopen = () => {
        if (this.socket !== nextSocket) {
          this.log('native-ws-open-stale-socket-ignored', { wsUrl });
          nextSocket.close();
          return;
        }

        this.log('native-ws-open');
        this.isConnected = true;
        this.isInitializing = false;
        this.setConnectionState('connected');
        this.stopBrowserElectionWatch();

        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }

        // 停止掉轮询降级
        if (this.pollingTimer) {
          clearInterval(this.pollingTimer);
          this.pollingTimer = null;
        }

        this.registerToRemoteWS();
        this.startHeartbeat();
        this.flushPendingPayloads();
        this.flushPendingSignalingMessages();
      };

      nextSocket.onmessage = (event) => {
        if (this.socket !== nextSocket) {
          return;
        }

        if (typeof event.data === 'string') {
          try {
            const message = JSON.parse(event.data) as WebrtcMessage;
            if (message.type && SocketService.SIGNALING_RUNTIME_TYPES.has(message.type)) {
              this.log('native-ws-signaling-message', {
                type: message.type,
                roomId: message.roomId,
                fromUser: message.fromUser,
                toUser: message.toUser
              });
              this.handleSignalingRuntimeMessage(message);
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

      nextSocket.onclose = (event) => {
        const isCurrentSocket = this.socket === nextSocket;
        this.log('native-ws-close', { code: event.code, reason: event.reason, wasCurrentSocket: isCurrentSocket });
        if (!isCurrentSocket) {
          return;
        }

        this.isConnected = false;
        this.isInitializing = false;
        this.socket = null;
        this.signalingWS = null;
        this.activeBrowserWsUrl = null;
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

      nextSocket.onerror = (error) => {
        if (this.socket !== nextSocket) {
          return;
        }

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

  private getActiveSignalingSocket(): WebSocket | null {
    if (this.signalingWS?.readyState === WebSocket.OPEN) {
      return this.signalingWS;
    }

    if (this.socket?.readyState === WebSocket.OPEN) {
      return this.socket;
    }

    return null;
  }

  private enqueueSignalingMessage(message: any): boolean {
    if (this.pendingSignalingMessages.length >= SocketService.MAX_PENDING_PAYLOADS) {
      console.error('Pending signaling message queue is full');
      return false;
    }

    this.pendingSignalingMessages.push(message);
    return true;
  }

  private flushPendingSignalingMessages() {
    const socket = this.getActiveSignalingSocket();
    if (!socket || this.pendingSignalingMessages.length === 0) {
      return;
    }

    const queuedMessages = [...this.pendingSignalingMessages];
    this.pendingSignalingMessages = [];
    this.log('flush-pending-signaling-messages', { count: queuedMessages.length });

    queuedMessages.forEach((message) => {
      socket.send(JSON.stringify(message));
    });
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
    // Support both the new namespaced socket bridge and older flat preload APIs
    // so renderer services can migrate without requiring lockstep upgrades.
    if (electronAPI.onSocketMessage || electronAPI.socket?.onMessage) {
      electronSocketBridge.onMessage((data: any) => {
        console.log('Received socket message:', data);
        this.handleMessage(data);
      });
    }

    // 监听连接事件
    if (electronAPI.onSocketConnected || electronAPI.socket?.onConnected) {
      electronSocketBridge.onConnected(() => {
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
    if (electronAPI.onSocketDisconnected || electronAPI.socket?.onDisconnected) {
      electronSocketBridge.onDisconnected(() => {
        console.log('Socket disconnected');
        this.isConnected = false;
        this.setConnectionState('reconnecting');
      });
    }

    // 监听错误事件
    if (electronAPI.onSocketError || electronAPI.socket?.onError) {
      electronSocketBridge.onError((error: any) => {
        console.error('Socket error:', error);
        this.setConnectionState('reconnecting');
      });
    }

    // 监听重连事件
    if (electronAPI.onSocketReconnecting || electronAPI.socket?.onReconnecting) {
      electronSocketBridge.onReconnecting((data: any) => {
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
        this.applyRealtimePayload(message.payload, { source: 'socket' });
        return;
        /*

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
          // Re-sync conversations after reconnect so unread counters and ordering
          // recover even if we missed updates during the reconnect window.
          (this.store.dispatch as any)(fetchConversations(this.userId));
        }
        */
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
        const result = await electronSocketBridge.sendMessage(payload);
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
    } else if (this.canProxyViaBrowserLeader()) {
      this.publishBrowserRealtimeEvent({
        kind: 'send-payload',
        payload
      });
      this.activate('manual');
      this.log('send-payload-browser-proxied', messageSummary);
      return { accepted: true, queued: true };
    } else {
      if (!skipQueue && !this.intentionToClose && this.enqueuePayload(payload)) {
        this.setConnectionState('reconnecting');
        this.activate('manual');
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
      const result = await electronSocketBridge.markRead({
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
            serverMsgId: lastMsgId,
            status: MessagesStatus.READ
          }
        });
        const buffer = BaseMessagePkg.encode(pkg).finish();
        this.socket.send(buffer);
        return true;
      } catch (err) {
        console.error('Failed to send ACK over WebSocket:', err);
        return false;
      }
    } else if (this.canProxyViaBrowserLeader()) {
      this.publishBrowserRealtimeEvent({
        kind: 'mark-read',
        conversationId,
        lastMsgId
      });
      this.activate('manual');
      return true;
    } else {
      this.activate('manual');
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
      const result = await electronSocketBridge.send(dataBase64);

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
      const result = await electronSocketBridge.isActive();
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
    this.pendingSignalingMessages = [];

    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }

    if (this.connectionCheckInterval) {
      clearInterval(this.connectionCheckInterval);
      this.connectionCheckInterval = null;
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
    this.activeBrowserWsUrl = null;

    if (this.signalingWS && this.signalingWS !== this.socket) {
      this.signalingWS.close();
      this.signalingWS = null;
    }

    if (this.isBrowserEnvironment()) {
      this.releaseBrowserLeadership();
      if (this.browserCoordinationReady) {
        window.removeEventListener('storage', this.onStorage);
        window.removeEventListener('beforeunload', this.onBeforeUnload);
        window.removeEventListener('online', this.onBrowserWindowOnline);
        window.removeEventListener('focus', this.onBrowserWindowFocus);
        document.removeEventListener('visibilitychange', this.onBrowserVisibilityChange);
        this.browserCoordinationReady = false;
      }
      if (this.browserRealtimeChannel) {
        this.browserRealtimeChannel.close();
        this.browserRealtimeChannel = null;
      }
    }

    this.stopBrowserLeaderHeartbeat();
    this.stopBrowserElectionWatch();

    const electronAPI = getElectronAPI();
    if (electronAPI && (window as any).electronAPI) {
      try {
        await electronSocketBridge.disconnect();
      } catch (error) {
        console.error('Error disconnecting socket:', error);
      }
    }
  }
}

export const socketService = new SocketService();
