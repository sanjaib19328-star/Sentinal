import { apiClient } from './client';
import { SystemHealthResponse } from '../types/systemHealth';

export const systemHealthApi = {
  getSystemHealth: async (): Promise<SystemHealthResponse> => {
    const response = await apiClient.get<SystemHealthResponse>('/api/v1/analytics/system/health');
    return response.data;
  },
};
