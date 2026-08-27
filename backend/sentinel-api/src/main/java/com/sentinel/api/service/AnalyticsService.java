package com.sentinel.api.service;

import com.sentinel.api.dto.AlertDto;
import com.sentinel.api.dto.ApiEndpointAnalyticsResponse;
import com.sentinel.api.dto.ErrorAnalyticsResponse;
import com.sentinel.api.dto.GlobalDashboardResponse;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.dto.RequestLogResponse;
import com.sentinel.api.dto.TimeSeriesPointDto;
import com.sentinel.api.dto.TimeSeriesResponse;
import com.sentinel.api.dto.TrafficBreakdownResponse;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ApplicationRepository applicationRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final RequestLogRepository requestLogRepository;
    private final AlertService alertService;

    public AnalyticsService(
        ApplicationRepository applicationRepository,
        ApiEndpointRepository apiEndpointRepository,
        RequestLogRepository requestLogRepository,
        AlertService alertService
    ) {
        this.applicationRepository = applicationRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.requestLogRepository = requestLogRepository;
        this.alertService = alertService;
    }

    public ApiEndpointAnalyticsResponse getApplicationAnalytics(Long ownerId, Long applicationId, Instant from, Instant to) {
        verifyApplicationOwnership(ownerId, applicationId);

        List<RequestLog> logs = fetchLogs(applicationId, from, to);

        if (logs.isEmpty()) {
            return new ApiEndpointAnalyticsResponse(
                0L, applicationId, "ALL", "/*", 0, 0, 0, 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0,
                Instant.now(), Instant.now(), List.of()
            );
        }

        long total = logs.size();
        long success = logs.stream().filter(l -> l.getStatusCode() >= 200 && l.getStatusCode() < 400).count();
        long errors = logs.stream().filter(l -> l.getStatusCode() >= 400).count();
        long c4xx = logs.stream().filter(l -> l.getStatusCode() >= 400 && l.getStatusCode() < 500 && l.getStatusCode() != 429).count();
        long c5xx = logs.stream().filter(l -> l.getStatusCode() >= 500).count();
        long c429 = logs.stream().filter(l -> l.getStatusCode() == 429).count();

        double avgLatency = logs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);

        List<Long> latencies = logs.stream().map(RequestLog::getLatencyMs).sorted().collect(Collectors.toList());
        double p50 = computePercentile(latencies, 0.50);
        double p95 = computePercentile(latencies, 0.95);
        double p99 = computePercentile(latencies, 0.99);

        double successRate = Math.round(((double) success / total) * 10000.0) / 100.0;
        double errorRate = Math.round(((double) errors / total) * 10000.0) / 100.0;

        Instant firstSeen = logs.get(logs.size() - 1).getTimestamp();
        Instant lastSeen = logs.get(0).getTimestamp();

        List<RequestLogResponse> recent = logs.stream().limit(10).map(this::mapToLogResponse).collect(Collectors.toList());

        return new ApiEndpointAnalyticsResponse(
            0L, applicationId, "ALL", "/*", total, success, errors, successRate, errorRate,
            Math.round(avgLatency * 100.0) / 100.0, p50, p95, p99, c4xx, c5xx, c429,
            firstSeen, lastSeen, recent
        );
    }

    public TimeSeriesResponse getTimeSeries(Long ownerId, Long applicationId, Instant from, Instant to, String interval) {
        verifyApplicationOwnership(ownerId, applicationId);

        long bucketSeconds = switch (interval != null ? interval.toLowerCase() : "minute") {
            case "5minute" -> 300;
            case "hour" -> 3600;
            case "day" -> 86400;
            default -> 60; // "minute"
        };

        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minusSeconds(bucketSeconds * 30);

        List<RequestLog> logs = requestLogRepository.findByApplicationIdAndTimestampBetween(applicationId, effectiveFrom, effectiveTo);

        // Bucket logs by epoch bucket
        Map<Long, List<RequestLog>> buckets = new LinkedHashMap<>();

        long startBucket = effectiveFrom.getEpochSecond() / bucketSeconds;
        long endBucket = effectiveTo.getEpochSecond() / bucketSeconds;

        for (long b = startBucket; b <= endBucket; b++) {
            buckets.put(b, new ArrayList<>());
        }

        for (RequestLog log : logs) {
            long b = log.getTimestamp().getEpochSecond() / bucketSeconds;
            if (buckets.containsKey(b)) {
                buckets.get(b).add(log);
            }
        }

        List<TimeSeriesPointDto> points = new ArrayList<>();

        for (Map.Entry<Long, List<RequestLog>> entry : buckets.entrySet()) {
            long bucketEpoch = entry.getKey() * bucketSeconds;
            Instant timestamp = Instant.ofEpochSecond(bucketEpoch);
            List<RequestLog> bucketLogs = entry.getValue();

            if (bucketLogs.isEmpty()) {
                points.add(new TimeSeriesPointDto(timestamp, 0, 0, 0, 0, 0.0, 0.0));
            } else {
                long total = bucketLogs.size();
                long success = bucketLogs.stream().filter(l -> l.getStatusCode() >= 200 && l.getStatusCode() < 400).count();
                long error = bucketLogs.stream().filter(l -> l.getStatusCode() >= 400).count();
                long rateLimited = bucketLogs.stream().filter(l -> l.getStatusCode() == 429).count();
                double avgLat = bucketLogs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);

                List<Long> latencies = bucketLogs.stream().map(RequestLog::getLatencyMs).sorted().collect(Collectors.toList());
                double p95Lat = computePercentile(latencies, 0.95);

                points.add(new TimeSeriesPointDto(
                    timestamp, total, success, error, rateLimited,
                    Math.round(avgLat * 100.0) / 100.0, p95Lat
                ));
            }
        }

        return new TimeSeriesResponse(applicationId, interval, points);
    }

    public TrafficBreakdownResponse getTrafficBreakdown(Long ownerId, Long applicationId, Instant from, Instant to) {
        verifyApplicationOwnership(ownerId, applicationId);

        List<RequestLog> logs = fetchLogs(applicationId, from, to);

        Map<String, Long> methodCounts = logs.stream()
            .collect(Collectors.groupingBy(RequestLog::getMethod, Collectors.counting()));

        Map<String, Long> statusClassCounts = new LinkedHashMap<>();
        statusClassCounts.put("2xx", logs.stream().filter(l -> l.getStatusCode() >= 200 && l.getStatusCode() < 300).count());
        statusClassCounts.put("3xx", logs.stream().filter(l -> l.getStatusCode() >= 300 && l.getStatusCode() < 400).count());
        statusClassCounts.put("4xx", logs.stream().filter(l -> l.getStatusCode() >= 400 && l.getStatusCode() < 500).count());
        statusClassCounts.put("5xx", logs.stream().filter(l -> l.getStatusCode() >= 500).count());

        // Group by (method, normalizedPath)
        Map<String, List<RequestLog>> endpointLogs = logs.stream()
            .collect(Collectors.groupingBy(l -> l.getMethod() + " " + (l.getNormalizedPath() != null ? l.getNormalizedPath() : l.getPath())));

        List<TrafficBreakdownResponse.EndpointRankDto> allRanked = new ArrayList<>();

        for (Map.Entry<String, List<RequestLog>> entry : endpointLogs.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(" ", 2);
            String method = parts[0];
            String path = parts.length > 1 ? parts[1] : "/";
            List<RequestLog> group = entry.getValue();
            long count = group.size();
            double avgLat = group.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);
            long errorCount = group.stream().filter(l -> l.getStatusCode() >= 400).count();
            double errorRate = Math.round(((double) errorCount / count) * 10000.0) / 100.0;
            long r429 = group.stream().filter(l -> l.getStatusCode() == 429).count();
            Long endpointId = group.get(0).getEndpointId();

            allRanked.add(new TrafficBreakdownResponse.EndpointRankDto(endpointId, method, path, count, avgLat));
        }

        // Top APIs by request count
        List<TrafficBreakdownResponse.EndpointRankDto> topApis = allRanked.stream()
            .sorted(Comparator.comparingLong(TrafficBreakdownResponse.EndpointRankDto::getCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        // Slowest APIs by average latency
        List<TrafficBreakdownResponse.EndpointRankDto> slowestApis = allRanked.stream()
            .sorted(Comparator.comparingDouble(TrafficBreakdownResponse.EndpointRankDto::getMetricValue).reversed())
            .limit(5)
            .collect(Collectors.toList());

        // Most error-prone APIs
        List<TrafficBreakdownResponse.EndpointRankDto> errorProneApis = endpointLogs.entrySet().stream()
            .map(e -> {
                String[] parts = e.getKey().split(" ", 2);
                long errorCount = e.getValue().stream().filter(l -> l.getStatusCode() >= 400).count();
                double errorRate = Math.round(((double) errorCount / e.getValue().size()) * 10000.0) / 100.0;
                return new TrafficBreakdownResponse.EndpointRankDto(
                    e.getValue().get(0).getEndpointId(), parts[0], parts.length > 1 ? parts[1] : "/", errorCount, errorRate
                );
            })
            .filter(r -> r.getCount() > 0)
            .sorted(Comparator.comparingLong(TrafficBreakdownResponse.EndpointRankDto::getCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        // Rate-limited APIs
        List<TrafficBreakdownResponse.EndpointRankDto> rateLimitedApis = endpointLogs.entrySet().stream()
            .map(e -> {
                String[] parts = e.getKey().split(" ", 2);
                long r429 = e.getValue().stream().filter(l -> l.getStatusCode() == 429).count();
                return new TrafficBreakdownResponse.EndpointRankDto(
                    e.getValue().get(0).getEndpointId(), parts[0], parts.length > 1 ? parts[1] : "/", r429, r429
                );
            })
            .filter(r -> r.getCount() > 0)
            .sorted(Comparator.comparingLong(TrafficBreakdownResponse.EndpointRankDto::getCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        return new TrafficBreakdownResponse(
            applicationId, methodCounts, statusClassCounts, topApis, slowestApis, errorProneApis, rateLimitedApis
        );
    }

    public ErrorAnalyticsResponse getErrorAnalytics(
        Long ownerId,
        Long applicationId,
        Integer statusCodeFilter,
        String methodFilter,
        Long endpointIdFilter,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        verifyApplicationOwnership(ownerId, applicationId);

        List<RequestLog> allLogs = fetchLogs(applicationId, from, to);
        List<RequestLog> errorLogs = allLogs.stream()
            .filter(l -> l.getStatusCode() >= 400)
            .collect(Collectors.toList());

        long totalAll = allLogs.size();
        long totalErrors = errorLogs.size();
        double errorRate = totalAll > 0 ? Math.round(((double) totalErrors / totalAll) * 10000.0) / 100.0 : 0.0;

        // Group by Status Code
        Map<String, Long> byStatus = errorLogs.stream()
            .collect(Collectors.groupingBy(l -> String.valueOf(l.getStatusCode()), Collectors.counting()));
        List<ErrorAnalyticsResponse.ErrorSummaryDto> errorByStatus = byStatus.entrySet().stream()
            .map(e -> new ErrorAnalyticsResponse.ErrorSummaryDto(
                e.getKey(), e.getValue(), totalErrors > 0 ? Math.round(((double) e.getValue() / totalErrors) * 10000.0) / 100.0 : 0.0
            ))
            .sorted(Comparator.comparingLong(ErrorAnalyticsResponse.ErrorSummaryDto::getCount).reversed())
            .collect(Collectors.toList());

        // Group by Endpoint
        Map<String, Long> byEndpoint = errorLogs.stream()
            .collect(Collectors.groupingBy(
                l -> l.getMethod() + " " + (l.getNormalizedPath() != null ? l.getNormalizedPath() : l.getPath()),
                Collectors.counting()
            ));
        List<ErrorAnalyticsResponse.ErrorSummaryDto> errorByEndpoint = byEndpoint.entrySet().stream()
            .map(e -> new ErrorAnalyticsResponse.ErrorSummaryDto(
                e.getKey(), e.getValue(), totalErrors > 0 ? Math.round(((double) e.getValue() / totalErrors) * 10000.0) / 100.0 : 0.0
            ))
            .sorted(Comparator.comparingLong(ErrorAnalyticsResponse.ErrorSummaryDto::getCount).reversed())
            .collect(Collectors.toList());

        // Apply filters to logs list
        List<RequestLog> filteredLogs = errorLogs.stream()
            .filter(l -> statusCodeFilter == null || l.getStatusCode() == statusCodeFilter)
            .filter(l -> methodFilter == null || l.getMethod().equalsIgnoreCase(methodFilter))
            .filter(l -> endpointIdFilter == null || (l.getEndpointId() != null && l.getEndpointId().equals(endpointIdFilter)))
            .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredLogs.size());
        List<RequestLogResponse> pagedList = (start <= end && start < filteredLogs.size())
            ? filteredLogs.subList(start, end).stream().map(this::mapToLogResponse).collect(Collectors.toList())
            : List.of();

        PagedResponse<RequestLogResponse> pagedResponse = new PagedResponse<>(
            pagedList,
            pageable.getPageNumber(),
            pageable.getPageSize(),
            filteredLogs.size(),
            (int) Math.ceil((double) filteredLogs.size() / pageable.getPageSize())
        );

        return new ErrorAnalyticsResponse(
            applicationId, totalErrors, errorRate, errorByStatus, errorByEndpoint, pagedResponse
        );
    }

    public GlobalDashboardResponse getGlobalDashboard(Long ownerId) {
        List<Application> userApps = applicationRepository.findAllByOwnerId(ownerId);
        List<Long> appIds = userApps.stream().map(Application::getId).collect(Collectors.toList());

        if (userApps.isEmpty()) {
            return new GlobalDashboardResponse(0, 0, 0, 0, 0, 0.0, 100.0, 0.0, 0.0, 0.0, 0.0, List.of(), List.of(), List.of(), List.of());
        }

        long healthy = userApps.stream().filter(a -> a.getHealthStatus() == HealthStatus.HEALTHY).count();
        long degraded = userApps.stream().filter(a -> a.getHealthStatus() == HealthStatus.DEGRADED).count();
        long down = userApps.stream().filter(a -> a.getHealthStatus() == HealthStatus.UNAVAILABLE).count();

        // Collect all logs for user's apps
        List<RequestLog> allTenantLogs = new ArrayList<>();
        List<GlobalDashboardResponse.ApplicationSummaryDto> appSummaries = new ArrayList<>();

        for (Application app : userApps) {
            List<RequestLog> appLogs = requestLogRepository.findAllByApplicationIdOrderByTimestampDesc(app.getId());
            allTenantLogs.addAll(appLogs);

            long appTotal = appLogs.size();
            long appErrors = appLogs.stream().filter(l -> l.getStatusCode() >= 400).count();
            double appErrRate = appTotal > 0 ? Math.round(((double) appErrors / appTotal) * 10000.0) / 100.0 : 0.0;
            double appAvgLat = appLogs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);

            appSummaries.add(new GlobalDashboardResponse.ApplicationSummaryDto(
                app.getId(), app.getName(), app.getHealthStatus().name(), appTotal, appErrRate, Math.round(appAvgLat * 100.0) / 100.0
            ));
        }

        long totalRequests = allTenantLogs.size();
        long successRequests = allTenantLogs.stream().filter(l -> l.getStatusCode() >= 200 && l.getStatusCode() < 400).count();
        long errorRequests = allTenantLogs.stream().filter(l -> l.getStatusCode() >= 400).count();
        long rateLimitedRequests = allTenantLogs.stream().filter(l -> l.getStatusCode() == 429).count();

        double successRate = totalRequests > 0 ? Math.round(((double) successRequests / totalRequests) * 10000.0) / 100.0 : 100.0;
        double errorRate = totalRequests > 0 ? Math.round(((double) errorRequests / totalRequests) * 10000.0) / 100.0 : 0.0;
        double r429Rate = totalRequests > 0 ? Math.round(((double) rateLimitedRequests / totalRequests) * 10000.0) / 100.0 : 0.0;
        double avgLat = allTenantLogs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);

        List<Long> latencies = allTenantLogs.stream().map(RequestLog::getLatencyMs).sorted().collect(Collectors.toList());
        double p95Lat = computePercentile(latencies, 0.95);

        // Requests per minute over active period
        double requestsPerMinute = 0.0;
        if (!allTenantLogs.isEmpty()) {
            Instant oldest = allTenantLogs.get(allTenantLogs.size() - 1).getTimestamp();
            Instant newest = allTenantLogs.get(0).getTimestamp();
            long diffSec = Math.max(60, newest.getEpochSecond() - oldest.getEpochSecond());
            requestsPerMinute = Math.round(((double) totalRequests / (diffSec / 60.0)) * 100.0) / 100.0;
        }

        // Top APIs across tenant
        Map<String, List<RequestLog>> tenantEndpointLogs = allTenantLogs.stream()
            .collect(Collectors.groupingBy(l -> l.getMethod() + " " + (l.getNormalizedPath() != null ? l.getNormalizedPath() : l.getPath())));

        List<TrafficBreakdownResponse.EndpointRankDto> topApis = tenantEndpointLogs.entrySet().stream()
            .map(e -> {
                String[] parts = e.getKey().split(" ", 2);
                return new TrafficBreakdownResponse.EndpointRankDto(
                    e.getValue().get(0).getEndpointId(), parts[0], parts.length > 1 ? parts[1] : "/", e.getValue().size(),
                    e.getValue().stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0)
                );
            })
            .sorted(Comparator.comparingLong(TrafficBreakdownResponse.EndpointRankDto::getCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        List<AlertDto> activeAlerts = alertService.listActiveAlertsForOwner(ownerId);

        List<RequestLogResponse> recentErrors = allTenantLogs.stream()
            .filter(l -> l.getStatusCode() >= 400)
            .limit(10)
            .map(this::mapToLogResponse)
            .collect(Collectors.toList());

        return new GlobalDashboardResponse(
            userApps.size(), healthy, degraded, down, totalRequests, requestsPerMinute, successRate, errorRate, r429Rate,
            Math.round(avgLat * 100.0) / 100.0, p95Lat, appSummaries, topApis, activeAlerts, recentErrors
        );
    }

    private List<RequestLog> fetchLogs(Long applicationId, Instant from, Instant to) {
        if (from != null && to != null) {
            return requestLogRepository.findByApplicationIdAndTimestampBetween(applicationId, from, to);
        }
        return requestLogRepository.findAllByApplicationIdOrderByTimestampDesc(applicationId);
    }

    private double computePercentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    private Application verifyApplicationOwnership(Long ownerId, Long applicationId) {
        return applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    public RequestLogResponse mapToLogResponse(RequestLog l) {
        return new RequestLogResponse(
            l.getRequestId(),
            l.getMethod(),
            l.getPath(),
            l.getStatusCode(),
            l.getLatencyMs(),
            l.getTimestamp(),
            l.getClientIp()
        );
    }
}
