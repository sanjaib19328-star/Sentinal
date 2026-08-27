package com.sentinel.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class CreateApiKeyRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Min(value = 1, message = "Rate limit per minute must be at least 1")
    @Max(value = 100000, message = "Rate limit per minute cannot exceed 100000")
    private int rateLimitPerMinute = 60;

    private Instant expiresAt;

    public CreateApiKeyRequest() {
    }

    public CreateApiKeyRequest(String name, int rateLimitPerMinute) {
        this(name, rateLimitPerMinute, null);
    }

    public CreateApiKeyRequest(String name, int rateLimitPerMinute, Instant expiresAt) {
        this.name = name;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.expiresAt = expiresAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
