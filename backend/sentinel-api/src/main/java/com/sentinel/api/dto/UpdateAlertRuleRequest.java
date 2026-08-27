package com.sentinel.api.dto;

import com.sentinel.api.model.AlertRuleType;

public class UpdateAlertRuleRequest {

    private AlertRuleType type;
    private Double threshold;
    private Integer evaluationWindowSeconds;
    private Boolean enabled;

    public UpdateAlertRuleRequest() {}

    public UpdateAlertRuleRequest(AlertRuleType type, Double threshold, Integer evaluationWindowSeconds, Boolean enabled) {
        this.type = type;
        this.threshold = threshold;
        this.evaluationWindowSeconds = evaluationWindowSeconds;
        this.enabled = enabled;
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
