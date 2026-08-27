package com.sentinel.api.dto;

import java.util.List;

public class BulkApiCheckRequest {

    private Long applicationId;
    private List<Long> endpointIds;
    private int batchIndex = 0;
    private int batchSize = 20;

    public BulkApiCheckRequest() {}

    public BulkApiCheckRequest(Long applicationId, List<Long> endpointIds, int batchIndex, int batchSize) {
        this.applicationId = applicationId;
        this.endpointIds = endpointIds;
        this.batchIndex = batchIndex;
        this.batchSize = batchSize;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public List<Long> getEndpointIds() {
        return endpointIds;
    }

    public void setEndpointIds(List<Long> endpointIds) {
        this.endpointIds = endpointIds;
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
}
