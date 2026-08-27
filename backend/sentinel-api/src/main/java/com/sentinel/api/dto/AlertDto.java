package com.sentinel.api.dto;

import com.sentinel.api.model.AlertSeverity;
import com.sentinel.api.model.AlertStatus;

import java.time.Instant;

public class AlertDto {

    private Long id;
    private Long alertRuleId;
    private Long applicationId;
    private Long apiEndpointId;
    private AlertStatus status;
    private AlertSeverity severity;
    private String message;
    private Instant triggeredAt;
    private Instant resolvedAt;

    public AlertDto() {}

    public AlertDto(Long id, Long alertRuleId, Long applicationId, Long apiEndpointId, AlertStatus status, AlertSeverity severity, String message, Instant triggeredAt, Instant resolvedAt) {
        this.id = id;
        this.alertRuleId = alertRuleId;
        this.applicationId = applicationId;
        this.apiEndpointId = apiEndpointId;
        this.status = status;
        this.severity = severity;
        this.message = message;
        this.triggeredAt = triggeredAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlertRuleId() {
        return alertRuleId;
    }

    public void setAlertRuleId(Long alertRuleId) {
        this.alertRuleId = alertRuleId;
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

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
