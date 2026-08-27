package com.sentinel.api.dto;

import java.util.List;

public class SystemHealthResponse {

    private String controlPlaneStatus;
    private DatabaseHealth mysql;
    private CacheHealth redis;
    private GatewayHealthSummary gateway;
    private List<ApplicationHealthDetail> targetApplications;

    public SystemHealthResponse() {}

    public SystemHealthResponse(String controlPlaneStatus, DatabaseHealth mysql, CacheHealth redis, GatewayHealthSummary gateway, List<ApplicationHealthDetail> targetApplications) {
        this.controlPlaneStatus = controlPlaneStatus;
        this.mysql = mysql;
        this.redis = redis;
        this.gateway = gateway;
        this.targetApplications = targetApplications;
    }

    public static class DatabaseHealth {
        private String status;
        private double latencyMs;

        public DatabaseHealth() {}
        public DatabaseHealth(String status, double latencyMs) {
            this.status = status;
            this.latencyMs = latencyMs;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getLatencyMs() { return latencyMs; }
        public void setLatencyMs(double latencyMs) { this.latencyMs = latencyMs; }
    }

    public static class CacheHealth {
        private String status;
        private double latencyMs;

        public CacheHealth() {}
        public CacheHealth(String status, double latencyMs) {
            this.status = status;
            this.latencyMs = latencyMs;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getLatencyMs() { return latencyMs; }
        public void setLatencyMs(double latencyMs) { this.latencyMs = latencyMs; }
    }

    public static class GatewayHealthSummary {
        private long totalRequests;
        private double errorRate;
        private double avgLatencyMs;
        private double p95LatencyMs;

        public GatewayHealthSummary() {}
        public GatewayHealthSummary(long totalRequests, double errorRate, double avgLatencyMs, double p95LatencyMs) {
            this.totalRequests = totalRequests;
            this.errorRate = errorRate;
            this.avgLatencyMs = avgLatencyMs;
            this.p95LatencyMs = p95LatencyMs;
        }
        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        public double getAvgLatencyMs() { return avgLatencyMs; }
        public void setAvgLatencyMs(double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
        public double getP95LatencyMs() { return p95LatencyMs; }
        public void setP95LatencyMs(double p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }
    }

    public static class ApplicationHealthDetail {
        private Long id;
        private String name;
        private String baseUrl;
        private String healthStatus;
        private String circuitState;
        private int consecutiveFailures;
        private long timeUntilRecoverySeconds;

        public ApplicationHealthDetail() {}
        public ApplicationHealthDetail(Long id, String name, String baseUrl, String healthStatus, String circuitState, int consecutiveFailures, long timeUntilRecoverySeconds) {
            this.id = id;
            this.name = name;
            this.baseUrl = baseUrl;
            this.healthStatus = healthStatus;
            this.circuitState = circuitState;
            this.consecutiveFailures = consecutiveFailures;
            this.timeUntilRecoverySeconds = timeUntilRecoverySeconds;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getHealthStatus() { return healthStatus; }
        public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
        public String getCircuitState() { return circuitState; }
        public void setCircuitState(String circuitState) { this.circuitState = circuitState; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
        public long getTimeUntilRecoverySeconds() { return timeUntilRecoverySeconds; }
        public void setTimeUntilRecoverySeconds(long timeUntilRecoverySeconds) { this.timeUntilRecoverySeconds = timeUntilRecoverySeconds; }
    }

    public String getControlPlaneStatus() { return controlPlaneStatus; }
    public void setControlPlaneStatus(String controlPlaneStatus) { this.controlPlaneStatus = controlPlaneStatus; }
    public DatabaseHealth getMysql() { return mysql; }
    public void setMysql(DatabaseHealth mysql) { this.mysql = mysql; }
    public CacheHealth getRedis() { return redis; }
    public void setRedis(CacheHealth redis) { this.redis = redis; }
    public GatewayHealthSummary getGateway() { return gateway; }
    public void setGateway(GatewayHealthSummary gateway) { this.gateway = gateway; }
    public List<ApplicationHealthDetail> getTargetApplications() { return targetApplications; }
    public void setTargetApplications(List<ApplicationHealthDetail> targetApplications) { this.targetApplications = targetApplications; }
}
