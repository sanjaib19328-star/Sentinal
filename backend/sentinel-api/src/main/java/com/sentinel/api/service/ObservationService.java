package com.sentinel.api.service;

import com.sentinel.api.dto.ConnectionTestResponse;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.ApplicationMetric;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.model.MetricType;
import com.sentinel.api.repository.ApplicationMetricRepository;
import com.sentinel.api.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ObservationService {

    private static final Logger log = LoggerFactory.getLogger(ObservationService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final HttpClient httpClient;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMetricRepository applicationMetricRepository;

    public ObservationService(
        ApplicationRepository applicationRepository,
        ApplicationMetricRepository applicationMetricRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationMetricRepository = applicationMetricRepository;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Transactional
    public ConnectionTestResponse testConnection(Application application) {
        String baseUrl = application.getBaseUrl();
        Instant checkedAt = Instant.now();

        if (baseUrl == null || baseUrl.isBlank()) {
            application.setHealthStatus(HealthStatus.UNKNOWN);
            applicationRepository.save(application);
            return new ConnectionTestResponse(
                application.getId(),
                false,
                HealthStatus.UNKNOWN,
                null,
                "No base URL configured for application",
                checkedAt
            );
        }

        String targetBase = baseUrl.trim();
        if (!targetBase.startsWith("http://") && !targetBase.startsWith("https://")) {
            targetBase = "http://" + targetBase;
        }
        targetBase = targetBase.replaceAll("/+$", "");

        // Candidate health probe paths to check
        List<String> probeCandidates;
        if (targetBase.endsWith("/actuator/health") || targetBase.endsWith("/health")) {
            probeCandidates = List.of(targetBase);
        } else {
            probeCandidates = List.of(
                targetBase + "/actuator/health",
                targetBase + "/health",
                targetBase
            );
        }

        HttpResponse<Void> lastResponse = null;
        long latencyMs = 0;
        Exception lastException = null;

        for (String probeUrl : probeCandidates) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(probeUrl))
                    .timeout(TIMEOUT)
                    .GET()
                    .header("User-Agent", "Sentinel-Observation-Agent/1.0")
                    .build();

                long startTime = System.currentTimeMillis();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                latencyMs = System.currentTimeMillis() - startTime;
                lastResponse = response;

                if (response.statusCode() >= 200 && response.statusCode() < 400) {
                    break;
                }
            } catch (Exception ex) {
                lastException = ex;
            }
        }

        if (lastResponse != null) {
            int statusCode = lastResponse.statusCode();
            HealthStatus healthStatus;
            String message;

            if (statusCode >= 200 && statusCode < 400) {
                healthStatus = (latencyMs <= 1000) ? HealthStatus.HEALTHY : HealthStatus.DEGRADED;
                message = "Reachable: HTTP " + statusCode + " (" + latencyMs + "ms)";
            } else {
                healthStatus = HealthStatus.DEGRADED;
                message = "Reachable with non-success status: HTTP " + statusCode + " (" + latencyMs + "ms)";
            }

            application.setHealthStatus(healthStatus);
            application.setLastSeenAt(checkedAt);
            applicationRepository.save(application);

            ApplicationMetric metric = new ApplicationMetric(
                application.getId(),
                MetricType.HEALTH_CHECK,
                (double) latencyMs,
                checkedAt
            );
            applicationMetricRepository.save(metric);

            return new ConnectionTestResponse(
                application.getId(),
                true,
                healthStatus,
                latencyMs,
                message,
                checkedAt
            );
        } else {
            log.info("Observation connection check failed for app {}: {}", application.getId(),
                lastException != null ? lastException.getMessage() : "Unknown");

            application.setHealthStatus(HealthStatus.UNAVAILABLE);
            applicationRepository.save(application);

            return new ConnectionTestResponse(
                application.getId(),
                false,
                HealthStatus.UNAVAILABLE,
                null,
                "Connection failed: " + (lastException != null ? lastException.getClass().getSimpleName() : "Unreachable"),
                checkedAt
            );
        }
    }
}
