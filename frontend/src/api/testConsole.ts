import { apiClient } from './client';
import { ApiTestConsoleRequest, ApiTestConsoleResult } from '../types/testConsole';

export const testConsoleApi = {
  executeTest: async (applicationId: number, req: ApiTestConsoleRequest): Promise<ApiTestConsoleResult> => {
    const response = await apiClient.post<ApiTestConsoleResult>(`/api/v1/applications/${applicationId}/apis/test-console`, req);
    return response.data;
  },
};
