import { apiClient } from './client';
import { ApiKey, CreateApiKeyRequest, UpdateApiKeyRequest } from '../types/apiKey';

export const apiKeysApi = {
  create: async (applicationId: number, data: CreateApiKeyRequest): Promise<ApiKey> => {
    const response = await apiClient.post<ApiKey>(`/api/v1/applications/${applicationId}/keys`, data);
    return response.data;
  },

  list: async (applicationId: number): Promise<ApiKey[]> => {
    const response = await apiClient.get<ApiKey[]>(`/api/v1/applications/${applicationId}/keys`);
    return response.data;
  },

  update: async (applicationId: number, keyId: number, data: UpdateApiKeyRequest): Promise<ApiKey> => {
    const response = await apiClient.put<ApiKey>(`/api/v1/applications/${applicationId}/keys/${keyId}`, data);
    return response.data;
  },

  revoke: async (applicationId: number, keyId: number): Promise<ApiKey> => {
    const response = await apiClient.post<ApiKey>(`/api/v1/applications/${applicationId}/keys/${keyId}/revoke`);
    return response.data;
  },

  regenerate: async (applicationId: number, keyId: number): Promise<ApiKey> => {
    const response = await apiClient.post<ApiKey>(`/api/v1/applications/${applicationId}/keys/${keyId}/regenerate`);
    return response.data;
  },

  delete: async (applicationId: number, keyId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/applications/${applicationId}/keys/${keyId}`);
  },
};
