export type AlertRuleType =
  | 'HIGH_ERROR_RATE'
  | 'HIGH_LATENCY'
  | 'API_UNAVAILABLE'
  | 'EXCESSIVE_429';

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export type AlertStatus = 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface AlertRule {
  id: number;
  applicationId: number;
  endpointId: number | null;
  type: AlertRuleType;
  threshold: number;
  windowSeconds: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAlertRuleRequest {
  endpointId?: number | null;
  type: AlertRuleType;
  threshold: number;
  windowSeconds: number;
  enabled?: boolean;
}

export interface UpdateAlertRuleRequest {
  threshold?: number;
  windowSeconds?: number;
  enabled?: boolean;
}

export interface Alert {
  id: number;
  ruleId: number;
  applicationId: number;
  endpointId: number | null;
  status: AlertStatus;
  severity: AlertSeverity;
  message: string;
  triggeredValue: number;
  threshold: number;
  triggeredAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
}
