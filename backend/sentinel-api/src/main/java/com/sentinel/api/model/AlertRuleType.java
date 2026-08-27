package com.sentinel.api.model;

public enum AlertRuleType {
    HIGH_ERROR_RATE,
    HIGH_LATENCY,
    API_UNAVAILABLE,
    EXCESSIVE_429,
    QUOTA_APPROACHING
}
