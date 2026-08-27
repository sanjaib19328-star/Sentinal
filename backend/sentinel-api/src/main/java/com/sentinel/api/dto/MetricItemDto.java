package com.sentinel.api.dto;

import com.sentinel.api.model.MetricType;

import java.time.Instant;

public class MetricItemDto {

    private MetricType type;
    private double value;
    private Instant recordedAt;

    public MetricItemDto() {
    }

    public MetricItemDto(MetricType type, double value, Instant recordedAt) {
        this.type = type;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
