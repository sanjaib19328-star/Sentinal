package com.sentinel.api.dto;

import com.sentinel.api.model.HealthStatus;

import java.time.Instant;

public class ConnectionTestResponse {

    private Long applicationId;
    private boolean reachable;
    private HealthStatus status;
    private Integer statusCode;
    private Long latencyMs;
    private String message;
    private Instant checkedAt;

    public ConnectionTestResponse() {
    }

    public ConnectionTestResponse(
        Long applicationId,
        boolean reachable,
        HealthStatus status,
        Long latencyMs,
        String message,
        Instant checkedAt
    ) {
        this(applicationId, reachable, status, null, latencyMs, message, checkedAt);
    }

    public ConnectionTestResponse(
        Long applicationId,
        boolean reachable,
        HealthStatus status,
        Integer statusCode,
        Long latencyMs,
        String message,
        Instant checkedAt
    ) {
        this.applicationId = applicationId;
        this.reachable = reachable;
        this.status = status;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.message = message;
        this.checkedAt = checkedAt;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public void setStatus(HealthStatus status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
