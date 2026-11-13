import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { User } from '../types/api';
import { authApi } from '../api';
import { clearSessionTokens, getRefreshToken, setSessionExpiryHandler, setSessionTokens } from '../api/axios';

type AuthValue = { user: User | null; ready: boolean; login: (email: string, password: string) => Promise<void>; register: (fullName: string, email: string, password: string) => Promise<void>; logout: () => Promise<void> };
const AuthContext = createContext<AuthValue | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);
  const expire = useCallback(() => setUser(null), []);
  useEffect(() => { setSessionExpiryHandler(expire); setReady(true); return () => setSessionExpiryHandler(() => {}); }, [expire]);
  const login = useCallback(async (email: string, password: string) => { const { data } = await authApi.login({ email, password }); setSessionTokens(data.accessToken, data.refreshToken); setUser(data.user); }, []);
  const register = useCallback(async (fullName: string, email: string, password: string) => { const { data } = await authApi.register({ fullName, email, password }); setSessionTokens(data.accessToken, data.refreshToken); setUser(data.user); }, []);
  const logout = useCallback(async () => {
    const refresh = getRefreshToken();
    try { if (refresh) await authApi.logout(refresh); } finally { clearSessionTokens(); setUser(null); }
  }, []);
  const value = useMemo(() => ({ user, ready, login, register, logout }), [user, ready, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('useAuth must be inside AuthProvider'); return value; }
