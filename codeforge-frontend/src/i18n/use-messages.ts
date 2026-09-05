import { type ParseKeys } from 'i18next';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

/**
 * Translates a code that only exists at runtime — every `code` the backend
 * sends in an error envelope.
 *
 * <p>Static `t('…')` calls stay fully key-checked; this is the one place that
 * takes an arbitrary string, and it degrades to the server's English text when
 * a backend code has no translation yet, so a new code is still readable.
 */
export const useMessages = () => {
  const { t, i18n } = useTranslation();

  return useCallback(
    (code: string, fallback?: string): string => (i18n.exists(code) ? t(code as ParseKeys) : (fallback ?? code)),
    [t, i18n],
  );
};
