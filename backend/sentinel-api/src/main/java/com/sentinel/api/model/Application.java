package com.sentinel.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "applications",
    indexes = {
        @Index(name = "idx_application_owner_id", columnList = "owner_id")
    }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_mode", nullable = false, length = 50)
    private ConnectionMode connectionMode = ConnectionMode.OBSERVATION;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 50)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "upstream_auth_type", nullable = false, length = 50)
    private UpstreamAuthType upstreamAuthType = UpstreamAuthType.NONE;

    @Column(name = "upstream_auth_enabled", nullable = false)
    private boolean upstreamAuthEnabled = false;

    @Column(name = "upstream_auth_config_encrypted", columnDefinition = "TEXT")
    private String upstreamAuthConfigEncrypted;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Application() {
    }

    public Application(Long ownerId, String name, String description, String baseUrl) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.connectionMode = ConnectionMode.OBSERVATION;
        this.active = true;
        this.healthStatus = HealthStatus.UNKNOWN;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
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

    public UpstreamAuthType getUpstreamAuthType() {
        return upstreamAuthType;
    }

    public void setUpstreamAuthType(UpstreamAuthType upstreamAuthType) {
        this.upstreamAuthType = upstreamAuthType != null ? upstreamAuthType : UpstreamAuthType.NONE;
    }

    public boolean isUpstreamAuthEnabled() {
        return upstreamAuthEnabled;
    }

    public void setUpstreamAuthEnabled(boolean upstreamAuthEnabled) {
        this.upstreamAuthEnabled = upstreamAuthEnabled;
    }

    public String getUpstreamAuthConfigEncrypted() {
        return upstreamAuthConfigEncrypted;
    }

    public void setUpstreamAuthConfigEncrypted(String upstreamAuthConfigEncrypted) {
        this.upstreamAuthConfigEncrypted = upstreamAuthConfigEncrypted;
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
