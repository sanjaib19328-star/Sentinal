package com.sentinel.api.service;

import com.sentinel.api.dto.ApiTestConsoleRequest;
import com.sentinel.api.dto.ApiTestConsoleResultDto;
import com.sentinel.api.dto.RateLimitResult;
import com.sentinel.api.exception.BadRequestException;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.Application;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.dto.PreparedUpstreamRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiTestConsoleService {

    private static final Logger log = LoggerFactory.getLogger(ApiTestConsoleService.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
        "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiDiscoveryService apiDiscoveryService;
    private final ApiPolicyService apiPolicyService;
    private final RateLimitService rateLimitService;
    private final RequestLoggingService requestLoggingService;
    private final CircuitBreakerService circuitBreakerService;
    private final UpstreamAuthenticationService upstreamAuthenticationService;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(DEFAULT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public ApiTestConsoleService(
        ApplicationRepository applicationRepository,
        ApiKeyRepository apiKeyRepository,
        ApiDiscoveryService apiDiscoveryService,
        ApiPolicyService apiPolicyService,
        RateLimitService rateLimitService,
        RequestLoggingService requestLoggingService,
        CircuitBreakerService circuitBreakerService,
        UpstreamAuthenticationService upstreamAuthenticationService
    ) {
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiDiscoveryService = apiDiscoveryService;
        this.apiPolicyService = apiPolicyService;
        this.rateLimitService = rateLimitService;
        this.requestLoggingService = requestLoggingService;
        this.circuitBreakerService = circuitBreakerService;
        this.upstreamAuthenticationService = upstreamAuthenticationService;
    }

    public ApiTestConsoleResultDto executeTest(Long ownerId, Long applicationId, ApiTestConsoleRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (request.getApiKeyId() == null) {
            throw new BadRequestException("An existing scoped API key must be selected for testing");
        }

        ApiKey apiKey = apiKeyRepository.findById(request.getApiKeyId())
            .filter(k -> k.getApplicationId() != null && k.getApplicationId().equals(applicationId))
            .orElseThrow(() -> new ResourceNotFoundException("Selected API key not found for this application"));

        if (!apiKey.isActive()) {
            throw new BadRequestException("Selected API key is revoked or inactive");
        }

        String method = (request.getMethod() != null ? request.getMethod() : "GET").toUpperCase(Locale.ROOT);
        String rawPath = (request.getPath() != null && !request.getPath().isBlank()) ? request.getPath().trim() : "/";
        String normalizedPath = PathNormalizer.normalize(rawPath);

        // 1. Auto-discover endpoint
        ApiEndpoint endpoint = apiDiscoveryService.discoverOrUpdateEndpoint(applicationId, method, normalizedPath);

        // 2. Fetch Policies
        ApiPolicy appPolicy = apiPolicyService.findAppPolicy(applicationId).orElse(null);
        ApiPolicy endpointPolicy = apiPolicyService.findEndpointPolicy(applicationId, endpoint.getId()).orElse(null);

        // 3. Allowed Methods Check
        if (!isMethodAllowed(method, endpointPolicy, appPolicy)) {
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId, applicationId, apiKey.getId(), endpoint.getId(),
                method, rawPath, normalizedPath, HttpStatus.METHOD_NOT_ALLOWED.value(), latencyMs, "127.0.0.1"
            );
            return new ApiTestConsoleResultDto(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                latencyMs,
                requestId,
                Map.of("Content-Type", "application/json"),
                "{\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"HTTP method " + method + " is not permitted by policy\"}",
                apiKey.getRateLimitPerMinute(),
                apiKey.getRateLimitPerMinute(),
                System.currentTimeMillis() / 1000 + 60,
                null
            );
        }

        // 4. Rate Limiting Check
        RateLimitResult rateLimitResult = rateLimitService.evaluateMultiLevel(
            apiKey.getId(),
            apiKey.getRateLimitPerMinute(),
            applicationId,
            appPolicy,
            endpoint.getId(),
            endpointPolicy
        );

        if (!rateLimitResult.isAllowed()) {
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId, applicationId, apiKey.getId(), endpoint.getId(),
                method, rawPath, normalizedPath, HttpStatus.TOO_MANY_REQUESTS.value(), latencyMs, "127.0.0.1"
            );
            return new ApiTestConsoleResultDto(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                latencyMs,
                requestId,
                Map.of("Content-Type", "application/json", "Retry-After", String.valueOf(rateLimitResult.getRetryAfterSeconds())),
                "{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Rate limit or quota exceeded (enforced by " + rateLimitResult.getThrottledBy() + ")\"}",
                rateLimitResult.getLimit(),
                rateLimitResult.getRemaining(),
                rateLimitResult.getResetEpochSeconds(),
                rateLimitResult.getThrottledBy()
            );
        }

        // 5. Circuit Breaker Check
        if (!circuitBreakerService.allowRequest(applicationId, appPolicy)) {
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId, applicationId, apiKey.getId(), endpoint.getId(),
                method, rawPath, normalizedPath, HttpStatus.SERVICE_UNAVAILABLE.value(), latencyMs, "127.0.0.1"
            );
            return new ApiTestConsoleResultDto(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                latencyMs,
                requestId,
                Map.of("Content-Type", "application/json"),
                "{\"error\":\"CIRCUIT_BREAKER_OPEN\",\"message\":\"Target service circuit breaker is OPEN due to repeated failures.\"}",
                rateLimitResult.getLimit(),
                rateLimitResult.getRemaining(),
                rateLimitResult.getResetEpochSeconds(),
                null
            );
        }

        // 6. Construct Target URI and Request with Upstream Authentication & custom timeout
        String queryString = buildQueryString(request.getQueryParams());
        PreparedUpstreamRequest preparedRequest = upstreamAuthenticationService.prepareTargetRequest(
            app.getBaseUrl(),
            rawPath,
            queryString,
            app
        );
        Duration timeout = resolveTimeout(endpointPolicy, appPolicy);

        try {
            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(preparedRequest.getTargetUri())
                .timeout(timeout);

            // Copy safe headers from request
            if (request.getHeaders() != null) {
                request.getHeaders().forEach((k, v) -> {
                    String lower = k.toLowerCase(Locale.ROOT);
                    if (!HOP_BY_HOP_HEADERS.contains(lower) && !lower.startsWith("x-sentinel-") && !lower.startsWith("x-internal-")) {
                        // If this is a multipart request with attached file, don't set raw Content-Type here; we will set the multipart boundary header below
                        if (!"content-type".equals(lower) || request.getFileName() == null) {
                            httpRequestBuilder.header(k, v);
                        }
                    }
                });
            }

            // Apply upstream authentication headers (Bearer token, custom header, basic auth, etc.)
            if (preparedRequest.getHeaders() != null) {
                preparedRequest.getHeaders().forEach(httpRequestBuilder::header);
            }

            httpRequestBuilder.header("X-Request-Id", requestId);
            httpRequestBuilder.header("X-Forwarded-For", "127.0.0.1");

            byte[] bodyBytes;
            if (request.getBinaryBodyBase64() != null && !request.getBinaryBodyBase64().isBlank()) {
                byte[] rawFileBytes = Base64.getDecoder().decode(request.getBinaryBodyBase64());
                if (request.getFileName() != null || request.getFileFieldName() != null) {
                    String boundary = "----SentinelBoundary" + UUID.randomUUID().toString().replace("-", "");
                    bodyBytes = buildMultipartFormData(rawFileBytes, request.getFileFieldName(), request.getFileName(), request.getFileContentType(), boundary);
                    httpRequestBuilder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
                } else {
                    bodyBytes = rawFileBytes;
                }
            } else if (request.getBody() != null && !request.getBody().isEmpty()) {
                bodyBytes = request.getBody().getBytes(StandardCharsets.UTF_8);
            } else {
                bodyBytes = new byte[0];
            }

            if (bodyBytes.length > 0 && supportsBody(method)) {
                httpRequestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
            } else if (supportsBody(method) && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
                httpRequestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (bodyBytes.length > 0) {
                    httpRequestBuilder.method("DELETE", HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
                } else {
                    httpRequestBuilder.DELETE();
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                httpRequestBuilder.GET();
            } else if ("HEAD".equalsIgnoreCase(method)) {
                httpRequestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            } else {
                httpRequestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> resp = httpClient.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;
            int sc = resp.statusCode();

            if (sc >= 500) {
                circuitBreakerService.recordFailure(applicationId, appPolicy);
            } else {
                circuitBreakerService.recordSuccess(applicationId);
            }

            requestLoggingService.logRequest(
                requestId, applicationId, apiKey.getId(), endpoint.getId(),
                method, rawPath, normalizedPath, sc, latencyMs, "127.0.0.1"
            );

            Map<String, String> respHeaders = new HashMap<>();
            resp.headers().map().forEach((k, vList) -> {
                if (!isHopByHopHeader(k)) {
                    respHeaders.put(k, String.join(", ", vList));
                }
            });
            respHeaders.put("X-Request-Id", requestId);

            return new ApiTestConsoleResultDto(
                sc,
                latencyMs,
                requestId,
                respHeaders,
                resp.body(),
                rateLimitResult.getLimit(),
                rateLimitResult.getRemaining(),
                rateLimitResult.getResetEpochSeconds(),
                null
            );

        } catch (Exception ex) {
            circuitBreakerService.recordFailure(applicationId, appPolicy);
            long latencyMs = System.currentTimeMillis() - startTime;

            requestLoggingService.logRequest(
                requestId, applicationId, apiKey.getId(), endpoint.getId(),
                method, rawPath, normalizedPath, HttpStatus.BAD_GATEWAY.value(), latencyMs, "127.0.0.1"
            );

            return new ApiTestConsoleResultDto(
                HttpStatus.BAD_GATEWAY.value(),
                latencyMs,
                requestId,
                Map.of("Content-Type", "application/json", "X-Request-Id", requestId),
                "{\"error\":\"BAD_GATEWAY\",\"message\":\"Target application unreachable: " + ex.getMessage() + "\"}",
                rateLimitResult.getLimit(),
                rateLimitResult.getRemaining(),
                rateLimitResult.getResetEpochSeconds(),
                null
            );
        }
    }

    private boolean isMethodAllowed(String method, ApiPolicy endpointPolicy, ApiPolicy appPolicy) {
        if (endpointPolicy != null && endpointPolicy.isEnabled() && endpointPolicy.getAllowedMethods() != null && !endpointPolicy.getAllowedMethods().isBlank()) {
            return isMethodInList(method, endpointPolicy.getAllowedMethods());
        }
        if (appPolicy != null && appPolicy.isEnabled() && appPolicy.getAllowedMethods() != null && !appPolicy.getAllowedMethods().isBlank()) {
            return isMethodInList(method, appPolicy.getAllowedMethods());
        }
        return true;
    }

    private boolean isMethodInList(String method, String allowedMethodsStr) {
        String[] allowed = allowedMethodsStr.split(",");
        return Arrays.stream(allowed).map(String::trim).anyMatch(m -> m.equalsIgnoreCase(method));
    }

    private Duration resolveTimeout(ApiPolicy endpointPolicy, ApiPolicy appPolicy) {
        if (endpointPolicy != null && endpointPolicy.isEnabled() && endpointPolicy.getTimeoutMs() > 0) {
            return Duration.ofMillis(endpointPolicy.getTimeoutMs());
        }
        if (appPolicy != null && appPolicy.isEnabled() && appPolicy.getTimeoutMs() > 0) {
            return Duration.ofMillis(appPolicy.getTimeoutMs());
        }
        return DEFAULT_TIMEOUT;
    }

    private String buildTargetUrl(String baseUrl, String path, Map<String, String> queryParams) {
        String base = baseUrl.trim().replaceAll("/+$", "");
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://" + base;
        }
        String p = path.startsWith("/") ? path : "/" + path;
        String fullUrl = base + p;

        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder qs = new StringBuilder();
            queryParams.forEach((k, v) -> {
                if (k != null && !k.isBlank()) {
                    if (qs.length() > 0) qs.append("&");
                    qs.append(k).append("=").append(v != null ? v : "");
                }
            });
            if (qs.length() > 0) {
                fullUrl += "?" + qs;
            }
        }
        return fullUrl;
    }

    private String buildQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return null;
        }
        StringBuilder qs = new StringBuilder();
        queryParams.forEach((k, v) -> {
            if (k != null && !k.isBlank()) {
                if (qs.length() > 0) qs.append("&");
                qs.append(k).append("=").append(v != null ? v : "");
            }
        });
        return qs.length() > 0 ? qs.toString() : null;
    }

    private boolean supportsBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) ||
               "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    private boolean isHopByHopHeader(String headerName) {
        if (headerName == null) return true;
        String lower = headerName.toLowerCase(Locale.ROOT);
        return HOP_BY_HOP_HEADERS.contains(lower) || lower.startsWith("x-sentinel-");
    }

    private byte[] buildMultipartFormData(byte[] fileBytes, String fieldName, String fileName, String contentType, String boundary) {
        String fName = (fieldName != null && !fieldName.isBlank()) ? fieldName.trim() : "file";
        String file = (fileName != null && !fileName.isBlank()) ? fileName.trim() : "upload.bin";
        String cType = (contentType != null && !contentType.isBlank()) ? contentType.trim() : "application/octet-stream";

        String header = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + fName + "\"; filename=\"" + file + "\"\r\n"
            + "Content-Type: " + cType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, result, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, result, headerBytes.length + fileBytes.length, footerBytes.length);

        return result;
    }
}
