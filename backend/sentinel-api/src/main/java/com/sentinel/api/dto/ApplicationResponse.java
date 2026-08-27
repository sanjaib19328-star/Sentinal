package com.sentinel.api.dto;

import com.sentinel.api.model.ConnectionMode;
import com.sentinel.api.model.HealthStatus;

import java.time.Instant;

public class ApplicationResponse {

    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String baseUrl;
    private ConnectionMode connectionMode;
    private boolean active;
    private HealthStatus healthStatus;
    private Instant lastSeenAt;
    private Instant createdAt;
    private Instant updatedAt;
    private UpstreamAuthConfigResponse upstreamAuth;

    public ApplicationResponse() {
    }

    public ApplicationResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String baseUrl,
        ConnectionMode connectionMode,
        boolean active,
        HealthStatus healthStatus,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, ownerId, name, description, baseUrl, connectionMode, active, healthStatus, lastSeenAt, createdAt, updatedAt, null);
    }

    public ApplicationResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String baseUrl,
        ConnectionMode connectionMode,
        boolean active,
        HealthStatus healthStatus,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt,
        UpstreamAuthConfigResponse upstreamAuth
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.connectionMode = connectionMode;
        this.active = active;
        this.healthStatus = healthStatus;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.upstreamAuth = upstreamAuth;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(ConnectionMode connectionMode) {
        this.connectionMode = connectionMode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
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

    public UpstreamAuthConfigResponse getUpstreamAuth() {
        return upstreamAuth;
    }

    public void setUpstreamAuth(UpstreamAuthConfigResponse upstreamAuth) {
        this.upstreamAuth = upstreamAuth;
    }
}
