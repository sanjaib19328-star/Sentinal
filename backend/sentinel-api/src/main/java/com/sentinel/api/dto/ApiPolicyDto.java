package com.sentinel.api.dto;

import java.time.Instant;

public class ApiPolicyDto {

    private Long id;
    private Long applicationId;
    private Long apiEndpointId;
    private boolean enabled;
    private int rateLimit;
    private int rateWindowSeconds;
    private Integer quotaLimit;
    private Integer quotaWindowSeconds;
    private int timeoutMs;
    private Long maxRequestBodyBytes;
    private Long maxResponseBodyBytes;
    private String allowedMethods;
    private int retryCount;
    private int retryDelayMs;
    private boolean retryNonIdempotent;
    private boolean circuitBreakerEnabled;
    private int circuitFailureThreshold;
    private int circuitRecoveryTimeoutSeconds;
    private Instant createdAt;
    private Instant updatedAt;

    public ApiPolicyDto() {}

    public ApiPolicyDto(Long id, Long applicationId, Long apiEndpointId, boolean enabled, int rateLimit, int rateWindowSeconds, Integer quotaLimit, Integer quotaWindowSeconds, int timeoutMs, Long maxRequestBodyBytes, Long maxResponseBodyBytes, String allowedMethods, int retryCount, int retryDelayMs, boolean retryNonIdempotent, boolean circuitBreakerEnabled, int circuitFailureThreshold, int circuitRecoveryTimeoutSeconds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.apiEndpointId = apiEndpointId;
        this.enabled = enabled;
        this.rateLimit = rateLimit;
        this.rateWindowSeconds = rateWindowSeconds;
        this.quotaLimit = quotaLimit;
        this.quotaWindowSeconds = quotaWindowSeconds;
        this.timeoutMs = timeoutMs;
        this.maxRequestBodyBytes = maxRequestBodyBytes;
        this.maxResponseBodyBytes = maxResponseBodyBytes;
        this.allowedMethods = allowedMethods;
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.retryNonIdempotent = retryNonIdempotent;
        this.circuitBreakerEnabled = circuitBreakerEnabled;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitRecoveryTimeoutSeconds = circuitRecoveryTimeoutSeconds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getApiEndpointId() {
        return apiEndpointId;
    }

    public void setApiEndpointId(Long apiEndpointId) {
        this.apiEndpointId = apiEndpointId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit;
    }

    public int getRateWindowSeconds() {
        return rateWindowSeconds;
    }

    public void setRateWindowSeconds(int rateWindowSeconds) {
        this.rateWindowSeconds = rateWindowSeconds;
    }

    public Integer getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Integer quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public Integer getQuotaWindowSeconds() {
        return quotaWindowSeconds;
    }

    public void setQuotaWindowSeconds(Integer quotaWindowSeconds) {
        this.quotaWindowSeconds = quotaWindowSeconds;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Long getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    public void setMaxRequestBodyBytes(Long maxRequestBodyBytes) {
        this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    public Long getMaxResponseBodyBytes() {
        return maxResponseBodyBytes;
    }

    public void setMaxResponseBodyBytes(Long maxResponseBodyBytes) {
        this.maxResponseBodyBytes = maxResponseBodyBytes;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(int retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public boolean isRetryNonIdempotent() {
        return retryNonIdempotent;
    }

    public void setRetryNonIdempotent(boolean retryNonIdempotent) {
        this.retryNonIdempotent = retryNonIdempotent;
    }

    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(int circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public int getCircuitRecoveryTimeoutSeconds() {
        return circuitRecoveryTimeoutSeconds;
    }

    public void setCircuitRecoveryTimeoutSeconds(int circuitRecoveryTimeoutSeconds) {
        this.circuitRecoveryTimeoutSeconds = circuitRecoveryTimeoutSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
