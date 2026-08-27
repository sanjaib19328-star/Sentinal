package com.sentinel.api.service;

import com.sentinel.api.dto.ConsumerKeyAnalyticsDto;
import com.sentinel.api.dto.TopEndpointMetricDto;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConsumerAnalyticsService {

    private final RequestLogRepository requestLogRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;

    public ConsumerAnalyticsService(
        RequestLogRepository requestLogRepository,
        ApiKeyRepository apiKeyRepository,
        ApplicationRepository applicationRepository
    ) {
        this.requestLogRepository = requestLogRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
    }

    public ConsumerKeyAnalyticsDto getKeyAnalytics(Long ownerId, Long applicationId, Long apiKeyId, Instant from, Instant to) {
        verifyApplicationOwnership(ownerId, applicationId);
        ApiKey key = apiKeyRepository.findById(apiKeyId)
            .filter(k -> k.getApplicationId() != null && k.getApplicationId().equals(applicationId))
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found for application"));

        List<RequestLog> logs;
        if (from != null && to != null) {
            logs = requestLogRepository.findByApiKeyIdAndTimestampBetweenOrderByTimestampDesc(apiKeyId, from, to);
        } else {
            logs = requestLogRepository.findByApiKeyIdOrderByTimestampDesc(apiKeyId);
        }

        return buildKeyAnalytics(key, logs);
    }

    public List<ConsumerKeyAnalyticsDto> getApplicationConsumers(Long ownerId, Long applicationId) {
        verifyApplicationOwnership(ownerId, applicationId);
        List<ApiKey> keys = apiKeyRepository.findByApplicationId(applicationId);

        List<ConsumerKeyAnalyticsDto> result = new ArrayList<>();
        for (ApiKey key : keys) {
            List<RequestLog> logs = requestLogRepository.findByApiKeyIdOrderByTimestampDesc(key.getId());
            result.add(buildKeyAnalytics(key, logs));
        }

        result.sort((a, b) -> Long.compare(b.getTotalRequests(), a.getTotalRequests()));
        return result;
    }

    public List<ConsumerKeyAnalyticsDto> getGlobalTopConsumers(Long ownerId, int limit) {
        List<Application> apps = applicationRepository.findByOwnerId(ownerId);
        List<ConsumerKeyAnalyticsDto> allConsumers = new ArrayList<>();

        for (Application app : apps) {
            List<ApiKey> keys = apiKeyRepository.findByApplicationId(app.getId());
            for (ApiKey key : keys) {
                List<RequestLog> logs = requestLogRepository.findByApiKeyIdOrderByTimestampDesc(key.getId());
                if (!logs.isEmpty()) {
                    allConsumers.add(buildKeyAnalytics(key, logs));
                }
            }
        }

        allConsumers.sort((a, b) -> Long.compare(b.getTotalRequests(), a.getTotalRequests()));
        return allConsumers.stream().limit(Math.max(1, limit)).collect(Collectors.toList());
    }

    private ConsumerKeyAnalyticsDto buildKeyAnalytics(ApiKey key, List<RequestLog> logs) {
        long total = logs.size();
        if (total == 0) {
            return new ConsumerKeyAnalyticsDto(
                key.getId(),
                key.getName(),
                "sk_sentinel_••••••••" + key.getId(),
                key.isActive(),
                key.getRateLimitPerMinute(),
                0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0,
                null,
                Collections.emptyList()
            );
        }

        long success = 0;
        long errors = 0;
        long c4xx = 0;
        long c5xx = 0;
        long c429 = 0;
        long totalLatency = 0;
        List<Long> latencies = new ArrayList<>();

        Instant lastUsed = null;

        for (RequestLog r : logs) {
            if (lastUsed == null || r.getTimestamp().isAfter(lastUsed)) {
                lastUsed = r.getTimestamp();
            }

            int sc = r.getStatusCode();
            if (sc >= 200 && sc < 400) {
                success++;
            } else {
                errors++;
                if (sc >= 400 && sc < 500) c4xx++;
                if (sc >= 500) c5xx++;
                if (sc == 429) c429++;
            }

            totalLatency += r.getLatencyMs();
            latencies.add(r.getLatencyMs());
        }

        double errorRate = Math.round(((double) errors / total) * 1000.0) / 10.0;
        double avgLatency = Math.round(((double) totalLatency / total) * 10.0) / 10.0;

        Collections.sort(latencies);
        double p50 = calculatePercentile(latencies, 50);
        double p95 = calculatePercentile(latencies, 95);
        double p99 = calculatePercentile(latencies, 99);

        // Group top endpoints
        Map<String, List<RequestLog>> byEndpoint = logs.stream()
            .collect(Collectors.groupingBy(r -> r.getMethod() + " " + (r.getNormalizedPath() != null ? r.getNormalizedPath() : r.getPath())));

        List<TopEndpointMetricDto> topEndpoints = byEndpoint.entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split(" ", 2);
                String m = parts[0];
                String path = parts.length > 1 ? parts[1] : "/";
                long count = entry.getValue().size();
                double avgLat = entry.getValue().stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);
                return new TopEndpointMetricDto(m, path, count, Math.round(avgLat * 10.0) / 10.0);
            })
            .sorted(Comparator.comparingLong(TopEndpointMetricDto::getCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        return new ConsumerKeyAnalyticsDto(
            key.getId(),
            key.getName(),
            "sk_sentinel_••••••••" + key.getId(),
            key.isActive(),
            key.getRateLimitPerMinute(),
            total,
            success,
            errors,
            c4xx,
            c5xx,
            c429,
            errorRate,
            avgLatency,
            p50,
            p95,
            p99,
            lastUsed,
            topEndpoints
        );
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

    private Application verifyApplicationOwnership(Long ownerId, Long applicationId) {
        return applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }
}
