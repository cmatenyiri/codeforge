import axios from 'axios';
import { toApiError } from './api-error';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * The one HTTP client.
 *
 * <p>`withCredentials` is what makes the browser send (and store) the httpOnly
 * session cookie. Because the token is httpOnly, there is deliberately no
 * Authorization header here — this code cannot read the token, which is exactly
 * the property that keeps an XSS bug from stealing the session.
 */
export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

// Normalise every failure at the boundary, so callers only ever catch ApiError.
apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiError(error)),
);
