import axios, { AxiosError } from 'axios';
import { User } from '../types/auth';

const TOKEN_KEY = 'sentinel_jwt_token';
const USER_KEY = 'sentinel_user';

export const getStoredToken = (): string | null => {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
};

export const setStoredToken = (token: string): void => {
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch (e) {
    console.error('Failed to persist token', e);
  }
};

export const removeStoredToken = (): void => {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch (e) {
    console.error('Failed to remove token', e);
  }
};

export const getStoredUser = (): User | null => {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
};

export const setStoredUser = (user: User): void => {
  try {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch (e) {
    console.error('Failed to persist user', e);
  }
};

export const removeStoredUser = (): void => {
  try {
    localStorage.removeItem(USER_KEY);
  } catch (e) {
    console.error('Failed to remove user', e);
  }
};

// Base URL: In dev with proxy, relative path works or VITE_API_BASE_URL
const baseURL = import.meta.env.VITE_API_BASE_URL || '';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request Interceptor: Attach JWT Token
apiClient.interceptors.request.use(
  (config) => {
    const token = getStoredToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle Authentication Expiry & Global Errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Clear token and trigger custom auth event if token expired/invalid
      const currentPath = window.location.pathname;
      if (!currentPath.includes('/login') && !currentPath.includes('/register')) {
        removeStoredToken();
        removeStoredUser();
        window.dispatchEvent(new CustomEvent('sentinel:unauthorized'));
      }
    }
    return Promise.reject(error);
  }
);

export const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined;
    if (data?.message) {
      return data.message;
    }
    if (data?.error) {
      return data.error;
    }
    if (error.response?.status === 401) {
      return 'Your session has expired or authentication failed. Please log in again.';
    }
    if (error.response?.status === 403) {
      return 'Access denied. You do not have permission to view this resource.';
    }
    if (error.response?.status === 404) {
      return 'Application or requested resource not found.';
    }
    if (error.response?.status === 409) {
      return 'A conflicting record with this information already exists.';
    }
    if (error.response?.status === 429) {
      return 'Too many requests. Please wait a moment and try again.';
    }
    if (error.response?.status === 500) {
      return 'Sentinel encountered an internal server error.';
    }
    if (error.code === 'ECONNABORTED' || (error.message && error.message.toLowerCase().includes('timeout'))) {
      return 'Sentinel backend request timed out. Please verify your connection or try again.';
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Unable to connect to Sentinel server. Please verify Sentinel backend is running and reachable.';
    }
    return error.message || 'An unexpected network error occurred.';
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'An unexpected error occurred.';
};
