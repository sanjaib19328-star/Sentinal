package com.sentinel.api.dto;

import com.sentinel.api.model.DocumentationStatus;

import java.time.Instant;

public class ApiEndpointDto {

    private Long id;
    private Long applicationId;
    private String method;
    private String normalizedPath;
    private DocumentationStatus documentationStatus;
    private String summary;
    private String description;
    private String parametersJson;
    private String requestBodySchemaJson;
    private String responsesJson;
    private boolean deprecated;
    private Instant firstSeenAt;
    private Instant lastSeenAt;

    // Telemetry fields
    private Long totalRequests;
    private Long errorCount;
    private Double avgLatencyMs;
    private Double successRate;

    public ApiEndpointDto() {}

    public ApiEndpointDto(Long id, Long applicationId, String method, String normalizedPath, DocumentationStatus documentationStatus, String summary, String description, String parametersJson, String requestBodySchemaJson, String responsesJson, boolean deprecated, Instant firstSeenAt, Instant lastSeenAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.documentationStatus = documentationStatus;
        this.summary = summary;
        this.description = description;
        this.parametersJson = parametersJson;
        this.requestBodySchemaJson = requestBodySchemaJson;
        this.responsesJson = responsesJson;
        this.deprecated = deprecated;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public ApiEndpointDto(Long id, Long applicationId, String method, String normalizedPath, DocumentationStatus documentationStatus, String summary, String description, String parametersJson, String requestBodySchemaJson, String responsesJson, boolean deprecated, Instant firstSeenAt, Instant lastSeenAt, Long totalRequests, Long errorCount, Double avgLatencyMs, Double successRate) {
        this.id = id;
        this.applicationId = applicationId;
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.documentationStatus = documentationStatus;
        this.summary = summary;
        this.description = description;
        this.parametersJson = parametersJson;
        this.requestBodySchemaJson = requestBodySchemaJson;
        this.responsesJson = responsesJson;
        this.deprecated = deprecated;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.totalRequests = totalRequests;
        this.errorCount = errorCount;
        this.avgLatencyMs = avgLatencyMs;
        this.successRate = successRate;
    }

    // Backward-compatible constructor for ApiCatalogService
    public ApiEndpointDto(Long id, Long applicationId, String method, String normalizedPath, Instant firstSeenAt, Instant lastSeenAt, Long totalRequests, Long errorCount, Double avgLatencyMs, Double successRate) {
        this.id = id;
        this.applicationId = applicationId;
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.documentationStatus = DocumentationStatus.DISCOVERED;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.totalRequests = totalRequests;
        this.errorCount = errorCount;
        this.avgLatencyMs = avgLatencyMs;
        this.successRate = successRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public void setNormalizedPath(String normalizedPath) {
        this.normalizedPath = normalizedPath;
    }

    public DocumentationStatus getDocumentationStatus() {
        return documentationStatus;
    }

    public void setDocumentationStatus(DocumentationStatus documentationStatus) {
        this.documentationStatus = documentationStatus;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public String getRequestBodySchemaJson() {
        return requestBodySchemaJson;
    }

    public void setRequestBodySchemaJson(String requestBodySchemaJson) {
        this.requestBodySchemaJson = requestBodySchemaJson;
    }

    public String getResponsesJson() {
        return responsesJson;
    }

    public void setResponsesJson(String responsesJson) {
        this.responsesJson = responsesJson;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }
}
