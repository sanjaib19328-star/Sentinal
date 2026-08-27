package com.sentinel.api.dto;

public class BulkApiEndpointResultDto {

    private Long endpointId;
    private String method;
    private String path;
    private String status; // VALID, WARNING, ERROR, REQUIRES_INPUT
    private Integer statusCode;
    private Long latencyMs;
    private String responseValidity;
    private String detectedProblems;
    private String recommendation;
    private int parametersCount;
    private boolean hasRequestBody;

    public BulkApiEndpointResultDto() {}

    public Long getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(Long endpointId) {
        this.endpointId = endpointId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getResponseValidity() {
        return responseValidity;
    }

    public void setResponseValidity(String responseValidity) {
        this.responseValidity = responseValidity;
    }

    public String getDetectedProblems() {
        return detectedProblems;
    }

    public void setDetectedProblems(String detectedProblems) {
        this.detectedProblems = detectedProblems;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public int getParametersCount() {
        return parametersCount;
    }

    public void setParametersCount(int parametersCount) {
        this.parametersCount = parametersCount;
    }

    public boolean isHasRequestBody() {
        return hasRequestBody;
    }

    public void setHasRequestBody(boolean hasRequestBody) {
        this.hasRequestBody = hasRequestBody;
    }
}
