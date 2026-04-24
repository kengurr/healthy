import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type {
  AdminDashboardResponse,
  ProviderVerificationItem,
  AdminBookingResponse,
  CreateServiceRequest,
  UpdateServiceRequest,
  CreatePackageRequest,
  UpdateEscalationStatusRequest,
  AdminEscalationResponse,
} from '@zdravdom/types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export interface AuthToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
}

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: inject JWT from localStorage
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('zdravdom_access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 (redirect to login), 403, errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('zdravdom_access_token');
      localStorage.removeItem('zdravdom_refresh_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;

// Auth helpers
export const authApi = {
  login: (email: string, password: string) =>
    apiClient.post<{ accessToken: string; refreshToken: string; expiresIn: number }>(
      '/auth/login',
      { email, password }
    ),

  refresh: (refreshToken: string) =>
    apiClient.post<{ accessToken: string; refreshToken: string; expiresIn: number }>(
      '/auth/refresh',
      { refreshToken }
    ),

  register: (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone: string;
    dateOfBirth?: string;
  }) => apiClient.post('/auth/register', payload),
};

export function setAuthTokens(tokens: AuthToken) {
  localStorage.setItem('zdravdom_access_token', tokens.accessToken);
  localStorage.setItem('zdravdom_refresh_token', tokens.refreshToken);
}

export function clearAuthTokens() {
  localStorage.removeItem('zdravdom_access_token');
  localStorage.removeItem('zdravdom_refresh_token');
}

export function getAccessToken(): string | null {
  return localStorage.getItem('zdravdom_access_token');
}

// Admin API
export const adminApi = {
  // Dashboard
  getDashboard: () =>
    apiClient.get<AdminDashboardResponse>('/admin/analytics/dashboard'),

  // Providers
  listProviders: (status?: string) =>
    apiClient.get('/admin/providers', { params: { status } }),

  getVerificationQueue: () =>
    apiClient.get<ProviderVerificationItem[]>('/admin/providers/verification-queue'),

  approveProvider: (id: number) =>
    apiClient.put(`/admin/providers/${id}/approve`),

  rejectProvider: (id: number, reason?: string) =>
    apiClient.put(`/admin/providers/${id}/reject`, { reason }),

  // Bookings
  listBookings: (status?: string) =>
    apiClient.get<AdminBookingResponse[]>('/admin/bookings', { params: { status } }),

  getBooking: (id: number) =>
    apiClient.get<AdminBookingResponse>(`/admin/bookings/${id}`),

  assignProvider: (id: number, providerId: number, reason?: string) =>
    apiClient.put(`/admin/bookings/${id}/assign-provider`, { providerId, reason }),

  cancelBooking: (id: number, reason?: string) =>
    apiClient.put(`/admin/bookings/${id}/cancel`, { reason }),

  // CMS - Services
  createService: (data: CreateServiceRequest) =>
    apiClient.post('/admin/services', data),

  updateService: (uuid: string, data: UpdateServiceRequest) =>
    apiClient.put(`/admin/services/${uuid}`, data),

  deleteService: (uuid: string) =>
    apiClient.delete(`/admin/services/${uuid}`),

  // CMS - Packages
  createPackage: (data: CreatePackageRequest) =>
    apiClient.post('/admin/packages', data),

  deletePackage: (id: number) =>
    apiClient.delete(`/admin/packages/${id}`),

  // Escalations
  listEscalations: (status?: string) =>
    apiClient.get<AdminEscalationResponse[]>('/admin/escalations', { params: { status } }),

  getEscalation: (id: number) =>
    apiClient.get<AdminEscalationResponse>(`/admin/escalations/${id}`),

  updateEscalationStatus: (id: number, data: UpdateEscalationStatusRequest) =>
    apiClient.put(`/admin/escalations/${id}/status`, data),

  // Users (backend endpoint not yet built — placeholder)
  listUsers: () => apiClient.get('/admin/users'),
};
