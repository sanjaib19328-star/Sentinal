export interface GlobalApiEndpoint {
  id: number;
  applicationId: number;
  applicationName: string;
  applicationBaseUrl: string;
  method: string;
  normalizedPath: string;
  documentationStatus: 'DISCOVERED' | 'DOCUMENTED' | 'DOCUMENTED_AND_DISCOVERED';
  summary?: string | null;
  description?: string | null;
  parametersJson?: string | null;
  requestBodySchemaJson?: string | null;
  responsesJson?: string | null;
  deprecated: boolean;
  totalRequests: number;
  errorCount: number;
  errorRate: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
  successRate: number;
  firstSeenAt: string;
  lastSeenAt: string;
}

export interface GlobalApiFilterParams {
  search?: string;
  applicationId?: number;
  method?: string;
  documentationStatus?: string;
  deprecated?: boolean;
  sortBy?: 'requests' | 'latency' | 'p95' | 'errors' | 'lastseen' | 'path';
  sortDir?: 'asc' | 'desc';
}
