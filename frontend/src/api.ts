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

export async function requestData<T>(method: 'get' | 'post' | 'put' | 'delete', url: string, body?: unknown): Promise<T> {
  const response = await api.request<ApiEnvelope<T>>({
    method,
    url,
    data: body
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
