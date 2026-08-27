package com.sentinel.api.dto;

import com.sentinel.api.model.HealthStatus;

import java.util.List;

public class ConnectAndDiscoverResponse {

    private Long applicationId;
    private String applicationName;
    private String backendUrl;
    private String sentinelGatewayUrl;
    private String apiKey;
    private HealthStatus healthStatus;
    private boolean backendHealthy;
    private int apisDiscoveredCount;
    private List<ApiEndpointDto> discoveredApis;
    private String message;

    public ConnectAndDiscoverResponse() {}

    public ConnectAndDiscoverResponse(
        Long applicationId,
        String applicationName,
        String backendUrl,
        String sentinelGatewayUrl,
        String apiKey,
        HealthStatus healthStatus,
        boolean backendHealthy,
        int apisDiscoveredCount,
        List<ApiEndpointDto> discoveredApis,
        String message
    ) {
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.backendUrl = backendUrl;
        this.sentinelGatewayUrl = sentinelGatewayUrl;
        this.apiKey = apiKey;
        this.healthStatus = healthStatus;
        this.backendHealthy = backendHealthy;
        this.apisDiscoveredCount = apisDiscoveredCount;
        this.discoveredApis = discoveredApis;
        this.message = message;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public String getSentinelGatewayUrl() {
        return sentinelGatewayUrl;
    }

    public void setSentinelGatewayUrl(String sentinelGatewayUrl) {
        this.sentinelGatewayUrl = sentinelGatewayUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public boolean isBackendHealthy() {
        return backendHealthy;
    }

    public void setBackendHealthy(boolean backendHealthy) {
        this.backendHealthy = backendHealthy;
    }

    public int getApisDiscoveredCount() {
        return apisDiscoveredCount;
    }

    public void setApisDiscoveredCount(int apisDiscoveredCount) {
        this.apisDiscoveredCount = apisDiscoveredCount;
    }

    public List<ApiEndpointDto> getDiscoveredApis() {
        return discoveredApis;
    }

    public void setDiscoveredApis(List<ApiEndpointDto> discoveredApis) {
        this.discoveredApis = discoveredApis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
