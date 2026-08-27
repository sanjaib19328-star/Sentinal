package com.sentinel.api.service;

import com.sentinel.api.dto.CircuitBreakerStatusDto;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.CircuitState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerServiceTest {

    private InMemoryCircuitBreakerService circuitBreakerService;
    private ApiPolicy policy;
    private final Long appId = 9999L;

    @BeforeEach
    void setUp() {
        circuitBreakerService = new InMemoryCircuitBreakerService();
        policy = new ApiPolicy();
        policy.setCircuitBreakerEnabled(true);
        policy.setCircuitFailureThreshold(3);
        policy.setCircuitRecoveryTimeoutSeconds(2);
    }

    @Test
    void testInitialStateIsClosedAndAllowsRequests() {
        assertTrue(circuitBreakerService.allowRequest(appId, policy));
        CircuitBreakerStatusDto status = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.CLOSED, status.getState());
        assertEquals(0, status.getConsecutiveFailures());
        assertEquals(3, status.getFailureThreshold());
    }

    @Test
    void testFailuresBelowThresholdRemainClosed() {
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);

        assertTrue(circuitBreakerService.allowRequest(appId, policy));
        CircuitBreakerStatusDto status = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.CLOSED, status.getState());
        assertEquals(2, status.getConsecutiveFailures());
    }

    @Test
    void testTrippingCircuitToOpenAfterThreshold() {
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy); // 3rd failure = trips

        assertFalse(circuitBreakerService.allowRequest(appId, policy));
        CircuitBreakerStatusDto status = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.OPEN, status.getState());
        assertEquals(3, status.getConsecutiveFailures());
    }

    @Test
    void testSuccessResetsFailureCounter() {
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordSuccess(appId);

        CircuitBreakerStatusDto status = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.CLOSED, status.getState());
        assertEquals(0, status.getConsecutiveFailures());
    }

    @Test
    void testTransitionToHalfOpenAndRecovery() throws InterruptedException {
        // Trip circuit
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        assertFalse(circuitBreakerService.allowRequest(appId, policy));

        // Wait for 2s recovery timeout
        Thread.sleep(2100);

        // Next request transitions to HALF_OPEN and is allowed
        assertTrue(circuitBreakerService.allowRequest(appId, policy));
        CircuitBreakerStatusDto halfOpenStatus = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.HALF_OPEN, halfOpenStatus.getState());

        // Successful trial request recovers circuit to CLOSED
        circuitBreakerService.recordSuccess(appId);
        CircuitBreakerStatusDto closedStatus = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.CLOSED, closedStatus.getState());
        assertEquals(0, closedStatus.getConsecutiveFailures());
    }

    @Test
    void testHalfOpenFailureImmediatelyReOpensCircuit() throws InterruptedException {
        // Trip circuit
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);

        // Wait for recovery timeout
        Thread.sleep(2100);
        assertTrue(circuitBreakerService.allowRequest(appId, policy)); // Enters HALF_OPEN

        // Trial request fails
        circuitBreakerService.recordFailure(appId, policy);

        CircuitBreakerStatusDto status = circuitBreakerService.getStatus(appId, policy);
        assertEquals(CircuitState.OPEN, status.getState());
        assertFalse(circuitBreakerService.allowRequest(appId, policy));
    }

    @Test
    void testCircuitBreakerDisabledAllowsAllRequests() {
        policy.setCircuitBreakerEnabled(false);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);
        circuitBreakerService.recordFailure(appId, policy);

        assertTrue(circuitBreakerService.allowRequest(appId, policy));
    }
}
