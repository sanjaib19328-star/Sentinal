package com.sentinel.api.controller;

import com.sentinel.api.dto.ErrorResponse;
import com.sentinel.api.dto.GatewayResponse;
import com.sentinel.api.dto.RateLimitResult;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.Application;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.security.ApiKeyAuthenticationFilter;
import com.sentinel.api.service.ApiDiscoveryService;
import com.sentinel.api.service.ApiPolicyService;
import com.sentinel.api.service.CircuitBreakerService;
import com.sentinel.api.service.PathNormalizer;
import com.sentinel.api.service.RateLimitService;
import com.sentinel.api.service.RequestLoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sentinel.api.service.UpstreamAuthenticationService;
import com.sentinel.api.dto.PreparedUpstreamRequest;

@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);
    private static final Duration DEFAULT_FORWARD_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_HEADER_TOTAL_BYTES = 16384; // 16 KB header ceiling

    // Standard hop-by-hop headers to drop
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "connection",
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade",
        "host",
        "content-length"
    );

    private final RateLimitService rateLimitService;
    private final RequestLoggingService requestLoggingService;
    private final ApplicationRepository applicationRepository;
    private final ApiDiscoveryService apiDiscoveryService;
    private final ApiPolicyService apiPolicyService;
    private final CircuitBreakerService circuitBreakerService;
    private final UpstreamAuthenticationService upstreamAuthenticationService;
    private final HttpClient httpClient;

    public GatewayController(
        RateLimitService rateLimitService,
        RequestLoggingService requestLoggingService,
        ApplicationRepository applicationRepository,
        ApiDiscoveryService apiDiscoveryService,
        ApiPolicyService apiPolicyService,
        CircuitBreakerService circuitBreakerService,
        UpstreamAuthenticationService upstreamAuthenticationService
    ) {
        this.rateLimitService = rateLimitService;
        this.requestLoggingService = requestLoggingService;
        this.applicationRepository = applicationRepository;
        this.apiDiscoveryService = apiDiscoveryService;
        this.apiPolicyService = apiPolicyService;
        this.circuitBreakerService = circuitBreakerService;
        this.upstreamAuthenticationService = upstreamAuthenticationService;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_FORWARD_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Backward-compatible test endpoint
     */
    @PostMapping("/test")
    public ResponseEntity<?> testGateway(HttpServletRequest request, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        String requestId = extractOrGenerateRequestId(request);

        ApiKey apiKey = (ApiKey) request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ATTRIBUTE);
        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Valid Sentinel API key required"));
        }

        RateLimitResult rateLimitResult = rateLimitService.checkRateLimit(
            apiKey.getId(),
            apiKey.getRateLimitPerMinute()
        );

        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitResult.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetEpochSeconds()));

        String clientIp = extractClientIp(request);
        String subpath = "/test";
        String normalizedPath = PathNormalizer.normalize(subpath);

        ApiEndpoint endpoint = null;
        if (apiKey.getApplicationId() != null) {
            endpoint = apiDiscoveryService.discoverOrUpdateEndpoint(apiKey.getApplicationId(), "POST", normalizedPath);
        }

        if (!rateLimitResult.isAllowed()) {
            response.setHeader("Retry-After", String.valueOf(rateLimitResult.getRetryAfterSeconds()));
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId,
                apiKey.getApplicationId(),
                apiKey.getId(),
                endpoint != null ? endpoint.getId() : null,
                "POST",
                "/api/v1/gateway/test",
                normalizedPath,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                latencyMs,
                clientIp
            );

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", "Rate limit exceeded"));
        }

        long latencyMs = System.currentTimeMillis() - startTime;
        requestLoggingService.logRequest(
            requestId,
            apiKey.getApplicationId(),
            apiKey.getId(),
            endpoint != null ? endpoint.getId() : null,
            "POST",
            "/api/v1/gateway/test",
            normalizedPath,
            HttpStatus.OK.value(),
            latencyMs,
            clientIp
        );

        GatewayResponse gatewayResponse = new GatewayResponse(
            true,
            "Sentinel gateway request accepted",
            requestId,
            apiKey.getId()
        );

        return ResponseEntity.ok(gatewayResponse);
    }

    /**
     * Real Gateway Forwarding Endpoint with Circuit Breaker, Retries, and Hardened Security
     */
    @RequestMapping(
        value = "/**",
        method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
            RequestMethod.HEAD,
            RequestMethod.OPTIONS
        }
    )
    public ResponseEntity<?> forwardGatewayRequest(HttpServletRequest request, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        String requestId = extractOrGenerateRequestId(request);

        // Security check: Validate total header size
        if (calculateTotalHeaderSize(request) > MAX_HEADER_TOTAL_BYTES) {
            return ResponseEntity.status(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE)
                .body(new ErrorResponse("HEADER_TOO_LARGE", "Request headers exceed maximum permitted size of 16 KB"));
        }

        ApiKey apiKey = (ApiKey) request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ATTRIBUTE);
        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Valid Sentinel API key required"));
        }

        // Validate Application
        if (apiKey.getApplicationId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("INVALID_KEY_SCOPE", "API key is not associated with an application"));
        }

        Optional<Application> appOpt = applicationRepository.findById(apiKey.getApplicationId());
        if (appOpt.isEmpty() || !appOpt.get().isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("APPLICATION_INACTIVE", "Target application is deactivated or not found"));
        }

        Application application = appOpt.get();
        String clientIp = extractClientIp(request);
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        // Extract subpath after "/api/v1/gateway"
        String requestUri = request.getRequestURI();
        String subpath = extractSubpath(requestUri);
        String normalizedPath = PathNormalizer.normalize(subpath);

        // Auto-discover / update API endpoint
        ApiEndpoint endpoint = apiDiscoveryService.discoverOrUpdateEndpoint(
            application.getId(),
            method,
            normalizedPath
        );

        // Fetch Policies for Application & Endpoint
        ApiPolicy appPolicy = apiPolicyService.findAppPolicy(application.getId()).orElse(null);
        ApiPolicy endpointPolicy = apiPolicyService.findEndpointPolicy(application.getId(), endpoint.getId()).orElse(null);

        // 1. Enforce Allowed Methods Policy
        if (!isMethodAllowed(method, endpointPolicy, appPolicy)) {
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId,
                application.getId(),
                apiKey.getId(),
                endpoint.getId(),
                method,
                subpath,
                normalizedPath,
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                latencyMs,
                clientIp
            );
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("METHOD_NOT_ALLOWED", "HTTP method " + method + " is not permitted by policy"));
        }

        // 2. Read Request Body & Enforce Max Body Size Policy
        byte[] bodyBytes = readRequestBody(request);
        Long maxBodySize = resolveMaxRequestBodySize(endpointPolicy, appPolicy);
        if (maxBodySize != null && maxBodySize > 0 && bodyBytes.length > maxBodySize) {
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId,
                application.getId(),
                apiKey.getId(),
                endpoint.getId(),
                method,
                subpath,
                normalizedPath,
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                latencyMs,
                clientIp
            );
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("PAYLOAD_TOO_LARGE", "Request body exceeds max limit of " + maxBodySize + " bytes"));
        }

        // 3. Multi-level Rate Limiting & Quotas Evaluation
        RateLimitResult rateLimitResult = rateLimitService.evaluateMultiLevel(
            apiKey.getId(),
            apiKey.getRateLimitPerMinute(),
            application.getId(),
            appPolicy,
            endpoint.getId(),
            endpointPolicy
        );

        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitResult.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetEpochSeconds()));

        if (!rateLimitResult.isAllowed()) {
            response.setHeader("Retry-After", String.valueOf(rateLimitResult.getRetryAfterSeconds()));
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId,
                application.getId(),
                apiKey.getId(),
                endpoint.getId(),
                method,
                subpath,
                normalizedPath,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                latencyMs,
                clientIp
            );

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMIT_EXCEEDED", "Rate limit or quota exceeded (enforced by " + rateLimitResult.getThrottledBy() + ")"));
        }

        // 4. Circuit Breaker Check
        if (!circuitBreakerService.allowRequest(application.getId(), appPolicy)) {
            long latencyMs = System.currentTimeMillis() - startTime;
            requestLoggingService.logRequest(
                requestId,
                application.getId(),
                apiKey.getId(),
                endpoint.getId(),
                method,
                subpath,
                normalizedPath,
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                latencyMs,
                clientIp
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("CIRCUIT_BREAKER_OPEN", "Target service circuit breaker is OPEN due to repeated failures. Requests fast-failed to protect backend."));
        }

        // 5. Construct Target URI and Request with Upstream Authentication & custom timeout
        PreparedUpstreamRequest preparedRequest = upstreamAuthenticationService.prepareTargetRequest(
            application.getBaseUrl(),
            subpath,
            request.getQueryString(),
            application
        );
        Duration timeout = resolveTimeout(endpointPolicy, appPolicy);

        int maxRetries = resolveMaxRetries(method, appPolicy);
        int retryDelay = appPolicy != null ? appPolicy.getRetryDelayMs() : 100;

        HttpResponse<byte[]> targetResponse = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0 && retryDelay > 0) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            try {
                HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(preparedRequest.getTargetUri())
                    .timeout(timeout);

                // Copy safe headers and apply upstream authentication headers
                copySafeRequestHeaders(request, httpRequestBuilder, requestId, clientIp, preparedRequest.getHeaders());

                // Set method and body
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

                targetResponse = httpClient.send(
                    httpRequestBuilder.build(),
                    HttpResponse.BodyHandlers.ofByteArray()
                );

                int sc = targetResponse.statusCode();
                if (sc == 502 || sc == 503 || sc == 504) {
                    // Retryable gateway status code
                    if (attempt < maxRetries) {
                        continue;
                    }
                }

                // If succeeded or non-retryable response, exit loop
                lastException = null;
                break;

            } catch (Exception ex) {
                lastException = ex;
                log.warn("Attempt {} to {} failed: {}", attempt + 1, preparedRequest.getTargetUri(), ex.getMessage());
            }
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        if (targetResponse != null && lastException == null) {
            int statusCode = targetResponse.statusCode();

            // Circuit Breaker State Recording
            if (statusCode >= 500) {
                circuitBreakerService.recordFailure(application.getId(), appPolicy);
            } else {
                circuitBreakerService.recordSuccess(application.getId());
            }

            // Record real request log
            requestLoggingService.logRequest(
                requestId,
                application.getId(),
                apiKey.getId(),
                endpoint.getId(),
                method,
                subpath,
                normalizedPath,
                statusCode,
                latencyMs,
                clientIp
            );

            // Build Response with forwarded headers
            HttpHeaders responseHeaders = new HttpHeaders();
            targetResponse.headers().map().forEach((headerName, headerValues) -> {
                if (!isHopByHopHeader(headerName)) {
                    for (String val : headerValues) {
                        responseHeaders.add(headerName, val);
                    }
                }
            });
            responseHeaders.set("X-Request-Id", requestId);
            responseHeaders.set("X-RateLimit-Limit", String.valueOf(rateLimitResult.getLimit()));
            responseHeaders.set("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
            responseHeaders.set("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetEpochSeconds()));

            byte[] responseBody = targetResponse.body();
            return ResponseEntity.status(statusCode)
                .headers(responseHeaders)
                .body(responseBody);
        } else {
            circuitBreakerService.recordFailure(application.getId(), appPolicy);

            requestLoggingService.logRequest(
                requestId,
                application.getId(),
                apiKey.getId(),
                endpoint.getId(),
                method,
                subpath,
                normalizedPath,
                HttpStatus.BAD_GATEWAY.value(),
                latencyMs,
                clientIp
            );

            String errMsg = lastException != null ? lastException.getMessage() : "Target application unreachable or timed out";
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("BAD_GATEWAY", "Target application unreachable or timed out: " + errMsg));
        }
    }

    private int resolveMaxRetries(String method, ApiPolicy appPolicy) {
        if (appPolicy == null || appPolicy.getRetryCount() <= 0) {
            return 0;
        }
        boolean isIdempotent = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
        if (isIdempotent) {
            return Math.min(appPolicy.getRetryCount(), 3);
        }
        // Non-idempotent only retries if explicitly enabled in policy
        if (appPolicy.isRetryNonIdempotent()) {
            return Math.min(appPolicy.getRetryCount(), 3);
        }
        return 0;
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String incomingId = request.getHeader("X-Request-Id");
        if (incomingId != null && !incomingId.isBlank() && incomingId.trim().length() <= 128) {
            return incomingId.trim();
        }
        return UUID.randomUUID().toString();
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
        return Arrays.stream(allowed)
            .map(String::trim)
            .anyMatch(m -> m.equalsIgnoreCase(method));
    }

    private Long resolveMaxRequestBodySize(ApiPolicy endpointPolicy, ApiPolicy appPolicy) {
        if (endpointPolicy != null && endpointPolicy.isEnabled() && endpointPolicy.getMaxRequestBodyBytes() != null) {
            return endpointPolicy.getMaxRequestBodyBytes();
        }
        if (appPolicy != null && appPolicy.isEnabled() && appPolicy.getMaxRequestBodyBytes() != null) {
            return appPolicy.getMaxRequestBodyBytes();
        }
        return null;
    }

    private Duration resolveTimeout(ApiPolicy endpointPolicy, ApiPolicy appPolicy) {
        if (endpointPolicy != null && endpointPolicy.isEnabled() && endpointPolicy.getTimeoutMs() > 0) {
            return Duration.ofMillis(endpointPolicy.getTimeoutMs());
        }
        if (appPolicy != null && appPolicy.isEnabled() && appPolicy.getTimeoutMs() > 0) {
            return Duration.ofMillis(appPolicy.getTimeoutMs());
        }
        return DEFAULT_FORWARD_TIMEOUT;
    }

    private String extractSubpath(String requestUri) {
        String gatewayPrefix = "/api/v1/gateway";
        int prefixIndex = requestUri.indexOf(gatewayPrefix);
        if (prefixIndex != -1) {
            String sub = requestUri.substring(prefixIndex + gatewayPrefix.length());
            return sub.isEmpty() ? "/" : sub;
        }
        return requestUri;
    }

    private String buildTargetUrl(String baseUrl, String subpath, String queryString) {
        String base = baseUrl.trim().replaceAll("/+$", "");
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://" + base;
        }
        String path = subpath.startsWith("/") ? subpath : "/" + subpath;
        String fullUrl = base + path;
        if (queryString != null && !queryString.isBlank()) {
            fullUrl += "?" + queryString;
        }
        return fullUrl;
    }

    private int calculateTotalHeaderSize(HttpServletRequest request) {
        int total = 0;
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                total += name.length();
                Enumeration<String> values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    total += values.nextElement().length();
                }
            }
        }
        return total;
    }

    private boolean isHopByHopHeader(String headerName) {
        if (headerName == null) return true;
        String lower = headerName.toLowerCase(Locale.ROOT);
        return HOP_BY_HOP_HEADERS.contains(lower) || lower.startsWith("x-sentinel-");
    }

    private void copySafeRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder, String requestId, String clientIp, java.util.Map<String, String> upstreamHeaders) {
        Set<String> upstreamHeaderKeysLower = new java.util.HashSet<>();
        if (upstreamHeaders != null) {
            for (String k : upstreamHeaders.keySet()) {
                upstreamHeaderKeysLower.add(k.toLowerCase(Locale.ROOT));
            }
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String lowerHeader = headerName.toLowerCase(Locale.ROOT);

                // Drop hop-by-hop, internal Sentinel headers, and any consumer header that would collide with upstream auth
                if (!HOP_BY_HOP_HEADERS.contains(lowerHeader)
                    && !lowerHeader.startsWith("x-sentinel-")
                    && !lowerHeader.startsWith("x-internal-")
                    && !upstreamHeaderKeysLower.contains(lowerHeader)) {

                    Enumeration<String> values = request.getHeaders(headerName);
                    while (values.hasMoreElements()) {
                        builder.header(headerName, values.nextElement());
                    }
                }
            }
        }

        // Apply upstream authentication headers
        if (upstreamHeaders != null) {
            for (Map.Entry<String, String> entry : upstreamHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        builder.header("X-Request-Id", requestId);
        builder.header("X-Forwarded-For", clientIp);
        builder.header("X-Forwarded-Proto", request.getScheme());
        builder.header("X-Forwarded-Host", request.getServerName());
    }

    private byte[] readRequestBody(HttpServletRequest request) {
        try (InputStream inputStream = request.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private boolean supportsBody(String method) {
        return "POST".equalsIgnoreCase(method) ||
               "PUT".equalsIgnoreCase(method) ||
               "PATCH".equalsIgnoreCase(method) ||
               "DELETE".equalsIgnoreCase(method);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
