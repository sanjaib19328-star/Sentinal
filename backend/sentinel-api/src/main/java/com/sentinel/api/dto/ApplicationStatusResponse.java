package com.sentinel.api.dto;

import com.sentinel.api.model.ConnectionMode;
import com.sentinel.api.model.HealthStatus;

import java.time.Instant;

public class ApplicationStatusResponse {

    private Long applicationId;
    private HealthStatus status;
    private Instant lastSeenAt;
    private ConnectionMode connectionMode;

    public ApplicationStatusResponse() {
    }

    public ApplicationStatusResponse(Long applicationId, HealthStatus status, Instant lastSeenAt, ConnectionMode connectionMode) {
        this.applicationId = applicationId;
        this.status = status;
        this.lastSeenAt = lastSeenAt;
        this.connectionMode = connectionMode;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public void setStatus(HealthStatus status) {
        this.status = status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(ConnectionMode connectionMode) {
        this.connectionMode = connectionMode;
    }
}
