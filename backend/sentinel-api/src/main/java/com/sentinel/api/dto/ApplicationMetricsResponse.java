package com.sentinel.api.dto;

import java.util.List;

public class ApplicationMetricsResponse {

    private Long applicationId;
    private List<MetricItemDto> metrics;

    public ApplicationMetricsResponse() {
    }

    public ApplicationMetricsResponse(Long applicationId, List<MetricItemDto> metrics) {
        this.applicationId = applicationId;
        this.metrics = metrics;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public List<MetricItemDto> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricItemDto> metrics) {
        this.metrics = metrics;
    }
}
