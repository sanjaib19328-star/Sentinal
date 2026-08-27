package com.sentinel.api.dto;

import com.sentinel.api.model.UpstreamAuthType;

public class UpstreamAuthConfigResponse {

    private UpstreamAuthType type;
    private boolean enabled;
    private boolean configured;
    private String headerName;
    private String queryParamName;
    private String maskedSecret;
    private String username;

    public UpstreamAuthConfigResponse() {}

    public UpstreamAuthConfigResponse(UpstreamAuthType type, boolean enabled, boolean configured, String headerName, String queryParamName, String maskedSecret, String username) {
        this.type = type;
        this.enabled = enabled;
        this.configured = configured;
        this.headerName = headerName;
        this.queryParamName = queryParamName;
        this.maskedSecret = maskedSecret;
        this.username = username;
    }

    public UpstreamAuthType getType() {
        return type;
    }

    public void setType(UpstreamAuthType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getQueryParamName() {
        return queryParamName;
    }

    public void setQueryParamName(String queryParamName) {
        this.queryParamName = queryParamName;
    }

    public String getMaskedSecret() {
        return maskedSecret;
    }

    public void setMaskedSecret(String maskedSecret) {
        this.maskedSecret = maskedSecret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
