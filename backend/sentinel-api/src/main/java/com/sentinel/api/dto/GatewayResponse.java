package com.sentinel.api.dto;

public class GatewayResponse {

    private boolean success;
    private String message;
    private String requestId;
    private Long apiKeyId;

    public GatewayResponse() {
    }

    public GatewayResponse(boolean success, String message, String requestId, Long apiKeyId) {
        this.success = success;
        this.message = message;
        this.requestId = requestId;
        this.apiKeyId = apiKeyId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
}
