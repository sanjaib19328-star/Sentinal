package com.sentinel.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ConnectAndDiscoverRequest {

    @NotBlank(message = "Application name is required")
    private String applicationName;

    @NotBlank(message = "Sentinel URL or Backend URL is required")
    private String sentinelUrl;

    private String apiKey;

    public ConnectAndDiscoverRequest() {}

    public ConnectAndDiscoverRequest(String applicationName, String sentinelUrl, String apiKey) {
        this.applicationName = applicationName;
        this.sentinelUrl = sentinelUrl;
        this.apiKey = apiKey;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSentinelUrl() {
        return sentinelUrl;
    }

    public void setSentinelUrl(String sentinelUrl) {
        this.sentinelUrl = sentinelUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
