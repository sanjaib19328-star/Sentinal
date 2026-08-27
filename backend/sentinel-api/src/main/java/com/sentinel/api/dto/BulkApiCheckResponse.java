package com.sentinel.api.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkApiCheckResponse {

    private Long applicationId;
    private int batchIndex;
    private int batchSize;
    private int totalBatches;
    private int totalEndpoints;
    private int completedCount;
    private int validCount;
    private int warningCount;
    private int errorCount;
    private boolean lastBatch;
    private List<BulkApiEndpointResultDto> results = new ArrayList<>();

    public BulkApiCheckResponse() {}

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public int getBatchIndex() {
        return batchIndex;
    }

    public void setBatchIndex(int batchIndex) {
        this.batchIndex = batchIndex;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(int totalBatches) {
        this.totalBatches = totalBatches;
    }

    public int getTotalEndpoints() {
        return totalEndpoints;
    }

    public void setTotalEndpoints(int totalEndpoints) {
        this.totalEndpoints = totalEndpoints;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getValidCount() {
        return validCount;
    }

    public void setValidCount(int validCount) {
        this.validCount = validCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public boolean isLastBatch() {
        return lastBatch;
    }

    public void setLastBatch(boolean lastBatch) {
        this.lastBatch = lastBatch;
    }

    public List<BulkApiEndpointResultDto> getResults() {
        return results;
    }

    public void setResults(List<BulkApiEndpointResultDto> results) {
        this.results = results;
    }
}
