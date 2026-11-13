import axios, { type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse, ApiErrorBody } from '../types/api';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
export const api = axios.create({ baseURL: `${baseURL.replace(/\/$/, '')}/api/v1`, timeout: 20000, withCredentials: false, headers: { 'Content-Type': 'application/json' } });
let accessToken: string | null = null;
let refreshToken: string | null = null;
let expireSession: (() => void) | null = null;
let refreshFlight: Promise<string | null> | null = null;

export function setSessionTokens(access: string, refresh: string) { accessToken = access; refreshToken = refresh; }
export function clearSessionTokens() { accessToken = null; refreshToken = null; }
export function setSessionExpiryHandler(handler: () => void) { expireSession = handler; }
export function getAccessToken() { return accessToken; }
export function getRefreshToken() { return refreshToken; }

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

type RetryConfig = InternalAxiosRequestConfig & { _retried?: boolean };
api.interceptors.response.use((response) => response, async (error: unknown) => {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return Promise.reject(error);
  const original = error.config as RetryConfig | undefined;
  const url = original?.url ?? '';
  if (error.response?.status !== 401 || !original || original._retried || /\/auth\/(login|register|refresh|logout)/.test(url)) return Promise.reject(error);
  original._retried = true;
  if (!refreshToken) { clearSessionTokens(); expireSession?.(); return Promise.reject(error); }
  if (!refreshFlight) {
    const credential = refreshToken;
    refreshFlight = axios.post<AuthResponse>(`${baseURL.replace(/\/$/, '')}/api/v1/auth/refresh`, { refreshToken: credential }, { timeout: 20000 })
      .then(({ data }) => { setSessionTokens(data.accessToken, data.refreshToken); return data.accessToken; })
      .catch(() => { clearSessionTokens(); expireSession?.(); return null; })
      .finally(() => { refreshFlight = null; });
  }
  const token = await refreshFlight;
  if (!token) return Promise.reject(error);
  original.headers.Authorization = `Bearer ${token}`;
  return api(original);
});

export function apiMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) return error.response?.data?.message || error.message || 'The request could not be completed.';
  return error instanceof Error ? error.message : 'The request could not be completed.';
}
