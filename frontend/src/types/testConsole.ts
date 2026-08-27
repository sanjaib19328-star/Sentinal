export interface ApiTestConsoleRequest {
  apiKeyId: number;
  method: string;
  path: string;
  queryParams?: Record<string, string>;
  headers?: Record<string, string>;
  body?: string;
  binaryBodyBase64?: string;
  fileName?: string;
  fileFieldName?: string;
  fileContentType?: string;
}

export interface ApiTestConsoleResult {
  statusCode: number;
  latencyMs: number;
  requestId: string;
  responseHeaders: Record<string, string>;
  responseBody: string;
  rateLimitLimit: number;
  rateLimitRemaining: number;
  rateLimitReset: number;
  throttledBy?: string;
}
