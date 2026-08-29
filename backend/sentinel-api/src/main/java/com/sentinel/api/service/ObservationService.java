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

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.SSLException;

@Service
public class ObservationService {

    private static final Logger log = LoggerFactory.getLogger(ObservationService.class);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);

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
            .connectTimeout(PROBE_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Observes all active registered applications across the platform.
     * When there are zero applications registered, performs ZERO external HTTP requests.
     */
    public List<ConnectionTestResponse> observeAllActiveApplications() {
        List<Application> activeApps = applicationRepository.findAllByActiveTrue();
        if (activeApps == null || activeApps.isEmpty()) {
            log.debug("Zero registered active applications found. Performing 0 observation health-check requests.");
            return Collections.emptyList();
        }

        return activeApps.stream()
            .map(this::testConnectionSafely)
            .collect(Collectors.toList());
    }

    /**
     * Observes active registered applications for a specific tenant/owner.
     * When the tenant has zero registered applications, performs ZERO external HTTP requests.
     */
    public List<ConnectionTestResponse> observeApplicationsForOwner(Long ownerId) {
        if (ownerId == null) {
            return Collections.emptyList();
        }
        List<Application> apps = applicationRepository.findAllByOwnerIdAndActiveTrue(ownerId);
        if (apps == null || apps.isEmpty()) {
            log.debug("Zero registered active applications for owner {}. Performing 0 observation requests.", ownerId);
            return Collections.emptyList();
        }

        return apps.stream()
            .map(this::testConnectionSafely)
            .collect(Collectors.toList());
    }

    /**
     * Fail-safe wrapper for testing an individual application's connection.
     * Prevents any timeout or error from propagating to other applications.
     */
    public ConnectionTestResponse testConnectionSafely(Application application) {
        try {
            return testConnection(application);
        } catch (Exception e) {
            log.warn("Observation probe execution error for app {}: {}",
                application != null ? application.getId() : "null", e.getMessage());
            Instant checkedAt = Instant.now();
            Long appId = application != null ? application.getId() : null;
            return new ConnectionTestResponse(
                appId,
                false,
                HealthStatus.UNAVAILABLE,
                null,
                null,
                "UNREACHABLE: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                checkedAt
            );
        }
    }

    @Transactional
    public ConnectionTestResponse testConnection(Application application) {
        Instant checkedAt = Instant.now();

        if (application == null) {
            return new ConnectionTestResponse(
                null,
                false,
                HealthStatus.UNKNOWN,
                null,
                null,
                "No application specified",
                checkedAt
            );
        }

        String baseUrl = application.getBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            application.setHealthStatus(HealthStatus.UNKNOWN);
            applicationRepository.save(application);
            return new ConnectionTestResponse(
                application.getId(),
                false,
                HealthStatus.UNKNOWN,
                null,
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
        if (targetBase.endsWith("/api/v1/health") || targetBase.endsWith("/actuator/health") || targetBase.endsWith("/health") || targetBase.endsWith("/ping") || targetBase.endsWith("/status")) {
            probeCandidates = List.of(targetBase);
        } else {
            probeCandidates = List.of(
                targetBase + "/api/v1/health",
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
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .header("User-Agent", "Sentinel-Observation-Agent/1.0")
                    .build();

                long startTime = System.currentTimeMillis();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                latencyMs = System.currentTimeMillis() - startTime;
                lastResponse = response;

                // Stop immediately on success
                if (response.statusCode() >= 200 && response.statusCode() < 400) {
                    break;
                }
            } catch (Exception ex) {
                lastException = ex;
                // Fail-fast: If host connection timed out or is completely unreachable, do NOT retry further paths on the dead host
                if (ex instanceof HttpTimeoutException || ex instanceof ConnectException || ex instanceof UnknownHostException || ex instanceof NoRouteToHostException || ex instanceof SSLException) {
                    log.debug("Host level connection failure on {} ({}), skipping remaining candidate probes.", probeUrl, ex.getClass().getSimpleName());
                    break;
                }
            }
        }

        if (lastResponse != null) {
            int statusCode = lastResponse.statusCode();
            HealthStatus healthStatus;
            String message;

            if (statusCode >= 200 && statusCode < 400) {
                healthStatus = (latencyMs <= 1000) ? HealthStatus.HEALTHY : HealthStatus.DEGRADED;
                message = "HEALTHY: Reachable HTTP " + statusCode + " (" + latencyMs + "ms)";
            } else {
                healthStatus = HealthStatus.DEGRADED;
                message = "HTTP_ERROR: Reachable with non-success status HTTP " + statusCode + " (" + latencyMs + "ms)";
            }

            application.setHealthStatus(healthStatus);
            application.setLastSeenAt(checkedAt);
            applicationRepository.save(application);

            try {
                ApplicationMetric metric = new ApplicationMetric(
                    application.getId(),
                    MetricType.HEALTH_CHECK,
                    (double) latencyMs,
                    checkedAt
                );
                applicationMetricRepository.save(metric);
            } catch (Exception metricEx) {
                log.warn("Could not record health metric for application {}: {}", application.getId(), metricEx.getMessage());
            }

            return new ConnectionTestResponse(
                application.getId(),
                statusCode >= 200 && statusCode < 400,
                healthStatus,
                statusCode,
                latencyMs,
                message,
                checkedAt
            );
        } else {
            String failureCategory = classifyException(lastException);
            String message = failureCategory + ": " + (lastException != null ? lastException.getMessage() : "Unreachable");

            log.info("Observation connection check failed for app {}: {}", application.getId(), message);

            application.setHealthStatus(HealthStatus.UNAVAILABLE);
            applicationRepository.save(application);

            return new ConnectionTestResponse(
                application.getId(),
                false,
                HealthStatus.UNAVAILABLE,
                null,
                null,
                message,
                checkedAt
            );
        }
    }

    private String classifyException(Exception ex) {
        if (ex == null) {
            return "UNREACHABLE";
        }
        if (ex instanceof HttpTimeoutException || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timed out"))) {
            return "TIMEOUT";
        }
        if (ex instanceof ConnectException || ex instanceof UnknownHostException || ex instanceof NoRouteToHostException) {
            return "UNREACHABLE";
        }
        if (ex instanceof SSLException) {
            return "SSL_ERROR";
        }
        return "UNREACHABLE";
    }
}
