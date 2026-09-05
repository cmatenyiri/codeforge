import axios from 'axios';
import { type ApiErrorBody, type ApiFieldError } from './types';

/**
 * A failed API call, normalised into one shape.
 *
 * <p>`fieldErrors` is the signal that a failure belongs under the form inputs;
 * when it is empty the failure is a general one and belongs in an alert.
 */
export class ApiError extends Error {
  readonly status: number;

  /** i18n key for the overall failure. */
  readonly code: string;

  readonly fieldErrors: ApiFieldError[];

  constructor(status: number, code: string, message: string, fieldErrors: ApiFieldError[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }

  /** True when the backend rejected specific fields, rather than the request as a whole. */
  get hasFieldErrors(): boolean {
    return this.fieldErrors.length > 0;
  }

  /** Field name → i18n code, ready to hand to a form. */
  fieldErrorsByName(): Record<string, string> {
    return Object.fromEntries(this.fieldErrors.map((error) => [error.field, error.code]));
  }
}

const isApiErrorBody = (value: unknown): value is ApiErrorBody =>
  typeof value === 'object' && value !== null && 'status' in value && 'code' in value;

/** Turns anything axios can throw into an {@link ApiError}. */
export const toApiError = (error: unknown): ApiError => {
  if (error instanceof ApiError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const body: unknown = error.response?.data;

    if (isApiErrorBody(body)) {
      return new ApiError(body.status, body.code, body.message, body.fieldErrors ?? []);
    }

    // No response at all: the server is down, CORS rejected us, or the request
    // was aborted. There is no backend code to translate, so use our own.
    if (error.response === undefined) {
      return new ApiError(0, 'error.network', error.message);
    }

    return new ApiError(error.response.status, 'error.internal', error.message);
  }

  return new ApiError(0, 'error.internal', error instanceof Error ? error.message : String(error));
};
