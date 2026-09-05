import { apiClient } from './client';
import { type AuthResponse, type LoginPayload, type RegisterPayload, type UserResponse } from './types';

export const authApi = {
  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/register', payload);
    return data;
  },

  async login(payload: LoginPayload): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/login', payload);
    return data;
  },

  /** Clears the session cookie server-side. */
  async logout(): Promise<void> {
    await apiClient.post('/api/auth/logout');
  },

  /**
   * Re-hydrates the session on boot. This is the only way to find out whether
   * the cookie is still valid, since the page cannot read it.
   */
  async me(): Promise<UserResponse> {
    const { data } = await apiClient.get<UserResponse>('/api/users/me');
    return data;
  },
};
