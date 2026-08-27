package com.sentinel.api.dto;

public class RateLimitResult {

    private final boolean allowed;
    private final long limit;
    private final long remaining;
    private final long resetEpochSeconds;
    private final long retryAfterSeconds;
    private final String throttledBy; // "API_KEY", "APPLICATION_POLICY", "ENDPOINT_POLICY", "QUOTA"

    public RateLimitResult(boolean allowed, long limit, long remaining, long resetEpochSeconds, long retryAfterSeconds) {
        this(allowed, limit, remaining, resetEpochSeconds, retryAfterSeconds, null);
    }

    public RateLimitResult(boolean allowed, long limit, long remaining, long resetEpochSeconds, long retryAfterSeconds, String throttledBy) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.resetEpochSeconds = resetEpochSeconds;
        this.retryAfterSeconds = retryAfterSeconds;
        this.throttledBy = throttledBy;
    }

    public static RateLimitResult allow(long limit, long remaining, long resetEpochSeconds) {
        return new RateLimitResult(true, limit, remaining, resetEpochSeconds, 0, null);
    }

    public static RateLimitResult deny(long limit, long resetEpochSeconds, long retryAfterSeconds, String throttledBy) {
        return new RateLimitResult(false, limit, 0, resetEpochSeconds, retryAfterSeconds, throttledBy);
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

    public String getThrottledBy() {
        return throttledBy;
    }
}
