package com.sentinel.api.service;

import com.sentinel.api.dto.CircuitBreakerStatusDto;
import com.sentinel.api.model.ApiPolicy;

public interface CircuitBreakerService {

    /**
     * Determines whether the gateway should permit a request to proceed to the target application.
     * If the circuit is OPEN and recovery timeout has not elapsed, returns false (fast fail).
     * If the recovery timeout has elapsed, transitions state to HALF_OPEN and returns true for a trial request.
     * If CLOSED, returns true.
     */
    boolean allowRequest(Long applicationId, ApiPolicy policy);

    /**
     * Records a successful response from the target application.
     * Resets failure counter and if in HALF_OPEN state, transitions circuit back to CLOSED.
     */
    void recordSuccess(Long applicationId);

    /**
     * Records a failed target attempt (connection refused, connect timeout, read timeout, 502, 503, 504).
     * Increments failure counter and trips circuit to OPEN if threshold reached.
     */
    void recordFailure(Long applicationId, ApiPolicy policy);

    /**
     * Returns current circuit breaker status for telemetry and monitoring.
     */
    CircuitBreakerStatusDto getStatus(Long applicationId, ApiPolicy policy);

    /**
     * Explicitly resets circuit breaker state back to CLOSED.
     */
    void reset(Long applicationId);
}
