package com.sentinel.api.dto;

import java.util.List;

public class ErrorAnalyticsResponse {

    private Long applicationId;
    private long totalErrors;
    private double errorRate;
    private List<ErrorSummaryDto> errorByStatusCode;
    private List<ErrorSummaryDto> errorByEndpoint;
    private PagedResponse<RequestLogResponse> errorLogs;

    public ErrorAnalyticsResponse() {}

    public ErrorAnalyticsResponse(Long applicationId, long totalErrors, double errorRate, List<ErrorSummaryDto> errorByStatusCode, List<ErrorSummaryDto> errorByEndpoint, PagedResponse<RequestLogResponse> errorLogs) {
        this.applicationId = applicationId;
        this.totalErrors = totalErrors;
        this.errorRate = errorRate;
        this.errorByStatusCode = errorByStatusCode;
        this.errorByEndpoint = errorByEndpoint;
        this.errorLogs = errorLogs;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(long totalErrors) {
        this.totalErrors = totalErrors;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public List<ErrorSummaryDto> getErrorByStatusCode() {
        return errorByStatusCode;
    }

    public void setErrorByStatusCode(List<ErrorSummaryDto> errorByStatusCode) {
        this.errorByStatusCode = errorByStatusCode;
    }

    public List<ErrorSummaryDto> getErrorByEndpoint() {
        return errorByEndpoint;
    }

    public void setErrorByEndpoint(List<ErrorSummaryDto> errorByEndpoint) {
        this.errorByEndpoint = errorByEndpoint;
    }

    public PagedResponse<RequestLogResponse> getErrorLogs() {
        return errorLogs;
    }

    public void setErrorLogs(PagedResponse<RequestLogResponse> errorLogs) {
        this.errorLogs = errorLogs;
    }

    public static class ErrorSummaryDto {
        private String key; // e.g. "502" or "GET /payments/{id}"
        private long count;
        private double percentage;

        public ErrorSummaryDto() {}

        public ErrorSummaryDto(String key, long count, double percentage) {
            this.key = key;
            this.count = count;
            this.percentage = percentage;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }
}
