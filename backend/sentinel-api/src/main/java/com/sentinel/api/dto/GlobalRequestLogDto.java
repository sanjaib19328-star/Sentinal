package com.sentinel.api.dto;

import java.time.Instant;

public class GlobalRequestLogDto {

    private Long id;
    private String requestId;
    private Long applicationId;
    private String applicationName;
    private Long apiKeyId;
    private String keyName;
    private String keyMasked;
    private Long endpointId;
    private String method;
    private String path;
    private String normalizedPath;
    private int statusCode;
    private long latencyMs;
    private String clientIp;
    private Instant timestamp;

    public GlobalRequestLogDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public Long getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(Long apiKeyId) { this.apiKeyId = apiKeyId; }
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    public String getKeyMasked() { return keyMasked; }
    public void setKeyMasked(String keyMasked) { this.keyMasked = keyMasked; }
    public Long getEndpointId() { return endpointId; }
    public void setEndpointId(Long endpointId) { this.endpointId = endpointId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getNormalizedPath() { return normalizedPath; }
    public void setNormalizedPath(String normalizedPath) { this.normalizedPath = normalizedPath; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
