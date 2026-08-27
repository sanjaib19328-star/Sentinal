package com.sentinel.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateApiKeyRequest {

    @Size(max = 100, message = "Key name must not exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Rate limit must be at least 1 request per minute")
    @Max(value = 100000, message = "Rate limit cannot exceed 100,000 requests per minute")
    private Integer rateLimitPerMinute;

    private Boolean active;

    public UpdateApiKeyRequest() {
    }

    public UpdateApiKeyRequest(String name, Integer rateLimitPerMinute, Boolean active) {
        this.name = name;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
