import { apiClient } from './client';
import { GlobalApiEndpoint, GlobalApiFilterParams } from '../types/globalApi';

export const globalApisApi = {
  listGlobalApis: async (params?: GlobalApiFilterParams): Promise<GlobalApiEndpoint[]> => {
    const response = await apiClient.get<GlobalApiEndpoint[]>('/api/v1/apis', { params });
    return response.data;
  },
};
