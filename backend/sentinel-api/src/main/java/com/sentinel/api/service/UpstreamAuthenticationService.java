package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sentinel.api.dto.ConnectionTestResponse;
import com.sentinel.api.dto.PreparedUpstreamRequest;
import com.sentinel.api.dto.UpstreamAuthConfigRequest;
import com.sentinel.api.dto.UpstreamAuthConfigResponse;
import com.sentinel.api.exception.BadRequestException;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.model.UpstreamAuthType;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.security.CredentialEncryptionService;
import com.sentinel.api.security.SsrfProtectionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpstreamAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(UpstreamAuthenticationService.class);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);

    private final ApplicationRepository applicationRepository;
    private final CredentialEncryptionService encryptionService;
    private final SsrfProtectionValidator ssrfValidator;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(PROBE_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public UpstreamAuthenticationService(
        ApplicationRepository applicationRepository,
        CredentialEncryptionService encryptionService,
        SsrfProtectionValidator ssrfValidator,
        AuditLogService auditLogService
    ) {
        this.applicationRepository = applicationRepository;
        this.encryptionService = encryptionService;
        this.ssrfValidator = ssrfValidator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UpstreamAuthConfigResponse configureAuthentication(Long ownerId, Long applicationId, UpstreamAuthConfigRequest req) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        validateConfiguration(req);

        UpstreamAuthType type = req.getType() != null ? req.getType() : UpstreamAuthType.NONE;
        boolean enabled = req.getEnabled() != null ? req.getEnabled() : true;

        if (type == UpstreamAuthType.NONE) {
            application.setUpstreamAuthType(UpstreamAuthType.NONE);
            application.setUpstreamAuthEnabled(false);
            application.setUpstreamAuthConfigEncrypted(null);
        } else {
            String encryptedConfig = buildEncryptedPayload(req);
            application.setUpstreamAuthType(type);
            application.setUpstreamAuthEnabled(enabled);
            application.setUpstreamAuthConfigEncrypted(encryptedConfig);
        }

        applicationRepository.save(application);

        AuditAction action = application.isUpstreamAuthEnabled() ? AuditAction.UPSTREAM_AUTH_CONFIGURED : AuditAction.UPSTREAM_AUTH_UPDATED;
        auditLogService.record(
            ownerId,
            applicationId,
            action,
            "APPLICATION",
            String.valueOf(applicationId),
            "Configured upstream authentication type=" + type + ", enabled=" + enabled,
            null
        );

        return getMaskedAuthConfig(application);
    }

    @Transactional(readOnly = true)
    public UpstreamAuthConfigResponse getAuthenticationConfig(Long ownerId, Long applicationId) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return getMaskedAuthConfig(application);
    }

    @Transactional
    public void disableAuthentication(Long ownerId, Long applicationId) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        application.setUpstreamAuthEnabled(false);
        applicationRepository.save(application);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.UPSTREAM_AUTH_DISABLED,
            "APPLICATION",
            String.valueOf(applicationId),
            "Disabled upstream authentication",
            null
        );
    }

    public void validateConfiguration(UpstreamAuthConfigRequest req) {
        if (req == null || req.getType() == null || req.getType() == UpstreamAuthType.NONE) {
            return;
        }

        switch (req.getType()) {
            case BEARER_TOKEN -> {
                if (req.getSecret() == null || req.getSecret().isBlank()) {
                    throw new BadRequestException("Bearer token secret is required");
                }
            }
            case API_KEY_HEADER -> {
                if (req.getHeaderName() == null || req.getHeaderName().isBlank()) {
                    throw new BadRequestException("Header name is required for API_KEY_HEADER");
                }
                if (req.getSecret() == null || req.getSecret().isBlank()) {
                    throw new BadRequestException("API key secret is required");
                }
            }
            case API_KEY_QUERY -> {
                if (req.getQueryParamName() == null || req.getQueryParamName().isBlank()) {
                    throw new BadRequestException("Query parameter name is required for API_KEY_QUERY");
                }
                if (req.getSecret() == null || req.getSecret().isBlank()) {
                    throw new BadRequestException("API key secret is required");
                }
            }
            case BASIC_AUTH -> {
                if (req.getUsername() == null || req.getUsername().isBlank()) {
                    throw new BadRequestException("Username is required for BASIC_AUTH");
                }
                if (req.getPassword() == null || req.getPassword().isBlank()) {
                    throw new BadRequestException("Password is required for BASIC_AUTH");
                }
            }
            case CUSTOM_HEADER -> {
                if (req.getHeaderName() == null || req.getHeaderName().isBlank()) {
                    throw new BadRequestException("Custom header name is required");
                }
                if (req.getSecret() == null || req.getSecret().isBlank()) {
                    throw new BadRequestException("Header secret value is required");
                }
            }
            default -> {}
        }
    }

    public UpstreamAuthConfigResponse getMaskedAuthConfig(Application app) {
        if (app == null || app.getUpstreamAuthType() == null || app.getUpstreamAuthType() == UpstreamAuthType.NONE) {
            return new UpstreamAuthConfigResponse(UpstreamAuthType.NONE, false, false, null, null, null, null);
        }

        if (app.getUpstreamAuthConfigEncrypted() == null || app.getUpstreamAuthConfigEncrypted().isBlank()) {
            return new UpstreamAuthConfigResponse(app.getUpstreamAuthType(), app.isUpstreamAuthEnabled(), false, null, null, null, null);
        }

        try {
            String decryptedJson = encryptionService.decrypt(app.getUpstreamAuthConfigEncrypted());
            JsonNode node = objectMapper.readTree(decryptedJson);

            String headerName = node.has("headerName") ? node.get("headerName").asText() : null;
            String queryParamName = node.has("queryParamName") ? node.get("queryParamName").asText() : null;
            String username = node.has("username") ? node.get("username").asText() : null;
            boolean hasSecret = node.has("secret") && !node.get("secret").asText().isBlank();
            boolean hasPass = node.has("password") && !node.get("password").asText().isBlank();

            String masked = (hasSecret || hasPass) ? "••••••••" : null;

            return new UpstreamAuthConfigResponse(
                app.getUpstreamAuthType(),
                app.isUpstreamAuthEnabled(),
                true,
                headerName,
                queryParamName,
                masked,
                username
            );
        } catch (Exception e) {
            log.warn("Failed to decrypt upstream auth config for application {}: {}", app.getId(), e.getMessage());
            return new UpstreamAuthConfigResponse(app.getUpstreamAuthType(), app.isUpstreamAuthEnabled(), false, null, null, null, null);
        }
    }

    public PreparedUpstreamRequest prepareTargetRequest(String baseUrl, String path, String queryString, Application app) {
        String normalizedBase = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";
        String normalizedPath = path != null ? (path.startsWith("/") ? path : "/" + path) : "";

        Map<String, String> authHeaders = new HashMap<>();
        String targetQuery = queryString != null && !queryString.isBlank() ? queryString : null;

        if (app != null && app.isUpstreamAuthEnabled() && app.getUpstreamAuthConfigEncrypted() != null) {
            try {
                String decryptedJson = encryptionService.decrypt(app.getUpstreamAuthConfigEncrypted());
                JsonNode node = objectMapper.readTree(decryptedJson);
                UpstreamAuthType type = app.getUpstreamAuthType();

                if (type == UpstreamAuthType.BEARER_TOKEN && node.has("secret")) {
                    authHeaders.put("Authorization", "Bearer " + node.get("secret").asText());
                } else if (type == UpstreamAuthType.API_KEY_HEADER && node.has("headerName") && node.has("secret")) {
                    authHeaders.put(node.get("headerName").asText(), node.get("secret").asText());
                } else if (type == UpstreamAuthType.CUSTOM_HEADER && node.has("headerName") && node.has("secret")) {
                    authHeaders.put(node.get("headerName").asText(), node.get("secret").asText());
                } else if (type == UpstreamAuthType.BASIC_AUTH && node.has("username") && node.has("password")) {
                    String userPass = node.get("username").asText() + ":" + node.get("password").asText();
                    String encoded = Base64.getEncoder().encodeToString(userPass.getBytes(StandardCharsets.UTF_8));
                    authHeaders.put("Authorization", "Basic " + encoded);
                } else if (type == UpstreamAuthType.API_KEY_QUERY && node.has("queryParamName") && node.has("secret")) {
                    String param = node.get("queryParamName").asText() + "=" + node.get("secret").asText();
                    if (targetQuery == null || targetQuery.isBlank()) {
                        targetQuery = param;
                    } else {
                        targetQuery = targetQuery + "&" + param;
                    }
                }
            } catch (Exception e) {
                log.error("Error applying upstream auth for app {}: {}", app.getId(), e.getMessage());
            }
        }

        String fullUrl = normalizedBase + normalizedPath + (targetQuery != null ? "?" + targetQuery : "");
        return new PreparedUpstreamRequest(URI.create(fullUrl), authHeaders);
    }

    @Transactional
    public ConnectionTestResponse testConnection(Long ownerId, Long applicationId) {
        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        String baseUrl = app.getBaseUrl();
        Instant checkedAt = Instant.now();

        if (baseUrl == null || baseUrl.isBlank()) {
            return new ConnectionTestResponse(applicationId, false, HealthStatus.UNKNOWN, null, "Base URL is not configured", checkedAt);
        }

        // Validate SSRF Protection on Base URL
        try {
            ssrfValidator.validateUrl(baseUrl);
        } catch (Exception se) {
            return new ConnectionTestResponse(applicationId, false, HealthStatus.UNAVAILABLE, 403, null, "SSRF Blocked: " + se.getMessage(), checkedAt);
        }

        String targetBase = baseUrl.trim().replaceAll("/+$", "");
        if (!targetBase.startsWith("http://") && !targetBase.startsWith("https://")) {
            targetBase = "http://" + targetBase;
        }

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
                PreparedUpstreamRequest prepared = prepareTargetRequest(probeUrl, "", null, app);
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(prepared.getTargetUri())
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .header("User-Agent", "Sentinel-Observation-Agent/1.0");

                for (Map.Entry<String, String> header : prepared.getHeaders().entrySet()) {
                    reqBuilder.header(header.getKey(), header.getValue());
                }

                long start = System.currentTimeMillis();
                HttpResponse<Void> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
                latencyMs = System.currentTimeMillis() - start;
                lastResponse = resp;

                if (resp.statusCode() >= 200 && resp.statusCode() < 400) {
                    break;
                }
            } catch (Exception ex) {
                lastException = ex;
                if (ex instanceof java.net.http.HttpTimeoutException || ex instanceof java.net.ConnectException || ex instanceof java.net.UnknownHostException || ex instanceof java.net.NoRouteToHostException || ex instanceof javax.net.ssl.SSLException) {
                    break;
                }
            }
        }

        if (lastResponse != null) {
            int statusCode = lastResponse.statusCode();
            boolean success = (statusCode >= 200 && statusCode < 400);
            HealthStatus healthStatus = success ? ((latencyMs <= 1000) ? HealthStatus.HEALTHY : HealthStatus.DEGRADED) : HealthStatus.DEGRADED;
            String message = success ? "Upstream application is reachable (HTTP " + statusCode + ")" : "Upstream returned HTTP " + statusCode;

            app.setHealthStatus(healthStatus);
            app.setLastSeenAt(checkedAt);
            applicationRepository.save(app);

            auditLogService.record(
                ownerId,
                applicationId,
                AuditAction.UPSTREAM_CONNECTION_TESTED,
                "APPLICATION",
                String.valueOf(applicationId),
                "Connection test result=" + (success ? "SUCCESS" : "DEGRADED") + ", statusCode=" + statusCode + ", latencyMs=" + latencyMs,
                null
            );

            return new ConnectionTestResponse(applicationId, true, healthStatus, statusCode, latencyMs, message, checkedAt);
        } else {
            app.setHealthStatus(HealthStatus.UNAVAILABLE);
            applicationRepository.save(app);

            auditLogService.record(
                ownerId,
                applicationId,
                AuditAction.UPSTREAM_CONNECTION_TESTED,
                "APPLICATION",
                String.valueOf(applicationId),
                "Connection test result=FAILURE, error=" + (lastException != null ? lastException.getMessage() : "Unreachable"),
                null
            );

            String failureCategory = "UNREACHABLE";
            if (lastException != null) {
                if (lastException instanceof java.net.http.HttpTimeoutException || (lastException.getMessage() != null && lastException.getMessage().toLowerCase().contains("timed out"))) {
                    failureCategory = "TIMEOUT";
                } else if (lastException instanceof javax.net.ssl.SSLException) {
                    failureCategory = "SSL_ERROR";
                }
            }
            String failMessage = failureCategory + ": " + (lastException != null ? lastException.getMessage() : "Unreachable");

            return new ConnectionTestResponse(applicationId, false, HealthStatus.UNAVAILABLE, 502, null, failMessage, checkedAt);
        }
    }

    private String buildEncryptedPayload(UpstreamAuthConfigRequest req) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", req.getType().name());
            if (req.getHeaderName() != null) node.put("headerName", req.getHeaderName().trim());
            if (req.getQueryParamName() != null) node.put("queryParamName", req.getQueryParamName().trim());
            if (req.getSecret() != null) node.put("secret", req.getSecret().trim());
            if (req.getUsername() != null) node.put("username", req.getUsername().trim());
            if (req.getPassword() != null) node.put("password", req.getPassword().trim());

            String json = objectMapper.writeValueAsString(node);
            return encryptionService.encrypt(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize upstream auth config", e);
        }
    }
}
