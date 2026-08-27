package com.sentinel.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_rule_id", nullable = false)
    private Long alertRuleId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "api_endpoint_id")
    private Long apiEndpointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus status = AlertStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertSeverity severity = AlertSeverity.WARNING;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private Instant triggeredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Alert() {}

    public Alert(Long alertRuleId, Long applicationId, Long apiEndpointId, AlertSeverity severity, String message) {
        this.alertRuleId = alertRuleId;
        this.applicationId = applicationId;
        this.apiEndpointId = apiEndpointId;
        this.severity = severity;
        this.message = message;
        this.status = AlertStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.triggeredAt = Instant.now();
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
