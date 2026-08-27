export interface ApiKey {
  id: number;
  name: string;
  applicationId?: number;
  apiKey?: string | null; // only present upon creation or regeneration
  maskedKey?: string;
  rateLimitPerMinute: number;
  active: boolean;
  createdAt: string;
  expiresAt: string | null;
  revokedAt: string | null;
  warning?: string | null;
}

export interface CreateApiKeyRequest {
  name: string;
  rateLimitPerMinute?: number;
  expiresAt?: string | null;
}

export interface UpdateApiKeyRequest {
  name?: string;
  rateLimitPerMinute?: number;
  active?: boolean;
}

export interface ApiKeyResponse {
  id: number;
  name: string;
  apiKey: string | null;
  maskedKey: string | null;
  rateLimitPerMinute: number;
  active: boolean;
  createdAt: string;
  expiresAt: string | null;
  revokedAt: string | null;
  warning: string | null;
}
