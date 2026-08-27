export interface DatabaseHealth {
  status: 'UP' | 'DOWN';
  latencyMs: number;
}

export interface CacheHealth {
  status: 'UP' | 'DOWN';
  latencyMs: number;
}

export interface GatewayHealthSummary {
  totalRequests?: number;
  totalRequestsHandled?: number;
  overallErrorRate?: number;
  errorRate?: number;
  averageLatencyMs?: number;
  avgLatencyMs?: number;
  p95LatencyMs?: number;
}

export interface ApplicationHealthDetail {
  applicationId?: number;
  id?: number;
  name: string;
  baseUrl: string;
  healthStatus: string;
  circuitState: 'CLOSED' | 'OPEN' | 'HALF_OPEN';
  consecutiveFailures: number;
  timeUntilRecoverySeconds: number;
}

export interface SystemHealthResponse {
  controlPlaneStatus: string;
  mysql: DatabaseHealth;
  redis: CacheHealth;
  gateway?: GatewayHealthSummary;
  gatewaySummary?: GatewayHealthSummary;
  targetApplications: ApplicationHealthDetail[];
}

