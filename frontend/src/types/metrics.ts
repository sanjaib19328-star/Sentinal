export type MetricType = 
  | 'REQUEST_COUNT'
  | 'SUCCESS_COUNT'
  | 'ERROR_COUNT'
  | 'AVG_LATENCY'
  | 'HEALTH_CHECK';

export interface MetricItem {
  type: MetricType;
  value: number;
  recordedAt: string;
}

export interface ApplicationMetricsResponse {
  applicationId: number;
  metrics: MetricItem[];
}
