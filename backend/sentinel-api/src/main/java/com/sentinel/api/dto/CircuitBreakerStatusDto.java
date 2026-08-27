package com.sentinel.api.dto;

import com.sentinel.api.model.CircuitState;

import java.time.Instant;

public class CircuitBreakerStatusDto {

    private Long applicationId;
    private CircuitState state;
    private int consecutiveFailures;
    private int failureThreshold;
    private long timeUntilRecoverySeconds;
    private Instant lastStateChange;
    private boolean enabled;

    public CircuitBreakerStatusDto() {}

    public CircuitBreakerStatusDto(Long applicationId, CircuitState state, int consecutiveFailures, int failureThreshold, long timeUntilRecoverySeconds, Instant lastStateChange, boolean enabled) {
        this.applicationId = applicationId;
        this.state = state;
        this.consecutiveFailures = consecutiveFailures;
        this.failureThreshold = failureThreshold;
        this.timeUntilRecoverySeconds = timeUntilRecoverySeconds;
        this.lastStateChange = lastStateChange;
        this.enabled = enabled;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public CircuitState getState() {
        return state;
    }

    public void setState(CircuitState state) {
        this.state = state;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getTimeUntilRecoverySeconds() {
        return timeUntilRecoverySeconds;
    }

    public void setTimeUntilRecoverySeconds(long timeUntilRecoverySeconds) {
        this.timeUntilRecoverySeconds = timeUntilRecoverySeconds;
    }

    public Instant getLastStateChange() {
        return lastStateChange;
    }

    public void setLastStateChange(Instant lastStateChange) {
        this.lastStateChange = lastStateChange;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
