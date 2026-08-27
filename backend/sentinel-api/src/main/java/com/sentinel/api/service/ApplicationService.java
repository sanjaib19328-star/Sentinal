package com.sentinel.api.service;

import com.sentinel.api.dto.ApplicationMetricsResponse;
import com.sentinel.api.dto.ApplicationResponse;
import com.sentinel.api.dto.ApplicationStatusResponse;
import com.sentinel.api.dto.ConnectionTestResponse;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.MetricItemDto;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.dto.RequestLogResponse;
import com.sentinel.api.dto.UpdateApplicationRequest;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.ApplicationMetric;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.model.ConnectionMode;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.model.MetricType;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApplicationMetricRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import com.sentinel.api.dto.ApiKeyResponse;
import com.sentinel.api.dto.ConnectAndDiscoverRequest;
import com.sentinel.api.dto.ConnectAndDiscoverResponse;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.OpenApiImportResponse;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.repository.ApiKeyRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMetricRepository applicationMetricRepository;
    private final RequestLogRepository requestLogRepository;
    private final ObservationService observationService;
    private final AuditLogService auditLogService;
    private final UpstreamAuthenticationService upstreamAuthenticationService;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;
    private final ApiCatalogService apiCatalogService;
    private final ApiDiscoveryService apiDiscoveryService;
    private final OpenApiImportService openApiImportService;

    public ApplicationService(
        ApplicationRepository applicationRepository,
        ApplicationMetricRepository applicationMetricRepository,
        RequestLogRepository requestLogRepository,
        ObservationService observationService,
        AuditLogService auditLogService,
        UpstreamAuthenticationService upstreamAuthenticationService,
        ApiKeyRepository apiKeyRepository,
        ApiKeyService apiKeyService,
        ApiCatalogService apiCatalogService,
        ApiDiscoveryService apiDiscoveryService,
        OpenApiImportService openApiImportService
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationMetricRepository = applicationMetricRepository;
        this.requestLogRepository = requestLogRepository;
        this.observationService = observationService;
        this.auditLogService = auditLogService;
        this.upstreamAuthenticationService = upstreamAuthenticationService;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyService = apiKeyService;
        this.apiCatalogService = apiCatalogService;
        this.apiDiscoveryService = apiDiscoveryService;
        this.openApiImportService = openApiImportService;
    }

    @Transactional
    public ApplicationResponse createApplication(Long ownerId, CreateApplicationRequest request) {
        String baseUrl = sanitizeUrl(request.getBaseUrl());

        Application application = new Application();
        application.setOwnerId(ownerId);
        application.setName(request.getName().trim());
        application.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        application.setBaseUrl(baseUrl);
        application.setConnectionMode(ConnectionMode.OBSERVATION);
        application.setActive(true);
        application.setHealthStatus(HealthStatus.UNKNOWN);

        Application saved = applicationRepository.save(application);

        auditLogService.record(
            ownerId,
            saved.getId(),
            AuditAction.APPLICATION_CREATED,
            "APPLICATION",
            String.valueOf(saved.getId()),
            "Created application '" + saved.getName() + "' pointing to " + saved.getBaseUrl(),
            null
        );

        if (request.getUpstreamAuth() != null) {
            upstreamAuthenticationService.configureAuthentication(ownerId, saved.getId(), request.getUpstreamAuth());
            saved = applicationRepository.findByIdAndOwnerId(saved.getId(), ownerId).orElse(saved);
        }

        return mapToResponse(saved);
    }

    public List<ApplicationResponse> listApplications(Long ownerId) {
        return applicationRepository.findAllByOwnerId(ownerId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public Application getApplicationEntity(Long ownerId, Long applicationId) {
        return applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    public ApplicationResponse getApplication(Long ownerId, Long applicationId) {
        Application application = getApplicationEntity(ownerId, applicationId);
        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse updateApplication(Long ownerId, Long applicationId, UpdateApplicationRequest request) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            application.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            application.setDescription(request.getDescription().trim());
        }
        if (request.getBaseUrl() != null && !request.getBaseUrl().isBlank()) {
            application.setBaseUrl(sanitizeUrl(request.getBaseUrl()));
        }
        if (request.getActive() != null) {
            application.setActive(request.getActive());
        }
        if (request.getConnectionMode() != null) {
            application.setConnectionMode(request.getConnectionMode());
        }

        Application updated = applicationRepository.save(application);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.APPLICATION_UPDATED,
            "APPLICATION",
            String.valueOf(updated.getId()),
            "Updated application '" + updated.getName() + "', active=" + updated.isActive(),
            null
        );

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteApplication(Long ownerId, Long applicationId) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        applicationRepository.delete(application);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.APPLICATION_DELETED,
            "APPLICATION",
            String.valueOf(applicationId),
            "Deleted application '" + application.getName() + "'",
            null
        );
    }

    public ApplicationStatusResponse getApplicationStatus(Long ownerId, Long applicationId) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        return new ApplicationStatusResponse(
            application.getId(),
            application.getHealthStatus(),
            application.getLastSeenAt(),
            application.getConnectionMode()
        );
    }

    public ConnectionTestResponse testConnection(Long ownerId, Long applicationId) {
        return upstreamAuthenticationService.testConnection(ownerId, applicationId);
    }

    public PagedResponse<RequestLogResponse> getApplicationRequests(Long ownerId, Long applicationId, Pageable pageable) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        Page<RequestLog> page = requestLogRepository.findByApplicationId(application.getId(), pageable);

        List<RequestLogResponse> items = page.getContent().stream()
            .map(log -> new RequestLogResponse(
                log.getRequestId(),
                log.getMethod(),
                log.getPath(),
                log.getStatusCode(),
                log.getLatencyMs(),
                log.getTimestamp(),
                log.getClientIp()
            ))
            .collect(Collectors.toList());

        return new PagedResponse<>(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    public ApplicationMetricsResponse getApplicationMetrics(
        Long ownerId,
        Long applicationId,
        Instant from,
        Instant to,
        MetricType metricFilter,
        Integer limit
    ) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<RequestLog> requestLogs;
        if (from != null && to != null) {
            requestLogs = requestLogRepository.findByApplicationIdAndTimestampBetween(applicationId, from, to);
        } else {
            requestLogs = requestLogRepository.findAllByApplicationIdOrderByTimestampDesc(applicationId);
        }

        List<MetricItemDto> metricItems = new ArrayList<>();

        if (!requestLogs.isEmpty()) {
            long totalRequests = requestLogs.size();
            long successRequests = requestLogs.stream()
                .filter(r -> r.getStatusCode() >= 200 && r.getStatusCode() < 400)
                .count();
            long errorRequests = requestLogs.stream()
                .filter(r -> r.getStatusCode() >= 400)
                .count();
            double avgLatency = requestLogs.stream()
                .mapToLong(RequestLog::getLatencyMs)
                .average()
                .orElse(0.0);

            Instant latestTime = requestLogs.get(0).getTimestamp();

            if (metricFilter == null || metricFilter == MetricType.REQUEST_COUNT) {
                metricItems.add(new MetricItemDto(MetricType.REQUEST_COUNT, (double) totalRequests, latestTime));
            }
            if (metricFilter == null || metricFilter == MetricType.SUCCESS_COUNT) {
                metricItems.add(new MetricItemDto(MetricType.SUCCESS_COUNT, (double) successRequests, latestTime));
            }
            if (metricFilter == null || metricFilter == MetricType.ERROR_COUNT) {
                metricItems.add(new MetricItemDto(MetricType.ERROR_COUNT, (double) errorRequests, latestTime));
            }
            if (metricFilter == null || metricFilter == MetricType.AVG_LATENCY) {
                metricItems.add(new MetricItemDto(MetricType.AVG_LATENCY, Math.round(avgLatency * 100.0) / 100.0, latestTime));
            }
        }

        // Recorded application metrics (e.g. HEALTH_CHECK or other recorded telemetry)
        List<ApplicationMetric> recordedMetrics;
        if (from != null && to != null) {
            recordedMetrics = applicationMetricRepository.findByApplicationIdAndRecordedAtBetweenOrderByRecordedAtDesc(applicationId, from, to);
        } else {
            recordedMetrics = applicationMetricRepository.findByApplicationIdOrderByRecordedAtDesc(applicationId);
        }

        for (ApplicationMetric m : recordedMetrics) {
            if (metricFilter == null || metricFilter == m.getMetricType()) {
                metricItems.add(new MetricItemDto(m.getMetricType(), m.getMetricValue(), m.getRecordedAt()));
            }
        }

        if (limit != null && limit > 0 && metricItems.size() > limit) {
            metricItems = metricItems.subList(0, limit);
        }

        return new ApplicationMetricsResponse(application.getId(), metricItems);
    }

    public ApplicationResponse mapToResponse(Application app) {
        return new ApplicationResponse(
            app.getId(),
            app.getOwnerId(),
            app.getName(),
            app.getDescription(),
            app.getBaseUrl(),
            app.getConnectionMode(),
            app.isActive(),
            app.getHealthStatus(),
            app.getLastSeenAt(),
            app.getCreatedAt(),
            app.getUpdatedAt(),
            upstreamAuthenticationService.getMaskedAuthConfig(app)
        );
    }

    @Transactional
    public ConnectAndDiscoverResponse connectAndDiscover(Long ownerId, ConnectAndDiscoverRequest request) {
        String appName = request.getApplicationName() != null ? request.getApplicationName().trim() : "App";
        String rawUrl = request.getSentinelUrl() != null ? request.getSentinelUrl().trim() : "";

        String targetBaseUrl = extractUpstreamBaseUrl(rawUrl);

        // Find existing application or create new one
        Application app = applicationRepository.findAllByOwnerId(ownerId).stream()
            .filter(a -> a.getName().equalsIgnoreCase(appName))
            .findFirst()
            .orElseGet(() -> {
                Application newApp = new Application();
                newApp.setOwnerId(ownerId);
                newApp.setName(appName);
                newApp.setBaseUrl(targetBaseUrl);
                newApp.setConnectionMode(ConnectionMode.OBSERVATION);
                newApp.setActive(true);
                newApp.setHealthStatus(HealthStatus.UNKNOWN);
                return applicationRepository.save(newApp);
            });

        // Update target URL if different
        if (targetBaseUrl != null && !targetBaseUrl.isBlank() && !targetBaseUrl.equals(app.getBaseUrl())) {
            app.setBaseUrl(targetBaseUrl);
            app = applicationRepository.save(app);
        }

        // 1. Manage the ONE Sentinel API Key for this application connection
        String returnApiKey = null;
        List<ApiKey> existingKeys = apiKeyRepository.findByApplicationId(app.getId());
        Optional<ApiKey> activeKeyOpt = existingKeys.stream().filter(ApiKey::isActive).findFirst();

        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            returnApiKey = request.getApiKey().trim();
        } else if (activeKeyOpt.isEmpty()) {
            ApiKeyResponse keyResp = apiKeyService.createApplicationApiKey(
                ownerId,
                app.getId(),
                new CreateApiKeyRequest("Sentinel Connection Key", 120, null)
            );
            returnApiKey = keyResp.getApiKey();
        } else {
            // Already has an active key
            returnApiKey = "sk_sentinel_active_key_configured";
        }

        // 2. Health check against application backend
        ConnectionTestResponse testResp = upstreamAuthenticationService.testConnection(ownerId, app.getId());
        HealthStatus healthStatus = testResp.getStatus();
        boolean isHealthy = testResp.isReachable();

        // 3. Automatic API Discovery
        OpenApiImportResponse importResult = openApiImportService.autoDiscoverAndImport(ownerId, app.getId(), app.getBaseUrl());

        // 4. If no spec is available, register verified live baseline routes
        if (importResult == null || importResult.getEndpointsImported() == 0) {
            if (isHealthy) {
                apiDiscoveryService.discoverOrUpdateEndpoint(app.getId(), "GET", "/health");
                apiDiscoveryService.discoverOrUpdateEndpoint(app.getId(), "GET", "/");
            }
        }

        // 5. Query updated API catalog
        List<com.sentinel.api.dto.ApiEndpointDto> endpoints = apiCatalogService.listApplicationEndpoints(ownerId, app.getId());
        String gatewayUrl = "http://localhost:8080/api/v1/gateway";

        String message = isHealthy
            ? "Successfully connected to " + app.getName() + " and discovered " + endpoints.size() + " APIs."
            : "Connected to " + app.getName() + " (health status: " + healthStatus + "). " + endpoints.size() + " APIs registered.";

        return new ConnectAndDiscoverResponse(
            app.getId(),
            app.getName(),
            app.getBaseUrl(),
            gatewayUrl,
            returnApiKey,
            healthStatus,
            isHealthy,
            endpoints.size(),
            endpoints,
            message
        );
    }

    private String extractUpstreamBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "http://localhost:5000";
        }
        String clean = raw.trim();
        // If user gave a full gateway path e.g. http://localhost:8080/api/v1/gateway/...
        if (clean.contains("/api/v1/gateway")) {
            // fallback to default or localhost if passed gateway url without explicit upstream
            return "http://localhost:5000";
        }
        return sanitizeUrl(clean);
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
        }
        return trimmed.replaceAll("/+$", "");
    }
}
