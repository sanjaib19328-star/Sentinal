import { DocumentationStatus } from './openapi';

export type ConnectionMode = 'OBSERVATION';

export type HealthStatus = 'UNKNOWN' | 'HEALTHY' | 'DEGRADED' | 'UNAVAILABLE';

export type UpstreamAuthType = 
  | 'NONE'
  | 'API_KEY_HEADER'
  | 'API_KEY_QUERY'
  | 'BEARER_TOKEN'
  | 'BASIC_AUTH'
  | 'CUSTOM_HEADER';

export interface UpstreamAuthConfigResponse {
  type: UpstreamAuthType;
  enabled: boolean;
  headerName?: string | null;
  queryParamName?: string | null;
  username?: string | null;
  maskedSecret?: string | null;
  configured: boolean;
}

export interface UpstreamAuthConfigRequest {
  type: UpstreamAuthType;
  enabled?: boolean;
  headerName?: string | null;
  queryParamName?: string | null;
  secret?: string | null;
  username?: string | null;
  password?: string | null;
}

export interface Application {
  id: number;
  ownerId: number;
  name: string;
  description: string | null;
  baseUrl: string;
  connectionMode: ConnectionMode;
  active: boolean;
  healthStatus: HealthStatus;
  lastSeenAt: string | null;
  createdAt: string;
  updatedAt: string;
  upstreamAuth?: UpstreamAuthConfigResponse | null;
}

export interface CreateApplicationRequest {
  name: string;
  description?: string;
  baseUrl: string;
  upstreamAuth?: UpstreamAuthConfigRequest | null;
}

export interface UpdateApplicationRequest {
  name?: string;
  description?: string;
  baseUrl?: string;
  active?: boolean;
  connectionMode?: ConnectionMode;
}

export interface ApplicationStatusResponse {
  applicationId: number;
  status: HealthStatus;
  lastSeenAt: string | null;
  connectionMode: ConnectionMode;
}

export interface ConnectionTestResponse {
  applicationId: number;
  reachable: boolean;
  status: HealthStatus;
  latencyMs: number | null;
  message: string;
  checkedAt: string;
}

export interface RequestLog {
  requestId: string;
  method: string;
  path: string;
  statusCode: number;
  latencyMs: number;
  timestamp: string;
  clientIp: string | null;
}

export interface ApiEndpoint {
  id: number;
  applicationId: number;
  method: string;
  normalizedPath: string;
  documentationStatus: DocumentationStatus;
  summary: string | null;
  description: string | null;
  parametersJson: string | null;
  requestBodySchemaJson: string | null;
  responsesJson: string | null;
  deprecated: boolean;
  firstSeenAt: string;
  lastSeenAt: string;
  totalRequests: number;
  errorCount: number;
  avgLatencyMs: number;
  successRate: number;
}

export interface ApiEndpointAnalytics {
  endpointId: number;
  applicationId: number;
  method: string;
  normalizedPath: string;
  totalRequests: number;
  successCount: number;
  errorCount: number;
  successRate: number;
  errorRate: number;
  avgLatencyMs: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  status4xxCount: number;
  status5xxCount: number;
  rateLimitedCount: number;
  firstSeenAt: string;
  lastSeenAt: string;
  recentRequests: RequestLog[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ConnectAndDiscoverRequest {
  applicationName: string;
  sentinelUrl: string;
  apiKey?: string;
}

export interface ConnectAndDiscoverResponse {
  applicationId: number;
  applicationName: string;
  backendUrl: string;
  sentinelGatewayUrl: string;
  apiKey: string;
  healthStatus: HealthStatus;
  backendHealthy: boolean;
  apisDiscoveredCount: number;
  discoveredApis: ApiEndpoint[];
  message: string;
}

export interface BulkApiCheckRequest {
  applicationId: number;
  endpointIds?: number[];
  batchIndex: number;
  batchSize: number;
}

export interface BulkApiEndpointResult {
  endpointId: number;
  method: string;
  path: string;
  status: 'VALID' | 'WARNING' | 'ERROR' | 'REQUIRES_INPUT';
  statusCode?: number;
  latencyMs?: number;
  responseValidity?: string;
  detectedProblems?: string;
  recommendation?: string;
  parametersCount: number;
  hasRequestBody: boolean;
}

export interface BulkApiCheckResponse {
  applicationId: number;
  batchIndex: number;
  batchSize: number;
  totalBatches: number;
  totalEndpoints: number;
  completedCount: number;
  validCount: number;
  warningCount: number;
  errorCount: number;
  lastBatch: boolean;
  results: BulkApiEndpointResult[];
}
