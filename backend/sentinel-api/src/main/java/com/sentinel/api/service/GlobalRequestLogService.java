package com.sentinel.api.service;

import com.sentinel.api.dto.GlobalRequestLogDto;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GlobalRequestLogService {

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final RequestLogRepository requestLogRepository;

    public GlobalRequestLogService(
        ApplicationRepository applicationRepository,
        ApiKeyRepository apiKeyRepository,
        RequestLogRepository requestLogRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<GlobalRequestLogDto> getGlobalRequestLogs(
        Long userId,
        Long applicationId,
        Long apiKeyId,
        String method,
        Integer statusCode,
        String statusClass,
        String requestId,
        String search,
        Instant from,
        Instant to,
        int page,
        int size
    ) {
        List<Application> userApps = applicationRepository.findByOwnerId(userId);
        if (userApps.isEmpty()) {
            return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0);
        }

        Map<Long, Application> appMap = userApps.stream()
            .collect(Collectors.toMap(Application::getId, a -> a));

        List<Long> appIds = applicationId != null
            ? (appMap.containsKey(applicationId) ? List.of(applicationId) : Collections.emptyList())
            : new ArrayList<>(appMap.keySet());

        if (appIds.isEmpty()) {
            return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0);
        }

        // Cache all user keys for fast resolution
        Map<Long, ApiKey> keyMap = new HashMap<>();
        for (Long appId : appIds) {
            apiKeyRepository.findByApplicationId(appId).forEach(k -> keyMap.put(k.getId(), k));
        }

        // Gather all request logs for these applications
        List<RequestLog> allLogs = new ArrayList<>();
        for (Long appId : appIds) {
            allLogs.addAll(requestLogRepository.findAllByApplicationIdOrderByTimestampDesc(appId));
        }

        // Filter in memory
        List<RequestLog> filtered = allLogs.stream()
            .filter(r -> {
                if (apiKeyId != null && !Objects.equals(r.getApiKeyId(), apiKeyId)) {
                    return false;
                }
                if (method != null && !method.equalsIgnoreCase("ALL") && !r.getMethod().equalsIgnoreCase(method.trim())) {
                    return false;
                }
                if (statusCode != null && r.getStatusCode() != statusCode) {
                    return false;
                }
                if (statusClass != null && !statusClass.equalsIgnoreCase("ALL")) {
                    String sc = statusClass.trim().toLowerCase();
                    if (sc.equals("2xx") && (r.getStatusCode() < 200 || r.getStatusCode() >= 300)) return false;
                    if (sc.equals("4xx") && (r.getStatusCode() < 400 || r.getStatusCode() >= 500)) return false;
                    if (sc.equals("5xx") && (r.getStatusCode() < 500 || r.getStatusCode() >= 600)) return false;
                    if (sc.equals("429") && r.getStatusCode() != 429) return false;
                }
                if (requestId != null && !requestId.trim().isEmpty() && !r.getRequestId().toLowerCase().contains(requestId.trim().toLowerCase())) {
                    return false;
                }
                if (search != null && !search.trim().isEmpty()) {
                    String q = search.trim().toLowerCase();
                    boolean matchPath = r.getPath() != null && r.getPath().toLowerCase().contains(q);
                    boolean matchNormPath = r.getNormalizedPath() != null && r.getNormalizedPath().toLowerCase().contains(q);
                    boolean matchReqId = r.getRequestId() != null && r.getRequestId().toLowerCase().contains(q);
                    boolean matchIp = r.getClientIp() != null && r.getClientIp().toLowerCase().contains(q);
                    if (!matchPath && !matchNormPath && !matchReqId && !matchIp) {
                        return false;
                    }
                }
                if (from != null && r.getTimestamp().isBefore(from)) {
                    return false;
                }
                if (to != null && r.getTimestamp().isAfter(to)) {
                    return false;
                }
                return true;
            })
            .sorted(Comparator.comparing(RequestLog::getTimestamp).reversed())
            .collect(Collectors.toList());

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);

        List<GlobalRequestLogDto> content = filtered.subList(start, end).stream().map(log -> {
            GlobalRequestLogDto dto = new GlobalRequestLogDto();
            dto.setId(log.getId());
            dto.setRequestId(log.getRequestId());
            dto.setApplicationId(log.getApplicationId());
            Application app = appMap.get(log.getApplicationId());
            dto.setApplicationName(app != null ? app.getName() : "App #" + log.getApplicationId());
            dto.setApiKeyId(log.getApiKeyId());
            ApiKey key = keyMap.get(log.getApiKeyId());
            if (key != null) {
                dto.setKeyName(key.getName());
                dto.setKeyMasked("sk_••••••••" + key.getId());
            } else if (log.getApiKeyId() != null) {
                dto.setKeyName("Key #" + log.getApiKeyId());
                dto.setKeyMasked("sk_••••••••" + log.getApiKeyId());
            }
            dto.setEndpointId(log.getEndpointId());
            dto.setMethod(log.getMethod());
            dto.setPath(log.getPath());
            dto.setNormalizedPath(log.getNormalizedPath());
            dto.setStatusCode(log.getStatusCode());
            dto.setLatencyMs(log.getLatencyMs());
            dto.setClientIp(log.getClientIp());
            dto.setTimestamp(log.getTimestamp());
            return dto;
        }).collect(Collectors.toList());

        return new PagedResponse<>(content, page, size, totalElements, totalPages);
    }
}
