package com.sentinel.api.dto;

import java.time.Instant;

public class RequestLogResponse {

    private String requestId;
    private String method;
    private String path;
    private int statusCode;
    private long latencyMs;
    private Instant timestamp;
    private String clientIp;

    public RequestLogResponse() {
    }

    public RequestLogResponse(
        String requestId,
        String method,
        String path,
        int statusCode,
        long latencyMs,
        Instant timestamp,
        String clientIp
    ) {
        this.requestId = requestId;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.timestamp = timestamp;
        this.clientIp = clientIp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}
