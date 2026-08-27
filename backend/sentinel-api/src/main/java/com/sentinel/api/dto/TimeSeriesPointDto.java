package com.sentinel.api.dto;

import java.time.Instant;

public class TimeSeriesPointDto {

    private Instant timestamp;
    private long requestCount;
    private long successCount;
    private long errorCount;
    private long rateLimitedCount;
    private double avgLatency;
    private double p95Latency;

    public TimeSeriesPointDto() {}

    public TimeSeriesPointDto(Instant timestamp, long requestCount, long successCount, long errorCount, long rateLimitedCount, double avgLatency, double p95Latency) {
        this.timestamp = timestamp;
        this.requestCount = requestCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.rateLimitedCount = rateLimitedCount;
        this.avgLatency = avgLatency;
        this.p95Latency = p95Latency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getRateLimitedCount() {
        return rateLimitedCount;
    }

    public void setRateLimitedCount(long rateLimitedCount) {
        this.rateLimitedCount = rateLimitedCount;
    }

    public double getAvgLatency() {
        return avgLatency;
    }

    public void setAvgLatency(double avgLatency) {
        this.avgLatency = avgLatency;
    }

    public double getP95Latency() {
        return p95Latency;
    }

    public void setP95Latency(double p95Latency) {
        this.p95Latency = p95Latency;
    }
}
