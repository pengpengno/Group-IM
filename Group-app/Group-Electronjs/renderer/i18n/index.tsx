import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { MessageKey, messages, SupportedLocale } from './messages';

const LOCALE_STORAGE_KEY = 'group.web.locale';
const DEFAULT_LOCALE: SupportedLocale = 'zh-CN';

type TranslationValues = Record<string, string | number | undefined | null>;

interface I18nContextValue {
  locale: SupportedLocale;
  setLocale: (locale: SupportedLocale) => void;
  t: (key: MessageKey, values?: TranslationValues) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);
const subscribers = new Set<(locale: SupportedLocale) => void>();

function normalizeLocale(locale?: string | null): SupportedLocale {
  if (!locale) {
    return DEFAULT_LOCALE;
  }

  if (locale.toLowerCase().startsWith('zh')) {
    return 'zh-CN';
  }

  return 'en-US';
}

function interpolate(template: string, values?: TranslationValues): string {
  if (!values) {
    return template;
  }

  return template.replace(/\{(\w+)\}/g, (_, key: string) => {
    const value = values[key];
    return value === undefined || value === null ? '' : String(value);
  });
}

function readInitialLocale(): SupportedLocale {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCALE;
  }

  const stored = window.localStorage.getItem(LOCALE_STORAGE_KEY);
  if (stored) {
    return normalizeLocale(stored);
  }

  return normalizeLocale(window.navigator.language);
}

let currentLocale: SupportedLocale = readInitialLocale();

export function getCurrentLocale(): SupportedLocale {
  return currentLocale;
}

export function setCurrentLocale(locale: SupportedLocale): void {
  currentLocale = normalizeLocale(locale);

  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, currentLocale);
  }

  subscribers.forEach((listener) => listener(currentLocale));
}

export function subscribeToLocale(listener: (locale: SupportedLocale) => void): () => void {
  subscribers.add(listener);
  return () => subscribers.delete(listener);
}

export function translate(key: MessageKey, values?: TranslationValues, locale: SupportedLocale = currentLocale): string {
  const dictionary = messages[locale] || messages[DEFAULT_LOCALE];
  const fallback = messages[DEFAULT_LOCALE][key];
  return interpolate(dictionary[key] || fallback || key, values);
}

export function formatDateTime(value: string | number | Date, locale: SupportedLocale = currentLocale): string {
  const date = value instanceof Date ? value : new Date(value);
  return date.toLocaleString(locale);
}

export function formatClockTime(value: string | number | Date, locale: SupportedLocale = currentLocale): string {
  const date = value instanceof Date ? value : new Date(value);
  return date.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
}

export const I18nProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [locale, setLocaleState] = useState<SupportedLocale>(getCurrentLocale());

  useEffect(() => subscribeToLocale(setLocaleState), []);

  const value = useMemo<I18nContextValue>(() => ({
    locale,
    setLocale: setCurrentLocale,
    t: (key, values) => translate(key, values, locale)
  }), [locale]);

  return (
    <I18nContext.Provider value={value}>
      {children}
    </I18nContext.Provider>
  );
};

export function useI18n(): I18nContextValue {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within I18nProvider');
  }
  return context;
}
