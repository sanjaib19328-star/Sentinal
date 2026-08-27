package com.sentinel.api.dto;

import com.sentinel.api.model.AlertRuleType;

import java.time.Instant;

public class AlertRuleDto {

    private Long id;
    private Long applicationId;
    private Long apiEndpointId;
    private AlertRuleType type;
    private double threshold;
    private int evaluationWindowSeconds;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public AlertRuleDto() {}

    public AlertRuleDto(Long id, Long applicationId, Long apiEndpointId, AlertRuleType type, double threshold, int evaluationWindowSeconds, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.apiEndpointId = apiEndpointId;
        this.type = type;
        this.threshold = threshold;
        this.evaluationWindowSeconds = evaluationWindowSeconds;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getApiEndpointId() {
        return apiEndpointId;
    }

    public void setApiEndpointId(Long apiEndpointId) {
        this.apiEndpointId = apiEndpointId;
    }

    public AlertRuleType getType() {
        return type;
    }

    public void setType(AlertRuleType type) {
        this.type = type;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getEvaluationWindowSeconds() {
        return evaluationWindowSeconds;
    }

    public void setEvaluationWindowSeconds(int evaluationWindowSeconds) {
        this.evaluationWindowSeconds = evaluationWindowSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
