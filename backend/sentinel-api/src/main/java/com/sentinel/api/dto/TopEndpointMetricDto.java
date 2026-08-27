package com.sentinel.api.dto;

public class TopEndpointMetricDto {

    private String method;
    private String normalizedPath;
    private long count;
    private double avgLatencyMs;

    public TopEndpointMetricDto() {}

    public TopEndpointMetricDto(String method, String normalizedPath, long count, double avgLatencyMs) {
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.count = count;
        this.avgLatencyMs = avgLatencyMs;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public void setNormalizedPath(String normalizedPath) {
        this.normalizedPath = normalizedPath;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }
}
