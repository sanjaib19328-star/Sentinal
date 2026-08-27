package com.sentinel.api.controller;

import com.sentinel.api.dto.ApiEndpointAnalyticsResponse;
import com.sentinel.api.dto.ConsumerKeyAnalyticsDto;
import com.sentinel.api.dto.ErrorAnalyticsResponse;
import com.sentinel.api.dto.GlobalDashboardResponse;
import com.sentinel.api.dto.SystemHealthResponse;
import com.sentinel.api.dto.TimeSeriesResponse;
import com.sentinel.api.dto.TrafficBreakdownResponse;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.AnalyticsService;
import com.sentinel.api.service.ConsumerAnalyticsService;
import com.sentinel.api.service.SystemHealthService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@PreAuthorize("hasRole('USER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ConsumerAnalyticsService consumerAnalyticsService;
    private final SystemHealthService systemHealthService;

    public AnalyticsController(
        AnalyticsService analyticsService,
        ConsumerAnalyticsService consumerAnalyticsService,
        SystemHealthService systemHealthService
    ) {
        this.analyticsService = analyticsService;
        this.consumerAnalyticsService = consumerAnalyticsService;
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/api/v1/dashboard/summary")
    public ResponseEntity<GlobalDashboardResponse> getGlobalDashboard(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        GlobalDashboardResponse response = analyticsService.getGlobalDashboard(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/analytics/system/health")
    public ResponseEntity<SystemHealthResponse> getSystemHealth(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        SystemHealthResponse response = systemHealthService.getSystemHealth(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/applications/{applicationId}/analytics")
    public ResponseEntity<ApiEndpointAnalyticsResponse> getApplicationAnalytics(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        ApiEndpointAnalyticsResponse response = analyticsService.getApplicationAnalytics(principal.getId(), applicationId, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/applications/{applicationId}/analytics/timeseries")
    public ResponseEntity<TimeSeriesResponse> getTimeSeries(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false, defaultValue = "minute") String interval
    ) {
        TimeSeriesResponse response = analyticsService.getTimeSeries(principal.getId(), applicationId, from, to, interval);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/applications/{applicationId}/analytics/breakdown")
    public ResponseEntity<TrafficBreakdownResponse> getTrafficBreakdown(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        TrafficBreakdownResponse response = analyticsService.getTrafficBreakdown(principal.getId(), applicationId, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/applications/{applicationId}/errors")
    public ResponseEntity<ErrorAnalyticsResponse> getErrorAnalytics(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) Long endpointId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        ErrorAnalyticsResponse response = analyticsService.getErrorAnalytics(
            principal.getId(), applicationId, status, method, endpointId, from, to, pageable
        );
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Phase 3: Consumer / API-Key Analytics Endpoints
    // ==========================================

    @GetMapping("/api/v1/applications/{applicationId}/keys/{keyId}/analytics")
    public ResponseEntity<ConsumerKeyAnalyticsDto> getKeyAnalytics(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @PathVariable Long keyId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        ConsumerKeyAnalyticsDto response = consumerAnalyticsService.getKeyAnalytics(
            principal.getId(), applicationId, keyId, from, to
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/applications/{applicationId}/consumers")
    public ResponseEntity<List<ConsumerKeyAnalyticsDto>> getApplicationConsumers(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        List<ConsumerKeyAnalyticsDto> response = consumerAnalyticsService.getApplicationConsumers(
            principal.getId(), applicationId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/analytics/consumers/top")
    public ResponseEntity<List<ConsumerKeyAnalyticsDto>> getGlobalTopConsumers(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<ConsumerKeyAnalyticsDto> response = consumerAnalyticsService.getGlobalTopConsumers(
            principal.getId(), limit
        );
        return ResponseEntity.ok(response);
    }
}
