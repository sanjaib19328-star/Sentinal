import { apiClient } from './client';
import { ConsumerKeyAnalytics } from '../types/consumer';

export const consumersApi = {
  getKeyAnalytics: async (applicationId: number, keyId: number, from?: string, to?: string): Promise<ConsumerKeyAnalytics> => {
    const params = new URLSearchParams();
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    const response = await apiClient.get<ConsumerKeyAnalytics>(
      `/api/v1/applications/${applicationId}/keys/${keyId}/analytics?${params.toString()}`
    );
    return response.data;
  },

  getApplicationConsumers: async (applicationId: number): Promise<ConsumerKeyAnalytics[]> => {
    const response = await apiClient.get<ConsumerKeyAnalytics[]>(`/api/v1/applications/${applicationId}/consumers`);
    return response.data;
  },

  getGlobalTopConsumers: async (limit: number = 10): Promise<ConsumerKeyAnalytics[]> => {
    const response = await apiClient.get<ConsumerKeyAnalytics[]>(`/api/v1/analytics/consumers/top?limit=${limit}`);
    return response.data;
  },
};
