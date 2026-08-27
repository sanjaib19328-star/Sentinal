package com.sentinel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateApplicationRequest {

    @NotBlank(message = "Application name is required")
    @Size(max = 100, message = "Application name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Base URL is required")
    @Size(max = 255, message = "Base URL cannot exceed 255 characters")
    private String baseUrl;

    private UpstreamAuthConfigRequest upstreamAuth;

    public CreateApplicationRequest() {
    }

    public CreateApplicationRequest(String name, String description, String baseUrl) {
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
    }

    public CreateApplicationRequest(String name, String description, String baseUrl, UpstreamAuthConfigRequest upstreamAuth) {
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.upstreamAuth = upstreamAuth;
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

    public UpstreamAuthConfigRequest getUpstreamAuth() {
        return upstreamAuth;
    }

    public void setUpstreamAuth(UpstreamAuthConfigRequest upstreamAuth) {
        this.upstreamAuth = upstreamAuth;
    }
}
