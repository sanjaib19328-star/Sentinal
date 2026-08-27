import { apiClient } from './client';
import {
  ErrorAnalyticsResponse,
  GlobalDashboardResponse,
  TimeSeriesResponse,
  TrafficBreakdown,
} from '../types/analytics';
import { ApiEndpointAnalytics } from '../types/application';

export const getGlobalDashboard = async (): Promise<GlobalDashboardResponse> => {
  const response = await apiClient.get<GlobalDashboardResponse>('/api/v1/dashboard/summary');
  return response.data;
};

export const getApplicationAnalytics = async (
  applicationId: number,
  params?: { from?: string; to?: string }
): Promise<ApiEndpointAnalytics> => {
  const response = await apiClient.get<ApiEndpointAnalytics>(`/api/v1/applications/${applicationId}/analytics`, { params });
  return response.data;
};

export const getTimeSeries = async (
  applicationId: number,
  params?: { from?: string; to?: string; interval?: string }
): Promise<TimeSeriesResponse> => {
  const response = await apiClient.get<TimeSeriesResponse>(`/api/v1/applications/${applicationId}/analytics/timeseries`, { params });
  return response.data;
};

export const getTrafficBreakdown = async (
  applicationId: number,
  params?: { from?: string; to?: string }
): Promise<TrafficBreakdown> => {
  const response = await apiClient.get<TrafficBreakdown>(`/api/v1/applications/${applicationId}/analytics/breakdown`, { params });
  return response.data;
};

export const getErrorAnalytics = async (
  applicationId: number,
  params?: {
    status?: number;
    method?: string;
    endpointId?: number;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }
): Promise<ErrorAnalyticsResponse> => {
  const response = await apiClient.get<ErrorAnalyticsResponse>(`/api/v1/applications/${applicationId}/errors`, { params });
  return response.data;
};
