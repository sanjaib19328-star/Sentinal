package com.sentinel.api.dto;

import java.time.Instant;
import java.util.List;

public class ConsumerKeyAnalyticsDto {

    private Long apiKeyId;
    private String keyName;
    private String keyPrefix;
    private boolean active;
    private int rateLimitPerMinute;
    private long totalRequests;
    private long successRequests;
    private long errorRequests;
    private long count4xx;
    private long count5xx;
    private long count429;
    private double errorRate;
    private double avgLatencyMs;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private Instant lastUsedAt;
    private List<TopEndpointMetricDto> topEndpoints;

    public ConsumerKeyAnalyticsDto() {}

    public ConsumerKeyAnalyticsDto(Long apiKeyId, String keyName, String keyPrefix, boolean active, int rateLimitPerMinute, long totalRequests, long successRequests, long errorRequests, long count4xx, long count5xx, long count429, double errorRate, double avgLatencyMs, double p50LatencyMs, double p95LatencyMs, double p99LatencyMs, Instant lastUsedAt, List<TopEndpointMetricDto> topEndpoints) {
        this.apiKeyId = apiKeyId;
        this.keyName = keyName;
        this.keyPrefix = keyPrefix;
        this.active = active;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.totalRequests = totalRequests;
        this.successRequests = successRequests;
        this.errorRequests = errorRequests;
        this.count4xx = count4xx;
        this.count5xx = count5xx;
        this.count429 = count429;
        this.errorRate = errorRate;
        this.avgLatencyMs = avgLatencyMs;
        this.p50LatencyMs = p50LatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.lastUsedAt = lastUsedAt;
        this.topEndpoints = topEndpoints;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getSuccessRequests() {
        return successRequests;
    }

    public void setSuccessRequests(long successRequests) {
        this.successRequests = successRequests;
    }

    public long getErrorRequests() {
        return errorRequests;
    }

    public void setErrorRequests(long errorRequests) {
        this.errorRequests = errorRequests;
    }

    public long getCount4xx() {
        return count4xx;
    }

    public void setCount4xx(long count4xx) {
        this.count4xx = count4xx;
    }

    public long getCount5xx() {
        return count5xx;
    }

    public void setCount5xx(long count5xx) {
        this.count5xx = count5xx;
    }

    public long getCount429() {
        return count429;
    }

    public void setCount429(long count429) {
        this.count429 = count429;
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

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public List<TopEndpointMetricDto> getTopEndpoints() {
        return topEndpoints;
    }

    public void setTopEndpoints(List<TopEndpointMetricDto> topEndpoints) {
        this.topEndpoints = topEndpoints;
    }
}
