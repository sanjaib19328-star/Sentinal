package com.sentinel.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "request_logs",
    indexes = {
        @Index(name = "idx_req_log_request_id", columnList = "request_id"),
        @Index(name = "idx_req_log_api_key_id", columnList = "api_key_id"),
        @Index(name = "idx_req_log_timestamp", columnList = "timestamp")
    }
)
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "endpoint_id")
    private Long endpointId;

    @Column(name = "normalized_path", length = 255)
    private String normalizedPath;

    public RequestLog() {
    }

    public RequestLog(String requestId, Long apiKeyId, String method, String path, int statusCode, long latencyMs, String clientIp) {
        this.requestId = requestId;
        this.apiKeyId = apiKeyId;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.clientIp = clientIp;
    }

    public RequestLog(String requestId, Long applicationId, Long apiKeyId, Long endpointId, String method, String path, String normalizedPath, int statusCode, long latencyMs, String clientIp) {
        this.requestId = requestId;
        this.applicationId = applicationId;
        this.apiKeyId = apiKeyId;
        this.endpointId = endpointId;
        this.method = method;
        this.path = path;
        this.normalizedPath = normalizedPath;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.clientIp = clientIp;
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(Long endpointId) {
        this.endpointId = endpointId;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public void setNormalizedPath(String normalizedPath) {
        this.normalizedPath = normalizedPath;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}
