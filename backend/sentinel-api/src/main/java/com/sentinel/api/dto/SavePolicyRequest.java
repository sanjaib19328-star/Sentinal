package com.sentinel.api.dto;

public class SavePolicyRequest {

    private Boolean enabled;
    private Integer rateLimit;
    private Integer rateWindowSeconds;
    private Integer quotaLimit;
    private Integer quotaWindowSeconds;
    private Integer timeoutMs;
    private Long maxRequestBodyBytes;
    private Long maxResponseBodyBytes;
    private String allowedMethods;
    private Integer retryCount;
    private Integer retryDelayMs;
    private Boolean retryNonIdempotent;
    private Boolean circuitBreakerEnabled;
    private Integer circuitFailureThreshold;
    private Integer circuitRecoveryTimeoutSeconds;

    public SavePolicyRequest() {}

    public SavePolicyRequest(Boolean enabled, Integer rateLimit, Integer rateWindowSeconds, Integer quotaLimit, Integer quotaWindowSeconds, Integer timeoutMs, Long maxRequestBodyBytes, Long maxResponseBodyBytes, String allowedMethods) {
        this.enabled = enabled;
        this.rateLimit = rateLimit;
        this.rateWindowSeconds = rateWindowSeconds;
        this.quotaLimit = quotaLimit;
        this.quotaWindowSeconds = quotaWindowSeconds;
        this.timeoutMs = timeoutMs;
        this.maxRequestBodyBytes = maxRequestBodyBytes;
        this.maxResponseBodyBytes = maxResponseBodyBytes;
        this.allowedMethods = allowedMethods;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Integer getRateWindowSeconds() {
        return rateWindowSeconds;
    }

    public void setRateWindowSeconds(Integer rateWindowSeconds) {
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

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
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

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(Integer retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public Boolean getRetryNonIdempotent() {
        return retryNonIdempotent;
    }

    public void setRetryNonIdempotent(Boolean retryNonIdempotent) {
        this.retryNonIdempotent = retryNonIdempotent;
    }

    public Boolean getCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(Boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public Integer getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(Integer circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public Integer getCircuitRecoveryTimeoutSeconds() {
        return circuitRecoveryTimeoutSeconds;
    }

    public void setCircuitRecoveryTimeoutSeconds(Integer circuitRecoveryTimeoutSeconds) {
        this.circuitRecoveryTimeoutSeconds = circuitRecoveryTimeoutSeconds;
    }
}
