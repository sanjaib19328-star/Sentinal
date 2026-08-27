export interface TopEndpointMetric {
  method: string;
  normalizedPath: string;
  requestCount: number;
  avgLatencyMs: number;
}

export interface ConsumerKeyAnalytics {
  apiKeyId: number;
  keyName: string;
  keyPrefix: string;
  totalRequests: number;
  successRequests: number;
  errorRequests: number;
  count4xx: number;
  count5xx: number;
  count429: number;
  errorRate: number;
  avgLatencyMs: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  lastUsedAt: string | null;
  createdAt: string;
  topEndpoints: TopEndpointMetric[];
}
