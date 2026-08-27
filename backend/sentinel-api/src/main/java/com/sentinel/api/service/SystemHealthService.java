package com.sentinel.api.service;

import com.sentinel.api.dto.CircuitBreakerStatusDto;
import com.sentinel.api.dto.SystemHealthResponse;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SystemHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationRepository applicationRepository;
    private final RequestLogRepository requestLogRepository;
    private final CircuitBreakerService circuitBreakerService;
    private final ApiPolicyService apiPolicyService;

    public SystemHealthService(
        JdbcTemplate jdbcTemplate,
        StringRedisTemplate redisTemplate,
        ApplicationRepository applicationRepository,
        RequestLogRepository requestLogRepository,
        CircuitBreakerService circuitBreakerService,
        ApiPolicyService apiPolicyService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.applicationRepository = applicationRepository;
        this.requestLogRepository = requestLogRepository;
        this.circuitBreakerService = circuitBreakerService;
        this.apiPolicyService = apiPolicyService;
    }

    public SystemHealthResponse getSystemHealth(Long ownerId) {
        // 1. MySQL Health & Ping
        SystemHealthResponse.DatabaseHealth dbHealth = checkDatabaseHealth();

        // 2. Redis Health & Ping
        SystemHealthResponse.CacheHealth cacheHealth = checkRedisHealth();

        // 3. Gateway Telemetry Summary
        List<Application> apps = applicationRepository.findByOwnerId(ownerId);
        List<RequestLog> allLogs = new ArrayList<>();
        List<SystemHealthResponse.ApplicationHealthDetail> appDetails = new ArrayList<>();

        for (Application app : apps) {
            List<RequestLog> logs = requestLogRepository.findAllByApplicationIdOrderByTimestampDesc(app.getId());
            allLogs.addAll(logs);

            ApiPolicy appPolicy = apiPolicyService.findAppPolicy(app.getId()).orElse(null);
            CircuitBreakerStatusDto cbStatus = circuitBreakerService.getStatus(app.getId(), appPolicy);

            appDetails.add(new SystemHealthResponse.ApplicationHealthDetail(
                app.getId(),
                app.getName(),
                app.getBaseUrl(),
                app.getHealthStatus() != null ? app.getHealthStatus().name() : "UNKNOWN",
                cbStatus.getState().name(),
                cbStatus.getConsecutiveFailures(),
                cbStatus.getTimeUntilRecoverySeconds()
            ));
        }

        SystemHealthResponse.GatewayHealthSummary gatewaySummary = calculateGatewaySummary(allLogs);

        return new SystemHealthResponse(
            "UP",
            dbHealth,
            cacheHealth,
            gatewaySummary,
            appDetails
        );
    }

    private SystemHealthResponse.DatabaseHealth checkDatabaseHealth() {
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long latency = System.currentTimeMillis() - start;
            return new SystemHealthResponse.DatabaseHealth("UP", latency);
        } catch (Exception e) {
            return new SystemHealthResponse.DatabaseHealth("DOWN", -1.0);
        }
    }

    private SystemHealthResponse.CacheHealth checkRedisHealth() {
        long start = System.currentTimeMillis();
        try {
            var conn = redisTemplate.getConnectionFactory() != null ? redisTemplate.getConnectionFactory().getConnection() : null;
            if (conn != null) {
                conn.ping();
                conn.close();
                long latency = System.currentTimeMillis() - start;
                return new SystemHealthResponse.CacheHealth("UP", latency);
            }
            return new SystemHealthResponse.CacheHealth("DOWN", -1.0);
        } catch (Exception e) {
            return new SystemHealthResponse.CacheHealth("DOWN", -1.0);
        }
    }

    private SystemHealthResponse.GatewayHealthSummary calculateGatewaySummary(List<RequestLog> logs) {
        long total = logs.size();
        if (total == 0) {
            return new SystemHealthResponse.GatewayHealthSummary(0, 0.0, 0.0, 0.0);
        }

        long errors = 0;
        long totalLatency = 0;
        List<Long> latencies = new ArrayList<>();

        for (RequestLog r : logs) {
            if (r.getStatusCode() >= 400) {
                errors++;
            }
            totalLatency += r.getLatencyMs();
            latencies.add(r.getLatencyMs());
        }

        double errorRate = Math.round(((double) errors / total) * 1000.0) / 10.0;
        double avgLatency = Math.round(((double) totalLatency / total) * 10.0) / 10.0;

        Collections.sort(latencies);
        double p95 = calculatePercentile(latencies, 95);

        return new SystemHealthResponse.GatewayHealthSummary(total, errorRate, avgLatency, p95);
    }

    private double calculatePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) return 0.0;
        if (sortedValues.size() == 1) return sortedValues.get(0);

        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sortedValues.get(lower);

        double weight = index - lower;
        return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
    }
}
