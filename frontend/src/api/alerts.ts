import { apiClient } from './client';
import { Alert, AlertRule, CreateAlertRuleRequest, UpdateAlertRuleRequest } from '../types/alert';

export const listAlerts = async (applicationId: number): Promise<Alert[]> => {
  const response = await apiClient.get<Alert[]>(`/api/v1/applications/${applicationId}/alerts`);
  return response.data;
};

export const listAlertRules = async (applicationId: number): Promise<AlertRule[]> => {
  const response = await apiClient.get<AlertRule[]>(`/api/v1/applications/${applicationId}/alert-rules`);
  return response.data;
};

export const createAlertRule = async (
  applicationId: number,
  request: CreateAlertRuleRequest
): Promise<AlertRule> => {
  const response = await apiClient.post<AlertRule>(`/api/v1/applications/${applicationId}/alert-rules`, request);
  return response.data;
};

export const updateAlertRule = async (
  applicationId: number,
  ruleId: number,
  request: UpdateAlertRuleRequest
): Promise<AlertRule> => {
  const response = await apiClient.put<AlertRule>(`/api/v1/applications/${applicationId}/alert-rules/${ruleId}`, request);
  return response.data;
};

export const deleteAlertRule = async (
  applicationId: number,
  ruleId: number
): Promise<void> => {
  await apiClient.delete(`/api/v1/applications/${applicationId}/alert-rules/${ruleId}`);
};

export const acknowledgeAlert = async (alertId: number): Promise<Alert> => {
  const response = await apiClient.post<Alert>(`/api/v1/alerts/${alertId}/acknowledge`);
  return response.data;
};

export const resolveAlert = async (alertId: number): Promise<Alert> => {
  const response = await apiClient.post<Alert>(`/api/v1/alerts/${alertId}/resolve`);
  return response.data;
};
