import { pushAPI } from './api/apiClient';
import { getElectronAPI, isElectronEnvironment } from './api/electronAPI';
import type { PushEndpointDTO, PushEndpointUpsertRequest } from '../types';

const PUSH_ENDPOINT_ID_KEY = 'pushEndpointId:web';
const PUSH_ENDPOINT_DEVICE_KEY = 'pushEndpointDeviceId:web';

function getOrCreateStableId(storageKey: string): string {
  const existing = localStorage.getItem(storageKey);
  if (existing) {
    return existing;
  }

  const value = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${storageKey}-${Date.now()}`;
  localStorage.setItem(storageKey, value);
  return value;
}

function buildWebCapabilityPayload(enabled: boolean): PushEndpointUpsertRequest {
  const endpointId = getOrCreateStableId(PUSH_ENDPOINT_ID_KEY);
  const deviceId = getOrCreateStableId(PUSH_ENDPOINT_DEVICE_KEY);

  return {
    endpointId,
    platform: 'WEB',
    provider: 'WEB_PUSH',
    deviceId,
    locale: navigator.language || 'en-US',
    appVersion: isElectronEnvironment() ? 'electron-renderer' : 'web-browser',
    enabled
  };
}

async function getNotificationPermission(): Promise<NotificationPermission | 'unsupported'> {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return 'unsupported';
  }

  return Notification.permission;
}

export async function syncCurrentPushEndpoint(): Promise<PushEndpointDTO | null> {
  const permission = await getNotificationPermission();
  if (permission !== 'granted') {
    return null;
  }

  const response = await pushAPI.upsertEndpoint(buildWebCapabilityPayload(true));
  return response.data?.data ?? response.data ?? null;
}

export async function disableCurrentPushEndpoint(): Promise<void> {
  const endpointId = localStorage.getItem(PUSH_ENDPOINT_ID_KEY);
  if (!endpointId) {
    return;
  }

  try {
    await pushAPI.upsertEndpoint(buildWebCapabilityPayload(false));
  } catch (error) {
    console.warn('Failed to disable current push endpoint:', error);
  }
}

export async function requestAndSyncBrowserNotifications(): Promise<PushEndpointDTO | null> {
  if (isElectronEnvironment()) {
    const electronAPI = getElectronAPI();
    const permission = await electronAPI.requestNotificationPermission?.();
    if (permission !== 'granted') {
      return null;
    }
    return syncCurrentPushEndpoint();
  }

  if (typeof window === 'undefined' || !('Notification' in window)) {
    return null;
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    return null;
  }

  return syncCurrentPushEndpoint();
}
