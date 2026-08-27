package com.sentinel.api.service;

import com.sentinel.api.dto.CircuitBreakerStatusDto;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.CircuitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class InMemoryCircuitBreakerService implements CircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCircuitBreakerService.class);

    private static class CircuitContext {
        final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
        final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        final AtomicReference<Instant> lastStateChange = new AtomicReference<>(Instant.now());
        final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(Instant.now());
    }

    private final ConcurrentHashMap<Long, CircuitContext> circuits = new ConcurrentHashMap<>();

    private CircuitContext getContext(Long appId) {
        return circuits.computeIfAbsent(appId, k -> new CircuitContext());
    }

    @Override
    public boolean allowRequest(Long applicationId, ApiPolicy policy) {
        if (applicationId == null || policy == null || !policy.isCircuitBreakerEnabled()) {
            return true;
        }

        CircuitContext ctx = getContext(applicationId);
        CircuitState currentState = ctx.state.get();

        if (currentState == CircuitState.CLOSED) {
            return true;
        }

        int recoveryTimeout = policy.getCircuitRecoveryTimeoutSeconds() > 0 ? policy.getCircuitRecoveryTimeoutSeconds() : 15;
        Instant stateChangedAt = ctx.lastStateChange.get();
        Instant now = Instant.now();
        long elapsedSeconds = Duration.between(stateChangedAt, now).getSeconds();

        if (currentState == CircuitState.OPEN) {
            if (elapsedSeconds >= recoveryTimeout) {
                // Transition to HALF_OPEN to allow trial request
                if (ctx.state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    ctx.lastStateChange.set(now);
                    log.info("Circuit breaker for appId {} transitioned from OPEN to HALF_OPEN (probing target)", applicationId);
                    return true;
                }
            }
            return false;
        }

        // HALF_OPEN allows trial traffic
        return true;
    }

    @Override
    public void recordSuccess(Long applicationId) {
        if (applicationId == null) return;
        CircuitContext ctx = getContext(applicationId);
        CircuitState prev = ctx.state.getAndSet(CircuitState.CLOSED);
        ctx.consecutiveFailures.set(0);
        if (prev != CircuitState.CLOSED) {
            ctx.lastStateChange.set(Instant.now());
            log.info("Circuit breaker for appId {} recovered and transitioned to CLOSED", applicationId);
        }
    }

    @Override
    public void recordFailure(Long applicationId, ApiPolicy policy) {
        if (applicationId == null) return;
        boolean enabled = policy == null || policy.isCircuitBreakerEnabled();
        if (!enabled) return;

        int threshold = (policy != null && policy.getCircuitFailureThreshold() > 0) ? policy.getCircuitFailureThreshold() : 5;
        CircuitContext ctx = getContext(applicationId);
        ctx.lastFailureTime.set(Instant.now());
        int failures = ctx.consecutiveFailures.incrementAndGet();

        if (ctx.state.get() == CircuitState.HALF_OPEN) {
            // Half-open attempt failed -> Immediately re-open circuit
            ctx.state.set(CircuitState.OPEN);
            ctx.lastStateChange.set(Instant.now());
            log.warn("Trial request failed for appId {}. Circuit breaker reverted to OPEN", applicationId);
        } else if (failures >= threshold && ctx.state.compareAndSet(CircuitState.CLOSED, CircuitState.OPEN)) {
            ctx.lastStateChange.set(Instant.now());
            log.warn("Circuit breaker TRIPPED to OPEN for appId {} after {} consecutive failures (threshold: {})", applicationId, failures, threshold);
        }
    }

    @Override
    public CircuitBreakerStatusDto getStatus(Long applicationId, ApiPolicy policy) {
        boolean enabled = policy == null || policy.isCircuitBreakerEnabled();
        int threshold = (policy != null && policy.getCircuitFailureThreshold() > 0) ? policy.getCircuitFailureThreshold() : 5;
        int recoveryTimeout = (policy != null && policy.getCircuitRecoveryTimeoutSeconds() > 0) ? policy.getCircuitRecoveryTimeoutSeconds() : 15;

        if (applicationId == null) {
            return new CircuitBreakerStatusDto(null, CircuitState.CLOSED, 0, threshold, 0, Instant.now(), enabled);
        }

        CircuitContext ctx = getContext(applicationId);
        CircuitState state = ctx.state.get();
        Instant lastChange = ctx.lastStateChange.get();

        long timeUntilRecovery = 0;
        if (state == CircuitState.OPEN) {
            long elapsed = Duration.between(lastChange, Instant.now()).getSeconds();
            timeUntilRecovery = Math.max(0, recoveryTimeout - elapsed);
            if (timeUntilRecovery == 0) {
                // Auto-transition to HALF_OPEN if checked after timeout
                if (ctx.state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    state = CircuitState.HALF_OPEN;
                    ctx.lastStateChange.set(Instant.now());
                    lastChange = ctx.lastStateChange.get();
                }
            }
        }

        return new CircuitBreakerStatusDto(
            applicationId,
            state,
            ctx.consecutiveFailures.get(),
            threshold,
            timeUntilRecovery,
            lastChange,
            enabled
        );
    }

    @Override
    public void reset(Long applicationId) {
        if (applicationId == null) return;
        CircuitContext ctx = getContext(applicationId);
        ctx.state.set(CircuitState.CLOSED);
        ctx.consecutiveFailures.set(0);
        ctx.lastStateChange.set(Instant.now());
    }
}
