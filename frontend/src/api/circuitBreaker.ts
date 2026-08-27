import { apiClient } from './client';
import { CircuitBreakerStatus } from '../types/circuitBreaker';

export const circuitBreakerApi = {
  getStatus: async (applicationId: number): Promise<CircuitBreakerStatus> => {
    const response = await apiClient.get<CircuitBreakerStatus>(`/api/v1/applications/${applicationId}/circuit-breaker`);
    return response.data;
  },
};
