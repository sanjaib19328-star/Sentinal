package com.sentinel.api.service;

public class RateLimitResult {

    private final boolean allowed;
    private final long limit;
    private final long remaining;
    private final long resetEpochSeconds;
    private final long retryAfterSeconds;

    public RateLimitResult(boolean allowed, long limit, long remaining, long resetEpochSeconds, long retryAfterSeconds) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.resetEpochSeconds = resetEpochSeconds;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getLimit() {
        return limit;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getResetEpochSeconds() {
        return resetEpochSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
