import axios, { AxiosError } from 'axios';

export type ApiEnvelope<T> = {
  success: boolean;
  correlationId?: string;
  message?: string;
  data: T;
  timestamp?: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

const storageKey = 'adaptive-gateway.auth';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
});

api.interceptors.request.use((config) => {
  const raw = localStorage.getItem(storageKey);
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as { accessToken?: string };
      if (parsed.accessToken) {
        config.headers.Authorization = `Bearer ${parsed.accessToken}`;
      }
    } catch {
      localStorage.removeItem(storageKey);
    }
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = String(error.config?.url ?? '');
    if (error.response?.status === 401 && !requestUrl.includes('/api/auth/login')) {
      localStorage.removeItem(storageKey);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export async function requestData<T>(
  method: 'get' | 'post' | 'put' | 'delete',
  url: string,
  body?: unknown,
  options?: { signal?: AbortSignal }
): Promise<T> {
  const response = await api.request<ApiEnvelope<T>>({
    method,
    url,
    data: body,
    signal: options?.signal
  });
  return response.data.data;
}

export function apiErrorMessage(error: unknown): string {
  const axiosError = error as AxiosError<ApiEnvelope<unknown> | { message?: string }>;
  const payload = axiosError.response?.data;
  if (payload && 'message' in payload && payload.message) {
    return payload.message;
  }
  return axiosError.message || 'Request failed';
}

export function liveWebSocketUrl(accessToken: string): string {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;
  const url = new URL('/api/live/ws', baseUrl);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.searchParams.set('access_token', accessToken);
  return url.toString();
}
