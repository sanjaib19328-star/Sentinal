package com.sentinel.api.controller;

import com.sentinel.api.dto.ApiEndpointAnalyticsResponse;
import com.sentinel.api.dto.ApiEndpointDto;
import com.sentinel.api.dto.ApiKeyResponse;
import com.sentinel.api.dto.ApiPolicyDto;
import com.sentinel.api.dto.ApiTestConsoleRequest;
import com.sentinel.api.dto.ApiTestConsoleResultDto;
import com.sentinel.api.dto.ApplicationMetricsResponse;
import com.sentinel.api.dto.ApplicationResponse;
import com.sentinel.api.dto.ApplicationStatusResponse;
import com.sentinel.api.dto.CircuitBreakerStatusDto;
import com.sentinel.api.dto.ConnectAndDiscoverRequest;
import com.sentinel.api.dto.ConnectAndDiscoverResponse;
import com.sentinel.api.dto.ConnectionTestResponse;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.OpenApiImportRequest;
import com.sentinel.api.dto.OpenApiImportResponse;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.dto.RequestLogResponse;
import com.sentinel.api.dto.UpdateApiKeyRequest;
import com.sentinel.api.dto.UpdateApplicationRequest;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.MetricType;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.ApiCatalogService;
import com.sentinel.api.service.ApiKeyService;
import com.sentinel.api.service.ApiPolicyService;
import com.sentinel.api.service.ApiTestConsoleService;
import com.sentinel.api.service.ApplicationService;
import com.sentinel.api.service.CircuitBreakerService;
import com.sentinel.api.service.OpenApiImportService;
import com.sentinel.api.service.UpstreamAuthenticationService;
import com.sentinel.api.dto.UpstreamAuthConfigRequest;
import com.sentinel.api.dto.UpstreamAuthConfigResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApiKeyService apiKeyService;
    private final ApiCatalogService apiCatalogService;
    private final OpenApiImportService openApiImportService;
    private final ApiTestConsoleService apiTestConsoleService;
    private final CircuitBreakerService circuitBreakerService;
    private final ApiPolicyService apiPolicyService;
    private final UpstreamAuthenticationService upstreamAuthenticationService;

    public ApplicationController(
        ApplicationService applicationService,
        ApiKeyService apiKeyService,
        ApiCatalogService apiCatalogService,
        OpenApiImportService openApiImportService,
        ApiTestConsoleService apiTestConsoleService,
        CircuitBreakerService circuitBreakerService,
        ApiPolicyService apiPolicyService,
        UpstreamAuthenticationService upstreamAuthenticationService
    ) {
        this.applicationService = applicationService;
        this.apiKeyService = apiKeyService;
        this.apiCatalogService = apiCatalogService;
        this.openApiImportService = openApiImportService;
        this.apiTestConsoleService = apiTestConsoleService;
        this.circuitBreakerService = circuitBreakerService;
        this.apiPolicyService = apiPolicyService;
        this.upstreamAuthenticationService = upstreamAuthenticationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.createApplication(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/connect-and-discover")
    public ResponseEntity<ConnectAndDiscoverResponse> connectAndDiscover(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ConnectAndDiscoverRequest request
    ) {
        ConnectAndDiscoverResponse response = applicationService.connectAndDiscover(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> listApplications(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ApplicationResponse> response = applicationService.listApplications(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplication(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        ApplicationResponse response = applicationService.getApplication(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @Valid @RequestBody UpdateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.updateApplication(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        applicationService.deleteApplication(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApplicationStatusResponse> getApplicationStatus(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        ApplicationStatusResponse response = applicationService.getApplicationStatus(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/connection-test")
    public ResponseEntity<ConnectionTestResponse> testConnection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        ConnectionTestResponse response = applicationService.testConnection(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/upstream-auth")
    public ResponseEntity<UpstreamAuthConfigResponse> getUpstreamAuth(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        UpstreamAuthConfigResponse response = upstreamAuthenticationService.getAuthenticationConfig(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/upstream-auth")
    public ResponseEntity<UpstreamAuthConfigResponse> updateUpstreamAuth(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @Valid @RequestBody UpstreamAuthConfigRequest request
    ) {
        UpstreamAuthConfigResponse response = upstreamAuthenticationService.configureAuthentication(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/upstream-auth")
    public ResponseEntity<Void> deleteUpstreamAuth(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        upstreamAuthenticationService.disableAuthentication(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/requests")
    public ResponseEntity<PagedResponse<RequestLogResponse>> getApplicationRequests(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        int pageSize = Math.min(Math.max(1, size), 100);
        int pageIndex = Math.max(0, page);
        PagedResponse<RequestLogResponse> response = applicationService.getApplicationRequests(
            principal.getId(),
            id,
            PageRequest.of(pageIndex, pageSize)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<ApplicationMetricsResponse> getApplicationMetrics(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(name = "metric", required = false) MetricType metric,
        @RequestParam(name = "limit", required = false) Integer limit
    ) {
        ApplicationMetricsResponse response = applicationService.getApplicationMetrics(
            principal.getId(),
            id,
            from,
            to,
            metric,
            limit
        );
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Phase 3: OpenAPI Spec Import Endpoint
    // ==========================================

    @PostMapping("/{id}/openapi/import")
    public ResponseEntity<OpenApiImportResponse> importOpenApiSpecification(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @Valid @RequestBody OpenApiImportRequest request
    ) {
        OpenApiImportResponse response = openApiImportService.importSpecification(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Phase 3: Developer API Test Console Endpoint
    // ==========================================

    @PostMapping("/{id}/apis/test-console")
    public ResponseEntity<ApiTestConsoleResultDto> executeApiTestConsole(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @Valid @RequestBody ApiTestConsoleRequest request
    ) {
        ApiTestConsoleResultDto result = apiTestConsoleService.executeTest(principal.getId(), id, request);
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // Phase 3: Circuit Breaker Status Endpoint
    // ==========================================

    @GetMapping("/{id}/circuit-breaker")
    public ResponseEntity<CircuitBreakerStatusDto> getCircuitBreakerStatus(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        applicationService.getApplication(principal.getId(), id); // verifies ownership
        ApiPolicy policy = apiPolicyService.findAppPolicy(id).orElse(null);
        CircuitBreakerStatusDto status = circuitBreakerService.getStatus(id, policy);
        return ResponseEntity.ok(status);
    }

    // ==========================================
    // API Key Management Lifecycle Endpoints
    // ==========================================

    @PostMapping("/{id}/keys")
    public ResponseEntity<ApiKeyResponse> createApplicationApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @Valid @RequestBody CreateApiKeyRequest request
    ) {
        ApiKeyResponse response = apiKeyService.createApplicationApiKey(principal.getId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/keys")
    public ResponseEntity<List<ApiKeyResponse>> listApplicationApiKeys(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        List<ApiKeyResponse> response = apiKeyService.listApplicationApiKeys(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/keys/{keyId}")
    public ResponseEntity<ApiKeyResponse> updateApplicationApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @PathVariable("keyId") Long keyId,
        @Valid @RequestBody UpdateApiKeyRequest request
    ) {
        ApiKeyResponse response = apiKeyService.updateApplicationApiKey(principal.getId(), id, keyId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/keys/{keyId}/revoke")
    public ResponseEntity<ApiKeyResponse> revokeApplicationApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @PathVariable("keyId") Long keyId
    ) {
        ApiKeyResponse response = apiKeyService.revokeApplicationApiKey(principal.getId(), id, keyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/keys/{keyId}/regenerate")
    public ResponseEntity<ApiKeyResponse> regenerateApplicationApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @PathVariable("keyId") Long keyId
    ) {
        ApiKeyResponse response = apiKeyService.regenerateApplicationApiKey(principal.getId(), id, keyId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/keys/{keyId}")
    public ResponseEntity<Void> deleteApplicationApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @PathVariable("keyId") Long keyId
    ) {
        apiKeyService.deleteApplicationApiKey(principal.getId(), id, keyId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // API Catalog & Per-API Analytics Endpoints
    // ==========================================

    @GetMapping("/{id}/apis")
    public ResponseEntity<List<ApiEndpointDto>> listApplicationEndpoints(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id
    ) {
        List<ApiEndpointDto> response = apiCatalogService.listApplicationEndpoints(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/apis/{apiId}")
    public ResponseEntity<ApiEndpointAnalyticsResponse> getEndpointAnalytics(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @PathVariable("apiId") Long apiId,
        @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        ApiEndpointAnalyticsResponse response = apiCatalogService.getEndpointAnalytics(
            principal.getId(),
            id,
            apiId,
            from,
            to
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/apis/{apiId}/requests")
    public ResponseEntity<PagedResponse<RequestLogResponse>> getEndpointRequests(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable("id") Long id,
        @PathVariable("apiId") Long apiId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        int pageSize = Math.min(Math.max(1, size), 100);
        int pageIndex = Math.max(0, page);
        PagedResponse<RequestLogResponse> response = apiCatalogService.getEndpointRequests(
            principal.getId(),
            id,
            apiId,
            PageRequest.of(pageIndex, pageSize)
        );
        return ResponseEntity.ok(response);
    }
}
