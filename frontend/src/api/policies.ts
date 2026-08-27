import { apiClient } from './client';
import { ApiPolicy, SavePolicyRequest } from '../types/policy';

export const getApplicationPolicy = async (applicationId: number): Promise<ApiPolicy> => {
  const response = await apiClient.get<ApiPolicy>(`/api/v1/applications/${applicationId}/policy`);
  return response.data;
};

export const saveApplicationPolicy = async (
  applicationId: number,
  request: SavePolicyRequest
): Promise<ApiPolicy> => {
  const response = await apiClient.put<ApiPolicy>(`/api/v1/applications/${applicationId}/policy`, request);
  return response.data;
};

export const deleteApplicationPolicy = async (applicationId: number): Promise<void> => {
  await apiClient.delete(`/api/v1/applications/${applicationId}/policy`);
};

export const getEndpointPolicy = async (
  applicationId: number,
  apiId: number
): Promise<ApiPolicy> => {
  const response = await apiClient.get<ApiPolicy>(`/api/v1/applications/${applicationId}/apis/${apiId}/policy`);
  return response.data;
};

export const saveEndpointPolicy = async (
  applicationId: number,
  apiId: number,
  request: SavePolicyRequest
): Promise<ApiPolicy> => {
  const response = await apiClient.put<ApiPolicy>(`/api/v1/applications/${applicationId}/apis/${apiId}/policy`, request);
  return response.data;
};

export const deleteEndpointPolicy = async (
  applicationId: number,
  apiId: number
): Promise<void> => {
  await apiClient.delete(`/api/v1/applications/${applicationId}/apis/${apiId}/policy`);
};
