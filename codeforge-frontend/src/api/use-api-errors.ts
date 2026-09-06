import { useCallback, useState } from 'react';
import { toApiError } from './api-error';
import { type ApiFieldError } from './types';

/**
 * Splits a failed request into the two things a form needs to show.
 *
 * <p>Field errors land under their inputs; anything else is a single alert
 * above the form. A failure is never both, so the two states are cleared
 * together.
 */
export const useApiErrors = () => {
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [allFieldErrors, setAllFieldErrors] = useState<ApiFieldError[]>([]);
  const [generalError, setGeneralError] = useState<{ code: string; message: string } | null>(null);

  const reset = useCallback(() => {
    setFieldErrors({});
    setAllFieldErrors([]);
    setGeneralError(null);
  }, []);

  const capture = useCallback((error: unknown) => {
    const apiError = toApiError(error);

    if (apiError.hasFieldErrors) {
      setFieldErrors(apiError.fieldErrorsByName());
      setAllFieldErrors(apiError.fieldErrors);
      setGeneralError(null);
      return;
    }

    setFieldErrors({});
    setAllFieldErrors([]);
    setGeneralError({ code: apiError.code, message: apiError.message });
  }, []);

  /**
   * Every code recorded against one field, not just the last.
   *
   * <p>`fieldErrors` keeps one message per input, which is right under a text
   * box. A single field can genuinely fail several ways at once, though — a
   * problem that cannot be published has as many reasons as it has gaps — and
   * showing them one save at a time turns one fix into four round trips.
   */
  const errorsFor = useCallback(
    (field: string): string[] => allFieldErrors.filter((error) => error.field === field).map((error) => error.code),
    [allFieldErrors],
  );

  /**
   * Clears every error whose field matches.
   *
   * <p>What a document-shaped form needs: its inputs are indexed paths
   * (`examples[0].input`) and its edits arrive a whole list at a time, so
   * "the author has touched the examples" has to clear all of theirs at once —
   * otherwise a message stays under a field that has already been fixed.
   */
  const clearMatching = useCallback((matches: (field: string) => boolean) => {
    setAllFieldErrors((current) => current.filter((error) => !matches(error.field)));
    setFieldErrors((current) => Object.fromEntries(Object.entries(current).filter(([field]) => !matches(field))));
  }, []);

  /** Clears one field's error as the user edits it, so the form stops nagging. */
  const clearField = useCallback((field: string) => {
    setAllFieldErrors((current) => current.filter((error) => error.field !== field));
    setFieldErrors((current) => {
      if (!(field in current)) {
        return current;
      }
      const { [field]: _removed, ...rest } = current;
      return rest;
    });
  }, []);

  return { fieldErrors, generalError, capture, reset, clearField, clearMatching, errorsFor };
};
