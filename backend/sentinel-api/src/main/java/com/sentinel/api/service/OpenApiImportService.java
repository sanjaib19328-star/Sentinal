package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sentinel.api.dto.ApiEndpointDto;
import com.sentinel.api.dto.OpenApiImportRequest;
import com.sentinel.api.dto.OpenApiImportResponse;
import com.sentinel.api.exception.BadRequestException;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.model.DocumentationStatus;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.security.SsrfProtectionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OpenApiImportService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiImportService.class);
    private static final int MAX_SPEC_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB limit
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);
    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    private final ApiEndpointRepository apiEndpointRepository;
    private final ApplicationRepository applicationRepository;
    private final SsrfProtectionValidator ssrfProtectionValidator;
    private final AuditLogService auditLogService;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(FETCH_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    public OpenApiImportService(
        ApiEndpointRepository apiEndpointRepository,
        ApplicationRepository applicationRepository,
        SsrfProtectionValidator ssrfProtectionValidator,
        AuditLogService auditLogService
    ) {
        this.apiEndpointRepository = apiEndpointRepository;
        this.applicationRepository = applicationRepository;
        this.ssrfProtectionValidator = ssrfProtectionValidator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OpenApiImportResponse importSpecification(Long ownerId, Long applicationId, OpenApiImportRequest request) {
        Application application = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        String rawContent = fetchRawContent(request);
        if (rawContent == null || rawContent.isBlank()) {
            throw new BadRequestException("Specification content is empty");
        }

        JsonNode rootNode = parseSpecification(rawContent);
        JsonNode pathsNode = rootNode.get("paths");
        if (pathsNode == null || !pathsNode.isObject() || pathsNode.isEmpty()) {
            throw new BadRequestException("Invalid OpenAPI specification: 'paths' object is missing or empty");
        }

        int importedCount = 0;
        int updatedCount = 0;
        int transitionsCount = 0;
        int parametersCount = 0;
        int requestBodiesCount = 0;
        List<ApiEndpointDto> resultList = new ArrayList<>();

        // Count schemas/components
        int schemasCount = 0;
        if (rootNode.has("components") && rootNode.get("components").has("schemas") && rootNode.get("components").get("schemas").isObject()) {
            schemasCount = rootNode.get("components").get("schemas").size();
        } else if (rootNode.has("definitions") && rootNode.get("definitions").isObject()) {
            schemasCount = rootNode.get("definitions").size();
        }

        Iterator<Map.Entry<String, JsonNode>> pathIterator = pathsNode.fields();
        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
            String rawPath = pathEntry.getKey();
            JsonNode pathMethods = pathEntry.getValue();

            if (pathMethods == null || !pathMethods.isObject()) continue;

            String normalizedPath = PathNormalizer.normalize(rawPath);

            Iterator<Map.Entry<String, JsonNode>> methodIterator = pathMethods.fields();
            while (methodIterator.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodIterator.next();
                String method = methodEntry.getKey().toUpperCase(Locale.ROOT);

                if (!SUPPORTED_METHODS.contains(method)) continue;

                JsonNode operationNode = methodEntry.getValue();
                if (operationNode == null || !operationNode.isObject()) continue;

                String summary = operationNode.has("summary") ? operationNode.get("summary").asText() : null;
                String description = operationNode.has("description") ? operationNode.get("description").asText() : null;
                boolean deprecated = operationNode.has("deprecated") && operationNode.get("deprecated").asBoolean();

                String parametersJson = operationNode.has("parameters") ? operationNode.get("parameters").toString() : null;
                if (operationNode.has("parameters") && operationNode.get("parameters").isArray()) {
                    parametersCount += operationNode.get("parameters").size();
                }

                String requestBodyJson = operationNode.has("requestBody") ? operationNode.get("requestBody").toString() : null;
                if (operationNode.has("requestBody")) {
                    requestBodiesCount++;
                }

                String responsesJson = operationNode.has("responses") ? operationNode.get("responses").toString() : null;

                // Upsert endpoint preserving discovered state
                var existingOpt = apiEndpointRepository.findByApplicationIdAndMethodAndNormalizedPath(
                    applicationId,
                    method,
                    normalizedPath
                );

                ApiEndpoint endpoint;
                if (existingOpt.isPresent()) {
                    endpoint = existingOpt.get();
                    if (endpoint.getDocumentationStatus() == DocumentationStatus.DISCOVERED) {
                        endpoint.setDocumentationStatus(DocumentationStatus.DOCUMENTED_AND_DISCOVERED);
                        transitionsCount++;
                    }
                    updatedCount++;
                } else {
                    endpoint = new ApiEndpoint();
                    endpoint.setApplicationId(applicationId);
                    endpoint.setMethod(method);
                    endpoint.setNormalizedPath(normalizedPath);
                    endpoint.setDocumentationStatus(DocumentationStatus.DOCUMENTED);
                    endpoint.setFirstSeenAt(Instant.now());
                    endpoint.setLastSeenAt(Instant.now());
                    importedCount++;
                }

                endpoint.setSummary(summary);
                endpoint.setDescription(description);
                endpoint.setParametersJson(parametersJson);
                endpoint.setRequestBodySchemaJson(requestBodyJson);
                endpoint.setResponsesJson(responsesJson);
                endpoint.setDeprecated(deprecated);

                ApiEndpoint saved = apiEndpointRepository.save(endpoint);
                resultList.add(mapToDto(saved));
            }
        }

        int totalDocumented = apiEndpointRepository.findByApplicationId(applicationId).size();

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.APPLICATION_UPDATED,
            "OPENAPI_SPEC",
            String.valueOf(applicationId),
            "Imported " + importedCount + " new endpoints, updated " + updatedCount + " existing endpoints (" + schemasCount + " schemas, " + parametersCount + " params) from OpenAPI specification",
            null
        );

        return new OpenApiImportResponse(
            importedCount,
            updatedCount,
            totalDocumented,
            schemasCount,
            parametersCount,
            requestBodiesCount,
            transitionsCount,
            resultList
        );
    }

    private String fetchRawContent(OpenApiImportRequest request) {
        if (request.getSpecContent() != null && !request.getSpecContent().isBlank()) {
            if (request.getSpecContent().length() > MAX_SPEC_SIZE_BYTES) {
                throw new BadRequestException("Specification exceeds maximum allowed size of 5 MB");
            }
            return request.getSpecContent().trim();
        }

        if (request.getSpecUrl() != null && !request.getSpecUrl().isBlank()) {
            String url = request.getSpecUrl().trim();
            // Validate SSRF Protection (Strictly block loopback/internal hosts in all environments)
            ssrfProtectionValidator.validateUrl(url, false);

            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(FETCH_TIMEOUT)
                    .header("Accept", "application/json, application/yaml, text/yaml, */*")
                    .GET()
                    .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    throw new BadRequestException("Failed to fetch OpenAPI spec from URL (HTTP " + resp.statusCode() + ")");
                }

                String body = resp.body();
                if (body != null && body.length() > MAX_SPEC_SIZE_BYTES) {
                    throw new BadRequestException("Fetched specification exceeds maximum allowed size of 5 MB");
                }
                return body;
            } catch (SecurityException se) {
                throw se;
            } catch (Exception e) {
                log.warn("Failed to fetch OpenAPI from URL {}: {}", url, e.getMessage());
                throw new BadRequestException("Error fetching specification from URL: " + e.getMessage());
            }
        }

        throw new BadRequestException("Either specContent or specUrl must be provided");
    }

    private JsonNode parseSpecification(String content) {
        // Try JSON first
        try {
            return jsonMapper.readTree(content);
        } catch (Exception jsonEx) {
            // Fallback to YAML
            try {
                return yamlMapper.readTree(content);
            } catch (Exception yamlEx) {
                throw new BadRequestException("Unable to parse specification. Ensure it is valid JSON or YAML.");
            }
        }
    }

    @Transactional
    public OpenApiImportResponse autoDiscoverAndImport(Long ownerId, Long applicationId, String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String cleanBase = baseUrl.trim().replaceAll("/+$", "");
        if (!cleanBase.startsWith("http://") && !cleanBase.startsWith("https://")) {
            cleanBase = "http://" + cleanBase;
        }

        List<String> specPaths = List.of(
            "/openapi.json",
            "/api/v1/openapi.json",
            "/v3/api-docs",
            "/v2/api-docs",
            "/swagger.json",
            "/docs/openapi.json",
            "/api-docs"
        );

        for (String specPath : specPaths) {
            String specUrl = cleanBase + specPath;
            try {
                ssrfProtectionValidator.validateUrl(specUrl, true);
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(specUrl))
                    .timeout(FETCH_TIMEOUT)
                    .header("Accept", "application/json, application/yaml, text/yaml, */*")
                    .header("User-Agent", "Sentinel-AutoDiscovery/1.0")
                    .GET()
                    .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    String body = resp.body();
                    if (body != null && !body.isBlank() && (body.contains("\"openapi\"") || body.contains("\"swagger\"") || body.contains("openapi:") || body.contains("swagger:"))) {
                        log.info("Auto-discovered OpenAPI specification at {} for app {}", specUrl, applicationId);
                        OpenApiImportRequest importReq = new OpenApiImportRequest();
                        importReq.setSpecContent(body);
                        return importSpecification(ownerId, applicationId, importReq);
                    }
                }
            } catch (Exception e) {
                log.debug("Spec probe at {} skipped: {}", specUrl, e.getMessage());
            }
        }

        return null;
    }

    public ApiEndpointDto mapToDto(ApiEndpoint e) {
        return new ApiEndpointDto(
            e.getId(),
            e.getApplicationId(),
            e.getMethod(),
            e.getNormalizedPath(),
            e.getDocumentationStatus(),
            e.getSummary(),
            e.getDescription(),
            e.getParametersJson(),
            e.getRequestBodySchemaJson(),
            e.getResponsesJson(),
            e.isDeprecated(),
            e.getFirstSeenAt(),
            e.getLastSeenAt()
        );
    }
}
