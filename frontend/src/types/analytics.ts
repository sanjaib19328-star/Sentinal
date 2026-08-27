import { Alert } from './alert';
import { RequestLog } from './application';

export interface TimeSeriesPoint {
  timestamp: string;
  totalRequests: number;
  successRequests: number;
  errorRequests: number;
  rateLimitedRequests: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
}

export interface TimeSeriesResponse {
  applicationId: number;
  interval: string;
  points: TimeSeriesPoint[];
}

export interface EndpointRank {
  endpointId: number | null;
  method: string;
  normalizedPath: string;
  count: number;
  metricValue: number;
}

export interface TrafficBreakdown {
  applicationId: number;
  methodCounts: Record<string, number>;
  statusClassCounts: Record<string, number>;
  topApis: EndpointRank[];
  slowestApis: EndpointRank[];
  errorProneApis: EndpointRank[];
  rateLimitedApis: EndpointRank[];
}

export interface ErrorSummary {
  key: string;
  count: number;
  percentage: number;
}

export interface ErrorAnalyticsResponse {
  applicationId: number;
  totalErrors: number;
  errorRate: number;
  errorByStatusCode: ErrorSummary[];
  errorByEndpoint: ErrorSummary[];
  errorLogs: {
    content: RequestLog[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface ApplicationSummary {
  id: number;
  name: string;
  healthStatus: string;
  totalRequests: number;
  errorRate: number;
  avgLatencyMs: number;
}

export interface GlobalDashboardResponse {
  totalApplications: number;
  healthyApplications: number;
  degradedApplications: number;
  downApplications: number;
  totalRequests: number;
  requestsPerMinute: number;
  overallSuccessRate: number;
  overallErrorRate: number;
  overall429Rate: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
  applicationSummaries: ApplicationSummary[];
  topApis: EndpointRank[];
  activeAlerts: Alert[];
  recentErrors: RequestLog[];
}
