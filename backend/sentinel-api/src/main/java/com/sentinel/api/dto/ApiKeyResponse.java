package com.sentinel.api.dto;

import java.time.Instant;

public class ApiKeyResponse {

    private Long id;
    private String name;
    private String apiKey;
    private String maskedKey;
    private int rateLimitPerMinute;
    private boolean active = true;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String warning;

    public ApiKeyResponse() {
    }

    public ApiKeyResponse(Long id, String name, String apiKey, int rateLimitPerMinute, Instant createdAt, Instant expiresAt, String warning) {
        this.id = id;
        this.name = name;
        this.apiKey = apiKey;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.warning = warning;
        this.active = true;
    }

    public ApiKeyResponse(Long id, String name, String apiKey, String maskedKey, int rateLimitPerMinute, boolean active, Instant createdAt, Instant expiresAt, Instant revokedAt, String warning) {
        this.id = id;
        this.name = name;
        this.apiKey = apiKey;
        this.maskedKey = maskedKey;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.active = active;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.warning = warning;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public void setMaskedKey(String maskedKey) {
        this.maskedKey = maskedKey;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
