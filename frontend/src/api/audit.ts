import { apiClient } from './client';
import { AuditAction, AuditLog } from '../types/audit';
import { PagedResponse } from '../types/application';

export const getAuditLogs = async (
  params?: {
    applicationId?: number;
    action?: AuditAction;
    page?: number;
    size?: number;
  }
): Promise<PagedResponse<AuditLog>> => {
  const response = await apiClient.get<PagedResponse<AuditLog>>('/api/v1/audit-logs', { params });
  return response.data;
};
