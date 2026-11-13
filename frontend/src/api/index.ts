import { api } from './axios';
import type { Account, AuthResponse, Page, Statement, Transaction, User } from '../types/api';

export const authApi = {
  login: (body: { email: string; password: string }) => api.post<AuthResponse>('/auth/login', body),
  register: (body: { email: string; password: string; fullName: string }) => api.post<AuthResponse>('/auth/register', body),
  logout: (refreshToken: string) => api.post('/auth/logout', { refreshToken }),
};
export const accountApi = {
  list: () => api.get<Account[]>('/accounts'), create: () => api.post<Account>('/accounts'),
  one: (id: number) => api.get<Account>(`/accounts/${id}`), balance: (id: number) => api.get(`/accounts/${id}/balance`),
  statement: (id: number, params: Record<string, unknown>) => api.get<Statement>(`/accounts/${id}/statement`, { params }),
};
export const transactionApi = {
  deposit: (body: { accountId: number; amount: number; description?: string }) => api.post<Transaction>('/transactions/deposit', body),
  withdraw: (body: { accountId: number; amount: number; description?: string }) => api.post<Transaction>('/transactions/withdraw', body),
  transfer: (body: { fromAccountId: number; toAccountId: number; amount: number; description?: string }, key: string) => api.post<Transaction>('/transactions/transfer', body, { headers: { 'Idempotency-Key': key } }),
  history: (params: Record<string, unknown>) => api.get<Page<Transaction>>('/transactions/history', { params }),
};
export const adminApi = {
  stats: () => api.get<Record<string, number>>('/admin/statistics'),
  users: (params: Record<string, unknown>) => api.get<Page<User>>('/admin/users', { params }),
  accounts: () => api.get<Account[]>('/admin/accounts'),
  transactions: (params: Record<string, unknown>) => api.get<Page<Transaction>>('/admin/transactions', { params }),
  suspicious: (params: Record<string, unknown>) => api.get<Page<Transaction>>('/admin/transactions/suspicious', { params }),
  setStatus: (id: number, status: Account['status']) => api.patch<Account>(`/admin/accounts/${id}/status`, { status }),
};
