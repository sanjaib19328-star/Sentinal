export interface ApiPolicy {
  id: number;
  applicationId: number;
  endpointId: number | null;
  enabled: boolean;
  rateLimit: number;
  rateLimitWindowSeconds: number;
  quotaLimit: number | null;
  quotaWindowSeconds: number | null;
  timeoutMs: number;
  maxRequestBodyBytes: number | null;
  ipWhitelist: string | null;
  allowedMethods: string | null;
  retryCount: number;
  retryDelayMs: number;
  retryNonIdempotent: boolean;
  circuitBreakerEnabled: boolean;
  circuitFailureThreshold: number;
  circuitRecoveryTimeoutSeconds: number;
  createdAt: string;
  updatedAt: string;
}

export interface SavePolicyRequest {
  enabled?: boolean;
  rateLimit?: number;
  rateLimitWindowSeconds?: number;
  quotaLimit?: number | null;
  quotaWindowSeconds?: number | null;
  timeoutMs?: number;
  maxRequestBodyBytes?: number | null;
  ipWhitelist?: string | null;
  allowedMethods?: string | null;
  retryCount?: number;
  retryDelayMs?: number;
  retryNonIdempotent?: boolean;
  circuitBreakerEnabled?: boolean;
  circuitFailureThreshold?: number;
  circuitRecoveryTimeoutSeconds?: number;
}
