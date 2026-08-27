package com.sentinel.api.service;

import com.sentinel.api.dto.GlobalApiEndpointDto;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.DocumentationStatus;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GlobalApiService {

    private final ApplicationRepository applicationRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final RequestLogRepository requestLogRepository;

    public GlobalApiService(
        ApplicationRepository applicationRepository,
        ApiEndpointRepository apiEndpointRepository,
        RequestLogRepository requestLogRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional(readOnly = true)
    public List<GlobalApiEndpointDto> listGlobalApis(
        Long userId,
        String search,
        Long applicationId,
        String method,
        DocumentationStatus documentationStatus,
        Boolean deprecated,
        String sortBy,
        String sortDir
    ) {
        List<Application> userApps = applicationRepository.findByOwnerId(userId);
        if (userApps.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Application> appMap = userApps.stream()
            .collect(Collectors.toMap(Application::getId, a -> a));

        List<Long> appIds = applicationId != null
            ? (appMap.containsKey(applicationId) ? List.of(applicationId) : Collections.emptyList())
            : new ArrayList<>(appMap.keySet());

        if (appIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByApplicationIdIn(appIds);
        if (endpoints.isEmpty()) {
            return Collections.emptyList();
        }

        List<GlobalApiEndpointDto> dtoList = endpoints.stream().map(ep -> {
            GlobalApiEndpointDto dto = new GlobalApiEndpointDto();
            dto.setId(ep.getId());
            dto.setApplicationId(ep.getApplicationId());
            Application app = appMap.get(ep.getApplicationId());
            if (app != null) {
                dto.setApplicationName(app.getName());
                dto.setApplicationBaseUrl(app.getBaseUrl());
            }
            dto.setMethod(ep.getMethod());
            dto.setNormalizedPath(ep.getNormalizedPath());
            dto.setDocumentationStatus(ep.getDocumentationStatus() != null ? ep.getDocumentationStatus() : DocumentationStatus.DISCOVERED);
            dto.setSummary(ep.getSummary());
            dto.setDescription(ep.getDescription());
            dto.setParametersJson(ep.getParametersJson());
            dto.setRequestBodySchemaJson(ep.getRequestBodySchemaJson());
            dto.setResponsesJson(ep.getResponsesJson());
            dto.setDeprecated(ep.isDeprecated());
            dto.setFirstSeenAt(ep.getFirstSeenAt());
            dto.setLastSeenAt(ep.getLastSeenAt());

            List<RequestLog> logs = requestLogRepository.findByEndpointIdOrderByTimestampDesc(ep.getId());
            long total = logs.size();
            long errors = logs.stream().filter(l -> l.getStatusCode() >= 400).count();
            double avgLat = logs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);
            
            // Calculate P95 latency
            double p95 = 0.0;
            if (!logs.isEmpty()) {
                List<Long> latencies = logs.stream().map(RequestLog::getLatencyMs).sorted().collect(Collectors.toList());
                int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
                p95 = latencies.get(Math.max(0, p95Index));
            }

            dto.setTotalRequests(total);
            dto.setErrorCount(errors);
            dto.setErrorRate(total > 0 ? Math.round((errors * 100.0 / total) * 10.0) / 10.0 : 0.0);
            dto.setAvgLatencyMs(Math.round(avgLat * 10.0) / 10.0);
            dto.setP95LatencyMs(p95);
            dto.setSuccessRate(total > 0 ? Math.round(((total - errors) * 100.0 / total) * 10.0) / 10.0 : 100.0);

            return dto;
        }).collect(Collectors.toList());

        // Apply filters
        return dtoList.stream()
            .filter(dto -> {
                if (search != null && !search.trim().isEmpty()) {
                    String query = search.trim().toLowerCase();
                    boolean matchPath = dto.getNormalizedPath() != null && dto.getNormalizedPath().toLowerCase().contains(query);
                    boolean matchMethod = dto.getMethod() != null && dto.getMethod().toLowerCase().contains(query);
                    boolean matchSummary = dto.getSummary() != null && dto.getSummary().toLowerCase().contains(query);
                    boolean matchAppName = dto.getApplicationName() != null && dto.getApplicationName().toLowerCase().contains(query);
                    if (!matchPath && !matchMethod && !matchSummary && !matchAppName) {
                        return false;
                    }
                }
                if (method != null && !method.equalsIgnoreCase("ALL")) {
                    if (!dto.getMethod().equalsIgnoreCase(method.trim())) {
                        return false;
                    }
                }
                if (documentationStatus != null) {
                    if (dto.getDocumentationStatus() != documentationStatus) {
                        return false;
                    }
                }
                if (deprecated != null) {
                    if (!Objects.equals(dto.getDeprecated(), deprecated)) {
                        return false;
                    }
                }
                return true;
            })
            .sorted((a, b) -> {
                boolean asc = "asc".equalsIgnoreCase(sortDir);
                int cmp = 0;
                String sort = sortBy != null ? sortBy.toLowerCase() : "requests";
                switch (sort) {
                    case "latency":
                    case "avglatency":
                        cmp = Double.compare(a.getAvgLatencyMs(), b.getAvgLatencyMs());
                        break;
                    case "p95":
                    case "p95latency":
                        cmp = Double.compare(a.getP95LatencyMs(), b.getP95LatencyMs());
                        break;
                    case "errors":
                    case "errorrate":
                        cmp = Double.compare(a.getErrorRate(), b.getErrorRate());
                        break;
                    case "lastseen":
                        Instant aTime = a.getLastSeenAt() != null ? a.getLastSeenAt() : Instant.EPOCH;
                        Instant bTime = b.getLastSeenAt() != null ? b.getLastSeenAt() : Instant.EPOCH;
                        cmp = aTime.compareTo(bTime);
                        break;
                    case "path":
                        cmp = a.getNormalizedPath().compareToIgnoreCase(b.getNormalizedPath());
                        break;
                    case "requests":
                    default:
                        cmp = Long.compare(a.getTotalRequests(), b.getTotalRequests());
                        break;
                }
                return asc ? cmp : -cmp;
            })
            .collect(Collectors.toList());
    }
}
