export type WorkbenchCardCategory = 'TASK' | 'APPROVAL' | 'ANNOUNCEMENT' | 'SCHEDULE' | 'REPORT';

export interface WorkbenchCardEnvelopeV1 {
  version: 1;
  eventId: string;
  category: WorkbenchCardCategory;
  action: string;
  resourceId: string;
  companyId: number;
  title: string;
  summary?: string | null;
  fallbackText: string;
  status?: string | null;
  occurredAt: string;
  deepLink: string;
}

export interface WorkbenchDeepLinkTarget {
  category: WorkbenchCardCategory;
  resourceId: string;
  companyId: number;
  deepLink: string;
}

export type WorkbenchCardParseResult =
  | { kind: 'valid'; card: WorkbenchCardEnvelopeV1; target: WorkbenchDeepLinkTarget }
  | { kind: 'fallback'; fallbackText: string; reason: 'malformed' | 'unsupported-version' | 'unknown-category' | 'unknown-action' | 'invalid-contract' };

const CATEGORIES = new Set<WorkbenchCardCategory>(['TASK', 'APPROVAL', 'ANNOUNCEMENT', 'SCHEDULE', 'REPORT']);
const CATEGORY_PATH: Record<WorkbenchCardCategory, string> = {
  TASK: 'task',
  APPROVAL: 'approval',
  ANNOUNCEMENT: 'announcement',
  SCHEDULE: 'schedule',
  REPORT: 'report',
};

const KNOWN_ACTIONS: Record<WorkbenchCardCategory, Set<string>> = {
  TASK: new Set(['ASSIGNED', 'COMPLETED', 'REOPENED', 'DUE_SOON', 'STATUS_CHANGED']),
  APPROVAL: new Set(['PENDING', 'APPROVED', 'REJECTED', 'RETURNED']),
  ANNOUNCEMENT: new Set(['PUBLISHED']),
  SCHEDULE: new Set(['CREATED', 'UPDATED', 'REMINDER']),
  REPORT: new Set(['CREATED', 'UPDATED', 'READY']),
};

const ACTION_PATTERN = /^[A-Z][A-Z0-9_]{0,63}$/;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const GENERIC_FALLBACK = '工作台消息暂无法展示，请进入工作台查看。';

const asRecord = (value: unknown): Record<string, unknown> | null => (
  value !== null && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null
);

const fallbackFrom = (record: Record<string, unknown> | null): string => {
  const fallback = record?.fallbackText;
  if (typeof fallback === 'string' && fallback.trim() && fallback.length <= 300) {
    return fallback.trim();
  }
  return GENERIC_FALLBACK;
};

const isText = (value: unknown, max: number, required = true): value is string => {
  if (value == null && !required) return true;
  return typeof value === 'string'
    && (!required || value.trim().length > 0)
    && value.length <= max;
};

export function parseWorkbenchDeepLink(value: string): WorkbenchDeepLinkTarget | null {
  try {
    const url = new URL(value);
    if (url.protocol !== 'group:' || url.hostname !== 'workbench') return null;
    let hasUnknownQueryParam = false;
    url.searchParams.forEach((_paramValue, key) => {
      if (key !== 'companyId') hasUnknownQueryParam = true;
    });
    if (hasUnknownQueryParam) return null;

    const companyRaw = url.searchParams.get('companyId');
    const companyId = companyRaw ? Number(companyRaw) : NaN;
    if (!Number.isSafeInteger(companyId) || companyId <= 0) return null;

    const parts = url.pathname.split('/').filter(Boolean);
    if (parts.length !== 2) return null;
    const [categoryPath, encodedResourceId] = parts;
    const category = (Object.keys(CATEGORY_PATH) as WorkbenchCardCategory[])
      .find((candidate) => CATEGORY_PATH[candidate] === categoryPath);
    if (!category) return null;

    const resourceId = decodeURIComponent(encodedResourceId);
    if (!resourceId.trim() || resourceId.length > 128) return null;

    const canonical = `group://workbench/${CATEGORY_PATH[category]}/${encodeURIComponent(resourceId)}?companyId=${companyId}`;
    if (canonical !== value) return null;

    return { category, resourceId, companyId, deepLink: canonical };
  } catch {
    return null;
  }
}

export function parseWorkbenchCard(content: string): WorkbenchCardParseResult {
  let raw: unknown;
  try {
    raw = JSON.parse(content);
  } catch {
    return { kind: 'fallback', fallbackText: GENERIC_FALLBACK, reason: 'malformed' };
  }

  const record = asRecord(raw);
  const fallbackText = fallbackFrom(record);
  if (!record) return { kind: 'fallback', fallbackText, reason: 'malformed' };

  if (record.version !== 1) {
    return { kind: 'fallback', fallbackText, reason: 'unsupported-version' };
  }

  if (typeof record.category !== 'string' || !CATEGORIES.has(record.category as WorkbenchCardCategory)) {
    return { kind: 'fallback', fallbackText, reason: 'unknown-category' };
  }
  const category = record.category as WorkbenchCardCategory;

  if (typeof record.action !== 'string' || !ACTION_PATTERN.test(record.action) || !KNOWN_ACTIONS[category].has(record.action)) {
    return { kind: 'fallback', fallbackText, reason: 'unknown-action' };
  }

  const companyId = Number(record.companyId);
  const occurredAt = typeof record.occurredAt === 'string' ? Date.parse(record.occurredAt) : NaN;
  const target = typeof record.deepLink === 'string' ? parseWorkbenchDeepLink(record.deepLink) : null;

  const contractValid =
    typeof record.eventId === 'string' && UUID_PATTERN.test(record.eventId)
    && isText(record.resourceId, 128)
    && Number.isSafeInteger(companyId) && companyId > 0
    && isText(record.title, 120)
    && isText(record.fallbackText, 300)
    && isText(record.summary, 300, false)
    && isText(record.status, 32, false)
    && Number.isFinite(occurredAt)
    && target !== null
    && target.category === category
    && target.companyId === companyId
    && target.resourceId === record.resourceId;

  if (!contractValid) {
    return { kind: 'fallback', fallbackText, reason: 'invalid-contract' };
  }

  const card: WorkbenchCardEnvelopeV1 = {
    version: 1,
    eventId: record.eventId as string,
    category,
    action: record.action as string,
    resourceId: record.resourceId as string,
    companyId,
    title: record.title as string,
    summary: typeof record.summary === 'string' ? record.summary : null,
    fallbackText: record.fallbackText as string,
    status: typeof record.status === 'string' ? record.status : null,
    occurredAt: record.occurredAt as string,
    deepLink: record.deepLink as string,
  };

  return { kind: 'valid', card, target: target as WorkbenchDeepLinkTarget };
}

export const WORKBENCH_NAVIGATION_EVENT = 'group:workbench-navigate';
export const PENDING_WORKBENCH_DEEP_LINK_KEY = 'group.workbench.pendingDeepLink';
