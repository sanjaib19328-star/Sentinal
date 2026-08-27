package com.sentinel.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sentinel.api.model.UpstreamAuthType;

public class UpstreamAuthConfigRequest {

    private UpstreamAuthType type = UpstreamAuthType.NONE;
    private Boolean enabled = true;
    private String headerName;
    private String queryParamName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secret;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public UpstreamAuthConfigRequest() {}

    public UpstreamAuthConfigRequest(UpstreamAuthType type, Boolean enabled, String headerName, String queryParamName, String secret, String username, String password) {
        this.type = type;
        this.enabled = enabled != null ? enabled : true;
        this.headerName = headerName;
        this.queryParamName = queryParamName;
        this.secret = secret;
        this.username = username;
        this.password = password;
    }

    public UpstreamAuthType getType() {
        return type;
    }

    public void setType(UpstreamAuthType type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
