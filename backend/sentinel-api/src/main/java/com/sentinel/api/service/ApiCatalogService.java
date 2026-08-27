package com.sentinel.api.service;

import com.sentinel.api.dto.ApiEndpointAnalyticsResponse;
import com.sentinel.api.dto.ApiEndpointDto;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.dto.RequestLogResponse;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApiCatalogService {

    private final ApiEndpointRepository apiEndpointRepository;
    private final RequestLogRepository requestLogRepository;
    private final ApplicationRepository applicationRepository;

    public ApiCatalogService(
        ApiEndpointRepository apiEndpointRepository,
        RequestLogRepository requestLogRepository,
        ApplicationRepository applicationRepository
    ) {
        this.apiEndpointRepository = apiEndpointRepository;
        this.requestLogRepository = requestLogRepository;
        this.applicationRepository = applicationRepository;
    }

    public List<ApiEndpointDto> listApplicationEndpoints(Long ownerId, Long applicationId) {
        // Validate application ownership
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByApplicationIdOrderByLastSeenAtDesc(applicationId);
        List<ApiEndpointDto> dtos = new ArrayList<>();

        for (ApiEndpoint ep : endpoints) {
            List<RequestLog> logs = requestLogRepository.findByEndpointIdOrderByTimestampDesc(ep.getId());
            long totalRequests = logs.size();
            long errorCount = logs.stream().filter(l -> l.getStatusCode() >= 400).count();
            double avgLatency = logs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);
            double successRate = totalRequests > 0
                ? ((double) (totalRequests - errorCount) * 100.0) / totalRequests
                : 100.0;

            dtos.add(new ApiEndpointDto(
                ep.getId(),
                ep.getApplicationId(),
                ep.getMethod(),
                ep.getNormalizedPath(),
                ep.getDocumentationStatus(),
                ep.getSummary(),
                ep.getDescription(),
                ep.getParametersJson(),
                ep.getRequestBodySchemaJson(),
                ep.getResponsesJson(),
                ep.isDeprecated(),
                ep.getFirstSeenAt(),
                ep.getLastSeenAt(),
                totalRequests,
                errorCount,
                Math.round(avgLatency * 100.0) / 100.0,
                Math.round(successRate * 100.0) / 100.0
            ));
        }

        return dtos;
    }

    public ApiEndpointAnalyticsResponse getEndpointAnalytics(Long ownerId, Long applicationId, Long endpointId, Instant from, Instant to) {
        // Validate application ownership
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApiEndpoint endpoint = apiEndpointRepository.findByIdAndApplicationId(endpointId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("API Endpoint not found"));

        List<RequestLog> logs;
        if (from != null && to != null) {
            logs = requestLogRepository.findByEndpointIdAndTimestampBetweenOrderByTimestampDesc(endpointId, from, to);
        } else {
            logs = requestLogRepository.findByEndpointIdOrderByTimestampDesc(endpointId);
        }

        long totalRequests = logs.size();
        long successCount = logs.stream().filter(l -> l.getStatusCode() >= 200 && l.getStatusCode() < 400).count();
        long errorCount = logs.stream().filter(l -> l.getStatusCode() >= 400).count();
        long status4xx = logs.stream().filter(l -> l.getStatusCode() >= 400 && l.getStatusCode() < 500).count();
        long status5xx = logs.stream().filter(l -> l.getStatusCode() >= 500).count();
        long rateLimited = logs.stream().filter(l -> l.getStatusCode() == 429).count();

        double successRate = totalRequests > 0 ? ((double) successCount * 100.0) / totalRequests : 100.0;
        double errorRate = totalRequests > 0 ? ((double) errorCount * 100.0) / totalRequests : 0.0;
        double avgLatency = logs.stream().mapToLong(RequestLog::getLatencyMs).average().orElse(0.0);

        // Calculate percentiles
        List<Long> latencies = logs.stream().map(RequestLog::getLatencyMs).sorted().collect(Collectors.toList());
        double p50 = calculatePercentile(latencies, 50);
        double p95 = calculatePercentile(latencies, 95);
        double p99 = calculatePercentile(latencies, 99);

        // Recent requests (up to 20)
        List<RequestLogResponse> recentRequests = logs.stream()
            .limit(20)
            .map(l -> new RequestLogResponse(
                l.getRequestId(),
                l.getMethod(),
                l.getPath(),
                l.getStatusCode(),
                l.getLatencyMs(),
                l.getTimestamp(),
                l.getClientIp()
            ))
            .collect(Collectors.toList());

        return new ApiEndpointAnalyticsResponse(
            endpoint.getId(),
            endpoint.getApplicationId(),
            endpoint.getMethod(),
            endpoint.getNormalizedPath(),
            totalRequests,
            successCount,
            errorCount,
            Math.round(successRate * 100.0) / 100.0,
            Math.round(errorRate * 100.0) / 100.0,
            Math.round(avgLatency * 100.0) / 100.0,
            p50,
            p95,
            p99,
            status4xx,
            status5xx,
            rateLimited,
            endpoint.getFirstSeenAt(),
            endpoint.getLastSeenAt(),
            recentRequests
        );
    }

    public PagedResponse<RequestLogResponse> getEndpointRequests(Long ownerId, Long applicationId, Long endpointId, Pageable pageable) {
        // Validate application ownership
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        apiEndpointRepository.findByIdAndApplicationId(endpointId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("API Endpoint not found"));

        Page<RequestLog> page = requestLogRepository.findByEndpointIdOrderByTimestampDesc(endpointId, pageable);

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

    private double calculatePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        if (sortedValues.size() == 1) {
            return sortedValues.get(0).doubleValue();
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index).doubleValue();
    }
}
