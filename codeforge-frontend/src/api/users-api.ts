import { apiClient } from './client';
import {
  type ChangeAvatarPayload,
  type ChangePasswordPayload,
  type ChangeUsernamePayload,
  type UserResponse,
  type UserStats,
} from './types';

/**
 * Profile edits. Each returns the updated user so the caller can refresh the
 * session in one round trip instead of re-fetching `/me`.
 */
export const usersApi = {
  /** Solved counts and acceptance rate, for the dashboard and the profile. */
  async stats(): Promise<UserStats> {
    const { data } = await apiClient.get<UserStats>('/api/users/me/stats');
    return data;
  },

  async updateAvatar(payload: ChangeAvatarPayload): Promise<UserResponse> {
    const { data } = await apiClient.patch<UserResponse>('/api/users/me/avatar', payload);
    return data;
  },

  async updateUsername(payload: ChangeUsernamePayload): Promise<UserResponse> {
    const { data } = await apiClient.patch<UserResponse>('/api/users/me/username', payload);
    return data;
  },

  async updatePassword(payload: ChangePasswordPayload): Promise<UserResponse> {
    const { data } = await apiClient.patch<UserResponse>('/api/users/me/password', payload);
    return data;
  },
};
