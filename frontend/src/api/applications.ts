import { apiClient } from './client';
import {
  Application,
  ApplicationStatusResponse,
  ConnectionTestResponse,
  CreateApplicationRequest,
  PagedResponse,
  RequestLog,
  UpdateApplicationRequest,
  ApiEndpoint,
  ApiEndpointAnalytics,
} from '../types/application';
import { ApplicationMetricsResponse, MetricType } from '../types/metrics';

export const applicationsApi = {
  list: async (): Promise<Application[]> => {
    const response = await apiClient.get<Application[]>('/api/v1/applications');
    return response.data;
  },

  getById: async (id: number): Promise<Application> => {
    const response = await apiClient.get<Application>(`/api/v1/applications/${id}`);
    return response.data;
  },

  create: async (data: CreateApplicationRequest): Promise<Application> => {
    const response = await apiClient.post<Application>('/api/v1/applications', data);
    return response.data;
  },

  update: async (id: number, data: UpdateApplicationRequest): Promise<Application> => {
    const response = await apiClient.put<Application>(`/api/v1/applications/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/applications/${id}`);
  },

  getStatus: async (id: number): Promise<ApplicationStatusResponse> => {
    const response = await apiClient.get<ApplicationStatusResponse>(`/api/v1/applications/${id}/status`);
    return response.data;
  },

  testConnection: async (id: number): Promise<ConnectionTestResponse> => {
    const response = await apiClient.post<ConnectionTestResponse>(`/api/v1/applications/${id}/connection-test`);
    return response.data;
  },

  getRequests: async (
    id: number,
    page: number = 0,
    size: number = 20
  ): Promise<PagedResponse<RequestLog>> => {
    const response = await apiClient.get<PagedResponse<RequestLog>>(`/api/v1/applications/${id}/requests`, {
      params: { page, size },
    });
    return response.data;
  },

  getMetrics: async (
    id: number,
    params?: {
      from?: string;
      to?: string;
      metric?: MetricType;
      limit?: number;
    }
  ): Promise<ApplicationMetricsResponse> => {
    const response = await apiClient.get<ApplicationMetricsResponse>(`/api/v1/applications/${id}/metrics`, {
      params,
    });
    return response.data;
  },

  getApis: async (id: number): Promise<ApiEndpoint[]> => {
    const response = await apiClient.get<ApiEndpoint[]>(`/api/v1/applications/${id}/apis`);
    return response.data;
  },

  getApiAnalytics: async (
    id: number,
    apiId: number,
    params?: { from?: string; to?: string }
  ): Promise<ApiEndpointAnalytics> => {
    const response = await apiClient.get<ApiEndpointAnalytics>(`/api/v1/applications/${id}/apis/${apiId}`, {
      params,
    });
    return response.data;
  },

  getApiRequests: async (
    id: number,
    apiId: number,
    page: number = 0,
    size: number = 20
  ): Promise<PagedResponse<RequestLog>> => {
    const response = await apiClient.get<PagedResponse<RequestLog>>(`/api/v1/applications/${id}/apis/${apiId}/requests`, {
      params: { page, size },
    });
    return response.data;
  },

  updateUpstreamAuth: async (id: number, config: import('../types/application').UpstreamAuthConfigRequest): Promise<import('../types/application').UpstreamAuthConfigResponse> => {
    const response = await apiClient.put<import('../types/application').UpstreamAuthConfigResponse>(`/api/v1/applications/${id}/upstream-auth`, config);
    return response.data;
  },

  deleteUpstreamAuth: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/applications/${id}/upstream-auth`);
  },

  connectAndDiscover: async (data: import('../types/application').ConnectAndDiscoverRequest): Promise<import('../types/application').ConnectAndDiscoverResponse> => {
    const response = await apiClient.post<import('../types/application').ConnectAndDiscoverResponse>('/api/v1/applications/connect-and-discover', data);
    return response.data;
  },

  bulkApiCheck: async (id: number, data: import('../types/application').BulkApiCheckRequest): Promise<import('../types/application').BulkApiCheckResponse> => {
    const response = await apiClient.post<import('../types/application').BulkApiCheckResponse>(`/api/v1/applications/${id}/bulk-api-check`, data);
    return response.data;
  },
};
