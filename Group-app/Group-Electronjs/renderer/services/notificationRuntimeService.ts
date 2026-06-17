import type { Store } from '@reduxjs/toolkit';
import type { RootState } from '../store';
import { getElectronAPI, isElectronEnvironment } from './api/electronAPI';
import { setActiveConversation } from '../features/chat/chatSlice';

type NavigationDetail =
  | { type: 'chat'; conversationId: number }
  | { type: 'meeting'; roomId: string; autoJoin?: boolean; senderId?: string; senderName?: string; senderAvatar?: string };

type NotificationPayload = {
  eventId?: string;
  eventType?: string;
  title?: string;
  body?: string;
  preview?: string;
  deepLink?: string;
  conversationId?: string | number;
  roomId?: string;
  senderId?: string | number;
  senderName?: string;
  messageId?: string | number;
  clickAction?: string;
  notificationKind?: string;
  actionKind?: string;
  meetingTitle?: string;
};

function isDocumentVisible(): boolean {
  return typeof document !== 'undefined' && document.visibilityState === 'visible' && document.hasFocus();
}

function parseNavigation(payload: NotificationPayload): NavigationDetail | null {
  if (payload.clickAction === 'open_chat' && payload.conversationId != null) {
    const conversationId = Number(payload.conversationId);
    if (!Number.isNaN(conversationId) && conversationId > 0) {
      return { type: 'chat', conversationId };
    }
  }

  if (payload.clickAction === 'open_meeting' && payload.roomId) {
    return {
      type: 'meeting',
      roomId: payload.roomId,
      autoJoin: false,
      senderId: payload.senderId != null ? String(payload.senderId) : undefined,
      senderName: payload.senderName
    };
  }

  if (payload.conversationId != null) {
    const conversationId = Number(payload.conversationId);
    if (!Number.isNaN(conversationId) && conversationId > 0) {
      return { type: 'chat', conversationId };
    }
  }

  if (payload.roomId) {
    return {
      type: 'meeting',
      roomId: payload.roomId,
      autoJoin: false,
      senderId: payload.senderId != null ? String(payload.senderId) : undefined,
      senderName: payload.senderName
    };
  }

  if (payload.deepLink?.startsWith('group://chat/')) {
    const conversationId = Number(payload.deepLink.split('/').pop());
    if (!Number.isNaN(conversationId) && conversationId > 0) {
      return { type: 'chat', conversationId };
    }
  }

  if (payload.deepLink?.startsWith('group://meeting/')) {
    const roomId = payload.deepLink.split('/').pop();
    if (roomId) {
      return {
        type: 'meeting',
        roomId,
        autoJoin: false,
        senderId: payload.senderId != null ? String(payload.senderId) : undefined,
        senderName: payload.senderName
      };
    }
  }

  return null;
}

function emitNavigation(detail: NavigationDetail) {
  window.dispatchEvent(new CustomEvent('group:navigate', { detail }));
}

function focusWindow() {
  if (typeof window !== 'undefined') {
    window.focus();
  }
}

function buildBrowserNotification(payload: NotificationPayload): Notification | null {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return null;
  }

  const notification = new Notification(payload.title || 'Group IM', {
    body: payload.body || payload.preview || 'You have a new notification'
  });

  notification.onclick = () => {
    focusWindow();
    const navigation = parseNavigation(payload);
    if (navigation) {
      emitNavigation(navigation);
    }
    notification.close();
  };

  return notification;
}

function showSystemNotification(payload: NotificationPayload) {
  if (isElectronEnvironment()) {
    const electronAPI = getElectronAPI();
    electronAPI.showNotification(
      payload.title || 'Group IM',
      payload.body || payload.preview || 'You have a new notification',
      payload
    );
    return;
  }

  buildBrowserNotification(payload);
}

class NotificationRuntimeService {
  private notificationClickRegistered = false;

  private log(scope: string, details?: Record<string, unknown>) {
    console.log('[NotificationRuntimeService]', details ? { scope, ...details } : { scope });
  }

  bindElectronNotificationClicks() {
    if (this.notificationClickRegistered || !isElectronEnvironment()) {
      return;
    }

    const electronAPI = getElectronAPI();
    electronAPI.onNotificationClick?.((data: NotificationPayload | null) => {
      if (!data) {
        return;
      }
      focusWindow();
      const navigation = parseNavigation(data);
      if (navigation) {
        emitNavigation(navigation);
      }
    });
    this.notificationClickRegistered = true;
  }

  handleRealtimeNotification(payload: NotificationPayload, store: Store<RootState>) {
    this.bindElectronNotificationClicks();

    const navigation = parseNavigation(payload);
    if (navigation?.type === 'chat' && isDocumentVisible()) {
      store.dispatch(setActiveConversation(navigation.conversationId));
      emitNavigation(navigation);
      return;
    }

    if (!isDocumentVisible()) {
      showSystemNotification(payload);
    }
  }

  handleMeetingInvite(message: any) {
    this.bindElectronNotificationClicks();
    this.log('handle-meeting-invite', {
      roomId: message.roomId,
      fromUser: message.fromUser,
      fromUserName: message.fromUserName,
      visible: isDocumentVisible()
    });

    if (isDocumentVisible()) {
      // Foreground web sessions do not show system notifications. Route the
      // invite into the same in-app navigation flow used by notification clicks
      // so the receiver still lands on the pre-join/invite surface immediately.
      this.log('meeting-invite-foreground-route', {
        roomId: message.roomId,
        fromUser: message.fromUser
      });
      emitNavigation({
        type: 'meeting',
        roomId: message.roomId,
        autoJoin: false,
        senderId: message.fromUser != null ? String(message.fromUser) : undefined,
        senderName: message.fromUserName,
        senderAvatar: message.fromAvatar
      });
      return;
    }

    this.log('meeting-invite-system-notification', {
      roomId: message.roomId,
      fromUser: message.fromUser
    });
    showSystemNotification({
      eventType: 'MEETING_INVITE_CREATED',
      notificationKind: 'meeting_invite',
      clickAction: 'open_meeting',
      actionKind: 'invite',
      title: message.fromUserName || 'Incoming meeting invite',
      body: message.title ? `Invited you to ${message.title}` : 'Tap to open the meeting invite',
      meetingTitle: message.title,
      roomId: message.roomId,
      senderId: message.fromUser,
      senderName: message.fromUserName,
      deepLink: message.roomId ? `group://meeting/${message.roomId}` : undefined
    });
  }
}

export const notificationRuntimeService = new NotificationRuntimeService();
