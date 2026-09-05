import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/auth-api';
import { type LoginPayload, type RegisterPayload, type UserResponse } from '../api/types';
import { AuthContext, type AuthContextValue } from './auth-context';

/**
 * Holds the session.
 *
 * <p>The token lives in an httpOnly cookie this code cannot read, so "am I
 * signed in?" is answered by asking the server once on boot rather than by
 * inspecting client state. That also means an expired or revoked session is
 * discovered at startup instead of on the user's first real action.
 */
export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [initialising, setInitialising] = useState(true);

  useEffect(() => {
    let cancelled = false;

    authApi
      .me()
      .then((currentUser) => {
        if (!cancelled) {
          setUser(currentUser);
        }
      })
      .catch(() => {
        // No cookie, or it is no longer valid: stay signed out.
      })
      .finally(() => {
        if (!cancelled) {
          setInitialising(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (payload: LoginPayload) => {
    const { user: authenticated } = await authApi.login(payload);
    setUser(authenticated);
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const { user: created } = await authApi.register(payload);
    setUser(created);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      // Drop local state even if the call failed; the cookie may already be gone.
      setUser(null);
    }
  }, []);

  const applyUser = useCallback((updated: UserResponse) => {
    setUser(updated);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      initialising,
      isAuthenticated: user !== null,
      login,
      register,
      logout,
      applyUser,
    }),
    [user, initialising, login, register, logout, applyUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
