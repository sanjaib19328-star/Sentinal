package com.sentinel.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.ApiEndpointDto;
import com.sentinel.api.dto.ApplicationMetricsResponse;
import com.sentinel.api.dto.ApplicationResponse;
import com.sentinel.api.dto.ApplicationStatusResponse;
import com.sentinel.api.dto.GlobalDashboardResponse;
import com.sentinel.api.dto.SystemHealthResponse;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;
    private final SystemHealthService systemHealthService;
    private final AnalyticsService analyticsService;
    private final ApiCatalogService apiCatalogService;
    private final RequestLogRepository requestLogRepository;

    public ToolExecutionService(
        ApplicationService applicationService,
        ApplicationRepository applicationRepository,
        SystemHealthService systemHealthService,
        AnalyticsService analyticsService,
        ApiCatalogService apiCatalogService,
        RequestLogRepository requestLogRepository
    ) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.systemHealthService = systemHealthService;
        this.analyticsService = analyticsService;
        this.apiCatalogService = apiCatalogService;
        this.requestLogRepository = requestLogRepository;
    }

    public String executeTool(String toolName, Map<String, Object> arguments, Long userId, Long currentAppId) {
        try {
            switch (toolName) {
                case "list_applications":
                    return MAPPER.writeValueAsString(listApplications(userId));

                case "get_application_health": {
                    Long appId = extractAppId(arguments, currentAppId);
                    if (appId == null) return "{\"error\":\"applicationId is required\"}";
                    return MAPPER.writeValueAsString(getApplicationHealth(userId, appId));
                }

                case "get_application_metrics": {
                    Long appId = extractAppId(arguments, currentAppId);
                    if (appId == null) return "{\"error\":\"applicationId is required\"}";
                    String timeRange = arguments.containsKey("timeRange") ? String.valueOf(arguments.get("timeRange")) : "24h";
                    return MAPPER.writeValueAsString(getApplicationMetrics(userId, appId, timeRange));
                }

                case "get_request_logs": {
                    Long appId = extractAppId(arguments, currentAppId);
                    int limit = arguments.containsKey("limit") ? Integer.parseInt(String.valueOf(arguments.get("limit"))) : 20;
                    Integer minStatus = arguments.containsKey("minStatusCode") ? Integer.parseInt(String.valueOf(arguments.get("minStatusCode"))) : null;
                    return MAPPER.writeValueAsString(getRecentRequestLogs(userId, appId, limit, minStatus));
                }

                case "get_api_catalog": {
                    Long appId = extractAppId(arguments, currentAppId);
                    if (appId == null) return "{\"error\":\"applicationId is required\"}";
                    return MAPPER.writeValueAsString(getApiCatalog(userId, appId));
                }

                case "get_system_overview":
                    return MAPPER.writeValueAsString(getSystemOverview(userId));

                default:
                    return "{\"error\":\"Unknown tool: " + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", toolName, e.getMessage(), e);
            return "{\"error\":\"Failed to execute tool: " + e.getMessage() + "\"}";
        }
    }

    private Long extractAppId(Map<String, Object> arguments, Long fallbackAppId) {
        if (arguments != null && arguments.containsKey("applicationId") && arguments.get("applicationId") != null) {
            try {
                return Long.parseLong(String.valueOf(arguments.get("applicationId")));
            } catch (NumberFormatException ignored) {}
        }
        return fallbackAppId;
    }

    public Map<String, Object> listApplications(Long userId) {
        List<Application> apps = applicationRepository.findByOwnerId(userId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Application app : apps) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("name", app.getName());
            item.put("baseUrl", app.getBaseUrl());
            item.put("healthStatus", app.getHealthStatus());
            item.put("connectionMode", app.getConnectionMode());
            item.put("upstreamAuthType", app.getUpstreamAuthType());
            item.put("createdAt", app.getCreatedAt() != null ? app.getCreatedAt().toString() : null);
            list.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("source", "sentinel_database");
        result.put("queriedAt", Instant.now().toString());
        result.put("totalCount", list.size());
        result.put("applications", list);
        return result;
    }

    public Map<String, Object> getApplicationHealth(Long userId, Long applicationId) {
        ApplicationStatusResponse status = applicationService.getApplicationStatus(userId, applicationId);
        Map<String, Object> result = new HashMap<>();
        result.put("source", "sentinel_database");
        result.put("queriedAt", Instant.now().toString());
        result.put("applicationId", status.getApplicationId());
        result.put("healthStatus", status.getStatus());
        result.put("lastSeenAt", status.getLastSeenAt() != null ? status.getLastSeenAt().toString() : null);
        result.put("connectionMode", status.getConnectionMode());
        return result;
    }

    public Map<String, Object> getApplicationMetrics(Long userId, Long applicationId, String timeRange) {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(24));
        if ("1h".equalsIgnoreCase(timeRange)) {
            from = now.minus(Duration.ofHours(1));
        } else if ("7d".equalsIgnoreCase(timeRange)) {
            from = now.minus(Duration.ofDays(7));
        }

        com.sentinel.api.dto.ApiEndpointAnalyticsResponse analytics = analyticsService.getApplicationAnalytics(userId, applicationId, from, now);
        Map<String, Object> result = new HashMap<>();
        result.put("source", "sentinel_database");
        result.put("queriedAt", Instant.now().toString());
        result.put("timeRange", timeRange != null ? timeRange : "24h");
        result.put("applicationId", analytics.getApplicationId());
        result.put("totalRequests", analytics.getTotalRequests());
        result.put("successfulRequests", analytics.getSuccessCount());
        result.put("failedRequests", analytics.getErrorCount());
        result.put("avgLatencyMs", analytics.getAvgLatencyMs());
        result.put("errorRate", analytics.getErrorRate());
        result.put("successRate", analytics.getSuccessRate());
        result.put("status4xx", analytics.getStatus4xxCount());
        result.put("status5xx", analytics.getStatus5xxCount());
        result.put("rateLimitedCount", analytics.getRateLimitedCount());
        return result;
    }

    public Map<String, Object> getRecentRequestLogs(Long userId, Long applicationId, int limit, Integer minStatusCode) {
        List<RequestLog> logs;
        if (applicationId != null) {
            applicationRepository.findByIdAndOwnerId(applicationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
            logs = requestLogRepository.findByApplicationId(applicationId, PageRequest.of(0, Math.min(limit, 50))).getContent();
        } else {
            List<Application> userApps = applicationRepository.findByOwnerId(userId);
            logs = new ArrayList<>();
            for (Application app : userApps) {
                logs.addAll(requestLogRepository.findByApplicationId(app.getId(), PageRequest.of(0, Math.min(limit, 20))).getContent());
            }
        }

        List<Map<String, Object>> logList = new ArrayList<>();
        for (RequestLog l : logs) {
            if (minStatusCode != null && l.getStatusCode() < minStatusCode) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", l.getId());
            item.put("applicationId", l.getApplicationId());
            item.put("method", l.getMethod());
            item.put("path", l.getPath());
            item.put("normalizedPath", l.getNormalizedPath());
            item.put("statusCode", l.getStatusCode());
            item.put("latencyMs", l.getLatencyMs());
            item.put("clientIp", l.getClientIp());
            item.put("timestamp", l.getTimestamp() != null ? l.getTimestamp().toString() : null);
            logList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("source", "sentinel_database");
        result.put("queriedAt", Instant.now().toString());
        result.put("applicationId", applicationId);
        result.put("returnedCount", logList.size());
        result.put("logs", logList);
        return result;
    }

    public Map<String, Object> getApiCatalog(Long userId, Long applicationId) {
        List<ApiEndpointDto> endpoints = apiCatalogService.listApplicationEndpoints(userId, applicationId);
        Map<String, Object> result = new HashMap<>();
        result.put("source", "sentinel_database");
        result.put("queriedAt", Instant.now().toString());
        result.put("applicationId", applicationId);
        result.put("totalEndpoints", endpoints.size());
        result.put("endpoints", endpoints);
        return result;
    }

    public Map<String, Object> getSystemOverview(Long userId) {
        GlobalDashboardResponse global = analyticsService.getGlobalDashboard(userId);
        SystemHealthResponse health = systemHealthService.getSystemHealth(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("source", "sentinel_database");
        result.put("queriedAt", Instant.now().toString());
        result.put("totalApplications", global.getTotalApplications());
        result.put("healthyApplications", global.getHealthyApplications());
        result.put("degradedApplications", global.getDegradedApplications());
        result.put("downApplications", global.getDownApplications());
        result.put("totalRequests24h", global.getTotalRequests());
        result.put("successRate24h", global.getOverallSuccessRate());
        result.put("errorRate24h", global.getOverallErrorRate());
        result.put("avgLatencyMs24h", global.getAvgLatencyMs());
        result.put("mysqlStatus", health.getMysql() != null ? health.getMysql().getStatus() : "UNKNOWN");
        result.put("redisStatus", health.getRedis() != null ? health.getRedis().getStatus() : "UNKNOWN");
        return result;
    }
}
