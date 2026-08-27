package com.sentinel.api.dto;

import com.sentinel.api.model.ConnectionMode;
import jakarta.validation.constraints.Size;

public class UpdateApplicationRequest {

    @Size(max = 100, message = "Application name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 255, message = "Base URL cannot exceed 255 characters")
    private String baseUrl;

    private Boolean active;

    private ConnectionMode connectionMode;

    public UpdateApplicationRequest() {
    }

    public UpdateApplicationRequest(String name, String description, String baseUrl, Boolean active, ConnectionMode connectionMode) {
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.active = active;
        this.connectionMode = connectionMode;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(ConnectionMode connectionMode) {
        this.connectionMode = connectionMode;
    }
}
