package com.sentinel.api.dto;

import com.sentinel.api.model.DocumentationStatus;

import java.time.Instant;

public class GlobalApiEndpointDto {

    private Long id;
    private Long applicationId;
    private String applicationName;
    private String applicationBaseUrl;
    private String method;
    private String normalizedPath;
    private DocumentationStatus documentationStatus;
    private String summary;
    private String description;
    private String parametersJson;
    private String requestBodySchemaJson;
    private String responsesJson;
    private Boolean deprecated;
    private long totalRequests;
    private long errorCount;
    private double errorRate;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private double successRate;
    private Instant firstSeenAt;
    private Instant lastSeenAt;

    public GlobalApiEndpointDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public String getApplicationBaseUrl() { return applicationBaseUrl; }
    public void setApplicationBaseUrl(String applicationBaseUrl) { this.applicationBaseUrl = applicationBaseUrl; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getNormalizedPath() { return normalizedPath; }
    public void setNormalizedPath(String normalizedPath) { this.normalizedPath = normalizedPath; }
    public DocumentationStatus getDocumentationStatus() { return documentationStatus; }
    public void setDocumentationStatus(DocumentationStatus documentationStatus) { this.documentationStatus = documentationStatus; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
    public String getRequestBodySchemaJson() { return requestBodySchemaJson; }
    public void setRequestBodySchemaJson(String requestBodySchemaJson) { this.requestBodySchemaJson = requestBodySchemaJson; }
    public String getResponsesJson() { return responsesJson; }
    public void setResponsesJson(String responsesJson) { this.responsesJson = responsesJson; }
    public Boolean getDeprecated() { return deprecated; }
    public void setDeprecated(Boolean deprecated) { this.deprecated = deprecated; }
    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
    public double getP95LatencyMs() { return p95LatencyMs; }
    public void setP95LatencyMs(double p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
