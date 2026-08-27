package com.sentinel.api.dto;

import java.util.Map;

public class ApiTestConsoleResultDto {

    private int statusCode;
    private long latencyMs;
    private String requestId;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private long rateLimitLimit;
    private long rateLimitRemaining;
    private long rateLimitReset;
    private String throttledBy;

    public ApiTestConsoleResultDto() {}

    public ApiTestConsoleResultDto(int statusCode, long latencyMs, String requestId, Map<String, String> responseHeaders, String responseBody, long rateLimitLimit, long rateLimitRemaining, long rateLimitReset, String throttledBy) {
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.requestId = requestId;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.rateLimitLimit = rateLimitLimit;
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitReset = rateLimitReset;
        this.throttledBy = throttledBy;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getRateLimitLimit() {
        return rateLimitLimit;
    }

    public void setRateLimitLimit(long rateLimitLimit) {
        this.rateLimitLimit = rateLimitLimit;
    }

    public long getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public void setRateLimitRemaining(long rateLimitRemaining) {
        this.rateLimitRemaining = rateLimitRemaining;
    }

    public long getRateLimitReset() {
        return rateLimitReset;
    }

    public void setRateLimitReset(long rateLimitReset) {
        this.rateLimitReset = rateLimitReset;
    }

    public String getThrottledBy() {
        return throttledBy;
    }

    public void setThrottledBy(String throttledBy) {
        this.throttledBy = throttledBy;
    }
}
