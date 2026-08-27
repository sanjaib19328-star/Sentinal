package com.sentinel.api.dto;

import com.sentinel.api.model.AlertRuleType;
import jakarta.validation.constraints.NotNull;

public class CreateAlertRuleRequest {

    private Long apiEndpointId;

    @NotNull(message = "Alert rule type is required")
    private AlertRuleType type;

    @NotNull(message = "Threshold is required")
    private Double threshold;

    private Integer evaluationWindowSeconds = 300;

    private Boolean enabled = true;

    public CreateAlertRuleRequest() {}

    public CreateAlertRuleRequest(Long apiEndpointId, AlertRuleType type, Double threshold, Integer evaluationWindowSeconds, Boolean enabled) {
        this.apiEndpointId = apiEndpointId;
        this.type = type;
        this.threshold = threshold;
        this.evaluationWindowSeconds = evaluationWindowSeconds;
        this.enabled = enabled;
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

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Integer getEvaluationWindowSeconds() {
        return evaluationWindowSeconds;
    }

    public void setEvaluationWindowSeconds(Integer evaluationWindowSeconds) {
        this.evaluationWindowSeconds = evaluationWindowSeconds;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
