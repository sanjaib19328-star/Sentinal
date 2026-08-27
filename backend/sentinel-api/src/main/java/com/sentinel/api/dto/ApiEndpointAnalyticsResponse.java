package com.sentinel.api.dto;

import java.time.Instant;
import java.util.List;

public class ApiEndpointAnalyticsResponse {

    private Long endpointId;
    private Long applicationId;
    private String method;
    private String normalizedPath;
    private long totalRequests;
    private long successCount;
    private long errorCount;
    private double successRate;
    private double errorRate;
    private double avgLatencyMs;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private long status4xxCount;
    private long status5xxCount;
    private long rateLimitedCount;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private List<RequestLogResponse> recentRequests;

    public ApiEndpointAnalyticsResponse() {
    }

    public ApiEndpointAnalyticsResponse(
        Long endpointId,
        Long applicationId,
        String method,
        String normalizedPath,
        long totalRequests,
        long successCount,
        long errorCount,
        double successRate,
        double errorRate,
        double avgLatencyMs,
        double p50LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        long status4xxCount,
        long status5xxCount,
        long rateLimitedCount,
        Instant firstSeenAt,
        Instant lastSeenAt,
        List<RequestLogResponse> recentRequests
    ) {
        this.endpointId = endpointId;
        this.applicationId = applicationId;
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.totalRequests = totalRequests;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.successRate = successRate;
        this.errorRate = errorRate;
        this.avgLatencyMs = avgLatencyMs;
        this.p50LatencyMs = p50LatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.status4xxCount = status4xxCount;
        this.status5xxCount = status5xxCount;
        this.rateLimitedCount = rateLimitedCount;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.recentRequests = recentRequests;
    }

    public Long getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(Long endpointId) {
        this.endpointId = endpointId;
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

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public double getP50LatencyMs() {
        return p50LatencyMs;
    }

    public void setP50LatencyMs(double p50LatencyMs) {
        this.p50LatencyMs = p50LatencyMs;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public void setP99LatencyMs(double p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
    }

    public long getStatus4xxCount() {
        return status4xxCount;
    }

    public void setStatus4xxCount(long status4xxCount) {
        this.status4xxCount = status4xxCount;
    }

    public long getStatus5xxCount() {
        return status5xxCount;
    }

    public void setStatus5xxCount(long status5xxCount) {
        this.status5xxCount = status5xxCount;
    }

    public long getRateLimitedCount() {
        return rateLimitedCount;
    }

    public void setRateLimitedCount(long rateLimitedCount) {
        this.rateLimitedCount = rateLimitedCount;
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

    public List<RequestLogResponse> getRecentRequests() {
        return recentRequests;
    }

    public void setRecentRequests(List<RequestLogResponse> recentRequests) {
        this.recentRequests = recentRequests;
    }
}
