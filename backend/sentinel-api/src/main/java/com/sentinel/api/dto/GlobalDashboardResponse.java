package com.sentinel.api.dto;

import java.util.List;

public class GlobalDashboardResponse {

    private long totalApplications;
    private long healthyApplications;
    private long degradedApplications;
    private long downApplications;
    private long totalRequests;
    private double requestsPerMinute;
    private double overallSuccessRate;
    private double overallErrorRate;
    private double overall429Rate;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private List<ApplicationSummaryDto> topApplications;
    private List<TrafficBreakdownResponse.EndpointRankDto> topApis;
    private List<AlertDto> activeAlerts;
    private List<RequestLogResponse> recentErrors;

    public GlobalDashboardResponse() {}

    public GlobalDashboardResponse(long totalApplications, long healthyApplications, long degradedApplications, long downApplications, long totalRequests, double requestsPerMinute, double overallSuccessRate, double overallErrorRate, double overall429Rate, double avgLatencyMs, double p95LatencyMs, List<ApplicationSummaryDto> topApplications, List<TrafficBreakdownResponse.EndpointRankDto> topApis, List<AlertDto> activeAlerts, List<RequestLogResponse> recentErrors) {
        this.totalApplications = totalApplications;
        this.healthyApplications = healthyApplications;
        this.degradedApplications = degradedApplications;
        this.downApplications = downApplications;
        this.totalRequests = totalRequests;
        this.requestsPerMinute = requestsPerMinute;
        this.overallSuccessRate = overallSuccessRate;
        this.overallErrorRate = overallErrorRate;
        this.overall429Rate = overall429Rate;
        this.avgLatencyMs = avgLatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.topApplications = topApplications;
        this.topApis = topApis;
        this.activeAlerts = activeAlerts;
        this.recentErrors = recentErrors;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getHealthyApplications() {
        return healthyApplications;
    }

    public void setHealthyApplications(long healthyApplications) {
        this.healthyApplications = healthyApplications;
    }

    public long getDegradedApplications() {
        return degradedApplications;
    }

    public void setDegradedApplications(long degradedApplications) {
        this.degradedApplications = degradedApplications;
    }

    public long getDownApplications() {
        return downApplications;
    }

    public void setDownApplications(long downApplications) {
        this.downApplications = downApplications;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public double getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(double requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public double getOverallSuccessRate() {
        return overallSuccessRate;
    }

    public void setOverallSuccessRate(double overallSuccessRate) {
        this.overallSuccessRate = overallSuccessRate;
    }

    public double getOverallErrorRate() {
        return overallErrorRate;
    }

    public void setOverallErrorRate(double overallErrorRate) {
        this.overallErrorRate = overallErrorRate;
    }

    public double getOverall429Rate() {
        return overall429Rate;
    }

    public void setOverall429Rate(double overall429Rate) {
        this.overall429Rate = overall429Rate;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public List<ApplicationSummaryDto> getTopApplications() {
        return topApplications;
    }

    public void setTopApplications(List<ApplicationSummaryDto> topApplications) {
        this.topApplications = topApplications;
    }

    public List<ApplicationSummaryDto> getApplicationSummaries() {
        return topApplications;
    }

    public void setApplicationSummaries(List<ApplicationSummaryDto> applicationSummaries) {
        this.topApplications = applicationSummaries;
    }

    public List<TrafficBreakdownResponse.EndpointRankDto> getTopApis() {
        return topApis;
    }

    public void setTopApis(List<TrafficBreakdownResponse.EndpointRankDto> topApis) {
        this.topApis = topApis;
    }

    public List<AlertDto> getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(List<AlertDto> activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public List<RequestLogResponse> getRecentErrors() {
        return recentErrors;
    }

    public void setRecentErrors(List<RequestLogResponse> recentErrors) {
        this.recentErrors = recentErrors;
    }

    public static class ApplicationSummaryDto {
        private Long id;
        private String name;
        private String healthStatus;
        private long requestCount;
        private double errorRate;
        private double avgLatencyMs;

        public ApplicationSummaryDto() {}

        public ApplicationSummaryDto(Long id, String name, String healthStatus, long requestCount, double errorRate, double avgLatencyMs) {
            this.id = id;
            this.name = name;
            this.healthStatus = healthStatus;
            this.requestCount = requestCount;
            this.errorRate = errorRate;
            this.avgLatencyMs = avgLatencyMs;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public void setHealthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public void setRequestCount(long requestCount) {
            this.requestCount = requestCount;
        }

        public long getTotalRequests() {
            return requestCount;
        }

        public void setTotalRequests(long totalRequests) {
            this.requestCount = totalRequests;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public void setErrorRate(double errorRate) {
            this.errorRate = errorRate;
        }

        public double getAvgLatencyMs() {
            return avgLatencyMs;
        }

        public void setAvgLatencyMs(double avgLatencyMs) {
            this.avgLatencyMs = avgLatencyMs;
        }
    }
}
