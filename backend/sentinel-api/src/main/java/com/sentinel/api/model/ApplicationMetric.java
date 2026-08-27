package com.sentinel.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "application_metrics",
    indexes = {
        @Index(name = "idx_app_metrics_app_id", columnList = "application_id"),
        @Index(name = "idx_app_metrics_recorded_at", columnList = "recorded_at")
    }
)
public class ApplicationMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @Column(name = "metric_value", nullable = false)
    private double metricValue;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public ApplicationMetric() {
    }

    public ApplicationMetric(Long applicationId, MetricType metricType, double metricValue) {
        this.applicationId = applicationId;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.recordedAt = Instant.now();
    }

    public ApplicationMetric(Long applicationId, MetricType metricType, double metricValue, Instant recordedAt) {
        this.applicationId = applicationId;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.recordedAt = recordedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
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

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
