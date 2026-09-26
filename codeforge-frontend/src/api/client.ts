import axios from 'axios';
import { toApiError } from './api-error';

/**
 * The backend origin, shared with the WebSocket connection in `realtime/`.
 *
 * <p>Unset in development, where the Vite dev server and the backend listen on
 * different ports. Set to an empty string by the Docker build, where nginx
 * serves the app and the API on one origin and every call is relative.
 */
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * The one HTTP client.
 *
 * <p>`withCredentials` is what makes the browser send (and store) the httpOnly
 * session cookie. Because the token is httpOnly, there is deliberately no
 * Authorization header here — this code cannot read the token, which is exactly
 * the property that keeps an XSS bug from stealing the session.
 */
export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

// Normalise every failure at the boundary, so callers only ever catch ApiError.
apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiError(error)),
);
