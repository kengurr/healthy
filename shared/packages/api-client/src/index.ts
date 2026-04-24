import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';

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
