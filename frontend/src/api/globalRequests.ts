import { apiClient } from './client';
import { GlobalRequestLog, GlobalRequestFilterParams } from '../types/globalRequest';
import { PagedResponse } from '../types/application';

export const globalRequestsApi = {
  getGlobalRequests: async (params?: GlobalRequestFilterParams): Promise<PagedResponse<GlobalRequestLog>> => {
    const response = await apiClient.get<PagedResponse<GlobalRequestLog>>('/api/v1/requests', { params });
    return response.data;
  },
};
