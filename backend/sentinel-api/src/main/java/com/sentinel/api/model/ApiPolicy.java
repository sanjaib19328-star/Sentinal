package com.sentinel.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "api_policies")
public class ApiPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "api_endpoint_id")
    private Long apiEndpointId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "rate_limit", nullable = false)
    private int rateLimit = 60;

    @Column(name = "rate_window_seconds", nullable = false)
    private int rateWindowSeconds = 60;

    @Column(name = "quota_limit")
    private Integer quotaLimit;

    @Column(name = "quota_window_seconds")
    private Integer quotaWindowSeconds;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs = 5000;

    @Column(name = "max_request_body_bytes")
    private Long maxRequestBodyBytes;

    @Column(name = "max_response_body_bytes")
    private Long maxResponseBodyBytes;

    @Column(name = "allowed_methods", length = 100)
    private String allowedMethods; // e.g. "GET,POST,PUT" or null for all

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "retry_delay_ms", nullable = false)
    private int retryDelayMs = 100;

    @Column(name = "retry_non_idempotent", nullable = false)
    private boolean retryNonIdempotent = false;

    @Column(name = "circuit_breaker_enabled", nullable = false)
    private boolean circuitBreakerEnabled = true;

    @Column(name = "circuit_failure_threshold", nullable = false)
    private int circuitFailureThreshold = 5;

    @Column(name = "circuit_recovery_timeout_seconds", nullable = false)
    private int circuitRecoveryTimeoutSeconds = 15;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ApiPolicy() {}

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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
