package com.sentinel.api.dto;

import java.util.List;
import java.util.Map;

public class TrafficBreakdownResponse {

    private Long applicationId;
    private Map<String, Long> methodCounts; // GET, POST, PUT, DELETE, etc.
    private Map<String, Long> statusClassCounts; // 2xx, 3xx, 4xx, 5xx
    private List<EndpointRankDto> topApis;
    private List<EndpointRankDto> slowestApis;
    private List<EndpointRankDto> errorProneApis;
    private List<EndpointRankDto> rateLimitedApis;

    public TrafficBreakdownResponse() {}

    public TrafficBreakdownResponse(Long applicationId, Map<String, Long> methodCounts, Map<String, Long> statusClassCounts, List<EndpointRankDto> topApis, List<EndpointRankDto> slowestApis, List<EndpointRankDto> errorProneApis, List<EndpointRankDto> rateLimitedApis) {
        this.applicationId = applicationId;
        this.methodCounts = methodCounts;
        this.statusClassCounts = statusClassCounts;
        this.topApis = topApis;
        this.slowestApis = slowestApis;
        this.errorProneApis = errorProneApis;
        this.rateLimitedApis = rateLimitedApis;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Map<String, Long> getMethodCounts() {
        return methodCounts;
    }

    public void setMethodCounts(Map<String, Long> methodCounts) {
        this.methodCounts = methodCounts;
    }

    public Map<String, Long> getStatusClassCounts() {
        return statusClassCounts;
    }

    public void setStatusClassCounts(Map<String, Long> statusClassCounts) {
        this.statusClassCounts = statusClassCounts;
    }

    public List<EndpointRankDto> getTopApis() {
        return topApis;
    }

    public void setTopApis(List<EndpointRankDto> topApis) {
        this.topApis = topApis;
    }

    public List<EndpointRankDto> getSlowestApis() {
        return slowestApis;
    }

    public void setSlowestApis(List<EndpointRankDto> slowestApis) {
        this.slowestApis = slowestApis;
    }

    public List<EndpointRankDto> getErrorProneApis() {
        return errorProneApis;
    }

    public void setErrorProneApis(List<EndpointRankDto> errorProneApis) {
        this.errorProneApis = errorProneApis;
    }

    public List<EndpointRankDto> getRateLimitedApis() {
        return rateLimitedApis;
    }

    public void setRateLimitedApis(List<EndpointRankDto> rateLimitedApis) {
        this.rateLimitedApis = rateLimitedApis;
    }

    public static class EndpointRankDto {
        private Long endpointId;
        private String method;
        private String normalizedPath;
        private long count;
        private double metricValue; // latency, error rate %, etc.

        public EndpointRankDto() {}

        public EndpointRankDto(Long endpointId, String method, String normalizedPath, long count, double metricValue) {
            this.endpointId = endpointId;
            this.method = method;
            this.normalizedPath = normalizedPath;
            this.count = count;
            this.metricValue = metricValue;
        }

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

        public double getMetricValue() {
            return metricValue;
        }

        public void setMetricValue(double metricValue) {
            this.metricValue = metricValue;
        }
    }
}
