import { useCallback, useState } from 'react';
import { toApiError } from './api-error';

/**
 * Splits a failed request into the two things a form needs to show.
 *
 * <p>Field errors land under their inputs; anything else is a single alert
 * above the form. A failure is never both, so the two states are cleared
 * together.
 */
export const useApiErrors = () => {
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<{ code: string; message: string } | null>(null);

  const reset = useCallback(() => {
    setFieldErrors({});
    setGeneralError(null);
  }, []);

  const capture = useCallback((error: unknown) => {
    const apiError = toApiError(error);

    if (apiError.hasFieldErrors) {
      setFieldErrors(apiError.fieldErrorsByName());
      setGeneralError(null);
      return;
    }

    setFieldErrors({});
    setGeneralError({ code: apiError.code, message: apiError.message });
  }, []);

  /** Clears one field's error as the user edits it, so the form stops nagging. */
  const clearField = useCallback((field: string) => {
    setFieldErrors((current) => {
      if (!(field in current)) {
        return current;
      }
      const { [field]: _removed, ...rest } = current;
      return rest;
    });
  }, []);

  return { fieldErrors, generalError, capture, reset, clearField };
};
