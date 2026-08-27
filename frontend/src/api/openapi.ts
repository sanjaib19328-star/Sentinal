import { apiClient } from './client';
import { OpenApiImportRequest, OpenApiImportResponse } from '../types/openapi';

export const openApiApi = {
  importSpec: async (applicationId: number, req: OpenApiImportRequest): Promise<OpenApiImportResponse> => {
    const response = await apiClient.post<OpenApiImportResponse>(`/api/v1/applications/${applicationId}/openapi/import`, req);
    return response.data;
  },
};
