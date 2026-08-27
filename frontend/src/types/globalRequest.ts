export interface GlobalRequestLog {
  id: number;
  requestId: string;
  applicationId: number;
  applicationName: string;
  apiKeyId?: number | null;
  keyName?: string | null;
  keyMasked?: string | null;
  endpointId?: number | null;
  method: string;
  path: string;
  normalizedPath?: string | null;
  statusCode: number;
  latencyMs: number;
  clientIp?: string | null;
  timestamp: string;
}

export interface GlobalRequestFilterParams {
  applicationId?: number;
  apiKeyId?: number;
  method?: string;
  statusCode?: number;
  statusClass?: string;
  requestId?: string;
  search?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
