import { createContext } from 'react';
import { type LoginPayload, type RegisterPayload, type UserResponse } from '../api/types';

export type AuthContextValue = {
  user: UserResponse | null;
  /** True while the session cookie is being checked against the server on boot. */
  initialising: boolean;
  isAuthenticated: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  /** Publishes a user the server just returned, so the header reflects profile edits. */
  applyUser: (user: UserResponse) => void;
};

export const AuthContext = createContext<AuthContextValue | null>(null);
